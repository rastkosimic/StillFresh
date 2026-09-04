package com.stillfresh.app.vendorservice.controller;

import com.stillfresh.app.vendorservice.client.AuthorizationServiceClient;
import com.stillfresh.app.sharedentities.dto.CheckAvailabilityRequest;
import com.stillfresh.app.sharedentities.dto.OfferDto;
import com.stillfresh.app.sharedentities.responses.ApiResponse;
import com.stillfresh.app.sharedentities.responses.ErrorResponse;
import com.stillfresh.app.vendorservice.dto.DeleteVendorAccountRequest;
import com.stillfresh.app.vendorservice.dto.PasswordChangeRequest;
import com.stillfresh.app.vendorservice.dto.PendingVendorRegistrationRequest;
import com.stillfresh.app.vendorservice.dto.VendorCredentialsResponse;
import com.stillfresh.app.vendorservice.dto.VendorProfileUpdateRequest;
import com.stillfresh.app.vendorservice.dto.VendorTypeRequest;
import com.stillfresh.app.vendorservice.dto.HeadquartersRequest;
import com.stillfresh.app.vendorservice.dto.BankingModelRequest;
import com.stillfresh.app.vendorservice.dto.ChainLocationStatsResponse;
import com.stillfresh.app.vendorservice.dto.LocationRequest;
import com.stillfresh.app.vendorservice.dto.SwitchBankingModelRequest;
import com.stillfresh.app.vendorservice.dto.WorkerRequest;
import com.stillfresh.app.vendorservice.model.Vendor;
import com.stillfresh.app.vendorservice.security.CustomVendorDetails;
import com.stillfresh.app.vendorservice.service.VendorService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.stillfresh.app.sharedentities.enums.PayoutModel;
import com.stillfresh.app.sharedentities.enums.Role;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

@RestController
@RequestMapping("/vendors")
@Tag(name = "Vendor Management", description = "APIs for managing vendor accounts, profiles, and offers")
public class VendorController {

	private static final Logger logger = LoggerFactory.getLogger(VendorController.class);
	 
    @Autowired
    private VendorService vendorService;
    
    @Autowired
    private AuthorizationServiceClient authorizationServiceClient;
    
    @Operation(
        summary = "Submit vendor application (Public)",
        description = "Allows potential vendors to submit an application with contact information. Creates a pending vendor account that will be reviewed and activated by platform admins. No authentication required."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Application submitted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed - missing required fields"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email already exists"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/apply")
    public ResponseEntity<?> submitVendorApplication(
        @Valid @RequestBody PendingVendorRegistrationRequest request,
        BindingResult result) throws IOException {
        try {
            if (result.hasErrors()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Validation failed: " + result.getAllErrors().get(0).getDefaultMessage()));
            }
            
            // Check if email already exists
            if (vendorService.findByEmail(request.getEmail()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("Vendor with this email already exists: " + request.getEmail()));
            }
            
            vendorService.registerPendingVendor(request);
            return ResponseEntity.ok(new ApiResponse(true, 
                "Vendor application submitted successfully. Your application will be reviewed by our team. " +
                "You will receive credentials via email once your application is approved."));
        } catch (Exception ex) {
            logger.error("Error during vendor application submission: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(new ErrorResponse("Failed to submit application: " + ex.getMessage()));
        }
    }
    
    @Operation(
        summary = "Register a new vendor",
        description = "Creates a new vendor account and sends a verification email. The vendor must verify their email before they can log in. Required fields: username, email, address, phone, password, zipCode."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Registration initiated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed - missing required fields"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Username or email already exists"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/register")
    public ResponseEntity<?> registerVendor(@Valid @RequestBody Vendor vendor, BindingResult result) throws IOException {
        try {
            // Check for validation errors
            if (result.hasErrors()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Validation failed: " + result.getAllErrors().get(0).getDefaultMessage()));
            }
            
            // Call the authorization service to check availability
            ApiResponse availabilityResponse = authorizationServiceClient.checkAvailability(
                new CheckAvailabilityRequest(vendor.getUsername(), vendor.getEmail()));

            // Check if the username/email is unavailable
            if (availabilityResponse == null || !availabilityResponse.isSuccess()) {
                String message = availabilityResponse != null ? availabilityResponse.getMessage() : "Availability check failed";
                logger.info("Availability check failed: {}", message);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(availabilityResponse);
            }

            // Proceed with registration
            vendorService.registerVendor(vendor);
            return ResponseEntity.ok(new ApiResponse(true, "Vendor registration initiated. Check your email for verification."));
        } catch (IllegalStateException dup) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                                 .body(new ApiResponse(false, dup.getMessage()));
        } catch (Exception ex) {
            logger.error("Error during vendor registration: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(new ErrorResponse("Failed to initiate registration: " + ex.getMessage()));
        }
    }

    @Operation(
        summary = "Register a new admin",
        description = "Creates a new admin account. This endpoint is restricted to existing admins and super admins. Required fields: username, email, address, phone, password, zipCode."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/register-admin")
    public ResponseEntity<String> registerAdmin(@Valid @RequestBody Vendor vendor, BindingResult result) throws IOException {
        // Check for validation errors
        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Validation failed: " + result.getAllErrors().get(0).getDefaultMessage());
        }
        
        vendorService.registerVendor(vendor, true);  // True indicates admin registration
        return ResponseEntity.ok("Admin registration successful. Please verify your email.");
    }

    @Operation(
        summary = "Promote vendor to admin",
        description = "Promotes an existing vendor to admin role. This endpoint is restricted to existing admins and super admins."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PutMapping("/{id}/promote-to-admin")
    public ResponseEntity<String> promoteVendorToAdmin(
        @Parameter(description = "ID of the vendor to promote") @PathVariable Long id) {
        vendorService.promoteVendorToAdmin(id);
        return ResponseEntity.ok("Vendor promoted to admin successfully");
    }
    
    @Operation(
        summary = "Create new VENDOR_ADMIN",
        description = "Creates a new vendor account with VENDOR_ADMIN role. ADMIN and SUPER_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/register-vendor-admin")
    public ResponseEntity<?> registerVendorAdmin(@Valid @RequestBody Vendor vendor, BindingResult result) throws IOException {
        try {
            if (result.hasErrors()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Validation failed: " + result.getAllErrors().get(0).getDefaultMessage()));
            }
            
            // Call the authorization service to check availability
            ApiResponse availabilityResponse = authorizationServiceClient.checkAvailability(
                new CheckAvailabilityRequest(vendor.getUsername(), vendor.getEmail()));

            if (availabilityResponse == null || !availabilityResponse.isSuccess()) {
                String message = availabilityResponse != null ? availabilityResponse.getMessage() : "Availability check failed";
                logger.info("Availability check failed: {}", message);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(availabilityResponse);
            }

            // Register as VENDOR_ADMIN
            vendor.setRole(Role.VENDOR_ADMIN);
            vendorService.registerVendor(vendor);
            return ResponseEntity.ok(new ApiResponse(true, "VENDOR_ADMIN created successfully. Check email for verification."));
        } catch (IllegalStateException dup) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                                 .body(new ApiResponse(false, dup.getMessage()));
        } catch (Exception ex) {
            logger.error("Error during VENDOR_ADMIN creation: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(new ErrorResponse("Failed to create VENDOR_ADMIN: " + ex.getMessage()));
        }
    }
    
    @Operation(
        summary = "Promote vendor to VENDOR_ADMIN",
        description = "Promotes an existing vendor to VENDOR_ADMIN role. ADMIN and SUPER_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PutMapping("/{id}/promote-to-vendor-admin")
    public ResponseEntity<String> promoteVendorToVendorAdmin(
        @Parameter(description = "ID of the vendor to promote") @PathVariable Long id) {
        vendorService.promoteVendorToVendorAdmin(id);
        return ResponseEntity.ok("Vendor promoted to VENDOR_ADMIN successfully");
    }
    
    @Operation(
        summary = "Demote VENDOR_ADMIN to VENDOR",
        description = "Demotes a VENDOR_ADMIN to regular VENDOR role. ADMIN and SUPER_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PutMapping("/{id}/demote-from-vendor-admin")
    public ResponseEntity<String> demoteVendorAdminToVendor(
        @Parameter(description = "ID of the VENDOR_ADMIN to demote") @PathVariable Long id) {
        vendorService.demoteVendorAdminToVendor(id);
        return ResponseEntity.ok("VENDOR_ADMIN demoted to VENDOR successfully");
    }

    @Operation(
        summary = "Verify vendor email",
        description = "Verifies a vendor's email address using the token sent during registration."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Email verified successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid verification token")
    })
    @GetMapping("/verify")
    public ResponseEntity<String> verifyVendor(
        @Parameter(description = "Verification token sent via email") @RequestParam("token") String token) {
        boolean isVerified = vendorService.verifyVendor(token);
        if (isVerified) {
            return ResponseEntity.ok("Vendor verified successfully.");
        } else {
            return ResponseEntity.status(400).body("Invalid token.");
        }
    }
    
    @Operation(
        summary = "Request password reset",
        description = "Initiates the password reset process by sending a reset link to the vendor's email."
    )
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
        @Parameter(description = "Vendor's email address") @RequestParam String email) throws IOException {
        vendorService.sendPasswordResetLink(email);
        return ResponseEntity.ok("Password reset link sent to your email");
    }

    @Operation(
        summary = "Reset password",
        description = "Resets the vendor's password using the token received via email."
    )
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
        @Parameter(description = "Password reset token") @RequestParam("token") String token,
        @Parameter(description = "New password") @RequestBody String newPassword) {
        vendorService.resetPassword(token, newPassword);
        return ResponseEntity.ok("Password reset successfully");
    }
    
    @Operation(
        summary = "Get vendor by ID",
        description = "Retrieves vendor information by their ID."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('VENDOR', 'VENDOR_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<Vendor> getVendorById(
        @Parameter(description = "Vendor ID") @PathVariable Long id) {
    	Vendor user = vendorService.findVendorById(id);
        return ResponseEntity.ok(user);
    }
    
    @Operation(
        summary = "Get vendor info for login (internal)",
        description = "Internal endpoint for authorization-service to fetch vendor info during login. Returns simplified vendor info."
    )
    // Authorization is enforced by InternalServiceFilter and WebSecurityConfig, which require
    // the shared internal secret for /vendors/internal/**. The previous check here only tested
    // that an X-Internal-Service header was non-empty, which any caller could satisfy.
    @GetMapping("/internal/{id}/login-info")
    public ResponseEntity<Map<String, Object>> getVendorLoginInfo(
        @Parameter(description = "Vendor ID") @PathVariable Long id) {
        try {
            Vendor vendor = vendorService.findVendorById(id);
            
            Map<String, Object> vendorInfo = new java.util.HashMap<>();
            vendorInfo.put("id", vendor.getId());
            vendorInfo.put("email", vendor.getEmail());
            vendorInfo.put("isHeadquarters", vendor.getIsHeadquarters() != null ? vendor.getIsHeadquarters() : false);
            vendorInfo.put("isChainLocation", vendor.getIsChainLocation() != null ? vendor.getIsChainLocation() : false);
            vendorInfo.put("isUniqueVendor", vendor.getIsUniqueVendor() != null ? vendor.getIsUniqueVendor() : false);
            vendorInfo.put("chainName", vendor.getChainName());
            vendorInfo.put("locationName", vendor.getLocationName());
            vendorInfo.put("usesSharedPaymentAccount", vendor.getUsesSharedPaymentAccount() != null ? vendor.getUsesSharedPaymentAccount() : false);
            
            return ResponseEntity.ok(vendorInfo);
        } catch (Exception e) {
            logger.error("Error fetching vendor login info for ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    
    @Operation(
        summary = "Update vendor profile",
        description = "Updates the vendor's profile information. Requires authentication and logs out the user after successful update. VENDOR_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    @PutMapping("/update-profile")
    public ResponseEntity<String> updateVendorProfile(
        @Valid @RequestBody VendorProfileUpdateRequest updatedVendor,
        BindingResult result) {
  
        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.getAllErrors().get(0).getDefaultMessage());
        }
        vendorService.updateVendorProfile(updatedVendor);
        return ResponseEntity.ok("Vendor profile updated successfully. You are logged out.");
    }
    
    @Operation(
        summary = "Change vendor password",
        description = "Changes the vendor's password and logs them out of all sessions. VENDOR_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('VENDOR_ADMIN', 'VENDOR')")
    @PutMapping("/change-password")
    public ResponseEntity<String> changeVendorPassword(
        @Valid @RequestBody PasswordChangeRequest passwordChangeRequest,
        HttpServletRequest request,
        BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body(result.getAllErrors().get(0).getDefaultMessage());
        }

        CustomVendorDetails vendorDetails = (CustomVendorDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Vendor currentVendor = vendorDetails.getVendor();

        ResponseEntity<String> passwordChangeResponse = vendorService.changeVendorPassword(currentVendor, passwordChangeRequest);
        
        // If password change was successful, proceed to invalidate the token and log out
        if (passwordChangeResponse.getStatusCode().is2xxSuccessful()) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                vendorService.logoutAndInvalidateToken(jwt);
            }
        }

        return passwordChangeResponse;
    }
    
    @Operation(
        summary = "Delete vendor account",
        description = "Deletes the vendor's account and deactivates all associated offers. VENDOR_ADMIN only. Optional body: reason (e.g. other, too_expensive, not_using, privacy) and message for feedback."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteVendor(@RequestBody(required = false) DeleteVendorAccountRequest body) {
        return vendorService.deleteVendorProfile(body);
    }
    
    @Operation(
        summary = "Create new offer",
        description = "Creates a new offer for the authenticated vendor."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('VENDOR', 'VENDOR_ADMIN')")
    @PostMapping("/offer-create")
    public ResponseEntity<?> createOffer(@RequestBody OfferDto request) {
        try {
            vendorService.createOffer(request);
            return ResponseEntity.ok("Offer created successfully");
        } catch (IllegalArgumentException e) {
            logger.warn("Offer creation validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating offer: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create offer: " + e.getMessage()));
        }
    }
    
    @Operation(
        summary = "Get active offers",
        description = "Retrieves all active offers for the authenticated vendor."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('VENDOR', 'VENDOR_ADMIN')")
    @GetMapping("/active-offers")
    public ResponseEntity<List<OfferDto>> getActiveOffersForVendor() {
        return ResponseEntity.ok(vendorService.getActiveOffersForVendor());
    }
    
    @Operation(
        summary = "Get all offers",
        description = "Retrieves all offers (active and inactive) for the authenticated vendor."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('VENDOR', 'VENDOR_ADMIN')")
    @GetMapping("/all-offers")
    public ResponseEntity<List<OfferDto>> getAllOffersForVendor() {
        return ResponseEntity.ok(vendorService.getAllOffersForVendor());
    }

    @Operation(
        summary = "Get vendor stats",
        description = "Retrieves sales statistics for the authenticated vendor, with optional date range. VENDOR can only see their location stats, VENDOR_ADMIN can see all stats."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('VENDOR', 'VENDOR_ADMIN')")
    @GetMapping("/stats")
    public ResponseEntity<com.stillfresh.app.sharedentities.dto.VendorStatsResponse> getVendorStats(
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to) {
        try {
            java.time.OffsetDateTime fromDt = (from == null || from.isBlank()) ? null : java.time.OffsetDateTime.parse(from);
            java.time.OffsetDateTime toDt = (to == null || to.isBlank()) ? null : java.time.OffsetDateTime.parse(to);
            return ResponseEntity.ok(vendorService.getVendorStats(fromDt, toDt));
        } catch (java.time.format.DateTimeParseException ex) {
            logger.warn("Invalid date-time format for stats query. from='{}', to='{}'", from, to);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null);
        }
    }
    
    @Operation(
        summary = "Invalidate offer",
        description = "Deactivates an existing offer by its ID."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('VENDOR', 'VENDOR_ADMIN')")
    @PostMapping("/invalidate-offer/{offerId}")
    public ResponseEntity<String> invalidateOffer(
        @Parameter(description = "ID of the offer to deactivate") @PathVariable Long offerId) {
    	vendorService.invalidateOffer(offerId);
    	return ResponseEntity.ok("Offer deactivated successfully.");
    }
    
    @Operation(
        summary = "Update offer",
        description = "Updates an existing offer's information."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('VENDOR', 'VENDOR_ADMIN')")
    @PostMapping("/update-offer/{offerId}")
    public ResponseEntity<?> updateOffer(
        @Parameter(description = "ID of the offer to update") @PathVariable Long offerId,
        @RequestBody OfferDto request) {
        try {
            vendorService.updateOffer(offerId, request);
            return ResponseEntity.ok("Offer updated successfully");
        } catch (IllegalArgumentException e) {
            logger.warn("Offer update validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating offer: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to update offer: " + e.getMessage()));
        }
    }
    
    @GetMapping("/{id}/stripe-account-id")
    @Operation(
        summary = "Get vendor Stripe account ID",
        description = "Retrieves the Stripe Connect account ID for a vendor (internal service endpoint)"
    )
    public ResponseEntity<String> getVendorStripeAccountId(
            @Parameter(description = "Vendor ID") @PathVariable Long id) {
        try {
            Vendor vendor = vendorService.findVendorById(id);
            String stripeAccountId = vendor.getStripeAccountId();
            if (stripeAccountId == null || stripeAccountId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Stripe account not found for vendor ID: " + id);
            }
            return ResponseEntity.ok(stripeAccountId);
        } catch (Exception e) {
            logger.error("Error retrieving Stripe account ID for vendor: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving Stripe account ID: " + e.getMessage());
        }
    }

    @PostMapping("/clear-cache")
    public ResponseEntity<String> clearVendorCache() {
        vendorService.clearVendorCache(null, null);
        return ResponseEntity.ok("Vendor cache cleared successfully");
    }

    @GetMapping("/stripe/onboarding-link")
    @Operation(
        summary = "Get Stripe onboarding link",
        description = "Returns the Stripe Connect onboarding URL for the authenticated vendor to complete payment setup. VENDOR_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    public ResponseEntity<?> getStripeOnboardingLink() {
        try {
            String onboardingUrl = vendorService.getStripeOnboardingLink();
            if (onboardingUrl == null || onboardingUrl.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Stripe account not found. Please contact support."));
            }
            return ResponseEntity.ok(Map.of("onboardingUrl", onboardingUrl));
        } catch (Exception e) {
            logger.error("Error getting Stripe onboarding link: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to get onboarding link: " + e.getMessage()));
        }
    }

    @GetMapping("/stripe/account-status")
    @Operation(
        summary = "Get Stripe account status",
        description = "Returns the status of the authenticated vendor's Stripe Connect account (ready to receive payments or not). VENDOR_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    public ResponseEntity<?> getStripeAccountStatus() {
        try {
            boolean isReady = vendorService.isStripeAccountReady();
            return ResponseEntity.ok(Map.of("isReady", isReady, 
                                          "hasAccount", vendorService.hasStripeAccount(),
                                          "message", isReady ? 
                                              "Your Stripe account is ready to receive payments." : 
                                              "Your Stripe account is not ready. Please complete onboarding."));
        } catch (Exception e) {
            logger.error("Error checking Stripe account status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to check account status: " + e.getMessage()));
        }
    }

    @GetMapping("/stripe/return")
    @Operation(
        summary = "Stripe onboarding return handler",
        description = "Handles the redirect from Stripe after vendor completes onboarding. Checks account status and returns result."
    )
    public ResponseEntity<?> handleStripeReturn(
            @Parameter(description = "Optional: Frontend redirect URL after processing") 
            @RequestParam(value = "redirect", required = false) String redirectUrl) {
        try {
            logger.info("Stripe onboarding return callback received");
            
            // Try to get vendor from context (if authenticated)
            // If not authenticated, we'll need to handle it differently
            try {
                Vendor vendor = vendorService.getVendorFromContext();
                boolean isReady = vendorService.isStripeAccountReady();
                boolean hasAccount = vendorService.hasStripeAccount();
                
                logger.info("Vendor {} returned from Stripe onboarding. Account ready: {}, Has account: {}", 
                           vendor.getEmail(), isReady, hasAccount);
                
                Map<String, Object> response = Map.of(
                    "success", true,
                    "isReady", isReady,
                    "hasAccount", hasAccount,
                    "message", isReady ? 
                        "Your Stripe account has been successfully set up and is ready to receive payments!" : 
                        "Your Stripe onboarding is in progress. Please wait for verification to complete.",
                    "vendorEmail", vendor.getEmail()
                );
                
                // If redirect URL is provided, return a redirect response
                // Otherwise, return JSON (frontend can handle the redirect)
                if (redirectUrl != null && !redirectUrl.isEmpty()) {
                    // For now, return JSON with redirect info - frontend can handle actual redirect
                    return ResponseEntity.ok(Map.of(
                        "redirect", redirectUrl,
                        "status", response
                    ));
                }
                
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                // Vendor not authenticated - return generic success message
                logger.warn("Stripe return callback received but vendor not authenticated: {}", e.getMessage());
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Stripe onboarding completed. Please log in to check your account status.",
                    "requiresAuth", true
                ));
            }
        } catch (Exception e) {
            logger.error("Error handling Stripe return callback: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Error processing Stripe return: " + e.getMessage()));
        }
    }

    @GetMapping("/stripe/refresh")
    @Operation(
        summary = "Stripe onboarding refresh handler",
        description = "Handles the redirect from Stripe when vendor needs to refresh their onboarding session. Generates a new onboarding link. VENDOR_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    public ResponseEntity<?> handleStripeRefresh(
            @Parameter(description = "Optional: Frontend redirect URL after processing") 
            @RequestParam(value = "redirect", required = false) String redirectUrl) {
        try {
            logger.info("Stripe onboarding refresh callback received");
            
            Vendor vendor = vendorService.getVendorFromContext();
            
            // Generate a new onboarding link
            String onboardingUrl = vendorService.getStripeOnboardingLink();
            
            if (onboardingUrl == null || onboardingUrl.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Failed to generate new onboarding link. Please contact support."));
            }
            
            logger.info("Generated new onboarding link for vendor: {}", vendor.getEmail());
            
            Map<String, Object> response = Map.of(
                "success", true,
                "onboardingUrl", onboardingUrl,
                "message", "A new onboarding link has been generated. Please complete the onboarding process.",
                "vendorEmail", vendor.getEmail()
            );
            
            // If redirect URL is provided, return redirect info
            if (redirectUrl != null && !redirectUrl.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "redirect", redirectUrl,
                    "onboardingUrl", onboardingUrl,
                    "status", response
                ));
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error handling Stripe refresh callback: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Error processing Stripe refresh: " + e.getMessage()));
        }
    }

    @GetMapping("/stripe/account")
    @Operation(
        summary = "Get Stripe account details",
        description = "Returns detailed information about the authenticated vendor's Stripe Connect account. VENDOR_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    public ResponseEntity<?> getStripeAccountDetails() {
        try {
            Map<String, Object> accountDetails = vendorService.getStripeAccountDetails();
            return ResponseEntity.ok(accountDetails);
        } catch (RuntimeException e) {
            logger.error("Error retrieving Stripe account details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error retrieving Stripe account details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve account details: " + e.getMessage()));
        }
    }

    @PostMapping("/stripe/login-link")
    @Operation(
        summary = "Get Stripe dashboard login link",
        description = "Generates a login link for the vendor to access their Stripe Express Dashboard. VENDOR_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    public ResponseEntity<?> getStripeLoginLink() {
        try {
            String loginUrl = vendorService.getStripeLoginLink();
            return ResponseEntity.ok(Map.of("loginUrl", loginUrl));
        } catch (RuntimeException e) {
            logger.error("Error creating Stripe login link: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error creating Stripe login link: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to create login link: " + e.getMessage()));
        }
    }

    @GetMapping("/stripe/payouts")
    @Operation(
        summary = "Get Stripe payout history",
        description = "Returns the payout history for the authenticated vendor's Stripe account. VENDOR_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    public ResponseEntity<?> getStripePayouts(
            @Parameter(description = "Maximum number of payouts to retrieve (default: 10, max: 100)")
            @RequestParam(value = "limit", required = false) Integer limit) {
        try {
            List<Map<String, Object>> payouts = vendorService.getStripePayouts(limit);
            return ResponseEntity.ok(payouts);
        } catch (RuntimeException e) {
            logger.error("Error retrieving Stripe payouts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error retrieving Stripe payouts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve payouts: " + e.getMessage()));
        }
    }

    @GetMapping("/stripe/payouts/{payoutId}")
    @Operation(
        summary = "Get specific Stripe payout",
        description = "Returns details for a specific payout by ID for the authenticated vendor. VENDOR_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    public ResponseEntity<?> getStripePayout(
            @Parameter(description = "Payout ID")
            @PathVariable String payoutId) {
        try {
            Map<String, Object> payout = vendorService.getStripePayout(payoutId);
            return ResponseEntity.ok(payout);
        } catch (RuntimeException e) {
            logger.error("Error retrieving Stripe payout {}: {}", payoutId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error retrieving Stripe payout {}: {}", payoutId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve payout: " + e.getMessage()));
        }
    }

    @GetMapping("/stripe/balance")
    @Operation(
        summary = "Get Stripe account balance",
        description = "Returns the current balance information for the authenticated vendor's Stripe account. VENDOR_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    public ResponseEntity<?> getStripeBalance() {
        try {
            Map<String, Object> balance = vendorService.getStripeBalance();
            return ResponseEntity.ok(balance);
        } catch (RuntimeException e) {
            logger.error("Error retrieving Stripe balance: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error retrieving Stripe balance: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve balance: " + e.getMessage()));
        }
    }

    @GetMapping("/stripe/transactions")
    @Operation(
        summary = "Get Stripe transaction history",
        description = "Returns the transaction history for the authenticated vendor's Stripe account. VENDOR_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    public ResponseEntity<?> getStripeTransactions(
            @Parameter(description = "Maximum number of transactions to retrieve (default: 10, max: 100)")
            @RequestParam(value = "limit", required = false) Integer limit) {
        try {
            List<Map<String, Object>> transactions = vendorService.getStripeTransactions(limit);
            return ResponseEntity.ok(transactions);
        } catch (RuntimeException e) {
            logger.error("Error retrieving Stripe transactions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error retrieving Stripe transactions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve transactions: " + e.getMessage()));
        }
    }

    @GetMapping("/stripe/requirements")
    @Operation(
        summary = "Get Stripe verification requirements",
        description = "Returns the verification requirements for the authenticated vendor's Stripe account. VENDOR_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    public ResponseEntity<?> getStripeRequirements() {
        try {
            Map<String, Object> requirements = vendorService.getStripeRequirements();
            return ResponseEntity.ok(requirements);
        } catch (RuntimeException e) {
            logger.error("Error retrieving Stripe requirements: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error retrieving Stripe requirements: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve requirements: " + e.getMessage()));
        }
    }
    
    // ========== Payment Provider Management Endpoints (Hybrid: Stripe + Payoneer) ==========
    
    @GetMapping("/payment/status")
    @Operation(
        summary = "Get payment account status",
        description = "Returns the payment account status for the authenticated vendor. Works with both Stripe and Payoneer based on vendor's country. VENDOR_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment account status retrieved successfully",
            content = @Content(schema = @Schema(implementation = Map.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getPaymentAccountStatus() {
        try {
            logger.info("Getting payment account status for authenticated vendor");
            Map<String, Object> status = vendorService.getPaymentAccountStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Error getting payment account status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to get payment account status: " + e.getMessage()));
        }
    }
    
    @PostMapping("/payment/onboarding-link")
    @Operation(
        summary = "Get payment onboarding link",
        description = "Returns the onboarding link for the authenticated vendor's payment provider (Stripe or Payoneer). Creates account if it doesn't exist. VENDOR_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Onboarding link generated successfully",
            content = @Content(schema = @Schema(implementation = Map.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getPaymentOnboardingLink() {
        try {
            logger.info("Getting payment onboarding link for authenticated vendor");
            String onboardingUrl = vendorService.getPaymentOnboardingLink();
            return ResponseEntity.ok(Map.of(
                "onboardingUrl", onboardingUrl,
                "message", "Onboarding link generated successfully"
            ));
        } catch (Exception e) {
            logger.error("Error getting payment onboarding link: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to get onboarding link: " + e.getMessage()));
        }
    }
    
    // ========== MoR (Merchant of Record) Specific Endpoints ==========
    
    @GetMapping("/mor/balance")
    @Operation(
        summary = "Get MoR vendor balance",
        description = "Returns the current balance for MoR vendors (vendors in unsupported countries). VENDOR_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Balance retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Vendor is not using MoR model"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<?> getMoRBalance() {
        try {
            Map<String, Object> balance = vendorService.getMoRBalance();
            return ResponseEntity.ok(balance);
        } catch (RuntimeException e) {
            logger.error("Error retrieving MoR balance: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error retrieving MoR balance: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve balance: " + e.getMessage()));
        }
    }
    
    @GetMapping("/mor/transactions")
    @Operation(
        summary = "Get MoR balance transaction history",
        description = "Returns transaction history for MoR vendor balance. VENDOR_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    public ResponseEntity<?> getMoRTransactions(
            @Parameter(description = "Maximum number of transactions to retrieve (default: 50)")
            @RequestParam(value = "limit", required = false) Integer limit) {
        try {
            List<Map<String, Object>> transactions = vendorService.getMoRTransactions(limit);
            return ResponseEntity.ok(transactions);
        } catch (RuntimeException e) {
            logger.error("Error retrieving MoR transactions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error retrieving MoR transactions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve transactions: " + e.getMessage()));
        }
    }
    
    @GetMapping("/mor/bank-details")
    @Operation(
        summary = "Get bank details for MoR vendor",
        description = "Returns the authenticated MoR vendor's current bank details. Account number and IBAN are MASKED (last 4 characters visible). VENDOR_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bank details retrieved successfully",
            content = @Content(schema = @Schema(implementation = com.stillfresh.app.vendorservice.dto.MorBankDetailsResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Vendor is not using MoR model"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getMoRBankDetails() {
        try {
            com.stillfresh.app.vendorservice.dto.MorBankDetailsResponse details = vendorService.getMoRBankDetails();
            return ResponseEntity.ok(details);
        } catch (RuntimeException e) {
            logger.error("Error retrieving MoR bank details: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error retrieving MoR bank details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve bank details: " + e.getMessage()));
        }
    }

    @PutMapping("/mor/bank-details")
    @Operation(
        summary = "Submit bank details for MoR vendor",
        description = "Allows MoR vendors to submit their bank account details for manual payouts. VENDOR_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    public ResponseEntity<?> submitBankDetails(@RequestBody Map<String, String> bankDetails) {
        try {
            vendorService.submitBankDetails(bankDetails);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Bank details submitted successfully"
            ));
        } catch (RuntimeException e) {
            logger.error("Error submitting bank details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error submitting bank details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to submit bank details: " + e.getMessage()));
        }
    }
    
    @GetMapping("/mor/payouts")
    @Operation(
        summary = "Get MoR payout history",
        description = "Returns payout request history for MoR vendors. VENDOR_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    public ResponseEntity<?> getMoRPayouts() {
        try {
            List<Map<String, Object>> payouts = vendorService.getMoRPayouts();
            return ResponseEntity.ok(payouts);
        } catch (RuntimeException e) {
            logger.error("Error retrieving MoR payouts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error retrieving MoR payouts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve payouts: " + e.getMessage()));
        }
    }
    
    @PostMapping("/mor/request-payout")
    @Operation(
        summary = "Request manual payout",
        description = "Allows MoR vendors to request a manual payout from their balance. VENDOR_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    public ResponseEntity<?> requestMoRPayout(@RequestBody Map<String, Object> payoutRequest) {
        try {
            Long amount = Long.parseLong(payoutRequest.get("amount").toString());
            String currency = payoutRequest.get("currency") != null ? 
                payoutRequest.get("currency").toString() : "EUR";
            String description = payoutRequest.get("description") != null ? 
                payoutRequest.get("description").toString() : "Manual payout request";
            
            String payoutId = vendorService.requestMoRPayout(amount, currency, description);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "payoutId", payoutId,
                "message", "Payout request created successfully. It will be processed manually."
            ));
        } catch (RuntimeException e) {
            logger.error("Error requesting MoR payout: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error requesting MoR payout: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to request payout: " + e.getMessage()));
        }
    }
    
    // ========== Internal Endpoints (for payment-service) ==========
    
    @GetMapping("/{vendorId}/payout-model")
    @Operation(
        summary = "Get vendor payout model (internal)",
        description = "Returns the payout model for a vendor. Used by payment-service to determine payment processing."
    )
    public ResponseEntity<String> getVendorPayoutModel(@PathVariable Long vendorId) {
        try {
            Optional<Vendor> vendorOpt = vendorService.getVendorById(vendorId);
            if (vendorOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Vendor vendor = vendorOpt.get();
            PayoutModel payoutModel = vendor.getPayoutModel();
            
            if (payoutModel == null) {
                // Determine from country if not set
                String country = vendor.getCountry();
                if (country != null && !country.isEmpty()) {
                    payoutModel = vendorService.getPaymentProviderService().determinePayoutModel(country);
                } else {
                    payoutModel = PayoutModel.MOR; // Default to MoR
                }
            }
            
            return ResponseEntity.ok(payoutModel.toString());
        } catch (Exception e) {
            logger.error("Error getting payout model for vendor {}: {}", vendorId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/{vendorId}/mor/update-balance")
    @Operation(
        summary = "Update MoR vendor balance (internal)",
        description = "Updates the balance for a MoR vendor after successful payment. Called by payment-service."
    )
    public ResponseEntity<Map<String, Object>> updateMoRBalance(
            @PathVariable Long vendorId,
            @RequestParam Long amount,
            @RequestParam String currency,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String description) {
        try {
            vendorService.updateMoRBalance(vendorId, amount, currency, orderId, 
                description != null ? description : "Payment for order #" + (orderId != null ? orderId : "unknown"));
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Balance updated successfully"
            ));
        } catch (RuntimeException e) {
            logger.error("Error updating MoR balance for vendor {}: {}", vendorId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error updating MoR balance for vendor {}: {}", vendorId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Failed to update balance"));
        }
    }
    
    // ========== VENDOR_ADMIN Management Endpoints ==========
    
    @Operation(
        summary = "Get all vendors",
        description = "Retrieves a list of all registered vendors. VENDOR_ADMIN, ADMIN, and SUPER_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('VENDOR_ADMIN', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/admin/all-vendors")
    public ResponseEntity<List<Vendor>> getAllVendors() {
        List<Vendor> vendors = vendorService.getAllVendors();
        return ResponseEntity.ok(vendors);
    }
    
    @Operation(
        summary = "Create new vendor (VENDOR role)",
        description = "Creates a new vendor account with VENDOR role. VENDOR_ADMIN, ADMIN, and SUPER_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('VENDOR_ADMIN', 'ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/admin/create-vendor")
    public ResponseEntity<?> createVendor(@Valid @RequestBody Vendor vendor, BindingResult result) throws IOException {
        try {
            if (result.hasErrors()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Validation failed: " + result.getAllErrors().get(0).getDefaultMessage()));
            }
            
            // Call the authorization service to check availability
            ApiResponse availabilityResponse = authorizationServiceClient.checkAvailability(
                new CheckAvailabilityRequest(vendor.getUsername(), vendor.getEmail()));

            if (availabilityResponse == null || !availabilityResponse.isSuccess()) {
                String message = availabilityResponse != null ? availabilityResponse.getMessage() : "Availability check failed";
                logger.info("Availability check failed: {}", message);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(availabilityResponse);
            }

            // Register as regular VENDOR (not admin)
            vendorService.registerVendor(vendor);
            return ResponseEntity.ok(new ApiResponse(true, "Vendor created successfully. Check email for verification."));
        } catch (IllegalStateException dup) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                                 .body(new ApiResponse(false, dup.getMessage()));
        } catch (Exception ex) {
            logger.error("Error during vendor creation: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(new ErrorResponse("Failed to create vendor: " + ex.getMessage()));
        }
    }
    
    @Operation(
        summary = "Update vendor account",
        description = "Updates a vendor account. VENDOR_ADMIN, ADMIN, and SUPER_ADMIN only. ADMIN can manage VENDOR_ADMIN."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('VENDOR_ADMIN', 'ADMIN', 'SUPER_ADMIN')")
    @PutMapping("/admin/vendors/{id}")
    public ResponseEntity<?> updateVendor(
        @Parameter(description = "Vendor ID") @PathVariable Long id,
        @Valid @RequestBody Vendor updatedVendor,
        BindingResult result) {
        
        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Validation failed: " + result.getAllErrors().get(0).getDefaultMessage()));
        }
        
        try {
            vendorService.updateVendorById(id, updatedVendor);
            return ResponseEntity.ok("Vendor updated successfully");
        } catch (Exception e) {
            logger.error("Error updating vendor: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to update vendor: " + e.getMessage()));
        }
    }
    
    @Operation(
        summary = "Delete vendor account by ID",
        description = "Deletes a vendor account by ID. VENDOR_ADMIN, ADMIN, and SUPER_ADMIN only. ADMIN can delete VENDOR_ADMIN."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('VENDOR_ADMIN', 'ADMIN', 'SUPER_ADMIN')")
    @DeleteMapping("/admin/vendors/{id}")
    public ResponseEntity<?> deleteVendorById(
        @Parameter(description = "Vendor ID") @PathVariable Long id) {
        try {
            vendorService.deleteVendorById(id);
            return ResponseEntity.ok("Vendor deleted successfully");
        } catch (Exception e) {
            logger.error("Error deleting vendor: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to delete vendor: " + e.getMessage()));
        }
    }
    
    @Operation(
        summary = "Activate vendor account",
        description = "Activates a vendor account. VENDOR_ADMIN, ADMIN, and SUPER_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('VENDOR_ADMIN', 'ADMIN', 'SUPER_ADMIN')")
    @PutMapping("/admin/vendors/{id}/activate")
    public ResponseEntity<?> activateVendor(
        @Parameter(description = "Vendor ID") @PathVariable Long id) {
        try {
            boolean isActive = vendorService.activateVendor(id);
            return ResponseEntity.ok(Map.of("success", true, "isActive", isActive, "message", "Vendor activated successfully"));
        } catch (Exception e) {
            logger.error("Error activating vendor: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to activate vendor: " + e.getMessage()));
        }
    }
    
    @Operation(
        summary = "Deactivate vendor account",
        description = "Deactivates a vendor account. VENDOR_ADMIN, ADMIN, and SUPER_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('VENDOR_ADMIN', 'ADMIN', 'SUPER_ADMIN')")
    @PutMapping("/admin/vendors/{id}/deactivate")
    public ResponseEntity<?> deactivateVendor(
        @Parameter(description = "Vendor ID") @PathVariable Long id) {
        try {
            boolean isInactive = vendorService.deactivateVendor(id);
            return ResponseEntity.ok(Map.of("success", true, "isActive", !isInactive, "message", "Vendor deactivated successfully"));
        } catch (Exception e) {
            logger.error("Error deactivating vendor: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to deactivate vendor: " + e.getMessage()));
        }
    }
    
    // ========== Admin Vendor Registration Endpoints ==========
    
    @Operation(
        summary = "Register pending vendor (Admin only)",
        description = "Creates a pending vendor account with basic contact information. Admin will verify and activate later. ADMIN and SUPER_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/admin/register-pending")
    public ResponseEntity<?> registerPendingVendor(
        @Valid @RequestBody PendingVendorRegistrationRequest request,
        BindingResult result) throws IOException {
        try {
            if (result.hasErrors()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Validation failed: " + result.getAllErrors().get(0).getDefaultMessage()));
            }
            
            // Check if email already exists
            if (vendorService.findByEmail(request.getEmail()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("Vendor with this email already exists: " + request.getEmail()));
            }
            
            Vendor vendor = vendorService.registerPendingVendor(request);
            return ResponseEntity.ok(new ApiResponse(true, 
                "Pending vendor registered successfully. Vendor ID: " + vendor.getId() + 
                ". Please verify the business and activate the account."));
        } catch (Exception ex) {
            logger.error("Error during pending vendor registration: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(new ErrorResponse("Failed to register pending vendor: " + ex.getMessage()));
        }
    }
    
    @Operation(
        summary = "Verify and activate pending vendor (Admin only)",
        description = "Verifies a pending vendor account and sends credentials to the vendor. ADMIN and SUPER_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PutMapping("/admin/{vendorId}/verify-and-activate")
    public ResponseEntity<?> verifyAndActivateVendor(
        @Parameter(description = "Vendor ID to verify and activate") @PathVariable Long vendorId) throws IOException {
        try {
            VendorCredentialsResponse credentials = vendorService.verifyAndActivateVendor(vendorId);
            return ResponseEntity.ok(credentials);
        } catch (RuntimeException e) {
            logger.warn("Vendor verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception ex) {
            logger.error("Error during vendor verification: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(new ErrorResponse("Failed to verify vendor: " + ex.getMessage()));
        }
    }
    
    @Operation(
        summary = "Get pending vendors (Admin only)",
        description = "Retrieves a list of all vendors pending verification. ADMIN and SUPER_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/admin/pending-vendors")
    public ResponseEntity<List<Vendor>> getPendingVendors() {
        List<Vendor> pendingVendors = vendorService.getPendingVendors();
        return ResponseEntity.ok(pendingVendors);
    }
    
    // ========== Vendor Onboarding Flow Endpoints ==========
    
    @Operation(
        summary = "Step 1: Set vendor type",
        description = "Sets vendor type as CHAIN or UNIQUE. VENDOR_ADMIN only. Must be VERIFIED status."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    @PutMapping("/onboarding/set-vendor-type")
    public ResponseEntity<?> setVendorType(
        @Valid @RequestBody VendorTypeRequest request,
        BindingResult result) {
        try {
            if (result.hasErrors()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Validation failed: " + result.getAllErrors().get(0).getDefaultMessage()));
            }
            
            vendorService.setVendorType(request);
            return ResponseEntity.ok(new ApiResponse(true, "Vendor type set successfully"));
        } catch (RuntimeException e) {
            logger.warn("Failed to set vendor type: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception ex) {
            logger.error("Error setting vendor type: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to set vendor type: " + ex.getMessage()));
        }
    }
    
    @Operation(
        summary = "Step 2: Add headquarters (CHAIN only)",
        description = "Adds headquarters location for chain vendors. VENDOR_ADMIN only. Must be CHAIN type."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    @PutMapping("/onboarding/add-headquarters")
    public ResponseEntity<?> addHeadquarters(
        @Valid @RequestBody HeadquartersRequest request,
        BindingResult result) {
        try {
            if (result.hasErrors()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Validation failed: " + result.getAllErrors().get(0).getDefaultMessage()));
            }
            
            vendorService.addHeadquarters(request);
            return ResponseEntity.ok(new ApiResponse(true, "Headquarters added successfully"));
        } catch (RuntimeException e) {
            logger.warn("Failed to add headquarters: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception ex) {
            logger.error("Error adding headquarters: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to add headquarters: " + ex.getMessage()));
        }
    }
    
    @Operation(
        summary = "Step 3: Set banking model",
        description = "Sets banking model as SHARED or INDIVIDUAL. VENDOR_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    @PutMapping("/onboarding/set-banking-model")
    public ResponseEntity<?> setBankingModel(
        @Valid @RequestBody BankingModelRequest request,
        BindingResult result) {
        try {
            if (result.hasErrors()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Validation failed: " + result.getAllErrors().get(0).getDefaultMessage()));
            }
            
            vendorService.setBankingModel(request);
            return ResponseEntity.ok(new ApiResponse(true, "Banking model set successfully"));
        } catch (RuntimeException e) {
            logger.warn("Failed to set banking model: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception ex) {
            logger.error("Error setting banking model: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to set banking model: " + ex.getMessage()));
        }
    }
    
    @Operation(
        summary = "Step 4: Setup payment account",
        description = "Initializes payment account based on country and banking model. VENDOR_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    @PostMapping("/onboarding/setup-payment-account")
    public ResponseEntity<?> setupPaymentAccount() {
        try {
            vendorService.setupPaymentAccount();
            return ResponseEntity.ok(new ApiResponse(true, "Payment account setup initiated successfully"));
        } catch (RuntimeException e) {
            logger.warn("Failed to setup payment account: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception ex) {
            logger.error("Error setting up payment account: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to setup payment account: " + ex.getMessage()));
        }
    }
    
    @Operation(
        summary = "Step 5: Complete onboarding",
        description = "Marks onboarding as complete. VENDOR_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    @PostMapping("/onboarding/complete")
    public ResponseEntity<?> completeOnboarding() {
        try {
            vendorService.completeOnboarding();
            return ResponseEntity.ok(new ApiResponse(true, "Onboarding completed successfully! You can now fully use the platform."));
        } catch (RuntimeException e) {
            logger.warn("Failed to complete onboarding: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception ex) {
            logger.error("Error completing onboarding: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to complete onboarding: " + ex.getMessage()));
        }
    }
    
    @Operation(
        summary = "Get onboarding status",
        description = "Returns current onboarding status and account flags for the authenticated vendor. "
            + "Allowed for VENDOR and VENDOR_ADMIN — the Android app calls this for both roles after login. "
            + "Workers (VENDOR) always have status COMPLETED; use assignedLocationId for offer operations."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('VENDOR_ADMIN', 'VENDOR')")
    @GetMapping("/onboarding/status")
    public ResponseEntity<?> getOnboardingStatus() {
        try {
            com.stillfresh.app.sharedentities.enums.OnboardingStatus status = vendorService.getOnboardingStatus();
            Vendor vendor = vendorService.getVendorFromContext();
            
            Map<String, Object> response = new java.util.HashMap<>();
            // Handle null status gracefully
            response.put("status", status != null ? status.toString() : "PENDING_VERIFICATION");
            response.put("id", vendor.getId());
            response.put("role", vendor.getRole() != null ? vendor.getRole().toString() : null);
            response.put("assignedLocationId", vendor.getAssignedLocationId());
            response.put("isChainLocation", vendor.getIsChainLocation() != null ? vendor.getIsChainLocation() : false);
            response.put("isUniqueVendor", vendor.getIsUniqueVendor() != null ? vendor.getIsUniqueVendor() : true);
            response.put("chainName", vendor.getChainName());
            response.put("locationName", vendor.getLocationName());
            response.put("isHeadquarters", vendor.getIsHeadquarters() != null ? vendor.getIsHeadquarters() : false);
            response.put("usesSharedPaymentAccount", vendor.getUsesSharedPaymentAccount() != null ? vendor.getUsesSharedPaymentAccount() : false);
            
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            logger.error("Error getting onboarding status: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to get onboarding status: " + ex.getMessage()));
        }
    }
    
    // ========== Chain Location Management Endpoints ==========
    
    @Operation(
        summary = "Add new location to chain",
        description = "Adds a new location to the vendor's chain. Creates a new VENDOR_ADMIN account for the location. VENDOR_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    @PostMapping("/chain/locations")
    public ResponseEntity<?> addChainLocation(
        @Valid @RequestBody LocationRequest request,
        BindingResult result) {
        try {
            if (result.hasErrors()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Validation failed: " + result.getAllErrors().get(0).getDefaultMessage()));
            }
            
            com.stillfresh.app.vendorservice.dto.LocationCreationResponse response = vendorService.addChainLocation(request);
            
            if (response.isEmailSent()) {
                return ResponseEntity.ok(response);
            } else {
                // Email failed - return 201 Created with warning in response body
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            }
        } catch (RuntimeException e) {
            logger.warn("Failed to add chain location: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception ex) {
            logger.error("Error adding chain location: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to add chain location: " + ex.getMessage()));
        }
    }
    
    @Operation(
        summary = "Get all chain locations",
        description = "Retrieves all locations in the vendor's chain. VENDOR_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    @GetMapping("/chain/locations")
    public ResponseEntity<List<Vendor>> getChainLocations() {
        try {
            List<Vendor> locations = vendorService.getChainLocations();
            return ResponseEntity.ok(locations);
        } catch (RuntimeException e) {
            logger.warn("Failed to get chain locations: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(null);
        } catch (Exception ex) {
            logger.error("Error getting chain locations: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null);
        }
    }

    @Operation(
        summary = "Get chain restaurant stats",
        description = "Returns sales statistics for all selling locations in the chain, plus rolled-up chain totals. "
            + "HQ VENDOR_ADMIN only (branch admins use per-location /vendors/stats). SUPER_ADMIN must pass chainId."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('VENDOR_ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/chain/stats")
    public ResponseEntity<?> getChainLocationStats(
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "chainId", required = false) String chainId) {
        try {
            java.time.OffsetDateTime fromDt = (from == null || from.isBlank()) ? null : java.time.OffsetDateTime.parse(from);
            java.time.OffsetDateTime toDt = (to == null || to.isBlank()) ? null : java.time.OffsetDateTime.parse(to);
            ChainLocationStatsResponse stats = vendorService.getChainLocationStats(fromDt, toDt, chainId);
            return ResponseEntity.ok(stats);
        } catch (java.time.format.DateTimeParseException ex) {
            logger.warn("Invalid date-time format for chain stats query. from='{}', to='{}'", from, to);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Invalid date-time format"));
        } catch (IllegalArgumentException e) {
            logger.warn("Chain stats request rejected: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("Only headquarters")) {
                logger.warn("Chain stats forbidden: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse(e.getMessage()));
            }
            logger.warn("Failed to get chain stats: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception ex) {
            logger.error("Error getting chain stats: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to get chain stats: " + ex.getMessage()));
        }
    }
    
    @Operation(
        summary = "Update chain location",
        description = "Updates a location in the vendor's chain. VENDOR_ADMIN only. Cannot update headquarters."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    @PutMapping("/chain/locations/{locationId}")
    public ResponseEntity<?> updateChainLocation(
        @Parameter(description = "Location ID to update") @PathVariable Long locationId,
        @Valid @RequestBody LocationRequest request,
        BindingResult result) {
        try {
            if (result.hasErrors()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Validation failed: " + result.getAllErrors().get(0).getDefaultMessage()));
            }
            
            vendorService.updateChainLocation(locationId, request);
            return ResponseEntity.ok(new ApiResponse(true, "Location updated successfully"));
        } catch (RuntimeException e) {
            logger.warn("Failed to update chain location: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception ex) {
            logger.error("Error updating chain location: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to update chain location: " + ex.getMessage()));
        }
    }
    
    @Operation(
        summary = "Remove chain location",
        description = "Removes (deactivates) a location from the vendor's chain. VENDOR_ADMIN only. Cannot remove headquarters."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    @DeleteMapping("/chain/locations/{locationId}")
    public ResponseEntity<?> removeChainLocation(
        @Parameter(description = "Location ID to remove") @PathVariable Long locationId) {
        try {
            vendorService.removeChainLocation(locationId);
            return ResponseEntity.ok(new ApiResponse(true, "Location removed successfully"));
        } catch (RuntimeException e) {
            logger.warn("Failed to remove chain location: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception ex) {
            logger.error("Error removing chain location: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to remove chain location: " + ex.getMessage()));
        }
    }
    
    // ========== Banking Model Management Endpoints ==========
    
    @Operation(
        summary = "Switch banking model",
        description = "Switches banking model for the entire chain (SHARED or INDIVIDUAL). Only Headquarters VENDOR_ADMIN can perform this action. Non-headquarters locations cannot switch banking models."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    @PutMapping("/chain/banking/switch-model")
    public ResponseEntity<?> switchBankingModel(
        @Valid @RequestBody SwitchBankingModelRequest request,
        BindingResult result) throws IOException {
        try {
            if (result.hasErrors()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Validation failed: " + result.getAllErrors().get(0).getDefaultMessage()));
            }
            
            vendorService.switchBankingModel(request);
            return ResponseEntity.ok(new ApiResponse(true, 
                "Banking model switched to " + request.getBankingModel() + " successfully"));
        } catch (RuntimeException e) {
            logger.warn("Failed to switch banking model: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception ex) {
            logger.error("Error switching banking model: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to switch banking model: " + ex.getMessage()));
        }
    }
    
    @Operation(
        summary = "Setup payment account for location",
        description = "Sets up individual payment account for a specific location. VENDOR_ADMIN only. Requires INDIVIDUAL banking model."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    @PostMapping("/chain/locations/{locationId}/setup-payment-account")
    public ResponseEntity<?> setupLocationPaymentAccount(
        @Parameter(description = "Location ID to setup payment account") @PathVariable Long locationId) throws IOException {
        try {
            vendorService.setupLocationPaymentAccount(locationId);
            return ResponseEntity.ok(new ApiResponse(true, "Payment account setup initiated for location"));
        } catch (RuntimeException e) {
            logger.warn("Failed to setup location payment account: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception ex) {
            logger.error("Error setting up location payment account: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to setup payment account: " + ex.getMessage()));
        }
    }

    @Operation(
        summary = "Get MoR bank details for a chain location",
        description = "Returns masked MoR bank details for a location on the INDIVIDUAL banking model. "
            + "Headquarters may read any location in the chain; a branch VENDOR_ADMIN only its own. "
            + "On SHARED banking, use GET /vendors/mor/bank-details on the headquarters account instead."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bank details retrieved",
            content = @Content(schema = @Schema(implementation = com.stillfresh.app.vendorservice.dto.MorBankDetailsResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Business rule violation"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/chain/locations/{locationId}/mor/bank-details")
    public ResponseEntity<?> getLocationMoRBankDetails(
        @Parameter(description = "Location ID") @PathVariable Long locationId) {
        try {
            return ResponseEntity.ok(vendorService.getMoRBankDetailsForLocation(locationId));
        } catch (RuntimeException e) {
            logger.warn("Failed to get location MoR bank details: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception ex) {
            logger.error("Error getting location MoR bank details: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to retrieve bank details: " + ex.getMessage()));
        }
    }

    @Operation(
        summary = "Submit MoR bank details for a chain location",
        description = "Writes MoR bank details onto a chain location (INDIVIDUAL banking). "
            + "Headquarters may update any location; a branch VENDOR_ADMIN only its own. "
            + "Same partial-update body and validation as PUT /vendors/mor/bank-details. "
            + "Initialises the MoR payout model when needed and marks onboarding COMPLETED once a payout destination exists. "
            + "Security email on destination change goes to the location's email."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bank details saved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Business rule or validation failure"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/chain/locations/{locationId}/mor/bank-details")
    public ResponseEntity<?> submitLocationBankDetails(
        @Parameter(description = "Location ID") @PathVariable Long locationId,
        @RequestBody Map<String, String> bankDetails) {
        try {
            vendorService.submitBankDetailsForLocation(locationId, bankDetails);
            return ResponseEntity.ok(new ApiResponse(true, "Bank details submitted successfully for location"));
        } catch (RuntimeException e) {
            logger.warn("Failed to submit location MoR bank details: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception ex) {
            logger.error("Error submitting location MoR bank details: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to submit bank details: " + ex.getMessage()));
        }
    }
    
    @Operation(
        summary = "Get banking model information",
        description = "Returns banking model information for the chain. VENDOR_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    @GetMapping("/chain/banking/info")
    public ResponseEntity<?> getBankingModelInfo() {
        try {
            Map<String, Object> info = vendorService.getBankingModelInfo();
            return ResponseEntity.ok(info);
        } catch (RuntimeException e) {
            logger.warn("Failed to get banking model info: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception ex) {
            logger.error("Error getting banking model info: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to get banking model info: " + ex.getMessage()));
        }
    }
    
    // ========== VENDOR Worker Management Endpoints ==========
    
    @Operation(
        summary = "Create worker for location",
        description = "Creates a VENDOR worker account for a specific location. Worker can only manage offers for their assigned location. Only Headquarters VENDOR_ADMIN can create workers for other locations. Non-headquarters VENDOR_ADMIN can only create workers for their own location."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    @PostMapping("/chain/locations/{locationId}/workers")
    public ResponseEntity<?> createWorker(
        @Parameter(description = "Location ID to create worker for") @PathVariable Long locationId,
        @Valid @RequestBody WorkerRequest request,
        BindingResult result) throws IOException {
        try {
            if (result.hasErrors()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Validation failed: " + result.getAllErrors().get(0).getDefaultMessage()));
            }
            
            Vendor worker = vendorService.createWorker(locationId, request);
            return ResponseEntity.ok(new ApiResponse(true, 
                "Worker created successfully. Credentials sent to: " + worker.getEmail()));
        } catch (RuntimeException e) {
            logger.warn("Failed to create worker: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception ex) {
            logger.error("Error creating worker: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to create worker: " + ex.getMessage()));
        }
    }
    
    @Operation(
        summary = "Update worker",
        description = "Updates a worker's username, phone or assigned location. Only Headquarters VENDOR_ADMIN can update workers of other locations or reassign a worker to a different location."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    @PutMapping("/chain/workers/{workerId}")
    public ResponseEntity<?> updateWorker(
        @Parameter(description = "Worker ID to update") @PathVariable Long workerId,
        @Valid @RequestBody com.stillfresh.app.vendorservice.dto.WorkerUpdateRequest request,
        BindingResult result) {
        try {
            if (result.hasErrors()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Validation failed: " + result.getAllErrors().get(0).getDefaultMessage()));
            }
            
            vendorService.updateWorker(workerId, request);
            return ResponseEntity.ok(new ApiResponse(true, "Worker updated successfully"));
        } catch (RuntimeException e) {
            logger.warn("Failed to update worker: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception ex) {
            logger.error("Error updating worker: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to update worker: " + ex.getMessage()));
        }
    }
    
    @Operation(
        summary = "Get workers for location",
        description = "Retrieves all VENDOR workers for a specific location. Only Headquarters VENDOR_ADMIN can access workers for other locations. Non-headquarters VENDOR_ADMIN can only access workers for their own location."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    @GetMapping("/chain/locations/{locationId}/workers")
    public ResponseEntity<List<Vendor>> getLocationWorkers(
        @Parameter(description = "Location ID") @PathVariable Long locationId) {
        try {
            List<Vendor> workers = vendorService.getLocationWorkers(locationId);
            return ResponseEntity.ok(workers);
        } catch (RuntimeException e) {
            logger.warn("Failed to get location workers: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(null);
        } catch (Exception ex) {
            logger.error("Error getting location workers: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null);
        }
    }
    
    @Operation(
        summary = "Activate worker",
        description = "Activates a worker account. Only Headquarters VENDOR_ADMIN can activate workers for other locations. Non-headquarters VENDOR_ADMIN can only activate workers for their own location."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    @PutMapping("/chain/workers/{workerId}/activate")
    public ResponseEntity<?> activateWorker(
        @Parameter(description = "Worker ID to activate") @PathVariable Long workerId) {
        try {
            vendorService.activateWorker(workerId);
            return ResponseEntity.ok(new ApiResponse(true, "Worker activated successfully"));
        } catch (RuntimeException e) {
            logger.warn("Failed to activate worker: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception ex) {
            logger.error("Error activating worker: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to activate worker: " + ex.getMessage()));
        }
    }
    
    @Operation(
        summary = "Deactivate worker",
        description = "Deactivates a worker account. Only Headquarters VENDOR_ADMIN can deactivate workers for other locations. Non-headquarters VENDOR_ADMIN can only deactivate workers for their own location."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    @PutMapping("/chain/workers/{workerId}/deactivate")
    public ResponseEntity<?> deactivateWorker(
        @Parameter(description = "Worker ID to deactivate") @PathVariable Long workerId) {
        try {
            vendorService.deactivateWorker(workerId);
            return ResponseEntity.ok(new ApiResponse(true, "Worker deactivated successfully"));
        } catch (RuntimeException e) {
            logger.warn("Failed to deactivate worker: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception ex) {
            logger.error("Error deactivating worker: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to deactivate worker: " + ex.getMessage()));
        }
    }
    
    @Operation(
        summary = "Delete worker",
        description = "Deletes a worker account. Only Headquarters VENDOR_ADMIN can delete workers for other locations. Non-headquarters VENDOR_ADMIN can only delete workers for their own location."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    @DeleteMapping("/chain/workers/{workerId}")
    public ResponseEntity<?> deleteWorker(
        @Parameter(description = "Worker ID to delete") @PathVariable Long workerId) {
        try {
            vendorService.deleteWorker(workerId);
            return ResponseEntity.ok(new ApiResponse(true, "Worker deleted successfully"));
        } catch (RuntimeException e) {
            logger.warn("Failed to delete worker: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception ex) {
            logger.error("Error deleting worker: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to delete worker: " + ex.getMessage()));
        }
    }
    
    // ========== Upgrade Unique Vendor to Chain ==========
    
    @Operation(
        summary = "Upgrade unique vendor to chain",
        description = "Upgrades a unique vendor to a chain, enabling multiple location management. VENDOR_ADMIN only."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_ADMIN')")
    @PostMapping("/upgrade-to-chain")
    public ResponseEntity<?> upgradeToChain(
        @RequestParam("chainName") String chainName) {
        try {
            // Manual validation for chainName
            if (chainName == null || chainName.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Chain name is required and cannot be empty"));
            }
            
            vendorService.upgradeToChain(chainName.trim());
            return ResponseEntity.ok(new ApiResponse(true, 
                "Vendor upgraded to chain successfully. You can now add multiple locations."));
        } catch (RuntimeException e) {
            logger.warn("Failed to upgrade vendor to chain: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception ex) {
            logger.error("Error upgrading vendor to chain: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to upgrade to chain: " + ex.getMessage()));
        }
    }

}
