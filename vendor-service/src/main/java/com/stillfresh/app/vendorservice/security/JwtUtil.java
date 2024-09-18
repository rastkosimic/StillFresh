package com.stillfresh.app.vendorservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.stillfresh.app.vendorservice.config.JwtConfig;
import com.stillfresh.app.vendorservice.security.CustomVendorDetails;

import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long tokenValidity = 1000 * 60 * 15; // 15 minutes
    private final long refreshTokenValidity = 1000 * 60 * 60 * 24 * 7; // 7 days

    public JwtUtil(JwtConfig jwtConfig) {
        this.secretKey = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes());
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);  // Extract role from token
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(UserDetails userDetails) {
        CustomVendorDetails customVendorDetails = (CustomVendorDetails) userDetails;
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, customVendorDetails.getUsername(), customVendorDetails.getVendor().getRole().name(), tokenValidity);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        CustomVendorDetails customVendorDetails = (CustomVendorDetails) userDetails;
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, customVendorDetails.getUsername(), customVendorDetails.getVendor().getRole().name(), refreshTokenValidity);
    }

    private String createToken(Map<String, Object> claims, String subject, String role, long validity) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .claim("role", role)  // Include role in token
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + validity))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}
