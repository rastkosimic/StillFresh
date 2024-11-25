package com.stillfresh.app.userservice.service;

import com.stillfresh.app.sharedentities.enums.Role;
import com.stillfresh.app.sharedentities.enums.Status;
import com.stillfresh.app.sharedentities.exceptions.ResourceNotFoundException;
import com.stillfresh.app.sharedentities.shared.events.TokenRequestEvent;
import com.stillfresh.app.sharedentities.shared.events.TokenValidationResponseEvent;
import com.stillfresh.app.sharedentities.user.events.UpdateUserProfileEvent;
import com.stillfresh.app.sharedentities.user.events.UserRegisteredEvent;
import com.stillfresh.app.sharedentities.user.events.UserVerifiedEvent;
import com.stillfresh.app.userservice.dto.PasswordChangeRequest;
import com.stillfresh.app.userservice.model.User;
import com.stillfresh.app.userservice.model.VerificationToken;
import com.stillfresh.app.userservice.publisher.UserEventPublisher;
import com.stillfresh.app.userservice.repository.UserRepository;
import com.stillfresh.app.userservice.repository.VerificationTokenRepository;
import com.stillfresh.app.userservice.security.CustomUserDetails;
import com.stillfresh.app.userservice.security.JwtUtil;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;
    
    @Autowired
    private UserEventPublisher eventPublisher;
    
    @Autowired
    private VerificationTokenRepository verificationTokenRepository;
    
    @Autowired
    private EmailService emailService;
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    
    @Caching(evict = {
        @CacheEvict(value = "users", key = "#user.username"),
        @CacheEvict(value = "users", key = "#user.email")
    })
    public User registerUser(User user) throws IOException {
        
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(Role.USER);  // Default role
        user.setStatus(Status.INACTIVE);
        
        logger.info("Registering user with username: {}", user.getUsername());
        userRepository.save(user);
        
        // Generate and save verification token
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationTokenRepository.save(verificationToken);

        // Send verification email
        String verificationUrl = "http://localhost:8081/users/verify?token=" + token;
        emailService.sendVerificationEmail(user.getEmail(), verificationUrl);
        
        //Creating an event that will be utilized by authorization-service
        eventPublisher.publishUserRegisteredEvent(new UserRegisteredEvent(user.getEmail(), user.getPassword(), user.getStatus(), user.getRole(), user.getUsername()));

        
        return user;
    }

//    @CachePut(value = "users", key = "#user.id") komentarisano jer nisam siguran da li radi kada sam promenio verifyUser na boolean
    public boolean verifyUser(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));
        User user = verificationToken.getUser();
        user.setStatus(Status.ACTIVE);
        userRepository.save(user);
        
      //Creating an event that will be utilized by authorization-service
        eventPublisher.publishUserVerifiedEvent(new UserVerifiedEvent(user.getEmail()));
        		
        return true;
    }
    
    @CachePut(value = "users", key = "#email")
    public void cacheUserOnLogin(String email) {
    	findByEmail(email);
    }
    
    public void updateUser(User updatedUser) {
        CustomUserDetails customUserDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = customUserDetails.getUser();

        currentUser.setUsername(updatedUser.getUsername());
        // Add other fields as needed
        userRepository.save(currentUser);
        
        //Creating an event that will be utilized by authorization-service
        eventPublisher.publishUpdateUserProfileEvent(new UpdateUserProfileEvent(currentUser.getUsername(), currentUser.getEmail(), currentUser.getPassword(), currentUser.getRole(), currentUser.getStatus()));
        
    }
    
    @CacheEvict(value = "users", allEntries = true)
    public void saveAdminUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(Role.ADMIN);  // Assign ADMIN role
        
        logger.info("Registering ADMIN with username: {}", user.getUsername());
        
        userRepository.save(user);
    }
    
    @CacheEvict(value = "users", allEntries = true)
    public void assignAdminRole(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setRole(Role.ADMIN);
        
        logger.info("ADMIN role assigned to the user with username: {}", user.getUsername());
        
        userRepository.save(user);
    }
    
    @CacheEvict(value = "users", allEntries = true)
    public List<User> findAllUsers() {
        logger.info("Finding all users");
        return userRepository.findAll();
    }

    @Cacheable(value = "users", key = "#id", unless = "#result == null")
    public User findUserById(Long id) {
        Optional<User> user = userRepository.findById(id);
        logger.info("Finding a user {}, with id: {}", user.map(User::getUsername).orElse("Not found"), id);
        return user.orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    @Cacheable(value = "users", key = "#username")
    public User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }
    
    @Cacheable(value = "users", key = "#email", unless = "#result == null")
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    @CachePut(value = "users", key = "#user.id")
    public User updateUserProfile(Long userId, User updatedUser) {
        Optional<User> existingUserOptional = userRepository.findById(userId);
        if (existingUserOptional.isPresent()) {
            User existingUser = existingUserOptional.get();
            existingUser.setUsername(updatedUser.getUsername());
            existingUser.setEmail(updatedUser.getEmail());
            // Add any other fields that can be updated
            
            logger.info("Updated details for the user with id: {}", existingUser.getId());
            return userRepository.save(existingUser);
        } else {
            throw new ResourceNotFoundException("User not found");
        }
    }
    
    @CacheEvict(value = "users", key = "#user.id")
    public void changeUserPassword(User user, String newPassword) {
        // Encode the new password
        logger.info("USERNAME: {}", user.getUsername());
        logger.info("PASSWORD: {}", user.getPassword());
        logger.info("EMAIL: {}", user.getEmail());
        logger.info("ID: {}", user.getId());
        logger.info("ACTIVE: {}", user.isActive());
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        userRepository.save(user);
        logger.info("Password changed for user: {}", user.getUsername());
    }
    
    public ResponseEntity<String> changeUserPassword(User user, PasswordChangeRequest passwordChangeRequest) {
        if (!passwordEncoder.matches(passwordChangeRequest.getOldPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Old password is incorrect");
        }

        String encodedPassword = passwordEncoder.encode(passwordChangeRequest.getNewPassword());
        user.setPassword(encodedPassword);
        userRepository.save(user);

        return ResponseEntity.ok("Password changed successfully");
    }

    public void logoutAndInvalidateToken(String jwt) {
        long expiryDurationInMillis = jwtUtil.getExpirationTimeInMillis(jwt) - System.currentTimeMillis();
        tokenBlacklistService.addTokenToBlacklist(jwt, expiryDurationInMillis);

        SecurityContextHolder.clearContext();
    }

    @CacheEvict(value = "users", allEntries = true)
	public ResponseEntity<String> deleteUserProfile(String authorizationHeader) {
	    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
	        throw new RuntimeException("Invalid Authorization header");
	    }
	    String jwt = authorizationHeader.substring(7); // Remove "Bearer " prefix
	    
	    String email = jwtUtil.extractEmail(jwt);
		
	    // Retrieve the user from the cache
	    Optional<User> cachedUser = findByEmail(email);
	    if (cachedUser.isEmpty()) {
	        throw new RuntimeException("User not found in cache");
	    }

	    User user = cachedUser.get();

	    // Mark the user as deleted
	    user.setStatus(Status.DELETED);
	    userRepository.save(user);
        //letting know authorization-service about the status change String username, String email, String password, Role role, Status status
	    eventPublisher.publishUpdateUserProfileEvent(new UpdateUserProfileEvent(user.getUsername(), user.getEmail(), user.getPassword(), user.getRole(), user.getStatus()));
	    eventPublisher.publishTokenInvalidationRequest(new TokenRequestEvent(jwt, null));
        return ResponseEntity.ok("User deleted successfully");
	}

}
