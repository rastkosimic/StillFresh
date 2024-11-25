package com.stillfresh.app.sharedentities.dto;

public class CheckAvailabilityRequest {
    
    private String username;

    private String email;
    
    // Constructors, Getters, Setters

    public CheckAvailabilityRequest() {}

    public CheckAvailabilityRequest(String username, String email) {
        this.username = username;
        this.email = email;
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
}
