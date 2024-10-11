package com.stillfresh.app.userservice.controller;

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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

@RestController
@RequestMapping("/users")
@Tag(name = "User Management", description = "Operations related to user management")
public class UserController {
	
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;
    
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private EmailService emailService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Operation(summary = "Register a new user", description = "This endpoint registers a new user and sends a verification email.")
    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@Valid @RequestBody User user) throws IOException {
        userService.registerUser(user);

        // Generate and save verification token
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationTokenRepository.save(verificationToken);

        // Send verification email
        String verificationUrl = "http://localhost:8081/users/verify?token=" + token;
        emailService.sendVerificationEmail(user.getEmail(), verificationUrl);

        return ResponseEntity.ok("User registered successfully. Please check your email for verification.");
    }

    @Operation(summary = "Verify a user", description = "Verifies a user account using the token sent via email.")
    @GetMapping("/verify")
    public ResponseEntity<String> verifyUser(@RequestParam("token") String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token);

        if (verificationToken == null || verificationToken.isExpired()) {
            return ResponseEntity.status(400).body("Invalid or expired token");
        }

        User user = verificationToken.getUser();
        user.setActive(true);
        userService.updateUser(user);

        return ResponseEntity.ok("Account verified successfully");
    }

    @Operation(summary = "Admin Endpoint", description = "Access restricted to users with ADMIN role.")
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> adminEndpoint() {
        return ResponseEntity.ok("Admin content");
    }

    @Operation(summary = "Get all users", description = "Retrieves a list of all registered users.")
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.findAllUsers();
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Get user by ID", description = "Retrieves a user by their ID.")
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.findUserById(id);
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Update user profile", description = "Allows a user to update their profile information.")
    @PutMapping("/profile")
    public ResponseEntity<User> updateUserProfile(@RequestBody User updatedUserDetails) {
        CustomUserDetails customUserDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = customUserDetails.getUser();

        currentUser.setUsername(updatedUserDetails.getUsername());
        currentUser.setEmail(updatedUserDetails.getEmail());
        // Add other fields as needed

        User updatedUser = userService.updateUser(currentUser);
        return ResponseEntity.ok(updatedUser);
    }

    @Operation(summary = "Change password", description = "Allows a user to change their password.")
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
}
