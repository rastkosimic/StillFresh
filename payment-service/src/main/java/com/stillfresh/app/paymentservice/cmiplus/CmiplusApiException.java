package com.stillfresh.app.paymentservice.cmiplus;

public class CmiplusApiException extends RuntimeException {
    public CmiplusApiException(String message) {
        super(message);
    }

    public CmiplusApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
