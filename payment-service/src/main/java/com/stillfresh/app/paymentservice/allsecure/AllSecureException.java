package com.stillfresh.app.paymentservice.allsecure;

/**
 * Raised when an AllSecure transaction request fails (transport error or a gateway-level error result).
 */
public class AllSecureException extends RuntimeException {

    private final String errorCode;

    public AllSecureException(String message) {
        this(message, null, null);
    }

    public AllSecureException(String message, String errorCode) {
        this(message, errorCode, null);
    }

    public AllSecureException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
