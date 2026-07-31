package com.stillfresh.app.vendorservice.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO for updating an existing VENDOR worker. All fields are optional; only the provided ones
 * are applied.
 */
public class WorkerUpdateRequest {

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    private String phone;

    /** Move the worker to another location of the same chain (headquarters only). */
    private Long assignedLocationId;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Long getAssignedLocationId() {
        return assignedLocationId;
    }

    public void setAssignedLocationId(Long assignedLocationId) {
        this.assignedLocationId = assignedLocationId;
    }
}
