package com.stillfresh.app.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtValidationFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(JwtValidationFilter.class);
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    // Headers to add to downstream requests
    private static final String X_USER_ID = "X-User-Id";
    private static final String X_USERNAME = "X-Username";
    private static final String X_USER_EMAIL = "X-User-Email";
    private static final String X_USER_ROLE = "X-User-Role";
    private static final String X_AUTHENTICATED = "X-Authenticated";
    private static final String X_GATEWAY_SECRET = "X-Gateway-Secret";

    @Value("${gateway.jwt.local-validation-enabled:true}")
    private boolean localValidationEnabled;

    @Value("${gateway.internal.secret}")
    private String internalSecret;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Always strip trust headers from client requests to prevent forgery.
        // These headers are only set by the gateway itself, never trusted from outside.
        ServerHttpRequest stripped = request.mutate()
                .headers(headers -> {
                    headers.remove(X_AUTHENTICATED);
                    headers.remove(X_USER_ID);
                    headers.remove(X_USERNAME);
                    headers.remove(X_USER_EMAIL);
                    headers.remove(X_USER_ROLE);
                    headers.remove(X_GATEWAY_SECRET);
                })
                .build();
        ServerWebExchange strippedExchange = exchange.mutate().request(stripped).build();

        // Skip JWT validation for public endpoints (trust headers already stripped above).
        // Still stamp X-Gateway-Secret so downstream services can reject direct access.
        if (isPublicEndpoint(path)) {
            logger.debug("Skipping JWT validation for public endpoint: {}", path);
            ServerHttpRequest publicWithSecret = stripped.mutate()
                    .header(X_GATEWAY_SECRET, internalSecret)
                    .build();
            return chain.filter(strippedExchange.mutate().request(publicWithSecret).build());
        }

        if (!localValidationEnabled) {
            logger.info("Local JWT validation is disabled via feature flag; " +
                    "forwarding request without gateway-side token validation for path: {}", path);
            ServerHttpRequest withSecret = stripped.mutate()
                    .header(X_GATEWAY_SECRET, internalSecret)
                    .build();
            return chain.filter(strippedExchange.mutate().request(withSecret).build());
        }

        long startNanos = System.nanoTime();

        // Extract Authorization header from the already-stripped request
        String authHeader = stripped.getHeaders().getFirst(AUTH_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            long durationMillis = (System.nanoTime() - startNanos) / 1_000_000;
            logger.warn("Missing or invalid Authorization header for path: {} (validation time={} ms)",
                    path, durationMillis);
            return unauthorized(strippedExchange, "Missing or invalid Authorization header");
        }

        String jwt = authHeader.substring(BEARER_PREFIX.length());

        logger.debug("Locally validating JWT token for path: {}", path);

        try {
            // Basic validation: parse token, ensure we can extract required claims
            String jti = jwtUtil.extractJti(jwt);
            boolean blacklisted = false;
            if (jti != null) {
                blacklisted = tokenBlacklistService.isTokenIdBlacklisted(jti);
            }

            if (blacklisted) {
                long durationMillis = (System.nanoTime() - startNanos) / 1_000_000;
                logger.warn("JWT token is blacklisted (jti) for path: {} (validation time={} ms)",
                        path, durationMillis);
                return unauthorized(exchange, "Token is blacklisted");
            }

            // IMPORTANT: Only accept access tokens as Bearer tokens at the gateway.
            // Refresh tokens must never be used in Authorization headers.
            if (!jwtUtil.isAccessToken(jwt)) {
                long durationMillis = (System.nanoTime() - startNanos) / 1_000_000;
                logger.warn("Rejected non-access JWT token for path: {} (validation time={} ms)", path, durationMillis);
                return unauthorized(strippedExchange, "Invalid token type");
            }

            Long userId = jwtUtil.extractUserId(jwt);
            String username = jwtUtil.extractUsername(jwt);
            String role = jwtUtil.extractRole(jwt);
            String email = jwtUtil.extractEmail(jwt);

            if (username == null || role == null) {
                long durationMillis = (System.nanoTime() - startNanos) / 1_000_000;
                logger.warn("Invalid JWT token: missing required claims for path: {} (validation time={} ms)",
                        path, durationMillis);
                return unauthorized(exchange, "Invalid or expired token");
            }

            long durationMillis = (System.nanoTime() - startNanos) / 1_000_000;
            logger.info("JWT validation succeeded for path: {} (validation time={} ms)", path, durationMillis);

            // Add user context headers to downstream request.
            // X_GATEWAY_SECRET proves to downstream services that this request came from the gateway.
            ServerHttpRequest modifiedRequest = stripped.mutate()
                .header(AUTH_HEADER, authHeader)  // Forward original Authorization header
                .header(X_USER_ID, userId != null ? userId.toString() : "")
                .header(X_USERNAME, username)
                .header(X_USER_EMAIL, email != null ? email : "")
                .header(X_USER_ROLE, role)
                .header(X_GATEWAY_SECRET, internalSecret)
                .header(X_AUTHENTICATED, "true")
                .build();

            return chain.filter(strippedExchange.mutate().request(modifiedRequest).build());

        } catch (Exception e) {
            long durationMillis = (System.nanoTime() - startNanos) / 1_000_000;
            logger.error("Error during local JWT validation for path: {} (validation time={} ms)",
                    path, durationMillis, e);
            return unauthorized(strippedExchange, "Token validation error");
        }
    }

    private boolean isPublicEndpoint(String path) {
        // Public endpoints that don't require authentication
        return path.startsWith("/auth/login") ||
               path.startsWith("/auth/register") ||
               path.startsWith("/auth/verify") ||
               path.startsWith("/auth/forgot-password") ||
               path.startsWith("/auth/reset-password") ||
               path.startsWith("/auth/refresh-token") ||
               path.startsWith("/auth/google-login") ||
               path.startsWith("/auth/check-availability") ||
               path.startsWith("/auth/oauth2") ||  // OAuth2 endpoints (Google sign-in)
               path.startsWith("/oauth2") ||  // Spring OAuth2 endpoints
               path.startsWith("/vendors/register") ||
               path.startsWith("/vendors/apply") ||  // Vendor application endpoint (public)
               path.startsWith("/vendors/verify") ||
               path.startsWith("/vendors/forgot-password") ||
               path.startsWith("/vendors/reset-password") ||
               path.startsWith("/vendors/uploads/") ||  // Publicly served vendor/offer images (GET only, static)
               path.startsWith("/users/register") ||
               path.startsWith("/users/verify") ||
               path.startsWith("/users/forgot-password") ||
               path.startsWith("/users/reset-password") ||
               path.startsWith("/api/notifications/fcm-token/register") ||  // FCM token registration
               path.startsWith("/payment/allsecure/callback") ||  // AllSecure server-to-server postback (HMAC inside controller)
               path.startsWith("/payment/allsecure/return") ||    // AllSecure hosted-flow browser landing
               path.startsWith("/admin/create-initial-admin") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/swagger-ui.html") ||
               path.startsWith("/swagger-resources") ||
               path.startsWith("/webjars") ||
               path.equals("/") ||
               path.equals("/swagger-ui/index.html");
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        String body = "{\"error\":\"" + message + "\"}";
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        // High priority - run before other filters
        return -100;
    }
}

