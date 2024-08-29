package com.stillfresh.app.userservice.controller;

import com.stillfresh.app.userservice.model.AuthenticationRequest;
import com.stillfresh.app.userservice.service.CustomUserDetailsService;
import com.stillfresh.app.userservice.service.TokenBlacklistService;

import jakarta.servlet.http.HttpServletRequest;

import com.stillfresh.app.userservice.security.CustomUserDetails;
import com.stillfresh.app.userservice.security.JwtUtil;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

	private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);

	@Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    
    // Testing purposes
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    
    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthenticationRequest authenticationRequest) throws Exception {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authenticationRequest.getUsername(), authenticationRequest.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new Exception("Incorrect username or password", e.getCause());
        }
        

        // Load user details
        final UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationRequest.getUsername());

        // Testing purposes
        logger.info("COMPARING The PASSWORDS FROM LOGIN {}", passwordEncoder.matches(authenticationRequest.getPassword(), userDetails.getPassword()));

        // Cast to your custom UserDetails class
        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;

        // Check if the user is active
        if (!customUserDetails.getUser().isActive()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User account is not verified.");
        }

        // Generate JWT token using CustomUserDetails
        final String jwt = jwtUtil.generateToken(userDetails);

        return ResponseEntity.ok(jwt);
    }


    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            // Add the token to a blacklist (store in a database or cache)
            tokenBlacklistService.addTokenToBlacklist(jwt);
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok("User logged out successfully");
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || jwtUtil.isTokenExpired(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired refresh token");
        }

        String username = jwtUtil.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        String newToken = jwtUtil.generateToken(userDetails);
        return ResponseEntity.ok(newToken);
    }
}
