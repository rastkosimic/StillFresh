package com.stillfresh.app.vendorservice.controller;

import com.stillfresh.app.vendorservice.model.Vendor;
import com.stillfresh.app.vendorservice.service.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@Tag(name = "Admin Management", description = "Operations related to administration")
public class AdminController {

    @Autowired
    private VendorService vendorService;
    
    // Method to create a Super-Admin
    @Operation(summary = "Create Initial Super-Admin", description = "Create the initial super-admin. Can only be used if no Super-Admin exists.")
    @PostMapping("/create-initial-admin")
    public ResponseEntity<String> createInitialAdmin(@RequestBody Vendor admin) {
        // Check if a Super-Admin already exists
        if (vendorService.hasSuperAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Super-Admin already exists. You cannot create more than one Super-Admin.");
        }

        try {
            // Register a Super-Admin
            vendorService.registerSuperAdmin(admin);
            return ResponseEntity.ok("Initial Super-Admin created successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error creating Super-Admin: " + e.getMessage());
        }
    }

    @Operation(summary = "Get Admin Dashboard", description = "Provides access to the Admin Dashboard. Restricted to users with ADMIN role.")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/dashboard")
    public ResponseEntity<String> getAdminDashboard() {
        return ResponseEntity.ok("Welcome to the Admin Dashboard!");
    }
    
    // Super-Admin Only: Delete admin by ID
    @Operation(summary = "Delete Admin", description = "Delete an admin. Only Super-Admins can delete other admins.")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/admins/{id}")
    public ResponseEntity<String> deleteAdmin(@PathVariable Long id) {
        try {
            vendorService.deleteAdminById(id);
            return ResponseEntity.ok("Admin deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error deleting admin: " + e.getMessage());
        }
    }

    // Get all vendors
    @Operation(summary = "Get All Vendors", description = "Retrieve a list of all registered vendors.")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/vendors")
    public ResponseEntity<List<Vendor>> getAllVendors() {
        List<Vendor> vendors = vendorService.getAllVendors();
        return ResponseEntity.ok(vendors);
    }

    // Activate or deactivate a vendor
    @Operation(summary = "Activate Vendor", description = "Activate a vendor.")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PutMapping("/vendors/{id}/activate")
    public ResponseEntity<String> activateVendor(@PathVariable Long id) {
        vendorService.activateVendor(id);
        return ResponseEntity.ok("Vendor activated");
    }

    // Deactivate a vendor
    @Operation(summary = "Deactivate Vendor", description = "Deactivate a vendor.")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PutMapping("/vendors/{id}/deactivate")
    public ResponseEntity<String> deactivateVendor(@PathVariable Long id) {
        vendorService.deactivateVendor(id);
        return ResponseEntity.ok("Vendor deactivated");
    }

    // Delete a vendor by ID
    @Operation(summary = "Delete Vendor", description = "Delete a vendor by their ID.")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @DeleteMapping("/vendors/{id}")
    public ResponseEntity<String> deleteVendor(@PathVariable Long id) {
        try {
            vendorService.deleteVendorById(id);
            return ResponseEntity.ok("Vendor deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());
        }
    }


    // Get vendor details by ID
    @Operation(summary = "Get Vendor Details", description = "Retrieve the details of a specific vendor by their ID.")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/vendors/{id}")
    public ResponseEntity<Vendor> getVendorById(@PathVariable Long id) {
        Vendor vendor = vendorService.findVendorById(id);
        return ResponseEntity.ok(vendor);
    }

    // Both Admin and Super-Admin: Promote vendor to admin
    @Operation(summary = "Promote Vendor to Admin", description = "Promote an existing vendor to admin. Accessible to both Admins and Super-Admins.")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/vendors/{id}/promote")
    public ResponseEntity<String> promoteVendorToAdmin(@PathVariable Long id) {
        try {
            vendorService.promoteVendorToAdmin(id);
            return ResponseEntity.ok("Vendor promoted to admin successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error promoting vendor to admin: " + e.getMessage());
        }
    }
    
    @Operation(summary = "Demote Admin to Vendor", description = "Demote an existing admin to vendor.")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PutMapping("/vendors/{id}/demote")
    public ResponseEntity<String> demoteVendorFromAdmin(@PathVariable Long id) {
        try {
            vendorService.demoteVendorFromAdmin(id);
            return ResponseEntity.ok("Admin demoted to vendor successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error demoting admin: " + e.getMessage());
        }
    }

}
