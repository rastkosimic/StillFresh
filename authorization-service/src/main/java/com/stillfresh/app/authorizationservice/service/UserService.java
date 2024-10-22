package com.stillfresh.app.authorizationservice.service;

import com.stillfresh.app.authorizationservice.model.PasswordResetToken;
import com.stillfresh.app.authorizationservice.model.User;
import com.stillfresh.app.authorizationservice.model.User.Role;
import com.stillfresh.app.authorizationservice.model.UserVerificationToken;
import com.stillfresh.app.authorizationservice.repository.PasswordResetTokenRepository;
import com.stillfresh.app.authorizationservice.repository.UserRepository;
import com.stillfresh.app.authorizationservice.repository.UserVerificationTokenRepository;
import com.stillfresh.app.authorizationservice.security.JwtUtil;
import com.stillfresh.app.authorizationservice.service.EmailService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.Date;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserVerificationTokenRepository userVerificationTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private JwtUtil jwtUtil;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    // Check if there is any Super-Admin
    public boolean hasSuperAdmin() {
        return userRepository.existsByRole(Role.SUPER_ADMIN);
    }

    // Register a Super-Admin
    public User registerSuperAdmin(User admin) throws IOException {
        if (hasSuperAdmin()) {
            throw new RuntimeException("Super-Admin already exists.");
        }
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        admin.setRole(Role.SUPER_ADMIN);
        admin.setActive(true);
        return userRepository.save(admin);
    }

    // Register an Admin
    public User registerAdmin(User admin) throws IOException {
        if (userRepository.existsByEmail(admin.getEmail())) {
            throw new RuntimeException("Email is already registered.");
        }
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        admin.setRole(Role.ADMIN);
        admin.setActive(true);  // Admin is active by default
        return userRepository.save(admin);
    }

    // Register a User
    public User registerUser(User user) throws IOException {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("User already registered with this email");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(Role.USER);
        user.setActive(false);  // Inactive until verified
        userRepository.save(user);

        // Generate verification token
        String token = UUID.randomUUID().toString();
        UserVerificationToken verificationToken = new UserVerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        userVerificationTokenRepository.save(verificationToken);

        // Send verification email
        String verificationUrl = "http://localhost:8082/users/verify?token=" + token;
        emailService.sendVerificationEmail(user.getEmail(), verificationUrl);

        return user;
    }

    // Verify user account
    public boolean verifyUser(String token) {
        UserVerificationToken verificationToken = userVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));
        User user = verificationToken.getUser();
        user.setActive(true);
        userRepository.save(user);
        return true;
    }

    // Send password reset link
    public void sendPasswordResetLink(String email) throws IOException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        Optional<PasswordResetToken> existingToken = passwordResetTokenRepository.findByUser(user);
        existingToken.ifPresent(token -> passwordResetTokenRepository.delete(token));

        // Generate a new reset token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(calculateExpiryDate(24));
        passwordResetTokenRepository.save(resetToken);

        // Send reset email
        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }

    // Reset user password
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired password reset token"));

        User user = resetToken.getUser();
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);
    }

    // Get user by ID
    public User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Activate user
    public boolean activateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(true);
        userRepository.save(user);
        return user.isActive();
    }

    // Deactivate user
    public boolean deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(false);
        userRepository.save(user);
        return !user.isActive();
    }

    // Promote user to Admin
    public void promoteUserToAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() == Role.ADMIN || user.getRole() == Role.SUPER_ADMIN) {
            throw new RuntimeException("User is already an admin or super-admin");
        }

        user.setRole(Role.ADMIN);
        userRepository.save(user);
    }

    // Demote Admin to User
    public void demoteAdminFromUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("This user is not an admin.");
        }

        user.setRole(Role.USER);
        userRepository.save(user);
    }

    // Delete Admin (restricted to Super-Admin)
    public void deleteAdminById(Long id) {
        User admin = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (admin.getRole() == Role.SUPER_ADMIN) {
            throw new RuntimeException("Cannot delete a Super-Admin.");
        }

        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("This user is not an admin.");
        }

        userRepository.deleteById(id);
    }

    // Delete user by ID
    public void deleteUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.USER) {
            throw new RuntimeException("You can only delete regular users.");
        }

        userRepository.deleteById(id);
    }

    // Token Blacklist handling
    public void logoutAndInvalidateToken(String jwt) {
        long expiryDurationInMillis = jwtUtil.getExpirationTimeInMillis(jwt) - System.currentTimeMillis();
        tokenBlacklistService.addTokenToBlacklist(jwt, expiryDurationInMillis);
        SecurityContextHolder.clearContext();
    }

    // Calculate expiry date for tokens
    private Date calculateExpiryDate(int hours) {
        Date now = new Date();
        return new Date(now.getTime() + (hours * 60 * 60 * 1000));  // Expiry time in milliseconds
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
