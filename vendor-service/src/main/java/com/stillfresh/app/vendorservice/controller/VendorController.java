package com.stillfresh.app.vendorservice.controller;

import com.stillfresh.app.vendorservice.client.AuthorizationServiceClient;
import com.stillfresh.app.sharedentities.dto.CheckAvailabilityRequest;
import com.stillfresh.app.sharedentities.offer.events.OfferCreationEvent;
import com.stillfresh.app.sharedentities.offer.events.OfferUpdateEvent;
import com.stillfresh.app.sharedentities.dto.OfferDto;
import com.stillfresh.app.sharedentities.responses.ApiResponse;
import com.stillfresh.app.sharedentities.responses.ErrorResponse;
import com.stillfresh.app.vendorservice.dto.PasswordChangeRequest;
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
        summary = "Register a new vendor",
        description = "Creates a new vendor account and sends a verification email. The vendor must verify their email before they can log in."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Registration initiated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Username or email already exists"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/register")
    public ResponseEntity<?> registerVendor(@RequestBody Vendor vendor) throws IOException {
        try {
            // Call the authorization service to check availability
            ApiResponse availabilityResponse = authorizationServiceClient.checkAvailability(
                new CheckAvailabilityRequest(vendor.getUsername(), vendor.getEmail()));

            // Check if the username/email is unavailable
            if (!availabilityResponse.isSuccess()) {
                logger.info("Availability check failed: {}", availabilityResponse.getMessage());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(availabilityResponse);
            }

            // Proceed with registration
            vendorService.registerVendor(vendor);
            return ResponseEntity.ok(new ApiResponse(true, "Vendor registration initiated. Check your email for verification."));
        } catch (Exception ex) {
            logger.error("Error during vendor registration: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(new ErrorResponse("Failed to initiate registration: " + ex.getMessage()));
        }
    }

    @Operation(
        summary = "Register a new admin",
        description = "Creates a new admin account. This endpoint is restricted to existing admins and super admins."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/register-admin")
    public ResponseEntity<String> registerAdmin(@RequestBody Vendor vendor) throws IOException {
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
    @PreAuthorize("hasRole('VENDOR')")
    @GetMapping("/{id}")
    public ResponseEntity<Vendor> getVendorById(
        @Parameter(description = "Vendor ID") @PathVariable Long id) {
    	Vendor user = vendorService.findVendorById(id);
        return ResponseEntity.ok(user);
    }
    
    @Operation(
        summary = "Update vendor profile",
        description = "Updates the vendor's profile information. Requires authentication and logs out the user after successful update."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR')")
    @PutMapping("/update-profile")
    public ResponseEntity<String> updateVendorProfile(
        @Valid @RequestBody Vendor updatedVendor,
        BindingResult result) {
  
        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.getAllErrors().get(0).getDefaultMessage());
        }
        vendorService.updateVendorProfile(updatedVendor);
        return ResponseEntity.ok("Vendor profile updated successfully. You are logged out.");
    }
    
    @Operation(
        summary = "Change vendor password",
        description = "Changes the vendor's password and logs them out of all sessions."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR')")
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
        description = "Deletes the vendor's account and deactivates all associated offers."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR')")
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteVendor() {
        return vendorService.deleteVendorProfile();
    }
    
    @Operation(
        summary = "Create new offer",
        description = "Creates a new offer for the authenticated vendor."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR')")
    @PostMapping("/offer-create")
    public ResponseEntity<String> createOffer(@RequestBody OfferDto request) {
        vendorService.createOffer(request);
        return ResponseEntity.ok("Offer created successfully");
    }
    
    @Operation(
        summary = "Get active offers",
        description = "Retrieves all active offers for the authenticated vendor."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR')")
    @GetMapping("/active-offers")
    public ResponseEntity<List<OfferDto>> getActiveOffersForVendor() {
        return ResponseEntity.ok(vendorService.getActiveOffersForVendor());
    }
    
    @Operation(
        summary = "Get all offers",
        description = "Retrieves all offers (active and inactive) for the authenticated vendor."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR')")
    @GetMapping("/all-offers")
    public ResponseEntity<List<OfferDto>> getAllOffersForVendor() {
        return ResponseEntity.ok(vendorService.getAllOffersForVendor());
    }
    
    @Operation(
        summary = "Invalidate offer",
        description = "Deactivates an existing offer by its ID."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR')")
    @PostMapping("/invalidate-offer/{offerId}")
    public ResponseEntity<String> invalidateOffer(
        @Parameter(description = "ID of the offer to deactivate") @PathVariable int offerId) {
    	vendorService.invalidateOffer(offerId);
    	return ResponseEntity.ok("Offer deactivated successfully.");
    }
    
    @Operation(
        summary = "Update offer",
        description = "Updates an existing offer's information."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR')")
    @PostMapping("/update-offer/{offerId}")
    public ResponseEntity<String> updateOffer(
        @Parameter(description = "ID of the offer to update") @PathVariable Long offerId,
        @RequestBody OfferDto request) {
        vendorService.updateOffer(offerId, request);
        return ResponseEntity.ok("Offer updated successfully");
    }

}
