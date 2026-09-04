package com.stillfresh.app.sharedentities.payment.events;

public class PaymentSuccessEvent {
    private String requestId;
    private Long userId;
    private Long offerId;
    private String paymentIntentId;  // Stripe PaymentIntent ID for manual capture/cancel

    /** Active provider that authorized the payment: {@code stripe} or {@code allsecure}. */
    private String paymentProvider;

    public PaymentSuccessEvent() {}

    public PaymentSuccessEvent(String requestId, Long userId, Long offerId) {
        this.requestId = requestId;
        this.userId = userId;
        this.offerId = offerId;
    }

    public PaymentSuccessEvent(String requestId, Long userId, Long offerId, String paymentIntentId) {
        this.requestId = requestId;
        this.userId = userId;
        this.offerId = offerId;
        this.paymentIntentId = paymentIntentId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getOfferId() {
        return offerId;
    }

    public void setOfferId(Long offerId) {
        this.offerId = offerId;
    }

    public String getPaymentIntentId() {
        return paymentIntentId;
    }

    public void setPaymentIntentId(String paymentIntentId) {
        this.paymentIntentId = paymentIntentId;
    }

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public void setPaymentProvider(String paymentProvider) {
        this.paymentProvider = paymentProvider;
    }

    @Override
    public String toString() {
        return "PaymentSuccessEvent{" +
                "requestId='" + requestId + '\'' +
                ", userId=" + userId +
                ", offerId=" + offerId +
                ", paymentIntentId='" + paymentIntentId + '\'' +
                '}';
    }
}
