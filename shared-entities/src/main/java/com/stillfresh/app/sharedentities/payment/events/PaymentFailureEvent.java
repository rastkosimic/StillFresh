package com.stillfresh.app.sharedentities.payment.events;

public class PaymentFailureEvent {
    private String requestId;
    private Long userId;
    private Long offerId;
    private String failureReason;

    public PaymentFailureEvent() {}

    public PaymentFailureEvent(String requestId, Long userId, Long offerId, String failureReason) {
        this.requestId = requestId;
        this.userId = userId;
        this.offerId = offerId;
        this.failureReason = failureReason;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getOfferId() {
        return offerId;
    }

    public void setOfferId(Long offerId) {
        this.offerId = offerId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    @Override
    public String toString() {
        return "PaymentFailureEvent{" +
                "requestId='" + requestId + '\'' +
                ", userId=" + userId +
                ", offerId=" + offerId +
                ", failureReason='" + failureReason + '\'' +
                '}';
    }
}
