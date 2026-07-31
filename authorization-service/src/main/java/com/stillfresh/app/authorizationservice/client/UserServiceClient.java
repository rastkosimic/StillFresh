package com.stillfresh.app.authorizationservice.client;

import com.stillfresh.app.authorizationservice.config.UserServiceFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service", configuration = UserServiceFeignConfig.class)
public interface UserServiceClient {

    /**
     * Create a user in user-service with a pre-set global ID (for OAuth2 registration)
     */
    @PostMapping("/users/create-oauth2")
    ResponseEntity<?> createOAuth2User(@RequestBody OAuth2UserRequest request);

    /**
     * Request DTO for OAuth2 user creation
     */
    class OAuth2UserRequest {
        private Long id;
        private String username;
        private String email;
        private String password;
        private String role;
        private String status;
        private String firstName;
        private String lastName;
        private String country;

        public OAuth2UserRequest() {
        }

        public OAuth2UserRequest(Long id, String username, String email, String password, String role, String status) {
            this(id, username, email, password, role, status, null, null, null);
        }

        public OAuth2UserRequest(Long id, String username, String email, String password, String role, String status,
                String firstName, String lastName, String country) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.password = password;
            this.role = role;
            this.status = status;
            this.firstName = firstName;
            this.lastName = lastName;
            this.country = country;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }
    }
}

