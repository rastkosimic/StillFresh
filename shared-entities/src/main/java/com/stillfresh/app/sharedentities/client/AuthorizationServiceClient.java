package com.stillfresh.app.sharedentities.client;

import com.stillfresh.app.sharedentities.dto.CheckAvailabilityRequest;
import com.stillfresh.app.sharedentities.enums.Role;
import com.stillfresh.app.sharedentities.enums.Status;
import com.stillfresh.app.sharedentities.responses.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "authorization-service")
public interface AuthorizationServiceClient {

    /**
     * Generates a global user ID for any user across all services.
     * 
     * @param request UserIdRequest containing email, username, and role
     * @return Response containing the global user ID
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
     * Verifies a user by their global ID.
     * 
     * @param globalUserId The global user ID
     * @return Response indicating success or failure
     */
    @PostMapping("/api/auth/verify-user")
    Map<String, Object> verifyUser(@RequestParam("globalUserId") Long globalUserId);

    /**
     * Gets user information by global ID.
     * 
     * @param globalUserId The global user ID
     * @return Response containing user information
     */
    @GetMapping("/api/auth/user/{globalUserId}")
    Map<String, Object> getUserByGlobalId(@PathVariable("globalUserId") Long globalUserId);

    /**
     * Gets user information by email.
     * 
     * @param email User's email
     * @return Response containing user information
     */
    @GetMapping("/api/auth/user/by-email")
    Map<String, Object> getUserByEmail(@RequestParam("email") String email);

    /**
     * Checks if username and email are available for registration.
     * 
     * @param request CheckAvailabilityRequest containing username and email
     * @return Response indicating availability
     */
    @PostMapping("/auth/check-availability")
    ResponseEntity<ApiResponse> checkAvailability(@RequestBody CheckAvailabilityRequest request);

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
