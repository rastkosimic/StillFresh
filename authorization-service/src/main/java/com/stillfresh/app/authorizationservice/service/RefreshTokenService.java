package com.stillfresh.app.authorizationservice.service;

import com.stillfresh.app.authorizationservice.security.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

@Service
public class RefreshTokenService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String USER_REFRESH_SET_PREFIX = "refresh_tokens_user:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Store refresh token in Redis with TTL matching JWT expiry.
     * We store only a SHA-256 hash of the token as the key to avoid persisting raw tokens.
     */
    public void storeRefreshToken(String refreshToken) {
        Claims claims = jwtUtil.extractAllClaims(refreshToken);
        jwtUtil.requireRefreshToken(claims);

        Long userId = jwtUtil.extractUserId(claims);
        String username = claims.getSubject();
        String jti = jwtUtil.extractJti(claims);
        Date expiresAt = claims.getExpiration();
        Date issuedAt = claims.getIssuedAt();

        if (userId == null || username == null || expiresAt == null) {
            throw new JwtException("Invalid refresh token claims");
        }

        long ttlMs = expiresAt.getTime() - System.currentTimeMillis();
        if (ttlMs <= 0) {
            throw new JwtException("Refresh token is expired");
        }

        String tokenHash = sha256Hex(refreshToken);
        String tokenKey = REFRESH_TOKEN_PREFIX + tokenHash;
        String userSetKey = USER_REFRESH_SET_PREFIX + userId;

        RefreshTokenRecord record = new RefreshTokenRecord(
            userId,
            username,
            jti,
            issuedAt != null ? issuedAt.toInstant().toEpochMilli() : Instant.now().toEpochMilli(),
            expiresAt.toInstant().toEpochMilli()
        );

        redisTemplate.opsForValue().set(tokenKey, record, ttlMs, TimeUnit.MILLISECONDS);
        redisTemplate.opsForSet().add(userSetKey, tokenHash);
        // Keep the user set around at least as long as this token.
        redisTemplate.expire(userSetKey, ttlMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Validate that the given refresh token is a refresh token AND still present in Redis.
     * Returns the stored record if valid; throws on invalid.
     */
    public RefreshTokenRecord validateStoredRefreshToken(String refreshToken) {
        Claims claims = jwtUtil.extractAllClaims(refreshToken);
        jwtUtil.requireRefreshToken(claims);

        String tokenHash = sha256Hex(refreshToken);
        String tokenKey = REFRESH_TOKEN_PREFIX + tokenHash;
        Object value = redisTemplate.opsForValue().get(tokenKey);
        if (!(value instanceof RefreshTokenRecord record)) {
            throw new JwtException("Invalid or revoked refresh token");
        }
        return record;
    }

    /**
     * Rotation: consume (revoke) the old refresh token (one-time use).
     */
    public void consumeRefreshToken(String refreshToken) {
        Claims claims = jwtUtil.extractAllClaims(refreshToken);
        jwtUtil.requireRefreshToken(claims);

        Long userId = jwtUtil.extractUserId(claims);
        if (userId == null) {
            throw new JwtException("Invalid refresh token claims");
        }

        String tokenHash = sha256Hex(refreshToken);
        String tokenKey = REFRESH_TOKEN_PREFIX + tokenHash;
        String userSetKey = USER_REFRESH_SET_PREFIX + userId;

        redisTemplate.delete(tokenKey);
        redisTemplate.opsForSet().remove(userSetKey, tokenHash);
    }

    /**
     * Revoke a refresh token if present (logout).
     */
    public void revokeRefreshToken(String refreshToken) {
        try {
            consumeRefreshToken(refreshToken);
        } catch (Exception ignored) {
            // Logout should be idempotent; ignore invalid/expired token.
        }
    }

    /**
     * Revoke ALL refresh tokens for a user (e.g. on suspension). Deletes every stored refresh-token
     * record tracked in the user's set and removes the set itself, so no further token rotation is
     * possible. Note: already-issued short-lived access tokens are not individually revoked here;
     * per-jti access-token blacklisting on suspension is a TODO (see {@code TokenBlacklistService}).
     */
    public void revokeAllSessionsForUser(Long userId) {
        if (userId == null) {
            return;
        }
        String userSetKey = USER_REFRESH_SET_PREFIX + userId;
        java.util.Set<Object> hashes = redisTemplate.opsForSet().members(userSetKey);
        if (hashes != null) {
            for (Object hash : hashes) {
                redisTemplate.delete(REFRESH_TOKEN_PREFIX + hash);
            }
        }
        redisTemplate.delete(userSetKey);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static class RefreshTokenRecord implements Serializable {
        private Long userId;
        private String username;
        private String jti;
        private Long issuedAtMs;
        private Long expiresAtMs;

        public RefreshTokenRecord() {}

        public RefreshTokenRecord(Long userId, String username, String jti, Long issuedAtMs, Long expiresAtMs) {
            this.userId = userId;
            this.username = username;
            this.jti = jti;
            this.issuedAtMs = issuedAtMs;
            this.expiresAtMs = expiresAtMs;
        }

        public Long getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getJti() {
            return jti;
        }

        public Long getIssuedAtMs() {
            return issuedAtMs;
        }

        public Long getExpiresAtMs() {
            return expiresAtMs;
        }
    }
}

