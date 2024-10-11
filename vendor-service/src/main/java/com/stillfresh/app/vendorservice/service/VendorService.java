package com.stillfresh.app.vendorservice.service;

import com.stillfresh.app.vendorservice.dto.PasswordChangeRequest;
import com.stillfresh.app.vendorservice.model.PasswordResetToken;
import com.stillfresh.app.vendorservice.model.Vendor;
import com.stillfresh.app.vendorservice.model.Vendor.Role;
import com.stillfresh.app.vendorservice.model.VendorVerificationToken;
import com.stillfresh.app.vendorservice.repository.PasswordResetTokenRepository;
import com.stillfresh.app.vendorservice.repository.VendorRepository;
import com.stillfresh.app.vendorservice.repository.VendorVerificationTokenRepository;
import com.stillfresh.app.vendorservice.security.JwtUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.Date;
import java.util.List;

@Service
public class VendorService {

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private VendorVerificationTokenRepository vendorVerificationTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;
    
    @Autowired
    private TokenBlacklistService tokenBlacklistService;
    
    @Autowired
    JwtUtil jwtUtil;

    private static final Logger logger = LoggerFactory.getLogger(VendorService.class);
    
    public boolean hasAdmin() {
        // Check if any vendor has the ADMIN role
        return vendorRepository.existsByRole(Role.ADMIN);
    }

    public Vendor registerAdmin(Vendor vendor) throws IOException {
        // Basic validation to prevent duplicate emails or names
        if (vendorRepository.existsByEmail(vendor.getEmail())) {
            throw new RuntimeException("Email is already registered.");
        }

        vendor.setPassword(passwordEncoder.encode(vendor.getPassword()));
        vendor.setRole(Role.ADMIN);  // Assign the ADMIN role
        vendor.setActive(true);  // Admin is active by default

        // Save the admin to the database
        return vendorRepository.save(vendor);
    }

    public Vendor registerVendor(Vendor vendor) throws IOException {
        if (vendorRepository.existsByEmail(vendor.getEmail())) {
            throw new RuntimeException("Vendor already registered with this email");
        }
        vendor.setPassword(passwordEncoder.encode(vendor.getPassword()));
        vendor.setRole(Role.VENDOR);
        vendor.setActive(false);  // Inactive until verified
        vendorRepository.save(vendor);

        // Generate verification token
        String token = UUID.randomUUID().toString();
        VendorVerificationToken verificationToken = new VendorVerificationToken();
        verificationToken.setToken(token);
        verificationToken.setVendor(vendor);
        vendorVerificationTokenRepository.save(verificationToken);

        // Send verification email
        
        String verificationUrl = "http://localhost:8083/vendors/verify?token=" + token;
        emailService.sendVerificationEmail(vendor.getEmail(), verificationUrl);

        return vendor;
    }

    public boolean verifyVendor(String token) {
        VendorVerificationToken verificationToken = vendorVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));
        Vendor vendor = verificationToken.getVendor();
        vendor.setActive(true);
        vendorRepository.save(vendor);
        return true;
    }
    
    public void sendPasswordResetLink(String email) throws IOException {
        Vendor vendor = vendorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Vendor not found with email: " + email));

        // Check if a token already exists for this vendor and remove it
        Optional<PasswordResetToken> existingToken = passwordResetTokenRepository.findByVendor(vendor);
        existingToken.ifPresent(token -> passwordResetTokenRepository.delete(token));

        // Generate a new reset token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setVendor(vendor);
        resetToken.setExpiryDate(calculateExpiryDate(24)); // Token valid for 24 hours
        passwordResetTokenRepository.save(resetToken);

        // Send the reset email
        emailService.sendPasswordResetEmail(vendor.getEmail(), token);
    }

    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired password reset token"));

        Vendor vendor = resetToken.getVendor();
        logger.info("Old Password Hash: " + vendor.getPassword());
        
        // Hash the new password
        String encodedPassword = passwordEncoder.encode(newPassword);
        vendor.setPassword(encodedPassword);

        logger.info("New Password Hash: " + encodedPassword);
        vendorRepository.save(vendor);

        // Remove the reset token after successful password reset
        passwordResetTokenRepository.delete(resetToken);
    }
    
    public Vendor findVendorById(Long id) {
        Optional<Vendor> user = vendorRepository.findById(id);
        logger.info("Finding a user {}, with id: {}", user.map(Vendor::getName).orElse("Not found"), id);
        return user.orElseThrow(() -> new RuntimeException("User not found"));
    }


    private Date calculateExpiryDate(int hours) {
        Date now = new Date();
        return new Date(now.getTime() + (hours * 60 * 60 * 1000));  // Expiry time in milliseconds
    }
    
    public void updateVendorProfile(Long id, Vendor updatedVendor) {
        Vendor existingVendor = vendorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        
        // Update only relevant fields
        existingVendor.setName(updatedVendor.getName());
        existingVendor.setAddress(updatedVendor.getAddress());
        existingVendor.setPhone(updatedVendor.getPhone());
        
        vendorRepository.save(existingVendor);
    }


	public ResponseEntity<String> changeVendorPassword(Vendor vendor, PasswordChangeRequest passwordChangeRequest) {
	
	    if (!passwordEncoder.matches(passwordChangeRequest.getOldPassword(), vendor.getPassword())) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Old password is incorrect");
	    }
	
	    // Change password and save
	    String encodedPassword = passwordEncoder.encode(passwordChangeRequest.getNewPassword());
	    vendor.setPassword(encodedPassword);
	    vendorRepository.save(vendor);
	
	    return ResponseEntity.ok("Password changed successfully");
	}
	
	public void logoutAndInvalidateToken(String jwt) {
	    long expiryDurationInMillis = jwtUtil.getExpirationTimeInMillis(jwt) - System.currentTimeMillis();
	    tokenBlacklistService.addTokenToBlacklist(jwt, expiryDurationInMillis);
	    
	    // Clear the security context (forces logout)
	    SecurityContextHolder.clearContext();
	}
	
    // Admin-only methods for vendor management
	
    // Check if a Super-Admin exists
    public boolean hasSuperAdmin() {
        return vendorRepository.existsByRole(Vendor.Role.SUPER_ADMIN);
    }

    // Method to register the first Super-Admin
    public Vendor registerSuperAdmin(Vendor admin) {
        if (hasSuperAdmin()) {
            throw new RuntimeException("Super-Admin already exists.");
        }

        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        admin.setRole(Role.SUPER_ADMIN);
        admin.setActive(true); // Automatically activate the Super-Admin
        return vendorRepository.save(admin);
    }
	
    // Method to delete an admin (restricted to Super-Admin)
    public void deleteAdminById(Long id) {
        Vendor admin = vendorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        // Prevent deleting Super-Admin
        if (admin.getRole() == Role.SUPER_ADMIN) {
            throw new RuntimeException("Cannot delete a Super-Admin.");
        }

        // Allow only the deletion of regular Admins
        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("This user is not an admin and cannot be deleted as such.");
        }

        vendorRepository.deleteById(id);
    }
    
    public Vendor registerVendor(Vendor vendor, boolean isAdmin) throws IOException {    	
        if (vendorRepository.existsByEmail(vendor.getEmail())) {
            throw new RuntimeException("Vendor already registered with this email");
        }
        vendor.setPassword(passwordEncoder.encode(vendor.getPassword()));
        vendor.setRole(isAdmin ? Role.ADMIN : Role.VENDOR);  // Set role based on input
        vendor.setActive(false);  // Inactive until verified
        vendorRepository.save(vendor);

      // Generate verification token
      String token = UUID.randomUUID().toString();
      VendorVerificationToken verificationToken = new VendorVerificationToken();
      verificationToken.setToken(token);
      verificationToken.setVendor(vendor);
      vendorVerificationTokenRepository.save(verificationToken);

      // Send verification email
      
      String verificationUrl = "http://localhost:8083/vendors/verify?token=" + token;
      emailService.sendVerificationEmail(vendor.getEmail(), verificationUrl);

        return vendor;
    }

    // Promote existing vendor to admin
    public void promoteVendorToAdmin(Long vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        // Check if the vendor is already an admin
        if (vendor.getRole() == Role.ADMIN || vendor.getRole() == Role.SUPER_ADMIN) {
            throw new RuntimeException("Vendor is already an admin or super-admin");
        }

        vendor.setRole(Role.ADMIN);
        vendorRepository.save(vendor);
    }

    public List<Vendor> getAllVendors() {
        return vendorRepository.findAll();
    }

    public boolean toggleVendorActivation(Long id) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        vendor.setActive(!vendor.isActive());
        vendorRepository.save(vendor);
        return vendor.isActive();
    }

    public void deleteVendorById(Long id) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        // Only allow deletion of vendors with the VENDOR role
        if (vendor.getRole() != Role.VENDOR) {
            throw new RuntimeException("You are not allowed to delete this user. Only users with the VENDOR role can be deleted.");
        }

        // Perform the deletion
        vendorRepository.deleteById(id);
    }


    // Method to activate a vendor
    public boolean activateVendor(Long id) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        vendor.setActive(true); // Set active to true
        vendorRepository.save(vendor);
        return vendor.isActive();
    }

    // Method to deactivate a vendor
    public boolean deactivateVendor(Long id) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        vendor.setActive(false); // Set active to false
        vendorRepository.save(vendor);
        return !vendor.isActive();
    }
    
    public void demoteVendorFromAdmin(Long id) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        if (vendor.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("This vendor is not an admin.");
        }

        // Set the role back to VENDOR
        vendor.setRole(Role.VENDOR);
        vendorRepository.save(vendor);
    }


}
