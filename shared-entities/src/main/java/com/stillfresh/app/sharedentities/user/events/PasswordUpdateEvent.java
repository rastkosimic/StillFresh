package com.stillfresh.app.sharedentities.user.events;

import com.stillfresh.app.sharedentities.enums.Role;

public class PasswordUpdateEvent {
    
    private Long userId;
    private String email;
    private String encodedPassword;
    
    private Role role;

    public PasswordUpdateEvent() {
    }

    public PasswordUpdateEvent(Long userId, String email, String encodedPassword, Role role) {
        this.userId = userId;
        this.email = email;
        this.encodedPassword = encodedPassword;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEncodedPassword() {
        return encodedPassword;
    }

    public void setEncodedPassword(String encodedPassword) {
        this.encodedPassword = encodedPassword;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "PasswordUpdateEvent{" +
                "userId=" + userId +
                ", email='" + email + '\'' +
                ", role=" + role +
                '}';
    }
}

