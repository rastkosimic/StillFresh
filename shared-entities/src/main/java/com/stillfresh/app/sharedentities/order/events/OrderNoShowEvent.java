package com.stillfresh.app.sharedentities.order.events;

/**
 * Emitted by order-service when an order is expired because the customer did not pick it up
 * within the pickup window. Consumed to increment the user's no-show strike counter.
 */
public class OrderNoShowEvent {

    private Long userId;
    private Long vendorId;
    private String orderId;

    public OrderNoShowEvent() {
    }

    public OrderNoShowEvent(Long userId, Long vendorId, String orderId) {
        this.userId = userId;
        this.vendorId = vendorId;
        this.orderId = orderId;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    @Override
    public String toString() {
        return "OrderNoShowEvent{userId=" + userId + ", vendorId=" + vendorId + ", orderId='" + orderId + "'}";
    }
}
