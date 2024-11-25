package com.stillfresh.app.userservice.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.stillfresh.app.sharedentities.shared.events.TokenRequestEvent;
import com.stillfresh.app.sharedentities.shared.events.TokenValidationResponseEvent;
import com.stillfresh.app.userservice.listener.TokenValidationResponseListener;
import com.stillfresh.app.userservice.publisher.UserEventPublisher;
import com.stillfresh.app.userservice.service.TokenBlacklistService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {
//
//    @Autowired
//    private JwtUtil jwtUtil;
//
//    @Autowired
//    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private UserDetailsService userDetailsService;
    

    @Autowired
    private UserEventPublisher eventPublisher;

    @Autowired
    private TokenValidationResponseListener tokenValidationResponseListener;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String jwt = authorizationHeader.substring(7);
            String correlationId = UUID.randomUUID().toString();

            // Publish token validation request
            eventPublisher.publishTokenValidationRequest(new TokenRequestEvent(jwt, correlationId));

            // Create a latch to wait for the response
            CountDownLatch latch = new CountDownLatch(1);
            tokenValidationResponseListener.registerLatch(correlationId, latch);

            try {
                // Wait for the response (timeout after 5 seconds)
                if (!latch.await(5, TimeUnit.SECONDS)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Token validation timed out");
                    return;
                }

                // Fetch the response
                TokenValidationResponseEvent validationResponse = tokenValidationResponseListener.getResponse(correlationId);

                if (!validationResponse.isValid()) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Invalid token: " + validationResponse.getMessage());
                    return;
                }

                // Set authentication in SecurityContextHolder
                UserDetails userDetails = userDetailsService.loadUserByUsername(validationResponse.getUsername());
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (InterruptedException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("Error while validating token");
                return;
            }
        }

        chain.doFilter(request, response);
    }
}

