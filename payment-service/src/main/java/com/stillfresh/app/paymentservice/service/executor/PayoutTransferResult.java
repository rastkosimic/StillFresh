package com.stillfresh.app.paymentservice.service.executor;

/**
 * Result of a single bank transfer execution attempt.
 */
public class PayoutTransferResult {

    private final boolean success;
    /** Bank-assigned transaction reference (null on failure). */
    private final String externalReference;
    private final String errorMessage;

    private PayoutTransferResult(boolean success, String externalReference, String errorMessage) {
        this.success = success;
        this.externalReference = externalReference;
        this.errorMessage = errorMessage;
    }

    public static PayoutTransferResult success(String externalReference) {
        return new PayoutTransferResult(true, externalReference, null);
    }

    public static PayoutTransferResult failure(String errorMessage) {
        return new PayoutTransferResult(false, null, errorMessage);
    }

    public boolean isSuccess() { return success; }
    public String getExternalReference() { return externalReference; }
    public String getErrorMessage() { return errorMessage; }
}
