package com.stillfresh.app.vendorservice.dto;

/**
 * DTO for vendor credentials sent after admin verification
 */
public class VendorCredentialsResponse {
    
    private String email;
    private String temporaryPassword;
    private String loginUrl;
    private String message;
    
    public VendorCredentialsResponse() {
    }
    
    public VendorCredentialsResponse(String email, String temporaryPassword, String loginUrl, String message) {
        this.email = email;
        this.temporaryPassword = temporaryPassword;
        this.loginUrl = loginUrl;
        this.message = message;
    }
    
    // Getters and Setters
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getTemporaryPassword() {
        return temporaryPassword;
    }
    
    public void setTemporaryPassword(String temporaryPassword) {
        this.temporaryPassword = temporaryPassword;
    }
    
    public String getLoginUrl() {
        return loginUrl;
    }
    
    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}

