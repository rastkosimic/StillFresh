package com.stillfresh.app.sharedentities.user.events;

public class UserVerifiedEvent {
    private String email;
    
    public UserVerifiedEvent() {
    }

    public UserVerifiedEvent(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
