package com.stillfresh.app.notificationservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    public String extractUserId(String token) {
        try {
            // Remove "Bearer " prefix if present
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // Extract userId from claims, fallback to subject if userId not found
            Object userId = claims.get("userId");
            if (userId != null) {
                return userId.toString();
            }
            
            // Fallback to subject if userId claim is not present
            return claims.getSubject();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT token: " + e.getMessage());
        }
    }

    public boolean validateToken(String token) {
        try {
            // Remove "Bearer " prefix if present
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}


