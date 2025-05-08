package com.stillfresh.app.paymentservice.dto;

public class PaymentRequest {
    private String paymentMethodId;
    private long amount; // in cents
    private String currency;

    public PaymentRequest(String paymentMethodId, long amount, String currency) {
        this.paymentMethodId = paymentMethodId;
        this.amount = amount;
        this.currency = currency;
    }

    public String getPaymentMethodId() { return paymentMethodId; }
    public long getAmount() { return amount; }
    public String getCurrency() { return currency; }
}
