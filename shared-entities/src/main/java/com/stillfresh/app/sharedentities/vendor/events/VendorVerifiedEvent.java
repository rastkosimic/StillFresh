package com.stillfresh.app.sharedentities.vendor.events;

public class VendorVerifiedEvent {
    private String email;
    
    public VendorVerifiedEvent() {
    }

    public VendorVerifiedEvent(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
