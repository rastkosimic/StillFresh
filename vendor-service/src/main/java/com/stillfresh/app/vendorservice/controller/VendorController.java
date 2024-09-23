package com.stillfresh.app.vendorservice.controller;

import com.stillfresh.app.vendorservice.model.Vendor;
import com.stillfresh.app.vendorservice.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/vendors")
public class VendorController {

    @Autowired
    private VendorService vendorService;

    @PostMapping("/register")
    public ResponseEntity<String> registerVendor(@RequestBody Vendor vendor) throws IOException {
        vendorService.registerVendor(vendor);
        return ResponseEntity.ok("Registration successful. Please check your email to verify your account.");
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
}
