package com.stillfresh.app.authorizationservice.controller;

import com.stillfresh.app.authorizationservice.model.AuthenticationRequest;
import com.stillfresh.app.authorizationservice.security.CustomUserDetails;
import com.stillfresh.app.authorizationservice.security.JwtUtil;
import com.stillfresh.app.authorizationservice.service.CustomUserDetailsService;
import com.stillfresh.app.authorizationservice.service.TokenBlacklistService;
import com.stillfresh.app.sharedentities.dto.CheckAvailabilityRequest;
import com.stillfresh.app.sharedentities.enums.Status;
import com.stillfresh.app.sharedentities.responses.ApiResponse;
import com.stillfresh.app.sharedentities.responses.ErrorResponse;
import com.stillfresh.app.authorizationservice.service.UserService;

import io.swagger.v3.oas.annotations.Operation;

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
    private UserService userService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    @Operation(summary = "Check if username and email are unique")
    @PostMapping("/check-availability")
    public ResponseEntity<ApiResponse> checkAvailability(@RequestBody CheckAvailabilityRequest request) {
        boolean isAvailable = userService.isAvailable(request);

        if (!isAvailable) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                                 .body(new ApiResponse(false, "Email or Username already taken"));
        }

        return ResponseEntity.ok(new ApiResponse(true, "Username and Email are available"));
    }
    

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody AuthenticationRequest authenticationRequest) throws Exception {
        String identifier = authenticationRequest.getIdentifier();  // Either email or username
        String password = authenticationRequest.getPassword();

        try {
            // Authenticate using the identifier and password
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identifier, password)
            );
        } catch (Exception e) {
            throw new Exception("Incorrect identifier or password", e);
        }

        // Load user by identifier (username or email)
        UserDetails userDetails = userDetailsService.loadUserByUsername(identifier);
        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;

        if (customUserDetails.getUser().getStatus() == Status.INACTIVE) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User account is not verified.");
        } else if (customUserDetails.getUser().getStatus() == Status.DELETED) {
            return ResponseEntity.status(HttpStatus.GONE).body("User account is deleted.");
        }

        
        //u zavisnosti da li je user vendor ili user okidati razlicite evente
        userService.cacheLoggedUser(customUserDetails.getUser());

        // Generate JWT token
        final String jwt = jwtUtil.generateToken(userDetails);
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
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        String newToken = jwtUtil.generateRefreshToken(userDetails);
        return ResponseEntity.ok(newToken);
    }
}
