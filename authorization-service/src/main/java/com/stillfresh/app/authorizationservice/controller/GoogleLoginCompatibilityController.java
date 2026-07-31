package com.stillfresh.app.authorizationservice.controller;

import com.stillfresh.app.authorizationservice.model.AuthenticationResponse;
import com.stillfresh.app.authorizationservice.model.OAuth2LoginResult;
import com.stillfresh.app.authorizationservice.service.OAuth2Service;
import com.stillfresh.app.sharedentities.enums.Role;
import com.stillfresh.app.sharedentities.responses.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Backward-compatible endpoint for clients still using /auth/google-login.
 * Delegates to the same OAuth2 token validation flow used by /auth/oauth2/google/login.
 */
@RestController
@RequestMapping("/auth")
public class GoogleLoginCompatibilityController {

    private static final Logger logger = LoggerFactory.getLogger(GoogleLoginCompatibilityController.class);

    private final OAuth2Service oauth2Service;

    public GoogleLoginCompatibilityController(OAuth2Service oauth2Service) {
        this.oauth2Service = oauth2Service;
    }

    @PostMapping("/google-login")
    public ResponseEntity<?> googleLoginCompatibility(@RequestBody Map<String, String> request) {
        try {
            String idToken = request.get("idToken");
            String roleStr = request.get("role");

            if (idToken == null || idToken.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("Google ID token is required"));
            }

            Role role = Role.USER;
            if (roleStr != null && !roleStr.isEmpty()) {
                try {
                    Role parsed = Role.valueOf(roleStr.toUpperCase());
                    if (parsed == Role.USER || parsed == Role.VENDOR) {
                        role = parsed;
                    } else {
                        logger.warn("Disallowed role '{}' requested via compatibility endpoint", roleStr);
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(new ErrorResponse("Invalid role for OAuth2 login"));
                    }
                } catch (IllegalArgumentException e) {
                    logger.warn("Unknown role parameter '{}' requested via compatibility endpoint", roleStr);
                }
            }

            Map<String, Object> googleUserInfo = oauth2Service.validateGoogleIdToken(idToken);
            OAuth2LoginResult result = oauth2Service.processOAuth2LoginFromToken(googleUserInfo, role);

            AuthenticationResponse response = new AuthenticationResponse(
                    result.getAccessJwt(),
                    result.getRefreshToken(),
                    role.name(),
                    result.isAccountWasDeleted()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing compatibility Google OAuth2 login", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error during OAuth2 login: " + e.getMessage()));
        }
    }
}
