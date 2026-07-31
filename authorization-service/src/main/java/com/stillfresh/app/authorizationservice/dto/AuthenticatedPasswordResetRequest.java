package com.stillfresh.app.authorizationservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthenticatedPasswordResetRequest {

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "New password cannot be blank")
    @Size(min = 6, message = "New password must be at least 6 characters long")
    private String newPassword;
    
    @NotBlank(message = "Confirm password cannot be blank")
    @Size(min = 6, message = "Confirm password must be at least 6 characters long")
    private String confirmPassword;
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getNewPassword() {
        return newPassword;
    }
    
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
    
    public String getConfirmPassword() {
        return confirmPassword;
    }
    
    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}

