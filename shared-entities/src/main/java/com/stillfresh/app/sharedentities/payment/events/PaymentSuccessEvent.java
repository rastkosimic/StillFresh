package com.stillfresh.app.sharedentities.payment.events;

public class PaymentSuccessEvent {
    private String requestId;
    private Long userId;
    private Long offerId;

    public PaymentSuccessEvent() {}

    public PaymentSuccessEvent(String requestId, Long userId, Long offerId) {
        this.requestId = requestId;
        this.userId = userId;
        this.offerId = offerId;
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

    @Override
    public String toString() {
        return "PaymentSuccessEvent{" +
                "requestId='" + requestId + '\'' +
                ", userId=" + userId +
                ", offerId=" + offerId +
                '}';
    }
}
