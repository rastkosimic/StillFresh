package com.stillfresh.app.authorizationservice.controller;

import com.stillfresh.app.authorizationservice.model.AuthenticationResponse;
import com.stillfresh.app.authorizationservice.model.OAuth2LoginResult;
import com.stillfresh.app.authorizationservice.service.OAuth2Service;
import com.stillfresh.app.sharedentities.enums.Role;
import com.stillfresh.app.sharedentities.responses.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth/oauth2")
public class OAuth2Controller {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2Controller.class);

    @Autowired
    private OAuth2Service oauth2Service;

    /**
     * REST endpoint for Google OAuth2 login
     * Mobile app sends Google ID token, backend validates and returns JWT
     */
    @Operation(
        summary = "Google OAuth2 login/signup",
        description = "Authenticates user with Google ID token. Creates account if user doesn't exist. Returns JWT token."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful",
            content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or missing parameters"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/google/login")
    public ResponseEntity<?> googleLogin(
            @RequestBody Map<String, String> request) {
        
        try {
            String idToken = request.get("idToken");
            String roleStr = request.get("role"); // "USER" or "VENDOR"
            
            if (idToken == null || idToken.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Google ID token is required"));
            }

            // Determine role — only USER and VENDOR are accepted from OAuth2 clients.
            // Elevated roles (ADMIN, SUPER_ADMIN) must be assigned through the back-office, never via OAuth2.
            Role role = Role.USER;
            if (roleStr != null && !roleStr.isEmpty()) {
                try {
                    Role parsed = Role.valueOf(roleStr.toUpperCase());
                    if (parsed == Role.USER || parsed == Role.VENDOR) {
                        role = parsed;
                    } else {
                        logger.warn("Disallowed role '{}' requested via OAuth2 — defaulting to USER", roleStr);
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new ErrorResponse("Invalid role for OAuth2 login"));
                    }
                } catch (IllegalArgumentException e) {
                    logger.warn("Unknown role parameter: {}. Defaulting to USER", roleStr);
                }
            }

            logger.info("Processing Google OAuth2 login for role: {}", role);

            // Validate Google ID token and get user info
            Map<String, Object> googleUserInfo = oauth2Service.validateGoogleIdToken(idToken);
            
            // Process OAuth2 login and generate JWT
            OAuth2LoginResult result = oauth2Service.processOAuth2LoginFromToken(googleUserInfo, role);

            // Return access + refresh tokens in the same format as normal login.
            AuthenticationResponse response = new AuthenticationResponse(
                result.getAccessJwt(), result.getRefreshToken(), role.name(), result.isAccountWasDeleted());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error processing Google OAuth2 login", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Error during OAuth2 login: " + e.getMessage()));
        }
    }

    /**
     * OAuth2 callback endpoint (for web-based OAuth2 flow)
     * This is called by Google after successful authentication
     */
    @Operation(
        summary = "OAuth2 callback (Web)",
        description = "Handles OAuth2 callback from Google for web clients and returns JWT token"
    )
    @GetMapping("/callback/google")
    public ResponseEntity<?> handleGoogleCallback(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @RequestParam(required = false) String role) {
        
        try {
            if (oauth2User == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("OAuth2 authentication failed"));
            }

            // Determine role — only USER and VENDOR are accepted from OAuth2 clients.
            Role userRole = Role.USER;
            if (role != null && !role.isEmpty()) {
                try {
                    Role parsed = Role.valueOf(role.toUpperCase());
                    if (parsed == Role.USER || parsed == Role.VENDOR) {
                        userRole = parsed;
                    } else {
                        logger.warn("Disallowed role '{}' requested via OAuth2 callback — defaulting to USER", role);
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new ErrorResponse("Invalid role for OAuth2 login"));
                    }
                } catch (IllegalArgumentException e) {
                    logger.warn("Unknown role parameter: {}. Defaulting to USER", role);
                }
            }

            logger.info("Processing Google OAuth2 callback for role: {}", userRole);

            // Process OAuth2 login and generate JWT
            OAuth2LoginResult result = oauth2Service.processOAuth2Login(oauth2User, userRole);

            // Return access + refresh tokens in the same format as normal login.
            AuthenticationResponse response = new AuthenticationResponse(
                result.getAccessJwt(), result.getRefreshToken(), userRole.name(), result.isAccountWasDeleted());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error processing OAuth2 callback", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Error during OAuth2 login: " + e.getMessage()));
        }
    }
}
