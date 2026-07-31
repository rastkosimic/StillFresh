package com.stillfresh.app.sharedentities.payment.events;

/**
 * Event sent from vendor-service to payment-service confirming payout status update
 */
public class MoRPayoutStatusUpdateResponseEvent {
    private String requestId;
    private Long payoutId;
    private boolean success;
    private String errorMessage;

    public MoRPayoutStatusUpdateResponseEvent() {}

    public MoRPayoutStatusUpdateResponseEvent(String requestId, Long payoutId, boolean success) {
        this.requestId = requestId;
        this.payoutId = payoutId;
        this.success = success;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Long getPayoutId() {
        return payoutId;
    }

    public void setPayoutId(Long payoutId) {
        this.payoutId = payoutId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

