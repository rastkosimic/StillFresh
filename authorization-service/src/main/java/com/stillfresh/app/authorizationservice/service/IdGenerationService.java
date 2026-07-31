package com.stillfresh.app.authorizationservice.service;

import com.stillfresh.app.authorizationservice.model.User;
import com.stillfresh.app.authorizationservice.repository.PasswordResetTokenRepository;
import com.stillfresh.app.authorizationservice.repository.UserRepository;
import com.stillfresh.app.authorizationservice.repository.UserVerificationTokenRepository;
import com.stillfresh.app.sharedentities.enums.Role;
import com.stillfresh.app.sharedentities.enums.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class IdGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(IdGenerationService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private UserVerificationTokenRepository userVerificationTokenRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    /**
     * Generates a global user ID for any user across all services.
     * If user already exists, returns the existing ID.
     * If user doesn't exist, creates a new user and returns the generated ID.
     * 
     * @param email User's email (unique identifier)
     * @param username User's username
     * @param role User's role (USER, VENDOR, ADMIN, etc.)
     * @return Global user ID that should be used across all services
     */
    @Transactional
    public Long generateUserId(String email, String username, Role role) {
        logger.info("Generating global user ID for email: {}, username: {}, role: {}", email, username, role);
        
        // Check if user already exists by email
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            Long existingId = existingUser.get().getId();
            logger.info("User already exists with global ID: {}", existingId);
            return existingId;
        }
        
        // Check if username is already taken
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username '" + username + "' is already taken");
        }
        
        // Create new user with global ID
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setRole(role);
        user.setStatus(Status.INACTIVE); // Will be activated when verified
        user.setPassword("TEMP_PASSWORD"); // Temporary password, will be updated by the service
        
        user = userRepository.save(user);
        Long globalId = user.getId();
        
        logger.info("Generated new global user ID: {} for email: {}", globalId, email);
        return globalId;
    }

    /**
     * Updates an existing user's password and status.
     * This is called after the service-specific registration is complete.
     * 
     * @param globalUserId The global user ID
     * @param encodedPassword The encoded password from the service
     * @param status The user's status
     */
    @Transactional
    public void updateUserCredentials(Long globalUserId, String encodedPassword, Status status) {
        logger.info("Updating credentials for global user ID: {}", globalUserId);
        
        Optional<User> userOpt = userRepository.findById(globalUserId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User with global ID " + globalUserId + " not found");
        }
        
        User user = userOpt.get();
        user.setPassword(encodedPassword);
        user.setStatus(status);
        userRepository.save(user);
        
        logger.info("Updated credentials for global user ID: {}", globalUserId);
    }
    
    /**
     * Updates an existing user's role.
     * This is called when a user's role needs to be changed (e.g., VENDOR to VENDOR_ADMIN).
     * 
     * @param globalUserId The global user ID
     * @param role The new role
     */
    @Transactional
    public void updateUserRole(Long globalUserId, Role role) {
        logger.info("Updating role for global user ID: {} to role: {}", globalUserId, role);
        
        Optional<User> userOpt = userRepository.findById(globalUserId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User with global ID " + globalUserId + " not found");
        }
        
        User user = userOpt.get();
        user.setRole(role);
        userRepository.save(user);
        
        logger.info("Updated role for global user ID: {} to role: {}", globalUserId, role);
    }

    /**
     * Updates an existing user's status without touching the password.
     * Called by the owning service whenever it activates, deactivates or suspends an account,
     * so that login here is denied for accounts the owning service has disabled. Any status
     * other than ACTIVE also revokes the user's refresh-token sessions, otherwise a disabled
     * account could keep rotating an existing session.
     *
     * @param globalUserId The global user ID
     * @param status The new status
     */
    @Transactional
    public void updateUserStatus(Long globalUserId, Status status) {
        logger.info("Updating status for global user ID: {} to {}", globalUserId, status);

        User user = userRepository.findById(globalUserId)
                .orElseThrow(() -> new RuntimeException("User with global ID " + globalUserId + " not found"));

        user.setStatus(status);
        userRepository.save(user);

        if (status != Status.ACTIVE) {
            refreshTokenService.revokeAllSessionsForUser(globalUserId);
            logger.info("Revoked all refresh-token sessions for user {} (status {})", globalUserId, status);
        }

        logger.info("Updated status for global user ID: {} to {}", globalUserId, status);
    }

    /**
     * Permanently removes a user's credentials. Used when the owning service deletes an account
     * outright (for example a vendor worker), so the credentials stop working and the email
     * becomes available for registration again.
     *
     * @param globalUserId The global user ID
     */
    @Transactional
    public void deleteUserAccount(Long globalUserId) {
        logger.info("Deleting authorization record for global user ID: {}", globalUserId);

        Optional<User> userOpt = userRepository.findById(globalUserId);
        if (userOpt.isEmpty()) {
            logger.info("No authorization record found for global user ID {}. Nothing to delete.", globalUserId);
            refreshTokenService.revokeAllSessionsForUser(globalUserId);
            return;
        }

        User user = userOpt.get();
        refreshTokenService.revokeAllSessionsForUser(globalUserId);
        passwordResetTokenRepository.findByUser(user).ifPresent(passwordResetTokenRepository::delete);
        userVerificationTokenRepository.findByUser(user).ifPresent(userVerificationTokenRepository::delete);
        userRepository.delete(user);

        logger.info("Deleted authorization record for global user ID: {} ({})", globalUserId, user.getEmail());
    }

    /**
     * Verifies a user by their global ID.
     * 
     * @param globalUserId The global user ID
     */
    @Transactional
    public void verifyUser(Long globalUserId) {
        logger.info("Verifying user with global ID: {}", globalUserId);
        
        Optional<User> userOpt = userRepository.findById(globalUserId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User with global ID " + globalUserId + " not found");
        }
        
        User user = userOpt.get();
        user.setStatus(Status.ACTIVE);
        userRepository.save(user);
        
        logger.info("Verified user with global ID: {}", globalUserId);
    }

    /**
     * Gets user information by global ID.
     * 
     * @param globalUserId The global user ID
     * @return User information
     */
    public User getUserByGlobalId(Long globalUserId) {
        return userRepository.findById(globalUserId)
                .orElseThrow(() -> new RuntimeException("User with global ID " + globalUserId + " not found"));
    }

    /**
     * Gets user information by email.
     * 
     * @param email User's email
     * @return User information
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User with email " + email + " not found"));
    }
}
