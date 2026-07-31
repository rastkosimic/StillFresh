package com.stillfresh.app.sharedentities.payment.events;

/**
 * Event sent from payment-service to vendor-service to request vendor payment information
 * (payout model and Stripe account ID) for processing payments
 */
public class VendorPaymentInfoRequestEvent {
    private String requestId;
    private Long vendorId;

    public VendorPaymentInfoRequestEvent() {}

    public VendorPaymentInfoRequestEvent(String requestId, Long vendorId) {
        this.requestId = requestId;
        this.vendorId = vendorId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }
}

