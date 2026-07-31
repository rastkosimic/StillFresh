package com.stillfresh.app.authorizationservice.controller;

import com.stillfresh.app.authorizationservice.dto.UserIdRequest;
import com.stillfresh.app.authorizationservice.service.IdGenerationService;
import com.stillfresh.app.authorizationservice.model.User;
import com.stillfresh.app.sharedentities.enums.Status;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class IdController {

    private static final Logger logger = LoggerFactory.getLogger(IdController.class);

    @Autowired
    private IdGenerationService idGenerationService;

    /**
     * Generates a global user ID for any user across all services.
     * This endpoint is called by user-service and vendor-service during registration.
     */
    @PostMapping("/generate-user-id")
    public ResponseEntity<Map<String, Object>> generateUserId(@Valid @RequestBody UserIdRequest request) {
        try {
            logger.info("Received request to generate user ID for: {}", request);
            
            Long globalUserId = idGenerationService.generateUserId(
                request.getEmail(), 
                request.getUsername(), 
                request.getRole()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("globalUserId", globalUserId);
            response.put("message", "Global user ID generated successfully");
            
            logger.info("Generated global user ID: {} for email: {}", globalUserId, request.getEmail());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to generate user ID for: {}", request, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to generate user ID: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Updates user credentials after service-specific registration is complete.
     * The password hash must travel in the JSON body: BCrypt values contain {@code $} and
     * {@code /}, which are corrupted when sent as query parameters.
     */
    @PostMapping("/update-user-credentials")
    public ResponseEntity<Map<String, Object>> updateUserCredentials(
            @RequestBody com.stillfresh.app.sharedentities.dto.UpdateUserCredentialsRequest request) {
        try {
            if (request == null || request.getGlobalUserId() == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "globalUserId is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            logger.info("Updating credentials for global user ID: {}", request.getGlobalUserId());
            
            idGenerationService.updateUserCredentials(
                request.getGlobalUserId(),
                request.getEncodedPassword(),
                request.getStatus());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User credentials updated successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Long id = request != null ? request.getGlobalUserId() : null;
            logger.error("Failed to update credentials for global user ID: {}", id, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update credentials: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Updates a user's status without changing their password.
     * Called by user-service and vendor-service whenever they activate, deactivate or suspend
     * an account, so login is denied here for accounts the owning service has disabled.
     */
    @PostMapping("/update-user-status")
    public ResponseEntity<Map<String, Object>> updateUserStatus(
            @RequestParam Long globalUserId,
            @RequestParam Status status) {
        try {
            logger.info("Updating status for global user ID: {} to {}", globalUserId, status);

            idGenerationService.updateUserStatus(globalUserId, status);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User status updated successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to update status for global user ID: {}", globalUserId, e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update status: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Permanently deletes a user's credentials.
     * Called by the owning service when an account is deleted outright, so the credentials stop
     * working and the email becomes available for registration again.
     */
    @DeleteMapping("/user/{globalUserId}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long globalUserId) {
        try {
            logger.info("Deleting authorization record for global user ID: {}", globalUserId);

            idGenerationService.deleteUserAccount(globalUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User deleted successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to delete authorization record for global user ID: {}", globalUserId, e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to delete user: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Verifies a user by their global ID.
     * This endpoint is called when a user verifies their email.
     */
    @PostMapping("/verify-user")
    public ResponseEntity<Map<String, Object>> verifyUser(@RequestParam Long globalUserId) {
        try {
            logger.info("Verifying user with global ID: {}", globalUserId);
            
            idGenerationService.verifyUser(globalUserId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User verified successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to verify user with global ID: {}", globalUserId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to verify user: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Gets user information by global ID.
     * This endpoint can be used by other services to get user information.
     */
    @GetMapping("/user/{globalUserId}")
    public ResponseEntity<Map<String, Object>> getUserByGlobalId(@PathVariable Long globalUserId) {
        try {
            logger.info("Getting user information for global ID: {}", globalUserId);
            
            User user = idGenerationService.getUserByGlobalId(globalUserId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "username", user.getUsername(),
                "role", user.getRole(),
                "status", user.getStatus()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get user information for global ID: {}", globalUserId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get user information: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Updates an existing user's role.
     * This endpoint is called when a user's role needs to be changed (e.g., VENDOR to VENDOR_ADMIN).
     */
    @PostMapping("/update-user-role")
    public ResponseEntity<Map<String, Object>> updateUserRole(
            @RequestParam Long globalUserId,
            @RequestParam com.stillfresh.app.sharedentities.enums.Role role) {
        try {
            logger.info("Updating role for global user ID: {} to role: {}", globalUserId, role);
            
            idGenerationService.updateUserRole(globalUserId, role);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User role updated successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to update role for global user ID: {}", globalUserId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update role: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    /**
     * Gets user information by email.
     * This endpoint can be used by other services to get user information.
     */
    @GetMapping("/user/by-email")
    public ResponseEntity<Map<String, Object>> getUserByEmail(@RequestParam String email) {
        try {
            logger.info("Getting user information for email: {}", email);
            
            User user = idGenerationService.getUserByEmail(email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "username", user.getUsername(),
                "role", user.getRole(),
                "status", user.getStatus()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get user information for email: {}", email, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get user information: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
}
