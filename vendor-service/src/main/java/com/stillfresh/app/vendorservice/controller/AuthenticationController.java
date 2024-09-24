package com.stillfresh.app.vendorservice.controller;

import com.stillfresh.app.vendorservice.model.AuthenticationRequest;
import com.stillfresh.app.vendorservice.security.CustomVendorDetails;
import com.stillfresh.app.vendorservice.security.JwtUtil;
import com.stillfresh.app.vendorservice.service.CustomVendorDetailsService;
import com.stillfresh.app.vendorservice.service.TokenBlacklistService;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {
	
    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomVendorDetailsService vendorDetailsService;

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody AuthenticationRequest authenticationRequest) throws Exception {
        String identifier = authenticationRequest.getIdentifier();  // Either email or name
        String password = authenticationRequest.getPassword();

        try {
            // Authenticate using the correct identifier (email or name) and password
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identifier, password)
            );
        } catch (Exception e) {
            throw new Exception("Incorrect identifier or password", e);
        }
        
        // Determine whether the identifier is an email or name
        UserDetails vendorDetails = vendorDetailsService.loadUserByUsername(identifier);
        
        CustomVendorDetails customVendorDetails = (CustomVendorDetails) vendorDetails;

        if (!customVendorDetails.getVendor().isActive()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vendor account is not verified.");
        }
        
        // Generate JWT token
        final String jwt = jwtUtil.generateToken(vendorDetails);

        return ResponseEntity.ok(jwt);
    }

    
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            long expiryDurationInMillis = jwtUtil.getExpirationTimeInMillis(jwt) - System.currentTimeMillis();
            tokenBlacklistService.addTokenToBlacklist(jwt, expiryDurationInMillis);
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok("Logged out successfully");
    }
    
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || jwtUtil.isTokenExpired(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired refresh token");
        }

        String username = jwtUtil.extractUsername(refreshToken);
        UserDetails userDetails = vendorDetailsService.loadUserByUsername(username);

        String newToken = jwtUtil.generateRefreshToken(userDetails);
        return ResponseEntity.ok(newToken);
    }
}
