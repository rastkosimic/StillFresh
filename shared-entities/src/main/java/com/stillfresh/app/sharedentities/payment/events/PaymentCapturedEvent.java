package com.stillfresh.app.sharedentities.payment.events;

public class PaymentCapturedEvent {
    private String paymentIntentId;
    private String status;  // PaymentIntent status (should be "succeeded")

    public PaymentCapturedEvent() {}

    public PaymentCapturedEvent(String paymentIntentId, String status) {
        this.paymentIntentId = paymentIntentId;
        this.status = status;
    }

    public String getPaymentIntentId() {
        return paymentIntentId;
    }

    public void setPaymentIntentId(String paymentIntentId) {
        this.paymentIntentId = paymentIntentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "PaymentCapturedEvent{" +
                "paymentIntentId='" + paymentIntentId + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}

