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

import jakarta.validation.Valid;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
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


    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> adminEndpoint() {
        return ResponseEntity.ok("Admin content");
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.findAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.findUserById(id);
        return ResponseEntity.ok(user);
    }

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

    @PutMapping("/change-password")
    public ResponseEntity<String> changeUserPassword(@RequestBody PasswordChangeRequest passwordChangeRequest) {
        CustomUserDetails customUserDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = customUserDetails.getUser();

        userService.changeUserPassword(currentUser, passwordChangeRequest.getNewPassword());

        return ResponseEntity.ok("Password changed successfully");
    }
    
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

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestParam("token") String token, @RequestBody String newPassword) {
    	logger.info("Received token: {}", token);
    	
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token);

        if (resetToken == null || resetToken.isExpired()) {
            return ResponseEntity.status(400).body("Invalid or expired token");
        }

        User user = resetToken.getUser();
//        user.setPassword(passwordEncoder.encode(newPassword));
//        userService.updateUser(user);
        
        userService.changeUserPassword(user, newPassword);

        return ResponseEntity.ok("Password reset successfully");
    }
}
