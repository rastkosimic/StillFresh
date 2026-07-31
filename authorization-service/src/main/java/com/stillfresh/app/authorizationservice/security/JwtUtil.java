package com.stillfresh.app.authorizationservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Component
public class JwtUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    private final SecretKey secretKey;
    private static final String CLAIM_TOKEN_TYPE = "type";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;

    // Inject the secret key from application.yml and convert it to SecretKey
    public JwtUtil(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.access-expiration-ms:900000}") long accessTokenValidityMs,
        @Value("${jwt.refresh-expiration-ms:2592000000}") long refreshTokenValidityMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityMs = accessTokenValidityMs;
        this.refreshTokenValidityMs = refreshTokenValidityMs;
    }

    // Extract username (subject) from token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extract email from token
    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    // Extract expiration date from token
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Extract any claim from token
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Extract all claims from token
    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Check if token is expired
    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Generate a token for a user
    public String generateToken(UserDetails userDetails) {
        return generateAccessToken(userDetails);
    }

    public String generateAccessToken(UserDetails userDetails) {
        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS);
        return createToken(claims, 
            customUserDetails.getUsername(), 
            customUserDetails.getUser().getId(),
            customUserDetails.getUser().getEmail(), 
            customUserDetails.getUser().getRole().name(), 
            accessTokenValidityMs
        );
    }

    // Generate a refresh token for a user
    public String generateRefreshToken(UserDetails userDetails) {
        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH);
        return createToken(claims, 
            customUserDetails.getUsername(), 
            customUserDetails.getUser().getId(),
            customUserDetails.getUser().getEmail(), 
            customUserDetails.getUser().getRole().name(), 
            refreshTokenValidityMs
        );
    }

    // Create a token with claims
    private String createToken(Map<String, Object> claims, String subject, Long userId, String email, String role, long validity) {
        try {
            logger.debug("Creating JWT token for userId: {}, role: {}, validity: {} ms", userId, role, validity);
            String jti = UUID.randomUUID().toString();

            String token = Jwts.builder()
                    .setClaims(claims)
                    .setSubject(subject)
                    .claim("jti", jti)
                    .claim("userId", userId)
                    .claim("email", email)  // Add email to claims
                    .claim("role", role)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + validity))
                    .signWith(secretKey, SignatureAlgorithm.HS256)
                    .compact();

            logger.debug("JWT token created successfully for userId: {}", userId);
            return token;
        } catch (Exception e) {
            logger.error("Error creating JWT token for userId: {}", userId, e);
            throw new RuntimeException("Failed to generate JWT token");
        }
    }

    // Validate token against user details
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    // Extract JWT ID (jti) from token
    public String extractJti(String token) {
        return extractClaim(token, claims -> claims.get("jti", String.class));
    }

    public String extractJti(Claims claims) {
        return claims.get("jti", String.class);
    }

    public Long extractUserId(Claims claims) {
        Object userIdObj = claims.get("userId");
        if (userIdObj instanceof Integer) return ((Integer) userIdObj).longValue();
        if (userIdObj instanceof Long) return (Long) userIdObj;
        return null;
    }

    public String extractTokenType(Claims claims) {
        return claims.get(CLAIM_TOKEN_TYPE, String.class);
    }

    public void requireRefreshToken(Claims claims) {
        String type = extractTokenType(claims);
        if (!TOKEN_TYPE_REFRESH.equals(type)) {
            throw new io.jsonwebtoken.JwtException("Token is not a refresh token");
        }
    }

    public void requireAccessToken(Claims claims) {
        String type = extractTokenType(claims);
        if (!TOKEN_TYPE_ACCESS.equals(type)) {
            throw new io.jsonwebtoken.JwtException("Token is not an access token");
        }
    }

    // New method: Get expiration time in milliseconds
    public long getExpirationTimeInMillis(String token) {
        return extractExpiration(token).getTime();
    }
}
