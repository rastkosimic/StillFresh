package com.stillfresh.app.vendorservice.security;

import com.stillfresh.app.vendorservice.service.CustomVendorDetailsService;
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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
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
    private CustomVendorDetailsService vendorDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {

        String authenticated = request.getHeader(X_AUTHENTICATED);
        String path = request.getRequestURI();

        // Only process if gateway has authenticated the request
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

                // Check if this is a rating endpoint - these can be accessed by regular users (not just vendors)
                boolean isRatingEndpoint = path != null && path.startsWith("/vendors/ratings");
                
                // Check if user is ADMIN or SUPER_ADMIN (these users exist in authorization-service, not vendor-service)
                boolean isAdminOrSuperAdmin = rolesHeader != null && 
                    (rolesHeader.contains("ADMIN") || rolesHeader.contains("SUPER_ADMIN"));

                if (isRatingEndpoint || isAdminOrSuperAdmin) {
                    // For rating endpoints or admin users, allow authentication without vendor entity lookup
                    // Admins exist in authorization-service, not vendor-service
                    logger.debug("Rating endpoint or admin user detected - allowing authentication without vendor entity lookup");

                    // Build authorities from role header
                    Collection<SimpleGrantedAuthority> authorities;
                    if (rolesHeader != null && !rolesHeader.isEmpty()) {
                        // Parse roles from header (comma-separated, e.g., "USER,VENDOR,ADMIN")
                        authorities = Arrays.stream(rolesHeader.split(","))
                                .map(String::trim)
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                                .collect(Collectors.toList());
                    } else {
                        // Default to USER role if not specified
                        authorities = Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"));
                    }

                    // Create a simple authentication token based on gateway headers
                    // We use username as principal since we don't have UserDetails for non-vendors
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(username, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    // Store userId in request attribute for easy access (critical for RatingController and admin operations)
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

                    logger.debug("Successfully authenticated user (rating endpoint or admin): {}", username);
                } else {
                    // For non-rating endpoints with vendor users, require vendor entity (existing behavior)
                    // Load vendor details from database (needed for business logic)
                    // We trust the gateway for authentication, but still need vendor entity for service methods
                    UserDetails userDetails;
                    try {
                        // Try username first, then email
                        if (email != null && !email.isEmpty()) {
                            userDetails = vendorDetailsService.loadUserByEmail(email);
                        } else {
                            userDetails = vendorDetailsService.loadUserByUsername(username);
                        }
                    } catch (Exception e) {
                        logger.error("Failed to load vendor details for username: {} or email: {}", username, email, e);
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("User not found");
                        return;
                    }

                    // A token stays valid until it expires, so a deactivated or suspended account
                    // must be rejected here rather than trusted for the rest of the token lifetime.
                    if (!userDetails.isEnabled()) {
                        logger.warn("Rejecting request for disabled vendor account: {}", username);
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.getWriter().write("Account is not active");
                        return;
                    }

                    // Build authorities from role header (if provided) or from loaded user details
                    Collection<SimpleGrantedAuthority> authorities;
                    if (rolesHeader != null && !rolesHeader.isEmpty()) {
                        // Parse roles from header (comma-separated, e.g., "VENDOR,ADMIN")
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

                    logger.debug("Successfully authenticated vendor from gateway: {}", username);
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

