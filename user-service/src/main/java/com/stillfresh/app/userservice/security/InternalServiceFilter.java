package com.stillfresh.app.userservice.security;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.stillfresh.app.sharedentities.security.InternalServiceHeaders;
import com.stillfresh.app.sharedentities.security.SharedSecret;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authenticates service-to-service calls that create accounts on behalf of a user who has no
 * token yet.
 *
 * <p>{@code POST /users/create-oauth2} is invoked by authorization-service during Google sign-in,
 * before the new user holds any credential. Leaving it open allowed anyone to create arbitrary
 * user records; a caller presenting the shared internal secret is granted
 * {@link InternalServiceHeaders#ROLE_INTERNAL_SERVICE} instead.
 */
@Component
public class InternalServiceFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(InternalServiceFilter.class);

    private static final List<String> INTERNAL_PATHS = List.of("/users/create-oauth2");

    @Value("${internal.service.secret}")
    private String internalSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path != null && INTERNAL_PATHS.stream().anyMatch(path::startsWith)) {
            String provided = request.getHeader(InternalServiceHeaders.INTERNAL_SECRET);
            if (SharedSecret.matches(internalSecret, provided)) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        "internal-service", null,
                        List.of(new SimpleGrantedAuthority(InternalServiceHeaders.ROLE_INTERNAL_SERVICE)));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                logger.warn("Rejected internal call to {} without a valid internal service secret from {}",
                        path, request.getRemoteAddr());
            }
        }

        chain.doFilter(request, response);
    }
}
