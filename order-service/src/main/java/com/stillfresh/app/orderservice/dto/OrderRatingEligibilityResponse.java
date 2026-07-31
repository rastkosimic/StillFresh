package com.stillfresh.app.orderservice.dto;

public class OrderRatingEligibilityResponse {

    private Long orderId;
    private Long userId;
    private Long vendorId;
    private String status;
    private boolean eligible;

    public OrderRatingEligibilityResponse() {}

    public OrderRatingEligibilityResponse(Long orderId, Long userId, Long vendorId, String status, boolean eligible) {
        this.orderId = orderId;
        this.userId = userId;
        this.vendorId = vendorId;
        this.status = status;
        this.eligible = eligible;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isEligible() {
        return eligible;
    }

    public void setEligible(boolean eligible) {
        this.eligible = eligible;
    }
}
