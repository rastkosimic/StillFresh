package com.stillfresh.app.paymentservice.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.stillfresh.app.paymentservice.listener.TokenValidationResponseListener;
import com.stillfresh.app.paymentservice.publisher.PaymentEventPublisher;
import com.stillfresh.app.sharedentities.shared.events.TokenRequestEvent;
import com.stillfresh.app.sharedentities.shared.events.TokenValidationResponseEvent;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);

    @Autowired
    private JwtUtil jwtUtil;  

    @Autowired
    private PaymentEventPublisher eventPublisher;

    @Autowired
    private TokenValidationResponseListener tokenValidationResponseListener;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String jwt = authorizationHeader.substring(7);
            String correlationId = UUID.randomUUID().toString();

            logger.info("JWT received for validation. Correlation ID: {}", correlationId);

            // Publish token validation request
            eventPublisher.publishTokenValidationRequest(new TokenRequestEvent(jwt, correlationId));

            // Create a latch to wait for the response
            CountDownLatch latch = new CountDownLatch(1);
            tokenValidationResponseListener.registerLatch(correlationId, latch);

            try {
                // Wait for the response (timeout after 5 seconds)
                if (!latch.await(5, TimeUnit.SECONDS)) {
                    logger.warn("Token validation timed out for Correlation ID: {}", correlationId);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Token validation timed out");
                    return;
                }

                // Fetch the response
                TokenValidationResponseEvent validationResponse = tokenValidationResponseListener.getResponse(correlationId);

                if (!validationResponse.isValid()) {
                    logger.warn("Invalid token: {}", validationResponse.getMessage());
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Invalid token: " + validationResponse.getMessage());
                    return;
                }

                // Extract user details from JWT
                String username = jwtUtil.extractUsername(jwt);
                Long userId = jwtUtil.extractUserId(jwt); // Ensure this method is implemented in JwtUtil
                List<SimpleGrantedAuthority> authorities = jwtUtil.extractRoles(jwt)
                        .stream()
                        .map(role -> new SimpleGrantedAuthority(role.getAuthority()))
                        .collect(Collectors.toList());

                logger.info("Authenticated user '{}' with userId '{}'", username, userId);

                // Set authentication in SecurityContextHolder
                UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(username, jwt, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (InterruptedException e) {
                logger.error("Error while validating token", e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("Error while validating token");
                return;
            }
        } else {
            logger.warn("No JWT found in Authorization header.");
        }

        chain.doFilter(request, response);
    }
}
