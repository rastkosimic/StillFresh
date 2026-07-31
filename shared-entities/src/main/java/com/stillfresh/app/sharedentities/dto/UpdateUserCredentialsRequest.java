package com.stillfresh.app.sharedentities.dto;

import com.stillfresh.app.sharedentities.enums.Status;

/**
 * Request body for syncing a service-owned password hash into authorization-service.
 * Sent as JSON because BCrypt hashes contain {@code $} and {@code /}, which are corrupted
 * when passed as URL query parameters.
 */
public class UpdateUserCredentialsRequest {

    private Long globalUserId;
    private String encodedPassword;
    private Status status;

    public UpdateUserCredentialsRequest() {
    }

    public UpdateUserCredentialsRequest(Long globalUserId, String encodedPassword, Status status) {
        this.globalUserId = globalUserId;
        this.encodedPassword = encodedPassword;
        this.status = status;
    }

    public Long getGlobalUserId() {
        return globalUserId;
    }

    public void setGlobalUserId(Long globalUserId) {
        this.globalUserId = globalUserId;
    }

    public String getEncodedPassword() {
        return encodedPassword;
    }

    public void setEncodedPassword(String encodedPassword) {
        this.encodedPassword = encodedPassword;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
