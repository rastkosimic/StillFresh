package com.stillfresh.app.sharedentities.shared.events;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;

public class TokenValidationResponseEvent {
    private boolean valid;
    private String username;
    private String email;
    private String correlationId;
    private String message; // Optional for error messages
    private Collection<? extends GrantedAuthority> authorities; // User authorities

    public TokenValidationResponseEvent() {}

    public TokenValidationResponseEvent(boolean valid, String username, String email, String correlationId, String message) {
        this.valid = valid;
        this.username = username;
        this.email = email;
        this.correlationId = correlationId;
        this.message = message;
    }
    
    public TokenValidationResponseEvent(boolean valid, String username, String email, String correlationId, String message,  Collection<? extends GrantedAuthority> authorities) {
        this.valid = valid;
        this.username = username;
        this.email = email;
        this.correlationId = correlationId;
        this.message = message;
        this.authorities = authorities;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
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

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(Collection<? extends GrantedAuthority> authorities) {
        this.authorities = authorities;
    }
}
