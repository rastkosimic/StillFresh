package com.stillfresh.app.authorizationservice.service;

import com.stillfresh.app.authorizationservice.model.PasswordResetToken;
import com.stillfresh.app.authorizationservice.model.User;
import com.stillfresh.app.authorizationservice.model.UserVerificationToken;
import com.stillfresh.app.authorizationservice.publisher.AuthorizationEventPublisher;
import com.stillfresh.app.authorizationservice.repository.PasswordResetTokenRepository;
import com.stillfresh.app.authorizationservice.repository.UserRepository;
import com.stillfresh.app.authorizationservice.repository.UserVerificationTokenRepository;
import com.stillfresh.app.authorizationservice.security.JwtUtil;
import com.stillfresh.app.sharedentities.dto.CheckAvailabilityRequest;
import com.stillfresh.app.sharedentities.enums.Role;
import com.stillfresh.app.sharedentities.enums.Status;
import com.stillfresh.app.sharedentities.shared.events.TokenRequestEvent;
import com.stillfresh.app.sharedentities.shared.events.TokenValidationResponseEvent;
import com.stillfresh.app.sharedentities.user.events.LoggedUserEvent;
import com.stillfresh.app.sharedentities.user.events.PasswordUpdateEvent;
import com.stillfresh.app.sharedentities.user.events.UpdateUserProfileEvent;
import com.stillfresh.app.sharedentities.user.events.UserRegisteredEvent;
import com.stillfresh.app.sharedentities.user.events.UserVerifiedEvent;
import com.stillfresh.app.sharedentities.vendor.events.LoggedVendorEvent;
import com.stillfresh.app.sharedentities.vendor.events.UpdateVendorProfileEvent;
import com.stillfresh.app.sharedentities.vendor.events.VendorRegisteredEvent;
import com.stillfresh.app.sharedentities.vendor.events.VendorVerifiedEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
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
    
    @Autowired
    private AuthorizationEventPublisher authorizationEventPublisher;

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
        admin.setStatus(Status.ACTIVE);
        return userRepository.save(admin);
    }

    // Register an Admin
    public User registerAdmin(User admin) throws IOException {
        if (userRepository.existsByEmail(admin.getEmail())) {
            throw new RuntimeException("Email is already registered.");
        }
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        admin.setRole(Role.ADMIN);
        admin.setStatus(Status.ACTIVE);  // Admin is active by default
        return userRepository.save(admin);
    }

    // Register a User
    public User registerUser(User user) throws IOException {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("User already registered with this email");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(Role.USER);
        user.setStatus(Status.INACTIVE);  // Inactive until verified
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
        user.setStatus(Status.ACTIVE);
        userRepository.save(user);
        return true;
    }

    // Request password reset with email and new password (universal - works for both users and vendors)
    // This creates a pending password reset that requires email verification
    public void requestPasswordReset(String email, String newPassword) throws IOException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        // Validate password length
        if (newPassword.length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters long");
        }

        // Delete any existing reset token for this user
        Optional<PasswordResetToken> existingToken = passwordResetTokenRepository.findByUser(user);
        if (existingToken.isPresent()) {
            passwordResetTokenRepository.delete(existingToken.get());
            passwordResetTokenRepository.flush();
        }

        // Encode the new password (will be applied after email verification)
        String encodedPassword = passwordEncoder.encode(newPassword);

        // Generate a new reset token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setNewPasswordHash(encodedPassword); // Store the new password hash temporarily
        resetToken.setExpiryDate(calculateExpiryDate(24)); // 24 hours
        passwordResetTokenRepository.save(resetToken);

        // Send verification email with confirmation link
        String verificationUrl = "http://localhost:8080/auth/reset-password?token=" + token;
        emailService.sendPasswordResetVerificationEmail(user.getEmail(), verificationUrl);
        
        logger.info("Password reset verification link sent to email: {} for user ID: {}, role: {}", 
                    user.getEmail(), user.getId(), user.getRole());
    }

    // Change password immediately for authenticated user (universal - works for both users and vendors)
    // Password is applied immediately; user will be logged out after this request
    public void changePasswordForAuthenticatedUser(Long userId, String email, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Verify that the email matches the authenticated user's email
        if (!user.getEmail().equalsIgnoreCase(email)) {
            throw new RuntimeException("Email does not match your account email");
        }

        // Validate password length
        if (newPassword.length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters long");
        }

        // Defensively clean up any stale reset token for this user
        Optional<PasswordResetToken> existingToken = passwordResetTokenRepository.findByUser(user);
        if (existingToken.isPresent()) {
            passwordResetTokenRepository.delete(existingToken.get());
            passwordResetTokenRepository.flush();
        }

        // Apply the new password immediately
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        userRepository.save(user);

        logger.info("Password changed immediately for authenticated user ID: {}, email: {}, role: {}",
                    user.getId(), user.getEmail(), user.getRole());

        // Publish password update event to Kafka so user-service / vendor-service stay in sync
        PasswordUpdateEvent passwordUpdateEvent = new PasswordUpdateEvent(
            user.getId(),
            user.getEmail(),
            encodedPassword,
            user.getRole()
        );
        authorizationEventPublisher.publishPasswordUpdateEvent(passwordUpdateEvent);

        // Send notification-only email (no link required); email failure must not roll back the password change
        try {
            emailService.sendPasswordChangedNotificationEmail(user.getEmail());
            logger.info("Password changed notification email sent to: {}", user.getEmail());
        } catch (IOException e) {
            logger.warn("Failed to send password changed notification email to: {}", user.getEmail(), e);
        }
    }

    // Validate password reset token (universal - works for both users and vendors)
    public Map<String, Object> validateResetToken(String token) {
        Optional<PasswordResetToken> resetTokenOpt = passwordResetTokenRepository.findByToken(token);
        if (resetTokenOpt.isEmpty()) {
            return Map.of(
                "valid", false,
                "message", "Invalid password reset token"
            );
        }
        
        PasswordResetToken resetToken = resetTokenOpt.get();
        if (resetToken.isExpired()) {
            return Map.of(
                "valid", false,
                "message", "Password reset token has expired"
            );
        }
        
        // Check if new password hash exists (required for new verification flow)
        if (resetToken.getNewPasswordHash() == null || resetToken.getNewPasswordHash().isEmpty()) {
            return Map.of(
                "valid", false,
                "message", "Password reset token is invalid - no password reset request found"
            );
        }
        
        User user = resetToken.getUser();
        return Map.of(
            "valid", true,
            "message", "Token is valid",
            "email", user.getEmail()
        );
    }

    // Confirm and apply password reset (universal - works for both users and vendors)
    // This is called when user clicks the verification link in email
    public void confirmPasswordReset(String token) {
        Optional<PasswordResetToken> resetTokenOpt = passwordResetTokenRepository.findByToken(token);
        if (resetTokenOpt.isEmpty()) {
            throw new RuntimeException("Invalid password reset token");
        }
        
        PasswordResetToken resetToken = resetTokenOpt.get();
        if (resetToken.isExpired()) {
            throw new RuntimeException("Password reset token has expired");
        }
        
        // Check if new password hash is stored (should always be present in new flow)
        if (resetToken.getNewPasswordHash() == null || resetToken.getNewPasswordHash().isEmpty()) {
            throw new RuntimeException("Password reset token is invalid - no new password found");
        }

        User user = resetToken.getUser();
        // Apply the new password that was stored in the token
        String encodedPassword = resetToken.getNewPasswordHash();
        user.setPassword(encodedPassword);
        userRepository.save(user);
        
        logger.info("Password reset confirmed and applied in authorization service for user ID: {}, email: {}, role: {}", 
                    user.getId(), user.getEmail(), user.getRole());

        // Publish password update event to Kafka for service-specific database updates
        PasswordUpdateEvent passwordUpdateEvent = new PasswordUpdateEvent(
            user.getId(),
            user.getEmail(),
            encodedPassword,
            user.getRole()
        );
        authorizationEventPublisher.publishPasswordUpdateEvent(passwordUpdateEvent);

        // Delete the reset token after successful password reset
        passwordResetTokenRepository.delete(resetToken);
        
        // Send confirmation email that password was reset
        try {
            emailService.sendPasswordResetConfirmationEmail(user.getEmail());
            logger.info("Password reset confirmation email sent to: {}", user.getEmail());
        } catch (IOException e) {
            logger.warn("Failed to send password reset confirmation email to: {}", user.getEmail(), e);
            // Don't fail the password reset if email fails
        }
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
        user.setStatus(Status.ACTIVE);
        userRepository.save(user);
        return user.isActive();
    }

    // Deactivate user
    public boolean deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(Status.INACTIVE);
        userRepository.save(user);
        return !user.isActive();
    }

    /**
     * If the user's status is DELETED, reactivates them (sets ACTIVE), saves, and publishes
     * the appropriate event so user-service or vendor-service can sync. Returns true if reactivation was performed.
     */
    public boolean reactivateIfDeleted(User user) {
        if (user == null) return false;
        User dbUser = userRepository.findById(user.getId()).orElse(null);
        if (dbUser == null || dbUser.getStatus() != Status.DELETED) return false;
        dbUser.setStatus(Status.ACTIVE);
        userRepository.save(dbUser);
        if (dbUser.getRole() == Role.USER) {
            UpdateUserProfileEvent event = new UpdateUserProfileEvent(
                dbUser.getUsername(), dbUser.getEmail(), dbUser.getPassword(),
                dbUser.getRole(), Status.ACTIVE
            );
            authorizationEventPublisher.publishUpdateUserProfileEvent(event);
        } else if (dbUser.getRole() == Role.VENDOR || dbUser.getRole() == Role.VENDOR_ADMIN) {
            UpdateVendorProfileEvent vendorEvent = new UpdateVendorProfileEvent(
                dbUser.getUsername(), dbUser.getEmail(), dbUser.getPassword(),
                dbUser.getRole(), Status.ACTIVE
            );
            authorizationEventPublisher.publishUpdateVendorProfileEvent(vendorEvent);
        }
        logger.info("Reactivated previously deleted account for user id: {}, email: {}, role: {}", dbUser.getId(), dbUser.getEmail(), dbUser.getRole());
        return true;
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
    private String extractTokenFromContext() {
        String authorizationHeader = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid Authorization header");
        }
        return authorizationHeader.substring(7); // Remove "Bearer " prefix
    }

    public void logoutAndInvalidateToken() {
        String jwt = extractTokenFromContext();
        invalidateToken(jwt);
    }

    public void invalidateToken(String jwt) {
        long expiryDurationInMillis = jwtUtil.getExpirationTimeInMillis(jwt) - System.currentTimeMillis();
        String jti = null;
        try {
            jti = jwtUtil.extractJti(jwt);
        } catch (Exception e) {
            logger.warn("Failed to extract jti from JWT for blacklist, falling back to full token key", e);
        }

        if (jti != null && !jti.isEmpty()) {
            tokenBlacklistService.addTokenIdToBlacklist(jti, expiryDurationInMillis);
        } else {
            tokenBlacklistService.addTokenToBlacklist(jwt, expiryDurationInMillis);
        }
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

    
    // Vendor events handling
    public void registerVendor(VendorRegisteredEvent event) {
        // Idempotent handling: update existing user instead of failing if already present.
        Optional<User> existing = userRepository.findByEmail(event.getEmail());
        if (existing.isPresent()) {
            User user = existing.get();
            user.setPassword(event.getPassword()); // already encoded by caller
            user.setRole(event.getRole());
            user.setStatus(event.getStatus());
            user.setUsername(event.getUsername());
            userRepository.save(user);
            logger.info("Updated existing vendor from VendorRegisteredEvent for email: {}", event.getEmail());
            return;
        }

        User user = new User();
        user.setEmail(event.getEmail());
        user.setPassword(event.getPassword()); // password already encoded in vendor service
        user.setRole(event.getRole());
        user.setStatus(event.getStatus());
        user.setUsername(event.getUsername());

        userRepository.save(user);
        logger.info("Created new vendor from VendorRegisteredEvent for email: {}", event.getEmail());
    }

	public void verifyVendor(VendorVerifiedEvent event) {
        if (!userRepository.existsByEmail(event.getEmail())) {
            throw new RuntimeException("Vendor is not registered with this email");
        }
        User user = userRepository.findByEmail(event.getEmail()).get();
        user.setStatus(Status.ACTIVE);
        userRepository.save(user);
	}

	public void updateVendor(UpdateVendorProfileEvent event) {
        if (!userRepository.existsByEmail(event.getEmail())) {
            throw new RuntimeException("Vendor is not registered with this email");
        }
        User user = userRepository.findByEmail(event.getEmail()).get();
        user.setUsername(event.getUsername());
        user.setEmail(event.getEmail());
        user.setRole(event.getRole());
        user.setStatus(event.getStatus());
        userRepository.save(user);
	}
	
	
    // User events handling	
    public void registerUser(UserRegisteredEvent event) {
        // Idempotent handling: update existing user instead of failing if already present.
        Optional<User> existing = userRepository.findByEmail(event.getEmail());
        if (existing.isPresent()) {
            User user = existing.get();
            user.setPassword(event.getPassword()); // already encoded by caller
            user.setRole(event.getRole());
            user.setStatus(event.getStatus());
            user.setUsername(event.getUsername());
            userRepository.save(user);
            logger.info("Updated existing user from UserRegisteredEvent for email: {}", event.getEmail());
            return;
        }

        User user = new User();
        user.setEmail(event.getEmail());
        user.setPassword(event.getPassword()); // password already encoded in user-service
        user.setRole(event.getRole());
        user.setStatus(event.getStatus());
        user.setUsername(event.getUsername());

        userRepository.save(user);
        logger.info("Created new user from UserRegisteredEvent for email: {}", event.getEmail());
    }

    //This is related to User from user-service
	public void verifyUser(UserVerifiedEvent event) {
        if (!userRepository.existsByEmail(event.getEmail())) {
            throw new RuntimeException("User is not registered with this email");
        }
        User user = userRepository.findByEmail(event.getEmail()).get();
        user.setStatus(Status.ACTIVE);
        userRepository.save(user);
		
	}

	public void updateUser(UpdateUserProfileEvent event) {
        if (!userRepository.existsByEmail(event.getEmail())) {
            throw new RuntimeException("User is not registered with this email");
        }
        User user = userRepository.findByEmail(event.getEmail()).get();
        user.setUsername(event.getUsername());
        user.setEmail(event.getEmail());
        user.setRole(event.getRole());
        user.setStatus(event.getStatus());
        userRepository.save(user);
	}
	
	public boolean isAvailable(CheckAvailabilityRequest request) {
	    boolean isEmailTaken = userRepository.existsByEmail(request.getEmail());
	    boolean isUsernameTaken = userRepository.existsByUsername(request.getUsername());
	    
	    return !(isEmailTaken || isUsernameTaken);  // Returns true only if both are available
	}

	public void cacheLoggedUser(User user) {
		if (user.getRole()==Role.USER) {
			authorizationEventPublisher.publishLoggedUserEvent(new LoggedUserEvent(user.getUsername(), user.getEmail()));
		}else if (user.getRole()==Role.VENDOR) {
			authorizationEventPublisher.publishLoggedVendorEvent(new LoggedVendorEvent(user.getUsername(), user.getEmail()));
		}else {
			//Cache admin if needed
		}
	}

	public void tokenValidation(TokenRequestEvent event) {
		String token = event.getToken();
		String correlationId = event.getCorrelationId();
				
        try {
            String jti = null;
            try {
                jti = jwtUtil.extractJti(token);
            } catch (Exception e) {
                logger.warn("Failed to extract jti during token validation, will fall back to token-based blacklist", e);
            }

            boolean isBlacklisted =
                (jti != null && !jti.isEmpty() && tokenBlacklistService.isTokenIdBlacklisted(jti))
                || tokenBlacklistService.isTokenBlacklisted(token);

            if (isBlacklisted) {
            	authorizationEventPublisher.publishTokenValidationResponseEvent(new TokenValidationResponseEvent(false, null, null, correlationId, "Token is blacklisted"));
            	logger.error("Token is blacklisted for correlationId: {}", correlationId);
           } else if (jwtUtil.isTokenExpired(token)) {
        	   authorizationEventPublisher.publishTokenValidationResponseEvent(new TokenValidationResponseEvent(false, null, null, correlationId, "Token is expired"));
        	   logger.error("Token is expired for correlationId: {}", correlationId);
            } else {
                String username = jwtUtil.extractUsername(token);
                String email = jwtUtil.extractEmail(token);
                authorizationEventPublisher.publishTokenValidationResponseEvent(new TokenValidationResponseEvent(true, username, email, correlationId, "Token is valid"));
         	   	logger.info("Token is valid for correlationId: {}", correlationId);
               
            }
		} catch (Exception e) {
			authorizationEventPublisher.publishTokenValidationResponseEvent(new TokenValidationResponseEvent(false, null, null, correlationId, e.getMessage()));
			logger.error("Token validation failed for correlationId {}: {}", correlationId, e.getMessage());
		}
		
	}
}
