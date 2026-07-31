package com.stillfresh.app.paymentservice.dto;

public class StripeConnectResponse {
    private String value;

    public StripeConnectResponse() {
    }

    public StripeConnectResponse(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}




