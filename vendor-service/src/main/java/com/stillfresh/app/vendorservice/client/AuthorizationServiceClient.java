package com.stillfresh.app.vendorservice.client;

import com.stillfresh.app.sharedentities.dto.CheckAvailabilityRequest;
import com.stillfresh.app.sharedentities.enums.Role;
import com.stillfresh.app.sharedentities.enums.Status;
import com.stillfresh.app.sharedentities.responses.ApiResponse;
import com.stillfresh.app.vendorservice.config.AuthorizationServiceFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "authorization-service", configuration = AuthorizationServiceFeignConfig.class)
public interface AuthorizationServiceClient {

    /**
     * Generates a global user ID for any user across all services.
     */
    @PostMapping("/api/auth/generate-user-id")
    Map<String, Object> generateUserId(@RequestBody UserIdRequest request);

    /**
     * Updates user credentials after service-specific registration is complete.
     * Password hash must be in the JSON body — BCrypt contains {@code $}/{@code /} which break as query params.
     */
    @PostMapping("/api/auth/update-user-credentials")
    Map<String, Object> updateUserCredentials(
            @RequestBody com.stillfresh.app.sharedentities.dto.UpdateUserCredentialsRequest request);

    /**
     * Updates a user's status without changing their password, so that accounts disabled here
     * are also denied login. Non-ACTIVE statuses revoke the user's refresh-token sessions.
     */
    @PostMapping("/api/auth/update-user-status")
    Map<String, Object> updateUserStatus(
            @RequestParam("globalUserId") Long globalUserId,
            @RequestParam("status") Status status);

    /**
     * Permanently removes a user's credentials, freeing the email for registration again.
     */
    @DeleteMapping("/api/auth/user/{globalUserId}")
    Map<String, Object> deleteUser(@PathVariable("globalUserId") Long globalUserId);

    /**
     * Verifies a user by their global ID.
     */
    @PostMapping("/api/auth/verify-user")
    Map<String, Object> verifyUser(@RequestParam("globalUserId") Long globalUserId);
    
    /**
     * Updates an existing user's role.
     */
    @PostMapping("/api/auth/update-user-role")
    Map<String, Object> updateUserRole(
            @RequestParam("globalUserId") Long globalUserId,
            @RequestParam("role") Role role);

    /**
     * Checks if username and email are available for registration.
     */
    @PostMapping("/auth/check-availability")
    ApiResponse checkAvailability(@RequestBody CheckAvailabilityRequest request);

    /**
     * Request DTO for generating user IDs
     */
    class UserIdRequest {
        private String email;
        private String username;
        private Role role;

        public UserIdRequest() {}

        public UserIdRequest(String email, String username, Role role) {
            this.email = email;
            this.username = username;
            this.role = role;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public Role getRole() {
            return role;
        }

        public void setRole(Role role) {
            this.role = role;
        }
    }
}