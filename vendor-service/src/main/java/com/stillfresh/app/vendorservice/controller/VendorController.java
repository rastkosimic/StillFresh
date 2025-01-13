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

@RestController
@RequestMapping("/vendors")
public class VendorController {

	private static final Logger logger = LoggerFactory.getLogger(VendorController.class);
	 
    @Autowired
    private VendorService vendorService;
    
    @Autowired
    private AuthorizationServiceClient authorizationServiceClient;
    
    // Regular vendor registration
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

    // Admin registration (can be restricted to other admins using @PreAuthorize)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/register-admin")
    public ResponseEntity<String> registerAdmin(@RequestBody Vendor vendor) throws IOException {
        vendorService.registerVendor(vendor, true);  // True indicates admin registration
        return ResponseEntity.ok("Admin registration successful. Please verify your email.");
    }

    // Promote an existing vendor to admin
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PutMapping("/{id}/promote-to-admin")
    public ResponseEntity<String> promoteVendorToAdmin(@PathVariable Long id) {
        vendorService.promoteVendorToAdmin(id);
        return ResponseEntity.ok("Vendor promoted to admin successfully");
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verifyVendor(@RequestParam("token") String token) {
        boolean isVerified = vendorService.verifyVendor(token);
        if (isVerified) {
            return ResponseEntity.ok("Vendor verified successfully.");
        } else {
            return ResponseEntity.status(400).body("Invalid token.");
        }
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestParam String email) throws IOException {
        vendorService.sendPasswordResetLink(email);
        return ResponseEntity.ok("Password reset link sent to your email");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestParam("token") String token, @RequestBody String newPassword) {
        vendorService.resetPassword(token, newPassword);
        return ResponseEntity.ok("Password reset successfully");
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Vendor> getVendorById(@PathVariable Long id) {
    	Vendor user = vendorService.findVendorById(id);
        return ResponseEntity.ok(user);
    }
    
    @PutMapping("/update-profile")
    public ResponseEntity<String> updateVendorProfile(@RequestHeader("Authorization") String token, @Valid @RequestBody Vendor updatedVendor, BindingResult result) {
  
        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.getAllErrors().get(0).getDefaultMessage());
        }
        vendorService.updateVendorProfile(token, updatedVendor);
        return ResponseEntity.ok("Vendor profile updated successfully. You are logged out.");
    }
    
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
    
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteVendor(@RequestHeader("Authorization") String token) {
    	return vendorService.deleteVendorProfile(token); //ovo mora da obrise i sve offere povezane sa vendorom . da ih deaktivira 
    }
    
    @PostMapping("/offer-create")
    public ResponseEntity<String> createOffer(@RequestHeader("Authorization") String token, @RequestBody OfferDto request) {
        vendorService.createOffer(token, request);
        return ResponseEntity.ok("Offer creation request submitted successfully.");
    }
    
    @GetMapping("/active-offers")
    public ResponseEntity<List<OfferDto>> getActiveOffersForVendor(@RequestHeader("Authorization") String token) {
        List<OfferDto> activeOffers = vendorService.getActiveOffersForVendor(token);
        return ResponseEntity.ok(activeOffers);
    }
    
    @GetMapping("/all-offers")
    public ResponseEntity<List<OfferDto>> getAllOffersForVendor(@RequestHeader("Authorization") String token) {
        List<OfferDto> activeOffers = vendorService.getAllOffersForVendor(token);
        return ResponseEntity.ok(activeOffers);
    }
    
    @PostMapping("/invalidate-offer/{offerId}")
    public ResponseEntity<String> invalidateOffer(@PathVariable int offerId){
    	vendorService.invalidateOffer(offerId);
    	return ResponseEntity.ok("Offer deactivated successfully.");
    }
    
    @PostMapping("/update-offer/{offerId}")
    public ResponseEntity<String> updateOffer(@RequestHeader("Authorization") String token, @PathVariable int offerId, @RequestBody OfferDto request){
    	vendorService.updateOffer(token, offerId, request);
    	return ResponseEntity.ok("Offer updated successfully.");
    }

}
