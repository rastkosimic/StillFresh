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

import com.stillfresh.app.sharedentities.security.InternalServiceHeaders;
import com.stillfresh.app.sharedentities.security.SharedSecret;

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
            if (!SharedSecret.matches(gatewaySecret, request.getHeader(X_GATEWAY_SECRET))) {
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

            // Vendor-service Feign calls present both a forwarded end-user JWT and the
            // internal secret. A JWT with a mismatched roles claim would 401 here and
            // never reach InternalServiceFilter, so skip JWT fallback when the secret is present.
            String internalSecretHeader = request.getHeader(InternalServiceHeaders.INTERNAL_SECRET);
            if (internalSecretHeader == null || internalSecretHeader.isBlank()) {
                if (!authenticateFromJwt(request, response)) {
                    return;
                }
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * @return {@code false} when the filter has already written an error response
     */
    private boolean authenticateFromJwt(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String authHeader = request.getHeader(AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            logger.debug("Request not authenticated by gateway and no Authorization header - may be public endpoint");
            return true;
        }

        String jwt = authHeader.substring(BEARER_PREFIX.length());
        if (jwtUtil == null) {
            logger.warn("JwtUtil not available, cannot validate JWT for service-to-service call");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("JWT validation not available");
            return false;
        }

        try {
            if (jwtUtil.isTokenExpired(jwt)) {
                logger.warn("JWT token is expired");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token expired");
                return false;
            }

            String username = jwtUtil.extractUsername(jwt);
            Long userId = jwtUtil.extractUserId(jwt);
            String email = jwtUtil.extractEmail(jwt);
            List<SimpleGrantedAuthority> authorities = jwtUtil.extractRoles(jwt);

            logger.debug("Authenticated service-to-service request - userId: {}", userId);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, jwt, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            if (userId != null) {
                request.setAttribute("userId", userId);
            }
            request.setAttribute("username", username);
            if (email != null) {
                request.setAttribute("email", email);
            }

            logger.debug("Successfully authenticated service-to-service request: {}", username);
            return true;
        } catch (Exception e) {
            logger.error("Error validating JWT token for service-to-service call", e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid token");
            return false;
        }
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
