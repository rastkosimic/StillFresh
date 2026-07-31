package com.stillfresh.app.authorizationservice.dto;

import com.stillfresh.app.sharedentities.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UserIdRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotNull(message = "Role is required")
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

    @Override
    public String toString() {
        return "UserIdRequest{" +
                "email='" + email + '\'' +
                ", username='" + username + '\'' +
                ", role=" + role +
                '}';
    }
}
