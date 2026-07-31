package com.stillfresh.app.paymentservice.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filter that trusts the API Gateway for authentication.
 * Extracts user context from gateway headers instead of validating JWT tokens.
 * Falls back to JWT validation for service-to-service calls (when gateway headers are missing).
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
    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${gateway.internal.secret}")
    private String gatewaySecret;

    @Autowired(required = false)
    private JwtUtil jwtUtil;

    private static final String ALLSECURE_PREFIX = "/payment/allsecure";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = requestPath(request);

        // AllSecure routes must transit the API gateway (X-Gateway-Secret). Direct hits to payment-service are rejected.
        if (path.startsWith(ALLSECURE_PREFIX)) {
            if (!gatewaySecret.equals(request.getHeader(X_GATEWAY_SECRET))) {
                logger.warn("Rejected direct AllSecure request without gateway secret: {} from {}",
                        path, request.getRemoteAddr());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("Forbidden");
                return;
            }
            if (isAllSecureGatewayPublicPath(path)) {
                chain.doFilter(request, response);
                return;
            }
            if (!"true".equals(request.getHeader(X_AUTHENTICATED))) {
                logger.warn("AllSecure app endpoint must be accessed via API gateway: {}", path);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Unauthorized");
                return;
            }
        }

        String authenticated = request.getHeader(X_AUTHENTICATED);

        // First priority: Check if gateway has authenticated the request
        if ("true".equals(authenticated)) {
            // Verify the request actually came from the gateway by checking the shared secret
            String secret = request.getHeader(X_GATEWAY_SECRET);
            if (!gatewaySecret.equals(secret)) {
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

                logger.debug("Gateway authenticated request - UserId: {}, Username: {}, Email: {}, Role: {}", 
                           userId, username, email, rolesHeader);

                // Validate required headers
                if (username == null || username.isEmpty()) {
                    logger.warn("Missing X-Username header from gateway");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Missing user information from gateway");
                    return;
                }

                // Build authorities from role header
                Collection<SimpleGrantedAuthority> authorities;
                if (rolesHeader != null && !rolesHeader.isEmpty()) {
                    // Parse roles from header (comma-separated, e.g., "USER,VENDOR")
                    authorities = Arrays.stream(rolesHeader.split(","))
                            .map(String::trim)
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                            .collect(Collectors.toList());
                } else {
                    // Default to USER role if not provided
                    logger.warn("No X-User-Role header provided, defaulting to ROLE_USER");
                    authorities = Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"));
                }

                // Set authentication in SecurityContext
                // Payment service uses username as principal (not UserDetails object)
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

                logger.debug("Successfully authenticated user from gateway: {}", username);

            } catch (Exception e) {
                logger.error("Error processing gateway authentication headers", e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("Error processing authentication");
                return;
            }
        } else {
            // AllSecure app endpoints never accept a bare JWT — gateway auth is mandatory (checked above).
            if (path.startsWith(ALLSECURE_PREFIX) && !isAllSecureGatewayPublicPath(path)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Unauthorized");
                return;
            }

            // Fallback: Check for Authorization header (service-to-service calls)
            // This allows services to call each other directly without going through the gateway
            String authHeader = request.getHeader(AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                String jwt = authHeader.substring(BEARER_PREFIX.length());
                
                if (jwtUtil != null) {
                    try {
                        // Validate JWT token directly (for service-to-service calls)
                        if (jwtUtil.isTokenExpired(jwt)) {
                            logger.warn("JWT token is expired");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("Token expired");
                            return;
                        }

                        // Extract user info from JWT
                        String username = jwtUtil.extractUsername(jwt);
                        Long userId = jwtUtil.extractUserId(jwt);
                        String email = jwtUtil.extractEmail(jwt);
                        List<SimpleGrantedAuthority> authorities = jwtUtil.extractRoles(jwt);

                        logger.debug("Authenticated service-to-service request - UserId: {}, Username: {}, Email: {}", 
                                   userId, username, email);

                        // Set authentication in SecurityContext
                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(username, jwt, authorities);
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        
                        // Store userId in request attribute for easy access
                        if (userId != null) {
                            request.setAttribute("userId", userId);
                        }
                        request.setAttribute("username", username);
                        if (email != null) {
                            request.setAttribute("email", email);
                        }

                        logger.debug("Successfully authenticated service-to-service request: {}", username);

                    } catch (Exception e) {
                        logger.error("Error validating JWT token for service-to-service call", e);
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("Invalid token");
                        return;
                    }
                } else {
                    logger.warn("JwtUtil not available, cannot validate JWT for service-to-service call");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("JWT validation not available");
                    return;
                }
            } else {
                // Not authenticated by gateway and no Authorization header - might be a public endpoint
                // Let it pass through (public endpoints are handled by WebSecurityConfig)
                logger.debug("Request not authenticated by gateway and no Authorization header - may be public endpoint");
            }
        }

        chain.doFilter(request, response);
    }

    private static String requestPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        int query = uri.indexOf('?');
        return query >= 0 ? uri.substring(0, query) : uri;
    }

    /** Callback and return are public at the gateway but still require the gateway trust stamp. */
    private static boolean isAllSecureGatewayPublicPath(String path) {
        return "/payment/allsecure/callback".equals(path) || "/payment/allsecure/return".equals(path);
    }
}

