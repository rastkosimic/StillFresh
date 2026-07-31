package com.stillfresh.app.paymentservice.exception;

/**
 * Custom exception for payment method operations
 * Used to provide user-friendly error messages with appropriate HTTP status codes
 */
public class PaymentMethodException extends RuntimeException {
    private final String errorCode;
    private final int httpStatus;

    public PaymentMethodException(String message) {
        super(message);
        this.errorCode = "PAYMENT_METHOD_ERROR";
        this.httpStatus = 400; // Bad Request by default
    }

    public PaymentMethodException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}

