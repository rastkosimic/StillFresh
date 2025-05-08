package com.stillfresh.app.paymentservice.dto;

public class CardRegistrationRequest {
    private String paymentMethodId;

    public CardRegistrationRequest() {}

    public CardRegistrationRequest(String paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    public String getPaymentMethodId() { return paymentMethodId; }
}
