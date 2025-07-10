package com.stillfresh.app.userservice.controller;

import com.stillfresh.app.userservice.client.AuthorizationServiceClient;
import com.stillfresh.app.sharedentities.dto.CheckAvailabilityRequest;
import com.stillfresh.app.sharedentities.dto.OfferDto;
import com.stillfresh.app.sharedentities.responses.ApiResponse;
import com.stillfresh.app.sharedentities.responses.ErrorResponse;
import com.stillfresh.app.userservice.dto.PasswordChangeRequest;
import com.stillfresh.app.userservice.model.AuthenticationRequest;
import com.stillfresh.app.userservice.model.PasswordResetToken;
import com.stillfresh.app.userservice.model.User;
import com.stillfresh.app.userservice.model.VerificationToken;
import com.stillfresh.app.userservice.security.CustomUserDetails;
import com.stillfresh.app.userservice.service.EmailService;
import com.stillfresh.app.userservice.service.LoginAttemptService;
import com.stillfresh.app.userservice.service.UserService;
import com.stillfresh.app.userservice.repository.PasswordResetTokenRepository;
import com.stillfresh.app.userservice.repository.VerificationTokenRepository;
import com.stillfresh.app.sharedentities.order.events.OrderRequestEvent;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/users")
@Tag(name = "User Management", description = "Operations related to user management")
public class UserController {
	
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;
    
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private EmailService emailService;
    
    @Autowired
    private AuthorizationServiceClient authorizationServiceClient;

    @Operation(summary = "Register a new user", description = "This endpoint registers a new user and sends a verification email.")
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody User user) throws IOException {
    	

        // Call the authorization service to check availability
        ResponseEntity<ApiResponse> availabilityResponse = authorizationServiceClient.checkAvailability(
            new CheckAvailabilityRequest(user.getUsername(), user.getEmail()));

        // If the response indicates a conflict (username/email already taken)
        if (availabilityResponse.getStatusCode() == HttpStatus.CONFLICT) {
            // Return the conflict response
            return ResponseEntity.status(HttpStatus.CONFLICT).body(availabilityResponse.getBody());
        }

        try {
            // If available, proceed to initiate registration
            userService.registerUser(user);
            return ResponseEntity.ok(new ApiResponse(true, "User registration initiated. Check your email for verification."));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(new ErrorResponse("Failed to initiate registration: " + ex.getMessage()));
        }
    }

    @Operation(summary = "Verify a user", description = "Verifies a user account using the token sent via email.")
    @GetMapping("/verify")
    public ResponseEntity<String> verifyUser(@RequestParam("token") String token) {
        boolean isVerified = userService.verifyUser(token);
        
        if (isVerified) {
            return ResponseEntity.ok("User verified successfully.");
        } else {
            return ResponseEntity.status(400).body("Invalid token.");
        }
        
    }

    @Operation(summary = "Admin Endpoint", description = "Access restricted to users with ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/admin")
    public ResponseEntity<String> adminEndpoint() {
        return ResponseEntity.ok("Admin content");
    }

    @Operation(summary = "Get all users", description = "Retrieves a list of all registered users.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/allUsers")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.findAllUsers();
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Get user", description = "Retrieves a user from authentication token using their email")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER')")
    @GetMapping
    public ResponseEntity<User> getUser() {
        User user = userService.getUserFromContext();
        return ResponseEntity.ok(user);
    }

    @Operation(
        summary = "Update user profile",
        description = "Updates the user's profile information. Requires authentication."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER')")
    @PutMapping("/update-profile")
    public ResponseEntity<String> updateUserProfile(
        @Valid @RequestBody User updatedUser,
        BindingResult result) {
  
        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.getAllErrors().get(0).getDefaultMessage());
        }
        userService.updateUser(updatedUser);
        return ResponseEntity.ok("User profile updated successfully");
    }

    @Operation(summary = "Change password", description = "Allows a user to change their password.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER')")
    @PutMapping("/change-password")
    public ResponseEntity<String> changeUserPassword(
        @Valid @RequestBody PasswordChangeRequest passwordChangeRequest, HttpServletRequest request, BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body(result.getAllErrors().get(0).getDefaultMessage());
        }

        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = userDetails.getUser();

        ResponseEntity<String> passwordChangeResponse = userService.changeUserPassword(currentUser, passwordChangeRequest);
        
        // If password change was successful, invalidate the token and log out
        if (passwordChangeResponse.getStatusCode().is2xxSuccessful()) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                userService.logoutAndInvalidateToken(jwt);
            }
        }

        return passwordChangeResponse;
    }

    @Operation(summary = "Forgot password", description = "Initiates the password reset process by sending a reset link to the user's email.")
    @Transactional
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestParam String email) throws IOException {
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if a token already exists for the user
        Optional<PasswordResetToken> existingToken = passwordResetTokenRepository.findByUser(user);

        if (existingToken.isPresent()) {
            // Delete the existing token
            passwordResetTokenRepository.delete(existingToken.get());
            // Ensure the entity manager flushes the changes to the database
            passwordResetTokenRepository.flush();
        }

        // Generate and save a new password reset token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(calculateExpiryDate());
        passwordResetTokenRepository.save(resetToken);
        
        logger.info("Sent token: {}", token);
        
        String resetUrl = "http://localhost:8081/users/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(user.getEmail(), resetUrl);

        return ResponseEntity.ok("Password reset link sent to your email");
    }

    private Date calculateExpiryDate() {
        // Set the token to expire in 24 hours (or any other duration)
        final int EXPIRATION_TIME_IN_MINUTES = 24 * 60; // 24 hours
        Date now = new Date();
        return new Date(now.getTime() + (EXPIRATION_TIME_IN_MINUTES * 60 * 1000));
    }

    @Operation(summary = "Reset password", description = "Allows a user to reset their password using a valid token.")
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestParam("token") String token, @RequestBody String newPassword) {
    	logger.info("Received token: {}", token);
    	
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token);

        if (resetToken == null || resetToken.isExpired()) {
            return ResponseEntity.status(400).body("Invalid or expired token");
        }

        User user = resetToken.getUser();
        userService.changeUserPassword(user, newPassword);

        return ResponseEntity.ok("Password reset successfully");
    }
    
    @Operation(
        summary = "Delete user account",
        description = "Deletes the user's account and invalidates their token."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteUser() {
        return userService.deleteUserProfile();
    }
    
    @GetMapping("/offers/nearby")
    public ResponseEntity<List<OfferDto>> getNearbyOffers( 
            @RequestParam double latitude, 
            @RequestParam double longitude, 
            @RequestParam double range) throws ExecutionException {
        List<OfferDto> nearbyOffers = userService.getNearbyOffers(latitude, longitude, range);
        return ResponseEntity.ok(nearbyOffers);
    }

    @Operation(
        summary = "Submit order request",
        description = "Submits an order request for the authenticated user."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/order-request")
    public ResponseEntity<String> submitOrderRequest(@RequestBody OrderRequestEvent orderRequest) {
        userService.publishOrderRequest(orderRequest);
        return ResponseEntity.ok("Order request submitted successfully");
    }

}
