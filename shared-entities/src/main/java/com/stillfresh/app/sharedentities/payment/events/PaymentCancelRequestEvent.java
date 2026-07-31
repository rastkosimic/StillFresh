package com.stillfresh.app.sharedentities.payment.events;

/**
 * Event published when an order is cancelled and the associated PaymentIntent needs to be cancelled.
 * This is used in the Too Good To Go style payment flow where payment is authorized at order placement
 * and must be cancelled if the order is cancelled before pickup.
 */
public class PaymentCancelRequestEvent {
    private String paymentIntentId;
    private Long orderId;
    private Long userId;
    private String reason;  // Optional reason for cancellation

    public PaymentCancelRequestEvent() {}

    public PaymentCancelRequestEvent(String paymentIntentId, Long orderId, Long userId) {
        this.paymentIntentId = paymentIntentId;
        this.orderId = orderId;
        this.userId = userId;
    }

    public PaymentCancelRequestEvent(String paymentIntentId, Long orderId, Long userId, String reason) {
        this.paymentIntentId = paymentIntentId;
        this.orderId = orderId;
        this.userId = userId;
        this.reason = reason;
    }

    public String getPaymentIntentId() {
        return paymentIntentId;
    }

    public void setPaymentIntentId(String paymentIntentId) {
        this.paymentIntentId = paymentIntentId;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "PaymentCancelRequestEvent{" +
                "paymentIntentId='" + paymentIntentId + '\'' +
                ", orderId=" + orderId +
                ", userId=" + userId +
                ", reason='" + reason + '\'' +
                '}';
    }
}

