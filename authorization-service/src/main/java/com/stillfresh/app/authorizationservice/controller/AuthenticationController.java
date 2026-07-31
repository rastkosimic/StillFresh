package com.stillfresh.app.authorizationservice.controller;

import com.stillfresh.app.authorizationservice.model.AuthenticationRequest;
import com.stillfresh.app.authorizationservice.model.AuthenticationResponse;
import com.stillfresh.app.authorizationservice.model.AuthTokensResponse;
import com.stillfresh.app.authorizationservice.security.CustomUserDetails;
import com.stillfresh.app.authorizationservice.security.JwtUtil;
import com.stillfresh.app.authorizationservice.service.CustomUserDetailsService;
import com.stillfresh.app.authorizationservice.service.TokenBlacklistService;
import com.stillfresh.app.authorizationservice.dto.PasswordResetRequest;
import com.stillfresh.app.authorizationservice.dto.AuthenticatedPasswordResetRequest;
import com.stillfresh.app.authorizationservice.dto.RefreshTokenRequest;
import com.stillfresh.app.authorizationservice.client.VendorServiceClient;
import com.stillfresh.app.authorizationservice.service.RefreshTokenService;

import jakarta.servlet.http.HttpServletRequest;
import com.stillfresh.app.sharedentities.dto.CheckAvailabilityRequest;
import com.stillfresh.app.sharedentities.enums.Status;
import com.stillfresh.app.sharedentities.responses.ApiResponse;
import com.stillfresh.app.sharedentities.responses.ErrorResponse;
import com.stillfresh.app.authorizationservice.service.UserService;

import java.io.IOException;

import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {
	
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);
    
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

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired(required = false)
    private VendorServiceClient vendorServiceClient;
    
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
    

    @Operation(
        summary = "User login",
        description = "Authenticates a user with email/username and password, returns JWT token and user role"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful", 
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = AuthenticationResponse.class)
            )),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials or unverified account"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "410", description = "Account deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthenticationRequest authenticationRequest) {
        String identifier = authenticationRequest.getIdentifier();  // Either email or username
        String password = authenticationRequest.getPassword();

        logger.debug("Login attempt received");

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identifier, password)
            );
        } catch (Exception e) {
            logger.warn("Authentication failed for identifier: {}. Error: {}", identifier, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Incorrect identifier or password"));
        }

        try {
            // Reuse principal from authenticate() to avoid second loadUserByUsername (DB round-trip)
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;

            if (customUserDetails.getUser().getStatus() == Status.INACTIVE) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("User account is not verified."));
            }

            boolean accountWasDeleted = false;
            if (customUserDetails.getUser().getStatus() == Status.DELETED) {
                userService.reactivateIfDeleted(customUserDetails.getUser());
                accountWasDeleted = true;
                com.stillfresh.app.authorizationservice.model.User updatedUser = userService.findUserById(customUserDetails.getUser().getId());
                customUserDetails = new com.stillfresh.app.authorizationservice.security.CustomUserDetails(updatedUser);
                userDetails = customUserDetails;
            }

            //u zavisnosti da li je user vendor ili user okidati razlicite evente
            userService.cacheLoggedUser(customUserDetails.getUser());

            // Generate access + refresh tokens
            final String accessJwt = jwtUtil.generateAccessToken(userDetails);
            final String refreshToken = jwtUtil.generateRefreshToken(userDetails);
            refreshTokenService.storeRefreshToken(refreshToken);
            
            // Create AuthenticationResponse with tokens and role; keep legacy jwt field populated too
            AuthenticationResponse response = new AuthenticationResponse(
                accessJwt,
                refreshToken,
                customUserDetails.getUser().getRole().name(),
                accountWasDeleted
            );
            // Vendor info is not fetched here to avoid blocking login on vendor-service. App should call GET /vendors/profile (or similar) after login if needed.
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error during login: " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        userService.logoutAndInvalidateToken();
        if (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank()) {
            refreshTokenService.revokeRefreshToken(request.getRefreshToken());
        }
        return ResponseEntity.ok("Logged out successfully");
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        try {
            RefreshTokenService.RefreshTokenRecord record = refreshTokenService.validateStoredRefreshToken(refreshToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(record.getUsername());

            // Rotation: one-time-use refresh token
            refreshTokenService.consumeRefreshToken(refreshToken);

            String newAccessJwt = jwtUtil.generateAccessToken(userDetails);
            String newRefreshToken = jwtUtil.generateRefreshToken(userDetails);
            refreshTokenService.storeRefreshToken(newRefreshToken);

            return ResponseEntity.ok(new AuthTokensResponse(newAccessJwt, newRefreshToken));
        } catch (JwtException e) {
            logger.debug("Invalid refresh token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired refresh token");
        } catch (Exception e) {
            logger.warn("Refresh token flow failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired refresh token");
        }
    }

    @Operation(
        summary = "Request password reset with verification",
        description = "Initiates password reset for any user (USER or VENDOR) by email and new password. Sends verification link to email. Password is only changed after email verification."
    )
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody PasswordResetRequest request) throws IOException {
        logger.info("Password reset requested for email: {}", request.getEmail());
        try {
            userService.requestPasswordReset(request.getEmail(), request.getNewPassword());
            return ResponseEntity.ok("Verification link sent to your email. Please click the link to confirm the password reset.");
        } catch (RuntimeException e) {
            logger.warn("Password reset request failed for email: {}", request.getEmail(), e);
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with email: " + request.getEmail());
            }
            if (e.getMessage().contains("at least 6")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error processing password reset request for email: {}", request.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send password reset verification link");
        }
    }

    @Operation(
        summary = "Confirm password reset",
        description = "Confirms and applies password reset when user clicks verification link from email. This actually changes the password."
    )
    @GetMapping("/reset-password")
    public ResponseEntity<?> confirmPasswordReset(
        @RequestParam("token") String token,
        @RequestHeader(value = "Accept", required = false, defaultValue = "text/html") String acceptHeader) {
        logger.info("Password reset confirmation requested with token");
        try {
            // First validate the token
            Map<String, Object> validationResult = userService.validateResetToken(token);
            boolean isValid = (Boolean) validationResult.get("valid");
            
            if (!isValid) {
                String message = (String) validationResult.get("message");
                
                if (acceptHeader != null && acceptHeader.contains("application/json")) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(validationResult);
                }
                
                // Return HTML error page
                String htmlResponse = generateErrorPage(message);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(org.springframework.http.MediaType.TEXT_HTML)
                    .body(htmlResponse);
            }
            
            // Token is valid, now confirm and apply the password reset
            userService.confirmPasswordReset(token);
            
            String email = (String) validationResult.get("email");
            
            // Return JSON response for API clients (mobile app)
            if (acceptHeader != null && acceptHeader.contains("application/json")) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Password reset successfully confirmed",
                    "email", email
                ));
            }
            
            // Return HTML success page for browser clients
            String htmlResponse = generatePasswordResetSuccessPage(email);
            return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.TEXT_HTML)
                .body(htmlResponse);
                
        } catch (RuntimeException e) {
            logger.warn("Password reset confirmation failed: {}", e.getMessage());
            String errorMessage = e.getMessage();
            
            if (acceptHeader != null && acceptHeader.contains("application/json")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", errorMessage));
            }
            
            String htmlResponse = generateErrorPage(errorMessage);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(org.springframework.http.MediaType.TEXT_HTML)
                .body(htmlResponse);
        } catch (Exception e) {
            logger.error("Error confirming password reset", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error confirming password reset"));
        }
    }

    // Note: POST /reset-password is deprecated in favor of GET /reset-password (email verification flow)
    // Keeping for backward compatibility, but new flow uses GET endpoint
    @Operation(
        summary = "Reset password (deprecated - use GET /reset-password instead)",
        description = "Legacy endpoint. New flow: POST /forgot-password with email and newPassword, then GET /reset-password?token={token} to confirm."
    )
    @PostMapping("/reset-password")
    @Deprecated
    public ResponseEntity<String> resetPassword(
        @RequestParam("token") String token,
        @RequestBody String newPassword) {
        
        logger.warn("Deprecated POST /reset-password endpoint used. Consider using new verification flow.");
        // For backward compatibility, we can still support this, but it's not the recommended flow
        try {
            // This would need to be updated if we want to support it, but for now we'll redirect to new flow
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("This endpoint is deprecated. Please use: 1) POST /forgot-password with email and newPassword, 2) Click verification link in email");
        } catch (Exception e) {
            logger.error("Error during password reset", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to reset password");
        }
    }
    
    private String generatePasswordResetSuccessPage(String email) {
        return "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "<title>Password Reset Successful - StillFresh</title>" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
            "<style>" +
            "body { font-family: Arial, sans-serif; max-width: 400px; margin: 50px auto; padding: 20px; text-align: center; }" +
            "h1 { color: #2c5530; }" +
            "p { color: #666; }" +
            ".success { background: #e8f5e9; padding: 20px; border-radius: 5px; margin: 20px 0; }" +
            ".success-icon { font-size: 48px; color: #4caf50; margin-bottom: 10px; }" +
            "a { color: #2c5530; text-decoration: none; font-weight: bold; }" +
            "a:hover { text-decoration: underline; }" +
            "</style>" +
            "</head>" +
            "<body>" +
            "<div class=\"success-icon\">✓</div>" +
            "<h1>Password Reset Successful!</h1>" +
            "<div class=\"success\">" +
            "<p><strong>Your password has been successfully reset.</strong></p>" +
            "<p>Email: " + email + "</p>" +
            "</div>" +
            "<p>You can now log in with your new password.</p>" +
            "<p><a href=\"http://localhost:8080/auth/login\">Go to Login</a></p>" +
            "<p style=\"font-size: 12px; color: #999; margin-top: 30px;\">" +
            "If you did not request this password reset, please contact support immediately." +
            "</p>" +
            "</body>" +
            "</html>";
    }
    
    private String generateErrorPage(String message) {
        return "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "<title>Invalid Reset Link - StillFresh</title>" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
            "<style>" +
            "body { font-family: Arial, sans-serif; max-width: 400px; margin: 50px auto; padding: 20px; }" +
            "h1 { color: #d32f2f; }" +
            ".error { background: #ffebee; padding: 15px; border-radius: 5px; margin: 20px 0; color: #c62828; }" +
            "a { color: #2c5530; }" +
            "</style>" +
            "</head>" +
            "<body>" +
            "<h1>Invalid Reset Link</h1>" +
            "<div class=\"error\">" +
            "<p><strong>" + message + "</strong></p>" +
            "</div>" +
            "<p>Please request a new password reset link from the app.</p>" +
            "<p><a href=\"http://localhost:8080\">Go to StillFresh</a></p>" +
            "</body>" +
            "</html>";
    }

    @Operation(
        summary = "Change password (authenticated user)",
        description = "Immediately changes the password for the authenticated user (USER or VENDOR). The new password is applied right away — no email verification link is required. A confirmation email is sent after the change. The user is logged out for security after the password is updated."
    )
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('USER', 'VENDOR', 'VENDOR_ADMIN')")
    @PostMapping("/change-password")
    public ResponseEntity<String> requestPasswordResetAuthenticated(
        @Valid @RequestBody AuthenticatedPasswordResetRequest request,
        HttpServletRequest httpRequest) {
        
        logger.info("Password reset request for authenticated user");
        try {
            // Get current authenticated user
            CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
            Long userId = userDetails.getUser().getId();
            
            // Validate passwords match
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("New password and confirm password do not match");
            }
            
            // Apply password change immediately
            userService.changePasswordForAuthenticatedUser(
                userId, 
                request.getEmail(), 
                request.getNewPassword()
            );
            
            // Log out the user by invalidating their token
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                userService.invalidateToken(jwt);
                logger.info("User logged out after password change for user ID: {}", userId);
            }
            
            return ResponseEntity.ok("Password changed successfully. You have been logged out for security.");
        } catch (RuntimeException e) {
            logger.warn("Password reset request failed: {}", e.getMessage());
            if (e.getMessage().contains("does not match")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
            }
            if (e.getMessage().contains("at least 6")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error during password reset request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to change password");
        }
    }
}
