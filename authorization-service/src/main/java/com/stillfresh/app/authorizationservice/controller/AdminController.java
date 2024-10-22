package com.stillfresh.app.authorizationservice.controller;

import com.stillfresh.app.authorizationservice.model.User;
import com.stillfresh.app.authorizationservice.service.UserService;
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
    private UserService userService;
    
    // Method to create a Super-Admin
    @Operation(summary = "Create Initial Super-Admin", description = "Create the initial super-admin. Can only be used if no Super-Admin exists.")
    @PostMapping("/create-initial-admin")
    public ResponseEntity<String> createInitialAdmin(@RequestBody User admin) {
        // Check if a Super-Admin already exists
        if (userService.hasSuperAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Super-Admin already exists. You cannot create more than one Super-Admin.");
        }

        try {
            // Register a Super-Admin
            userService.registerSuperAdmin(admin);
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
            userService.deleteAdminById(id);
            return ResponseEntity.ok("Admin deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error deleting admin: " + e.getMessage());
        }
    }

    // Get all users
    @Operation(summary = "Get All Users", description = "Retrieve a list of all registered users.")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    // Activate or deactivate a user
    @Operation(summary = "Activate User", description = "Activate a user.")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PutMapping("/users/{id}/activate")
    public ResponseEntity<String> activateUser(@PathVariable Long id) {
        userService.activateUser(id);
        return ResponseEntity.ok("User activated");
    }

    // Deactivate a user
    @Operation(summary = "Deactivate User", description = "Deactivate a user.")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PutMapping("/users/{id}/deactivate")
    public ResponseEntity<String> deactivateUser(@PathVariable Long id) {
        userService.deactivateUser(id);
        return ResponseEntity.ok("User deactivated");
    }

    // Delete a user by ID
    @Operation(summary = "Delete User", description = "Delete a user by their ID.")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUserById(id);
            return ResponseEntity.ok("User deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());
        }
    }

    // Get user details by ID
    @Operation(summary = "Get User Details", description = "Retrieve the details of a specific user by their ID.")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.findUserById(id);
        return ResponseEntity.ok(user);
    }

    // Both Admin and Super-Admin: Promote user to admin
    @Operation(summary = "Promote User to Admin", description = "Promote an existing user to admin. Accessible to both Admins and Super-Admins.")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/users/{id}/promote")
    public ResponseEntity<String> promoteUserToAdmin(@PathVariable Long id) {
        try {
            userService.promoteUserToAdmin(id);
            return ResponseEntity.ok("User promoted to admin successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error promoting user to admin: " + e.getMessage());
        }
    }
    
    @Operation(summary = "Demote Admin to User", description = "Demote an existing admin to a regular user.")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PutMapping("/users/{id}/demote")
    public ResponseEntity<String> demoteAdminFromUser(@PathVariable Long id) {
        try {
            userService.demoteAdminFromUser(id);
            return ResponseEntity.ok("Admin demoted to user successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error demoting admin: " + e.getMessage());
        }
    }

}
