package com.stillfresh.app.vendorservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long tokenValidity = 1000 * 60 * 15;  // 15 minutes
    private final long refreshTokenValidity = 1000 * 60 * 60 * 24 * 7;  // 7 days

    // Inject the secret key from application.yml and convert it to SecretKey
    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Extract username (subject) from token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extract email from token
    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    // Extract vendor ID from token
    public Long extractVendorId(String token) {
        Object vendorIdObj = extractClaim(token, claims -> claims.get("vendorId"));
        if (vendorIdObj == null) {
            return null;
        }
        if (vendorIdObj instanceof Number) {
            return ((Number) vendorIdObj).longValue();
        }
        if (vendorIdObj instanceof String) {
            return Long.parseLong((String) vendorIdObj);
        }
        return null;
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
    private Claims extractAllClaims(String token) {
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

    // Generate a token for a vendor
    public String generateToken(UserDetails vendorDetails) {
        CustomVendorDetails customVendorDetails = (CustomVendorDetails) vendorDetails;
        Map<String, Object> claims = new HashMap<>();
        return createToken(
            claims, 
            customVendorDetails.getUsername(), 
            customVendorDetails.getVendor().getEmail(), 
            customVendorDetails.getVendor().getRole().name(), 
            customVendorDetails.getVendor().getId(),
            tokenValidity
        );
    }

    // Generate a refresh token for a vendor
    public String generateRefreshToken(UserDetails vendorDetails) {
        CustomVendorDetails customVendorDetails = (CustomVendorDetails) vendorDetails;
        Map<String, Object> claims = new HashMap<>();
        return createToken(
            claims, 
            customVendorDetails.getUsername(), 
            customVendorDetails.getVendor().getEmail(), 
            customVendorDetails.getVendor().getRole().name(), 
            customVendorDetails.getVendor().getId(),
            refreshTokenValidity
        );
    }

    // Create a token with claims
    private String createToken(Map<String, Object> claims, String subject, String email, String role, Long vendorId, long validity) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .claim("email", email)  // Add email to claims
                .claim("role", role)
                .claim("vendorId", vendorId)  // Add vendor ID to claims
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + validity))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // Validate token against vendor details
    public Boolean validateToken(String token, UserDetails vendorDetails) {
        final String username = extractUsername(token);
        return (username.equals(vendorDetails.getUsername()) && !isTokenExpired(token));
    }

    // New method: Get expiration time in milliseconds
    public long getExpirationTimeInMillis(String token) {
        return extractExpiration(token).getTime();
    }
}
