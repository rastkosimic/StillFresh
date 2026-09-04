package com.stillfresh.app.userservice.security;

import com.stillfresh.app.userservice.service.CustomUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.stillfresh.app.sharedentities.security.SharedSecret;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Filter that trusts the API Gateway for authentication.
 * Extracts user context from gateway headers instead of validating JWT tokens.
 * This filter should be used instead of JwtRequestFilter when using centralized authentication at the gateway.
 */
@Component
public class GatewayTrustFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(GatewayTrustFilter.class);
    
    private static final String X_AUTHENTICATED = "X-Authenticated";
    private static final String X_GATEWAY_SECRET = "X-Gateway-Secret";
    private static final String X_USER_ID = "X-User-Id";
    private static final String X_USERNAME = "X-Username";
    private static final String X_USER_EMAIL = "X-User-Email";
    private static final String X_USER_ROLE = "X-User-Role";

    @Value("${gateway.internal.secret}")
    private String gatewaySecret;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String authenticated = request.getHeader(X_AUTHENTICATED);

        // Only process if gateway has authenticated the request
        if ("true".equals(authenticated)) {
            // Verify the request actually came from the gateway by checking the shared secret
            String secret = request.getHeader(X_GATEWAY_SECRET);
            if (!SharedSecret.matches(gatewaySecret, secret)) {
                logger.warn("Invalid or missing X-Gateway-Secret - rejecting request from {}", request.getRemoteAddr());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Unauthorized");
                return;
            }

            try {
                // Extract user info from gateway headers
                String userId = request.getHeader(X_USER_ID);
                String username = request.getHeader(X_USERNAME);
                String email = request.getHeader(X_USER_EMAIL);
                String rolesHeader = request.getHeader(X_USER_ROLE);

                logger.debug("Gateway authenticated request - userId: {}, role: {}", userId, rolesHeader);

                // Validate required headers
                if (username == null || username.isEmpty()) {
                    logger.warn("Missing X-Username header from gateway");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Missing user information from gateway");
                    return;
                }

                // ADMIN and SUPER_ADMIN exist in authorization-service only, not user-service DB
                boolean isAdminOrSuperAdmin = rolesHeader != null &&
                    (rolesHeader.contains("ADMIN") || rolesHeader.contains("SUPER_ADMIN"));

                if (isAdminOrSuperAdmin) {
                    logger.debug("Admin user detected - allowing authentication without user-service entity lookup");

                    Collection<SimpleGrantedAuthority> authorities = Arrays.stream(rolesHeader.split(","))
                            .map(String::trim)
                            .filter(role -> !role.isEmpty())
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                            .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    if (userId != null && !userId.isEmpty()) {
                        try {
                            request.setAttribute("userId", Long.parseLong(userId));
                        } catch (NumberFormatException e) {
                            logger.warn("Invalid userId format: {}", userId);
                        }
                    }
                    request.setAttribute("username", username);
                    if (email != null) {
                        request.setAttribute("email", email);
                    }

                    logger.debug("Successfully authenticated admin from gateway: {}", username);
                } else {
                // Load user details from database (needed for business logic)
                // We trust the gateway for authentication, but still need user entity for service methods
                UserDetails userDetails;
                try {
                    // Try username first, then email
                    if (email != null && !email.isEmpty()) {
                        userDetails = userDetailsService.loadUserByUsername(email);
                    } else {
                        userDetails = userDetailsService.loadUserByUsername(username);
                    }
                } catch (Exception e) {
                    logger.error("Failed to load user details for userId: {}", userId, e);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("User not found");
                    return;
                }

                // Build authorities from role header (if provided) or from loaded user details
                Collection<SimpleGrantedAuthority> authorities;
                if (rolesHeader != null && !rolesHeader.isEmpty()) {
                    // Parse roles from header (comma-separated, e.g., "USER,ADMIN")
                    authorities = Arrays.stream(rolesHeader.split(","))
                            .map(String::trim)
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                            .collect(Collectors.toList());
                } else {
                    // Fallback to authorities from loaded user details
                    authorities = userDetails.getAuthorities().stream()
                            .map(auth -> new SimpleGrantedAuthority(auth.getAuthority()))
                            .collect(Collectors.toList());
                }

                // Set authentication in SecurityContext
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                // Store userId in request attribute for easy access
                if (userId != null && !userId.isEmpty()) {
                    request.setAttribute("userId", Long.parseLong(userId));
                }
                request.setAttribute("username", username);
                if (email != null) {
                    request.setAttribute("email", email);
                }

                logger.debug("Successfully authenticated user from gateway: {}", username);
                }

            } catch (Exception e) {
                logger.error("Error processing gateway authentication headers", e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("Error processing authentication");
                return;
            }
        } else {
            // Not authenticated by gateway - might be a public endpoint
            // Let it pass through (public endpoints are handled by WebSecurityConfig)
            logger.debug("Request not authenticated by gateway - may be public endpoint");
        }

        chain.doFilter(request, response);
    }
}

