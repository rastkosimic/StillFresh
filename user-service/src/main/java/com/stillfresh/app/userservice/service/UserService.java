package com.stillfresh.app.userservice.service;

import com.stillfresh.app.sharedentities.dto.OfferDto;
import com.stillfresh.app.sharedentities.enums.Role;
import com.stillfresh.app.sharedentities.enums.Status;
import com.stillfresh.app.sharedentities.exceptions.ResourceNotFoundException;
import com.stillfresh.app.sharedentities.offer.events.OfferRequestEvent;
import com.stillfresh.app.sharedentities.order.events.OrderRequestEvent;
import com.stillfresh.app.sharedentities.payment.events.UpdatePaymentServiceEvent;
import com.stillfresh.app.sharedentities.user.events.UpdateUserProfileEvent;
import com.stillfresh.app.sharedentities.user.events.UserRegisteredEvent;
import com.stillfresh.app.sharedentities.user.events.UserVerifiedEvent;
import com.stillfresh.app.userservice.client.AuthorizationServiceClient;
import com.stillfresh.app.userservice.dto.PasswordChangeRequest;
import com.stillfresh.app.userservice.dto.DeleteAccountRequest;
import com.stillfresh.app.userservice.listener.AvailableOfferListener;
import com.stillfresh.app.userservice.model.DeletionFeedback;
import com.stillfresh.app.userservice.model.User;
import com.stillfresh.app.userservice.security.CustomUserDetails;
import com.stillfresh.app.userservice.model.VerificationToken;
import com.stillfresh.app.userservice.publisher.UserEventPublisher;
import com.stillfresh.app.sharedentities.user.events.PasswordUpdateEvent;
import com.stillfresh.app.userservice.repository.DeletionFeedbackRepository;
import com.stillfresh.app.userservice.repository.UserRepository;
import com.stillfresh.app.userservice.repository.VerificationTokenRepository;
import com.stillfresh.app.userservice.security.JwtUtil;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

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

    /** Timeout (ms) for waiting for AvailableOffersEvent after publishing OfferRequestEvent. Kept low given offer-service uses DB bounding-box queries. */
    private static final long NEARBY_OFFERS_RESPONSE_TIMEOUT_MS = 3000L;

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
    
    @Autowired    
    private AvailableOfferListener availableOfferListener;
    
    @Autowired
    private AuthorizationServiceClient authorizationServiceClient;

    @Autowired(required = false)
    private FavoriteService favoriteService;

    @Autowired
    private DeletionFeedbackRepository deletionFeedbackRepository;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    
    @Caching(evict = {
        @CacheEvict(value = "users", key = "#user.username"),
        @CacheEvict(value = "users", key = "#user.email")
    })
    public User registerUser(User user) throws IOException {

        // Local guard to prevent duplicate registration even if upstream availability check is skipped/races.
        if (userRepository.existsByEmail(user.getEmail()) || userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalStateException("User already exists with this email or username");
        }
        
        // 1. First, get global user ID from authorization service
        logger.info("Requesting global user ID for user: {}", user.getEmail());
        Map<String, Object> idResponse = authorizationServiceClient.generateUserId(
            new AuthorizationServiceClient.UserIdRequest(
                user.getEmail(), 
                user.getUsername(), 
                Role.USER
            )
        );
        
        if (!(Boolean) idResponse.get("success")) {
            throw new RuntimeException("Failed to generate global user ID: " + idResponse.get("message"));
        }
        
        Long globalUserId = ((Number) idResponse.get("globalUserId")).longValue();
        logger.info("Received global user ID: {} for user: {}", globalUserId, user.getEmail());
        
        // 2. Set the global ID and encode password
        user.setId(globalUserId); // Override auto-generation with global ID
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        user.setRole(Role.USER);  // Default role
        user.setStatus(Status.INACTIVE);

        // Record legal acceptance. The client sends the version of the document it displayed;
        // we stamp the acceptance timestamp server-side so it cannot be spoofed.
        LocalDateTime acceptedAt = LocalDateTime.now();
        if (user.getTermsVersion() != null && !user.getTermsVersion().isBlank()) {
            user.setTermsAcceptedAt(acceptedAt);
        }
        if (user.getPrivacyVersion() != null && !user.getPrivacyVersion().isBlank()) {
            user.setPrivacyAcceptedAt(acceptedAt);
        }
        
        logger.info("Registering user with username: {} and global ID: {}", user.getUsername(), globalUserId);
        userRepository.save(user);
        
        // 3. Update credentials in authorization service
        logger.info("Updating credentials in authorization service for global user ID: {}", globalUserId);
        Map<String, Object> credentialResponse = authorizationServiceClient.updateUserCredentials(
            new com.stillfresh.app.sharedentities.dto.UpdateUserCredentialsRequest(
                globalUserId, encodedPassword, Status.INACTIVE)
        );
        
        if (!(Boolean) credentialResponse.get("success")) {
            logger.error("Failed to update credentials in authorization service: {}", credentialResponse.get("message"));
            // Don't throw exception here as user is already saved locally
        }
        
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
        
        // Update authorization service with verification
        logger.info("Verifying user in authorization service with global user ID: {}", user.getId());
        Map<String, Object> verifyResponse = authorizationServiceClient.verifyUser(user.getId());
        
        if (!(Boolean) verifyResponse.get("success")) {
            logger.error("Failed to verify user in authorization service: {}", verifyResponse.get("message"));
            // Don't throw exception here as user is already verified locally
        }
        
      //Creating an event that will be utilized by authorization-service
        eventPublisher.publishUserVerifiedEvent(new UserVerifiedEvent(user.getEmail()));
    		
        return true;
    }
    
    /**
     * Cache user on login.
     * Only caches if user exists (prevents caching null values which Redis doesn't allow).
     */
    @CachePut(value = "users", key = "#email", unless = "#result == null")
    public User cacheUserOnLogin(String email) {
    	Optional<User> userOptional = findByEmail(email);
    	return userOptional.orElse(null); // Return null if not found, but @CachePut with unless will prevent caching null
    }
    
    public void updateUser(User updatedUser) {
        User currentUser = getUserFromContext();
        
        String oldUsername = currentUser.getUsername();

        currentUser.setUsername(updatedUser.getUsername());
        // Add other fields as needed
        userRepository.save(currentUser);
        
        //Creating an event that will be utilized by authorization-service
        eventPublisher.publishUpdateUserProfileEvent(new UpdateUserProfileEvent(currentUser.getUsername(), currentUser.getEmail(), currentUser.getPassword(), currentUser.getRole(), currentUser.getStatus()));
        
        //Creating and event that updates username in payment-service data table
        eventPublisher.publishPaymentServiceUpdateEvent(new UpdatePaymentServiceEvent(oldUsername, currentUser.getUsername()));
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
    
    private String extractTokenFromContext() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getDetails() == null) {
            throw new RuntimeException("No authentication found in context");
        }
        
        String authorizationHeader = authentication.getDetails().toString();
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid Authorization header");
        }
        return authorizationHeader.substring(7); // Remove "Bearer " prefix
    }

    /**
     * Resolves the current user from the security context.
     * When the request was authenticated by the API Gateway (GatewayTrustFilter), the principal
     * is CustomUserDetails and we use it directly. Otherwise we fall back to extracting the JWT
     * from the request and loading the user by email.
     */
    public User getUserFromContext() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new RuntimeException("No authentication found in context");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getUser();
        }
        // Fallback: extract JWT from details (e.g. when not behind gateway).
        // Read straight from the repository rather than the Redis-backed findByEmail cache:
        // the password is WRITE_ONLY, so a cached copy deserializes without it, and callers of
        // this method persist the entity and publish it to authorization-service.
        String jwt = extractTokenFromContext();
        String email = jwtUtil.extractEmail(jwt);
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @CacheEvict(value = "users", allEntries = true)
    public User updateName(String firstName, String lastName) {
        User user = getUserFromContext();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        return userRepository.save(user);
    }

    @CacheEvict(value = "users", allEntries = true)
    public User updateAddress(String address) {
        User user = getUserFromContext();
        user.setAddress(address);
        return userRepository.save(user);
    }

    @CacheEvict(value = "users", allEntries = true)
    public User updateCountry(String country) {
        User user = getUserFromContext();
        user.setCountry(country);
        return userRepository.save(user);
    }

    @CacheEvict(value = "users", allEntries = true)
    public User updateBirthday(LocalDate birthday) {
        User user = getUserFromContext();
        user.setBirthday(birthday);
        return userRepository.save(user);
    }

    @CacheEvict(value = "users", allEntries = true)
    public User updateDietaryPreference(String dietaryPreference) {
        User user = getUserFromContext();
        user.setDietaryPreference(dietaryPreference);
        return userRepository.save(user);
    }

    @CacheEvict(value = "users", allEntries = true)
    public User updatePhoneNumber(String phoneNumber) {
        User user = getUserFromContext();
        user.setPhoneNumber(phoneNumber);
        return userRepository.save(user);
    }

    /**
     * Updates the current user's profile with only the allowed optional fields from the given user.
     * Used to support PUT /users when the client sends a partial or full profile body.
     */
    @CacheEvict(value = "users", allEntries = true)
    public User updateProfileFromRequest(User partial) {
        User user = getUserFromContext();
        if (partial.getFirstName() != null) user.setFirstName(partial.getFirstName());
        if (partial.getLastName() != null) user.setLastName(partial.getLastName());
        if (partial.getAddress() != null) user.setAddress(partial.getAddress());
        if (partial.getCountry() != null) user.setCountry(partial.getCountry());
        if (partial.getBirthday() != null) user.setBirthday(partial.getBirthday());
        if (partial.getDietaryPreference() != null) user.setDietaryPreference(partial.getDietaryPreference());
        if (partial.getPhoneNumber() != null) user.setPhoneNumber(partial.getPhoneNumber());
        return userRepository.save(user);
    }

    public User extractUserFromToken(String authorizationHeader) {
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
		
		  return cachedUser.get();
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
        logger.debug("Changing password for user id: {}", user.getId());
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
        
        logger.info("Password changed in user-service database for user ID: {}, email: {}", 
                   user.getId(), user.getEmail());

        // Publish password update event to Kafka for authorization-service and other services
        PasswordUpdateEvent passwordUpdateEvent = new PasswordUpdateEvent(
            user.getId(),
            user.getEmail(),
            encodedPassword,
            user.getRole()
        );
        eventPublisher.publishPasswordUpdateEvent(passwordUpdateEvent);

        return ResponseEntity.ok("Password changed successfully");
    }

    public void logoutAndInvalidateToken(String jwt) {
        long expiryDurationInMillis = jwtUtil.getExpirationTimeInMillis(jwt) - System.currentTimeMillis();
        tokenBlacklistService.addTokenToBlacklist(jwt, expiryDurationInMillis);

        SecurityContextHolder.clearContext();
    }

    @CacheEvict(value = "users", allEntries = true)
    public ResponseEntity<String> deleteUserProfile(String jwt, DeleteAccountRequest body) {
        User user = getUserFromContext();
        if (body != null) {
            String reason = body.getReason() != null ? body.getReason().trim() : null;
            String message = body.getMessage() != null && !body.getMessage().isBlank() ? body.getMessage().trim() : null;
            if (reason != null || message != null) {
                DeletionFeedback feedback = new DeletionFeedback(user.getId(), reason, message);
                deletionFeedbackRepository.save(feedback);
                logger.info("Saved deletion feedback for user {}: reason={}", user.getId(), reason);
            }
        }
        user.setStatus(Status.DELETED);
        userRepository.save(user);
        eventPublisher.publishUpdateUserProfileEvent(new UpdateUserProfileEvent(
            user.getUsername(), user.getEmail(), user.getPassword(), user.getRole(), Status.DELETED));
        
        // Clean up user's favorites
        try {
            if (favoriteService != null) {
                favoriteService.deleteAllFavorites(user.getId());
                logger.info("Deleted all favorites for user: {}", user.getId());
            }
        } catch (Exception e) {
            logger.warn("Failed to delete favorites for user {}: {}", user.getId(), e.getMessage());
            // Continue with user deletion even if favorites cleanup fails
        }
        
        // Invalidate the token, if provided
        if (jwt != null && !jwt.isEmpty()) {
            try {
                long expiryDurationInMillis = jwtUtil.getExpirationTimeInMillis(jwt) - System.currentTimeMillis();
                tokenBlacklistService.addTokenToBlacklist(jwt, expiryDurationInMillis);
            } catch (Exception e) {
                logger.warn("Failed to add token to blacklist during user deletion: {}", e.getMessage());
            }
        } else {
            logger.warn("No JWT provided to deleteUserProfile; skipping token blacklist.");
        }
        
        return ResponseEntity.ok("User profile deleted successfully");
    }

    public List<OfferDto> getNearbyOffers(double latitude, double longitude, double range) throws ExecutionException {
        String requestId = UUID.randomUUID().toString();
        logger.info("Generated requestId: {}", requestId);

        // Register the requestId in the pendingRequests map
        CompletableFuture<List<OfferDto>> future = new CompletableFuture<>();
        availableOfferListener.registerPendingRequest(requestId, future);

        // Publish the OfferRequestEvent
        eventPublisher.publishOfferRequestEvent(new OfferRequestEvent(requestId, latitude, longitude, range));

        try {
            // Wait for and retrieve the response (offer-service uses optimized bounding-box query)
            return availableOfferListener.getAvailableOffers(requestId, NEARBY_OFFERS_RESPONSE_TIMEOUT_MS);
        } catch (TimeoutException e) {
            logger.error("Timed out while waiting for AvailableOffersEvent for requestId: {}", requestId);
            future.completeExceptionally(e); // Cleanup the future
            throw new RuntimeException("Timeout while fetching offers");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while waiting for AvailableOffersEvent for requestId: {}", requestId);
            future.completeExceptionally(e); // Cleanup the future
            throw new RuntimeException("Interrupted while fetching offers");
        } finally {
            availableOfferListener.removePendingRequest(requestId); // Cleanup the map
        }
    }

    public void publishOrderRequest(Principal principal, OrderRequestEvent orderRequest) {
        try {
            logger.info("Publishing OrderRequestEvent: {}", orderRequest);
            User user = findUserByUsername(principal.getName());
            orderRequest.setUserId(user.getId());
            orderRequest.setUsername(user.getUsername());
            orderRequest.setCustomerEmail(user.getEmail());
            eventPublisher.publishOrderRequestEvent(orderRequest);
        } catch (Exception e) {
            logger.error("Failed to publish OrderRequestEvent: {}", e.getMessage());
            throw new RuntimeException("Failed to submit order request.");
        }
    }

    public void publishOrderRequest(OrderRequestEvent orderRequest) {
        User user = getUserFromContext();
        orderRequest.setUserId(user.getId());
        orderRequest.setCustomerEmail(user.getEmail());
        eventPublisher.publishOrderRequestEvent(orderRequest);
    }

    /**
     * Create a user with a pre-set global ID (for OAuth2 registration from authorization-service)
     * This method skips the normal registration flow and directly creates the user with the provided ID
     */
    @Caching(evict = {
        @CacheEvict(value = "users", key = "#user.username"),
        @CacheEvict(value = "users", key = "#user.email")
    })
    public User createOAuth2User(User user) {
        // Validate that user has an ID set
        if (user.getId() == null) {
            throw new RuntimeException("User ID must be set for OAuth2 user creation");
        }

        // Check if user already exists
        if (userRepository.existsByEmail(user.getEmail())) {
            User existingUser = userRepository.findByEmail(user.getEmail())
                .orElseThrow(() -> new RuntimeException("User exists but could not be retrieved"));
            if (mergeGoogleProfile(existingUser, user)) {
                existingUser = userRepository.save(existingUser);
                logger.info("Merged Google profile fields for existing OAuth2 user ID: {}", existingUser.getId());
            } else {
                logger.warn("User with email {} already exists, skipping OAuth2 user creation", user.getEmail());
            }
            return existingUser;
        }

        // Never take role or status from the request. This endpoint creates ordinary customer
        // accounts during Google sign-in; honouring a caller-supplied role would let it mint an
        // ADMIN or SUPER_ADMIN account.
        user.setRole(Role.USER);
        user.setStatus(Status.ACTIVE);

        // OAuth2 users authenticate through Google and have no usable password. Store a hash of
        // an unguessable value rather than a caller-supplied plaintext, so the column can never
        // hold something that would satisfy a password login.
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

        logger.info("Creating OAuth2 user with global ID: {}", user.getId());

        User savedUser = userRepository.save(user);
        
        logger.info("Successfully created OAuth2 user in user-service with ID: {}", savedUser.getId());
        
        return savedUser;
    }

    /**
     * Fills empty profile fields from Google without overwriting values the user already set.
     */
    private boolean mergeGoogleProfile(User existingUser, User googleProfile) {
        boolean changed = false;
        if (isBlank(existingUser.getFirstName()) && !isBlank(googleProfile.getFirstName())) {
            existingUser.setFirstName(googleProfile.getFirstName());
            changed = true;
        }
        if (isBlank(existingUser.getLastName()) && !isBlank(googleProfile.getLastName())) {
            existingUser.setLastName(googleProfile.getLastName());
            changed = true;
        }
        if (isBlank(existingUser.getCountry()) && !isBlank(googleProfile.getCountry())) {
            existingUser.setCountry(googleProfile.getCountry());
            changed = true;
        }
        return changed;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

}
