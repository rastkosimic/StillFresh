package com.stillfresh.app.sharedentities.payment.events;

/**
 * Published when the customer confirms pickup for a Stripe manual-capture order.
 * Payment-service consumes this and captures the PaymentIntent (Too Good To Go style).
 */
public class PaymentCaptureRequestEvent {

    private String paymentIntentId;
    private Long orderId;
    private Long userId;

    public PaymentCaptureRequestEvent() {}

    public PaymentCaptureRequestEvent(String paymentIntentId, Long orderId, Long userId) {
        this.paymentIntentId = paymentIntentId;
        this.orderId = orderId;
        this.userId = userId;
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

    @Override
    public String toString() {
        return "PaymentCaptureRequestEvent{"
                + "paymentIntentId='" + paymentIntentId + '\''
                + ", orderId=" + orderId
                + ", userId=" + userId
                + '}';
    }
}
