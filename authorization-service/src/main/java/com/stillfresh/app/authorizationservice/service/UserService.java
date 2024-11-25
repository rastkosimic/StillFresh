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

    
    // Vendor events handling
    public void registerVendor(VendorRegisteredEvent event) {
        if (userRepository.existsByEmail(event.getEmail())) {
            throw new RuntimeException("Vendor already registered with this email");
        }

        User user = new User();
        user.setEmail(event.getEmail());
        user.setPassword(event.getPassword()); //password already encoded in vendor service
        user.setRole(event.getRole());
        user.setStatus(event.getStatus());
        user.setUsername(event.getUsername());

        userRepository.save(user);
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
        if (userRepository.existsByEmail(event.getEmail())) {
            throw new RuntimeException("User already registered with this email");
        }

        User user = new User();
        user.setEmail(event.getEmail());
        user.setPassword(event.getPassword()); //password already encoded in vendor service
        user.setRole(event.getRole());
        user.setStatus(event.getStatus());
        user.setUsername(event.getUsername());

        userRepository.save(user);
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
            if (tokenBlacklistService.isTokenBlacklisted(token)) {
            	authorizationEventPublisher.publishTokenValidationResponseEvent(new TokenValidationResponseEvent(false, null, null, null, "Token is blacklisted"));
            	logger.error("Token is blacklisted for correlationId: {}", event.getCorrelationId());
           } else if (jwtUtil.isTokenExpired(token)) {
        	   authorizationEventPublisher.publishTokenValidationResponseEvent(new TokenValidationResponseEvent(false, null, null, null, "Token is expired"));
        	   logger.error("Token is expired for correlationId: {}", event.getCorrelationId());
            } else {
                String username = jwtUtil.extractUsername(token);
                String email = jwtUtil.extractEmail(token);
                authorizationEventPublisher.publishTokenValidationResponseEvent(new TokenValidationResponseEvent(true, username, email, correlationId, "Token is valid"));
         	   	logger.info("Token is valid for correlationId: {}", event.getCorrelationId());
               
            }
		} catch (Exception e) {
			authorizationEventPublisher.publishTokenValidationResponseEvent(new TokenValidationResponseEvent(false, null, null, null, e.getMessage()));
			logger.error("Token validation failed for correlationId {}: {}", event.getCorrelationId(), e.getMessage());
		}
		
	}
}
