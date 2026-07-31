package com.stillfresh.app.sharedentities.order.events;

/**
 * Emitted by order-service when a cancellation is flagged as a potential user-vendor bypass
 * (customer physically at the pickup location, within the active pickup window, then cancels).
 * Consumed to increment bypass strike counters for both the user and the vendor.
 */
public class FraudFlagEvent {

    private Long userId;
    private Long vendorId;
    private String orderId;
    private String reason;

    public FraudFlagEvent() {
    }

    public FraudFlagEvent(Long userId, Long vendorId, String orderId, String reason) {
        this.userId = userId;
        this.vendorId = vendorId;
        this.orderId = orderId;
        this.reason = reason;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    @Override
    public String toString() {
        return "FraudFlagEvent{userId=" + userId + ", vendorId=" + vendorId
                + ", orderId='" + orderId + "', reason='" + reason + "'}";
    }
}
