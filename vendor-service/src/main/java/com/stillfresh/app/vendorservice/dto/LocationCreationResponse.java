package com.stillfresh.app.vendorservice.dto;

/**
 * Response DTO for chain location creation
 * Includes location details and email sending status
 */
public class LocationCreationResponse {
    
    private Long locationId;
    private String locationName;
    private String email;
    private boolean emailSent;
    private String emailError;
    private String username;
    /** @deprecated Never populated — plaintext passwords must not leave the server. */
    @Deprecated
    private String password;
    private String message;
    /** False when the location still needs a payout account before it may publish offers. */
    private boolean paymentAccountReady;
    
    public LocationCreationResponse() {
    }
    
    public LocationCreationResponse(Long locationId, String locationName, String email, 
                                   boolean emailSent, String message) {
        this.locationId = locationId;
        this.locationName = locationName;
        this.email = email;
        this.emailSent = emailSent;
        this.message = message;
    }
    
    // Getters and Setters
    
    public Long getLocationId() {
        return locationId;
    }
    
    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }
    
    public String getLocationName() {
        return locationName;
    }
    
    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public boolean isEmailSent() {
        return emailSent;
    }
    
    public void setEmailSent(boolean emailSent) {
        this.emailSent = emailSent;
    }
    
    public String getEmailError() {
        return emailError;
    }
    
    public void setEmailError(String emailError) {
        this.emailError = emailError;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public boolean isPaymentAccountReady() {
        return paymentAccountReady;
    }
    
    public void setPaymentAccountReady(boolean paymentAccountReady) {
        this.paymentAccountReady = paymentAccountReady;
    }
}

