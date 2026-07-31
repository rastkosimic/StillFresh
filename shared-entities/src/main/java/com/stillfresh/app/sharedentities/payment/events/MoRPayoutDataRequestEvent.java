package com.stillfresh.app.sharedentities.payment.events;

import java.time.OffsetDateTime;

/**
 * Event sent from payment-service to vendor-service to request MoR payout data
 */
public class MoRPayoutDataRequestEvent {
    private String requestId;
    private String requestType; // "PENDING_PAYOUTS", "VENDOR_BALANCES", "ORDER_PAYMENTS", "PAYOUT_SUMMARY", "VENDOR_PAYOUTS"
    private Long vendorId; // Optional, for vendor-specific requests
    private OffsetDateTime fromDate; // Optional, for date filtering
    private OffsetDateTime toDate; // Optional, for date filtering

    public MoRPayoutDataRequestEvent() {}

    public MoRPayoutDataRequestEvent(String requestId, String requestType) {
        this.requestId = requestId;
        this.requestType = requestType;
    }

    public MoRPayoutDataRequestEvent(String requestId, String requestType, Long vendorId) {
        this.requestId = requestId;
        this.requestType = requestType;
        this.vendorId = vendorId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public OffsetDateTime getFromDate() {
        return fromDate;
    }

    public void setFromDate(OffsetDateTime fromDate) {
        this.fromDate = fromDate;
    }

    public OffsetDateTime getToDate() {
        return toDate;
    }

    public void setToDate(OffsetDateTime toDate) {
        this.toDate = toDate;
    }
}

