package com.stillfresh.app.authorizationservice.service;

import com.stillfresh.app.authorizationservice.client.UserServiceClient;
import com.stillfresh.app.authorizationservice.model.OAuth2LoginResult;
import com.stillfresh.app.authorizationservice.model.User;
import com.stillfresh.app.authorizationservice.repository.UserRepository;
import com.stillfresh.app.authorizationservice.security.CustomUserDetails;
import com.stillfresh.app.authorizationservice.security.JwtUtil;
import com.stillfresh.app.sharedentities.enums.Role;
import com.stillfresh.app.sharedentities.enums.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class OAuth2Service {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2Service.class);
    private static final String GOOGLE_TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private UserService userService;

    @Autowired(required = false)
    private UserServiceClient userServiceClient;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired(required = false)
    private EmailService emailService;

    /**
     * Validate Google ID token and return user info
     */
    public Map<String, Object> validateGoogleIdToken(String idToken) {
        try {
            String url = GOOGLE_TOKEN_INFO_URL + idToken;
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("Failed to validate Google ID token");
            }
        } catch (Exception e) {
            logger.error("Error validating Google ID token", e);
            throw new RuntimeException("Invalid Google ID token: " + e.getMessage());
        }
    }

    /**
     * Process OAuth2 login from Google ID token (for mobile apps)
     */
    @Transactional
    public OAuth2LoginResult processOAuth2LoginFromToken(Map<String, Object> googleUserInfo, Role role) {
        String email = (String) googleUserInfo.get("email");
        String googleId = (String) googleUserInfo.get("sub"); // Google user ID

        if (email == null || email.isEmpty()) {
            throw new RuntimeException("Email not provided by Google");
        }

        GoogleProfileMapper.GoogleProfile profile = GoogleProfileMapper.fromGoogleUserInfo(googleUserInfo);
        logger.info("Processing OAuth2 login from token for email: {}, role: {}", email, role);

        return processOAuth2LoginInternal(email, googleId, role, profile);
    }

    /**
     * Process OAuth2 login/registration for Google (for web OAuth2 flow)
     */
    @Transactional
    public OAuth2LoginResult processOAuth2Login(OAuth2User oauth2User, Role role) {
        String email = oauth2User.getAttribute("email");
        String googleId = oauth2User.getAttribute("sub"); // Google user ID

        if (email == null || email.isEmpty()) {
            throw new RuntimeException("Email not provided by Google");
        }

        GoogleProfileMapper.GoogleProfile profile = GoogleProfileMapper.fromGoogleUserInfo(oauth2User.getAttributes());
        logger.info("Processing OAuth2 login for email: {}, role: {}", email, role);

        return processOAuth2LoginInternal(email, googleId, role, profile);
    }

    /**
     * Internal method to process OAuth2 login
     */
    private OAuth2LoginResult processOAuth2LoginInternal(String email, String googleId, Role role, GoogleProfileMapper.GoogleProfile profile) {
        // Check if user exists by OAuth2 provider ID
        Optional<User> existingUserByOAuth = userRepository.findByOauth2ProviderAndOauth2ProviderId("GOOGLE", googleId);
        
        // Check if user exists by email
        Optional<User> existingUserByEmail = userRepository.findByEmail(email);

        User user;

        if (existingUserByOAuth.isPresent()) {
            // User already registered with Google OAuth2
            user = existingUserByOAuth.get();
            logger.info("Found existing OAuth2 user: {}", email);
        } else if (existingUserByEmail.isPresent()) {
            // User exists with this email but not via OAuth2 - link the accounts
            user = existingUserByEmail.get();
            user.setOauth2Provider("GOOGLE");
            user.setOauth2ProviderId(googleId);
            userRepository.save(user);
            logger.info("Linked existing user account with Google OAuth2: {}", email);
            syncUserServiceProfileAsync(user, profile);
        } else {
            // New user - register via OAuth2
            user = registerOAuth2User(email, googleId, role, profile);
            logger.info("Registered new user via Google OAuth2: {}", email);
        }

        boolean accountWasDeleted = false;
        if (user.getStatus() == Status.DELETED) {
            userService.reactivateIfDeleted(user);
            user = userRepository.findById(user.getId()).orElse(user);
            accountWasDeleted = true;
        }

        // Ensure user is active (OAuth2 users don't need email verification)
        if (user.getStatus() != Status.ACTIVE) {
            user.setStatus(Status.ACTIVE);
            userRepository.save(user);
        }

        // Generate access + refresh tokens to match the standard login response format.
        UserDetails userDetails = new CustomUserDetails(user);
        String accessJwt = jwtUtil.generateAccessToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);
        refreshTokenService.storeRefreshToken(refreshToken);
        return new OAuth2LoginResult(accessJwt, refreshToken, accountWasDeleted);
    }

    /**
     * Register a new user via OAuth2. Single save path: create user once with all fields (no generateUserId + updateUserCredentials).
     */
    private User registerOAuth2User(String email, String googleId, Role role, GoogleProfileMapper.GoogleProfile profile) {
        String nameForUsername = buildDisplayName(profile);
        String username = generateUsername(email, nameForUsername);

        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setOauth2Provider("GOOGLE");
        user.setOauth2ProviderId(googleId);
        user.setRole(role);
        user.setStatus(Status.ACTIVE);
        user.setPassword(UUID.randomUUID().toString()); // unused for OAuth2; set for DB consistency

        user = userRepository.save(user);

        // Cache logged user (synchronous; Kafka is fast)
        userService.cacheLoggedUser(user);

        // Run user-service creation and welcome email asynchronously so JWT can be returned immediately
        final User userRef = user;
        final String welcomeName = nameForUsername != null ? nameForUsername : username;
        CompletableFuture.runAsync(() -> {
            syncUserServiceProfile(userRef, profile);
            if (emailService != null) {
                try {
                    emailService.sendWelcomeEmail(userRef.getEmail(), welcomeName);
                    logger.info("Welcome email sent to new Google OAuth2 user: {}", userRef.getEmail());
                } catch (Exception e) {
                    logger.error("Failed to send welcome email to new Google OAuth2 user: {}", userRef.getEmail(), e);
                }
            }
        });

        return user;
    }

    private void syncUserServiceProfileAsync(User user, GoogleProfileMapper.GoogleProfile profile) {
        CompletableFuture.runAsync(() -> syncUserServiceProfile(user, profile));
    }

    private void syncUserServiceProfile(User user, GoogleProfileMapper.GoogleProfile profile) {
        if (userServiceClient == null) {
            return;
        }
        try {
            UserServiceClient.OAuth2UserRequest oauth2UserRequest = new UserServiceClient.OAuth2UserRequest(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                // No password: Google users authenticate here, and user-service generates its own
                // unguessable placeholder. Sending the hash would put it on the wire and into
                // Feign request logs for no benefit.
                null,
                user.getRole().name(),
                user.getStatus().name(),
                profile.firstName(),
                profile.lastName(),
                profile.country()
            );
            ResponseEntity<?> response = userServiceClient.createOAuth2User(oauth2UserRequest);
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Synced Google profile to user-service for user ID: {}", user.getId());
            } else {
                logger.warn("Failed to sync Google profile to user-service: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Failed to sync Google profile to user-service: {}", e.getMessage(), e);
        }
    }

    private String buildDisplayName(GoogleProfileMapper.GoogleProfile profile) {
        if (profile.firstName() != null && profile.lastName() != null) {
            return profile.firstName() + " " + profile.lastName();
        }
        if (profile.firstName() != null) {
            return profile.firstName();
        }
        return profile.lastName();
    }

    private static final int MAX_USERNAME_UNIQUENESS_CHECKS = 5;

    /**
     * Generate a unique username from email or name. Capped iterations then random suffix to limit DB round-trips.
     */
    private String generateUsername(String email, String name) {
        String baseUsername;
        if (name != null && !name.isEmpty()) {
            String processedName = name.toLowerCase().replaceAll("[^a-z0-9]", "");
            int maxLength = Math.min(processedName.length(), 20);
            baseUsername = processedName.substring(0, maxLength);
        } else {
            baseUsername = email.split("@")[0];
        }

        String username = baseUsername;
        int counter = 1;
        while (userRepository.existsByUsername(username)) {
            if (counter > MAX_USERNAME_UNIQUENESS_CHECKS) {
                do {
                    username = baseUsername + "_" + UUID.randomUUID().toString().substring(0, 8);
                } while (userRepository.existsByUsername(username));
                break;
            }
            username = baseUsername + counter;
            counter++;
        }
        return username;
    }
}
