package com.stillfresh.app.vendorservice.controller;

import com.stillfresh.app.vendorservice.client.AuthorizationServiceClient;
import com.stillfresh.app.sharedentities.dto.CheckAvailabilityRequest;
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
    	
        // Call the authorization service to check availability
        ResponseEntity<ApiResponse> availabilityResponse = authorizationServiceClient.checkAvailability(
            new CheckAvailabilityRequest(vendor.getUsername(), vendor.getEmail()));

        // If the response indicates a conflict (username/email already taken)
        logger.info("availabilityResponse.getStatusCode()" + availabilityResponse.getStatusCode());
        logger.info("HttpStatus.CONFLICT" + HttpStatus.CONFLICT);
        if (availabilityResponse.getStatusCode() == HttpStatus.CONFLICT) {
            // Return the conflict response
        	logger.info("PROSLO Check za conflickt");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(availabilityResponse.getBody());
        }

        try {
            // If available, proceed to initiate registration
            vendorService.registerVendor(vendor);
            return ResponseEntity.ok(new ApiResponse(true, "Vendor registration initiated. Check your email for verification."));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(new ErrorResponse("Failed to initiate registration: " + ex.getMessage()));
        }
    	
    	
//        vendorService.registerVendor(vendor, false);  // False indicates normal vendor registration
//        return ResponseEntity.ok("Vendor registration successful. Please verify your email.");
    }

    // Admin registration (can be restricted to other admins using @PreAuthorize)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/register-admin")
    public ResponseEntity<String> registerAdmin(@RequestBody Vendor vendor) throws IOException {
        vendorService.registerVendor(vendor, true);  // True indicates admin registration
        return ResponseEntity.ok("Admin registration successful. Please verify your email.");
    }

    // Promote an existing vendor to admin
    @PreAuthorize("hasRole('ADMIN')")
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
    
    @PutMapping("/profile")
    public ResponseEntity<String> updateVendorProfile(@Valid @RequestBody Vendor updatedVendor, BindingResult result) {
  
        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.getAllErrors().get(0).getDefaultMessage());
        }
        vendorService.updateVendorProfile(updatedVendor);
        return ResponseEntity.ok("Vendor profile updated successfully");
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
    	return vendorService.deleteVendorProfile(token);
    }

}
