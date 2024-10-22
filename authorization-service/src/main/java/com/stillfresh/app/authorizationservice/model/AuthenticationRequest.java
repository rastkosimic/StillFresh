package com.stillfresh.app.authorizationservice.model;

public class AuthenticationRequest {
    private String identifier; // This will be used for both email or username
    private String password;

    // Getters and setters
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
