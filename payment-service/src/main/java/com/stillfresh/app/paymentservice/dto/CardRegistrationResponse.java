package com.stillfresh.app.paymentservice.dto;

public class CardRegistrationResponse {
    private String customerId;
    private String message;

    public CardRegistrationResponse(String customerId, String message) {
        this.customerId = customerId;
        this.message = message;
    }

    public String getCustomerId() { return customerId; }
    public String getMessage() { return message; }
}
