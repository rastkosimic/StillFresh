package com.stillfresh.app.orderservice.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
 *
 * <p>The identity headers are only meaningful when accompanied by the shared
 * {@code X-Gateway-Secret}. The gateway strips any client-supplied copies of these headers and
 * stamps its own, so a request carrying the secret provably came through the gateway. Without
 * that check a caller who can reach this service directly could assert any username and role.
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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String authenticated = request.getHeader(X_AUTHENTICATED);

        // Only process if gateway has authenticated the request
        if ("true".equals(authenticated)) {
            // Verify the request actually came from the gateway before trusting any identity header.
            if (!SharedSecret.matches(gatewaySecret, request.getHeader(X_GATEWAY_SECRET))) {
                logger.warn("Invalid or missing X-Gateway-Secret - rejecting request from {}",
                        request.getRemoteAddr());
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

                // Build authorities from role header. The gateway always stamps a role, so a
                // missing one means a malformed request rather than an anonymous user; granting
                // a default role here would silently widen access.
                if (rolesHeader == null || rolesHeader.isEmpty()) {
                    logger.warn("Missing X-User-Role header from gateway for userId: {}", userId);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Missing user information from gateway");
                    return;
                }
                // Parse roles from header (comma-separated, e.g., "USER,VENDOR")
                Collection<SimpleGrantedAuthority> authorities = Arrays.stream(rolesHeader.split(","))
                        .map(String::trim)
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                        .collect(Collectors.toList());

                // Set authentication in SecurityContext
                // Order service uses username as principal
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                // Store userId in request attribute for easy access
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

                logger.debug("Successfully authenticated userId {} from gateway", userId);

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

