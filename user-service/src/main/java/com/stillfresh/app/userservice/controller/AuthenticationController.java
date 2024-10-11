package com.stillfresh.app.userservice.controller;

import com.stillfresh.app.userservice.model.AuthenticationRequest;
import com.stillfresh.app.userservice.service.CustomUserDetailsService;
import com.stillfresh.app.userservice.service.TokenBlacklistService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Authentication", description = "Operations related to user authentication and session management")
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

    @Operation(summary = "User Login", description = "Authenticates a user and returns a JWT token. Username or email and password are used for logging in.")
    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthenticationRequest authenticationRequest) throws Exception {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authenticationRequest.getIdentifier(), authenticationRequest.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new Exception("Incorrect username or password", e.getCause());
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationRequest.getIdentifier());

        logger.info("COMPARING The PASSWORDS FROM LOGIN {}", passwordEncoder.matches(authenticationRequest.getPassword(), userDetails.getPassword()));

        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;

        if (!customUserDetails.getUser().isActive()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User account is not verified.");
        }

        final String jwt = jwtUtil.generateToken(userDetails);

        return ResponseEntity.ok(jwt);
    }

    @Operation(summary = "User Logout", description = "Logs out the user by blacklisting the current JWT token.")
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

    @Operation(summary = "Refresh Token", description = "Generates a new JWT token using a valid refresh token.")
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || jwtUtil.isTokenExpired(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired refresh token");
        }

        String username = jwtUtil.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        String newToken = jwtUtil.generateRefreshToken(userDetails);
        return ResponseEntity.ok(newToken);
    }
}

