package com.stillfresh.app.vendorservice.service;

import com.stillfresh.app.vendorservice.model.PasswordResetToken;
import com.stillfresh.app.vendorservice.model.Vendor;
import com.stillfresh.app.vendorservice.model.Vendor.Role;
import com.stillfresh.app.vendorservice.model.VendorVerificationToken;
import com.stillfresh.app.vendorservice.repository.PasswordResetTokenRepository;
import com.stillfresh.app.vendorservice.repository.VendorRepository;
import com.stillfresh.app.vendorservice.repository.VendorVerificationTokenRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.Date;

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

    private static final Logger logger = LoggerFactory.getLogger(VendorService.class);

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
}
