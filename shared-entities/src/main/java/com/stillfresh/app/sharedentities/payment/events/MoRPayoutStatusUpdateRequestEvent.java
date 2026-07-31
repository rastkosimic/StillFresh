package com.stillfresh.app.sharedentities.payment.events;

/**
 * Event sent from payment-service to vendor-service to update payout status
 */
public class MoRPayoutStatusUpdateRequestEvent {
    private String requestId;
    private Long payoutId;
    private String status; // PROCESSING, COMPLETED, FAILED
    private String transactionReference; // Optional
    private String notes; // Optional

    public MoRPayoutStatusUpdateRequestEvent() {}

    public MoRPayoutStatusUpdateRequestEvent(String requestId, Long payoutId, String status) {
        this.requestId = requestId;
        this.payoutId = payoutId;
        this.status = status;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

