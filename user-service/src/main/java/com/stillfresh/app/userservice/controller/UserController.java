package com.stillfresh.app.userservice.controller;

import com.stillfresh.app.userservice.client.AuthorizationServiceClient;
import com.stillfresh.app.sharedentities.dto.CheckAvailabilityRequest;
import com.stillfresh.app.sharedentities.dto.OfferDto;
import com.stillfresh.app.sharedentities.responses.ApiResponse;
import com.stillfresh.app.sharedentities.responses.ErrorResponse;
import com.stillfresh.app.userservice.dto.PasswordChangeRequest;
import com.stillfresh.app.userservice.dto.UpdateAddressRequest;
import com.stillfresh.app.userservice.dto.UpdateBirthdayRequest;
import com.stillfresh.app.userservice.dto.UpdateCountryRequest;
import com.stillfresh.app.userservice.dto.DeleteAccountRequest;
import com.stillfresh.app.userservice.dto.UpdateDietaryPreferenceRequest;
import com.stillfresh.app.userservice.dto.UpdateNameRequest;
import com.stillfresh.app.userservice.dto.UpdatePhoneRequest;
import com.stillfresh.app.userservice.model.PasswordResetToken;
import com.stillfresh.app.userservice.model.User;
import com.stillfresh.app.userservice.security.CustomUserDetails;
import com.stillfresh.app.userservice.service.EmailService;
import com.stillfresh.app.userservice.service.UserService;
import com.stillfresh.app.userservice.repository.PasswordResetTokenRepository;
import com.stillfresh.app.sharedentities.order.events.OrderRequestEvent;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
        ApiResponse availabilityResponse = authorizationServiceClient.checkAvailability(
            new CheckAvailabilityRequest(user.getUsername(), user.getEmail()));

        // If the response indicates a conflict (username/email already taken)
        if (availabilityResponse == null || !availabilityResponse.isSuccess()) {
            // Return the conflict response
            return ResponseEntity.status(HttpStatus.CONFLICT).body(availabilityResponse);
        }

        try {
            // If available, proceed to initiate registration
            userService.registerUser(user);
            return ResponseEntity.ok(new ApiResponse(true, "User registration initiated. Check your email for verification."));
        } catch (IllegalStateException dup) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiResponse(false, dup.getMessage()));
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

    @Operation(
        summary = "Update user name",
        description = "Updates the authenticated user's first and last name."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER')")
    @PutMapping("/profile/name")
    public ResponseEntity<User> updateName(@RequestBody UpdateNameRequest request) {
        User updatedUser = userService.updateName(request.getFirstName(), request.getLastName());
        return ResponseEntity.ok(updatedUser);
    }

    @Operation(
        summary = "Update user address",
        description = "Updates the authenticated user's address."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER')")
    @PutMapping("/profile/address")
    public ResponseEntity<User> updateAddress(@RequestBody UpdateAddressRequest request) {
        User updatedUser = userService.updateAddress(request.getAddress());
        return ResponseEntity.ok(updatedUser);
    }

    @Operation(
        summary = "Update user country",
        description = "Updates the authenticated user's country."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER')")
    @PutMapping("/profile/country")
    public ResponseEntity<User> updateCountry(@RequestBody UpdateCountryRequest request) {
        User updatedUser = userService.updateCountry(request.getCountry());
        return ResponseEntity.ok(updatedUser);
    }

    @Operation(
        summary = "Update user birthday",
        description = "Updates the authenticated user's birthday (ISO-8601 format yyyy-MM-dd)."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER')")
    @PutMapping("/profile/birthday")
    public ResponseEntity<?> updateBirthday(@RequestBody UpdateBirthdayRequest request) {
        try {
            LocalDate birthday = request.getBirthday() != null && !request.getBirthday().isEmpty()
                ? LocalDate.parse(request.getBirthday())
                : null;
            User updatedUser = userService.updateBirthday(birthday);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid birthday format. Expected yyyy-MM-dd.");
        }
    }

    @Operation(
        summary = "Update user dietary preference",
        description = "Updates the authenticated user's dietary preference."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER')")
    @PutMapping("/profile/dietary-preference")
    public ResponseEntity<User> updateDietaryPreference(@RequestBody UpdateDietaryPreferenceRequest request) {
        User updatedUser = userService.updateDietaryPreference(request.getDietaryPreference());
        return ResponseEntity.ok(updatedUser);
    }

    @Operation(
        summary = "Update user phone number",
        description = "Updates the authenticated user's mobile phone number."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER')")
    @PutMapping("/profile/phone")
    public ResponseEntity<User> updatePhone(@RequestBody UpdatePhoneRequest request) {
        User updatedUser = userService.updatePhoneNumber(request.getPhoneNumber());
        return ResponseEntity.ok(updatedUser);
    }

    @Operation(
        summary = "Update current user profile (partial)",
        description = "Updates the authenticated user's profile with the provided fields. Only optional profile fields are applied (firstName, lastName, address, country, birthday, dietaryPreference, phoneNumber). Send only the fields you want to update."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER')")
    @PutMapping
    public ResponseEntity<User> updateProfile(@RequestBody User partial) {
        User updatedUser = userService.updateProfileFromRequest(partial);
        return ResponseEntity.ok(updatedUser);
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
        
        logger.info("Password reset email sent to user id: {}", user.getId());
        
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
    	logger.info("Password reset token received");
    	
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
        description = "Deletes the user's account and invalidates their token. Optional body: reason (e.g. other, too_expensive, not_using, privacy) and message for feedback."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteUser(HttpServletRequest request,
            @RequestBody(required = false) DeleteAccountRequest body) {
        String authHeader = request.getHeader("Authorization");
        String jwt = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
        }
        return userService.deleteUserProfile(jwt, body);
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

    // ========== FAVORITES ENDPOINTS ==========

    @Autowired
    private com.stillfresh.app.userservice.service.FavoriteService favoriteService;

    @Operation(
        summary = "Add offer to favorites",
        description = "Adds an offer to the authenticated user's favorites list. Idempotent - if already favorited, returns existing favorite."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/favorites/{offerId}")
    public ResponseEntity<com.stillfresh.app.userservice.dto.FavoriteResponse> addFavorite(
            @PathVariable Long offerId) {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getUser().getId();
        
        com.stillfresh.app.userservice.dto.FavoriteResponse response = favoriteService.addFavorite(userId, offerId);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Remove offer from favorites",
        description = "Removes an offer from the authenticated user's favorites list."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/favorites/{offerId}")
    public ResponseEntity<String> removeFavorite(@PathVariable Long offerId) {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getUser().getId();
        
        favoriteService.removeFavorite(userId, offerId);
        return ResponseEntity.ok("Offer removed from favorites successfully");
    }

    @Operation(
        summary = "Get user's favorites",
        description = "Retrieves offers that the authenticated user has favorited, with full offer details. " +
                "Each offer includes isExpired, isSoldOut, isGreyedOut so expired/sold-out favorites can be marked and removed. " +
                "Response includes expiredCount and soldOutCount for the returned set (full list if unpaginated, current page if paginated)."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/favorites")
    public ResponseEntity<com.stillfresh.app.userservice.dto.FavoritesListResponse> getFavorites(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getUser().getId();

        com.stillfresh.app.userservice.dto.FavoritesListResponse response;
        if (page == 0 && size == Integer.MAX_VALUE) {
            response = favoriteService.getUserFavoritesWithSummary(userId);
        } else {
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
            response = favoriteService.getUserFavoritesWithSummary(userId, pageable);
        }
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get favorites summary",
        description = "Returns total count and counts of expired/sold-out offers in favorites. " +
                "Use this to notify the user e.g. 'You have N expired offers in your favorites.'"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/favorites/summary")
    public ResponseEntity<com.stillfresh.app.userservice.dto.FavoritesListResponse> getFavoritesSummary() {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getUser().getId();
        com.stillfresh.app.userservice.dto.FavoritesListResponse response = favoriteService.getFavoritesSummary(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Check if offer is favorited",
        description = "Checks if a specific offer is in the authenticated user's favorites list."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/favorites/{offerId}")
    public ResponseEntity<Map<String, Object>> checkFavorite(@PathVariable Long offerId) {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getUser().getId();
        
        boolean isFavorited = favoriteService.isFavorited(userId, offerId);
        return ResponseEntity.ok(Map.of("isFavorited", isFavorited, "offerId", offerId));
    }

    @Operation(
        summary = "Get favorites count",
        description = "Returns the total number of offers the authenticated user has favorited."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/favorites/count")
    public ResponseEntity<Map<String, Long>> getFavoriteCount() {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getUser().getId();
        
        long count = favoriteService.getFavoriteCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @Operation(
        summary = "Create OAuth2 user",
        description = "Creates a user in user-service with a pre-set global ID. Used by authorization-service for OAuth2 registration."
    )
    @PostMapping("/create-oauth2")
    public ResponseEntity<?> createOAuth2User(@RequestBody User user) {
        try {
            User createdUser = userService.createOAuth2User(user);
            // Return only the identifier the caller needs rather than the whole entity.
            return ResponseEntity.ok(Map.of("id", createdUser.getId()));
        } catch (Exception ex) {
            logger.error("Failed to create OAuth2 user: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to create OAuth2 user: " + ex.getMessage()));
        }
    }

}
