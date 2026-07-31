package com.stillfresh.app.paymentservice.dto;

/**
 * Poll result for an in-flight AllSecure preauthorization tied to a place-order {@code requestId}.
 */
public class PaymentStatusResponse {

    private String requestId;
    private String status;
    private String redirectUrl;
    private Long offerId;
    private String paymentIntentId;
    private String failureReason;
    private String message;

    public static PaymentStatusResponse processing(String requestId) {
        PaymentStatusResponse r = new PaymentStatusResponse();
        r.requestId = requestId;
        r.status = "PROCESSING";
        r.message = "Payment is being processed.";
        return r;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRedirectUrl() { return redirectUrl; }
    public void setRedirectUrl(String redirectUrl) { this.redirectUrl = redirectUrl; }

    public Long getOfferId() { return offerId; }
    public void setOfferId(Long offerId) { this.offerId = offerId; }

    public String getPaymentIntentId() { return paymentIntentId; }
    public void setPaymentIntentId(String paymentIntentId) { this.paymentIntentId = paymentIntentId; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
