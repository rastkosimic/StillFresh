package com.stillfresh.app.vendorservice.dto;

/**
 * Optional request body for DELETE vendor account.
 * Allows the client to send a reason and optional message for deletion feedback.
 * Reason values: "other", "too_expensive", "not_using", "privacy".
 */
public class DeleteVendorAccountRequest {

    private String reason;
    private String message;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
