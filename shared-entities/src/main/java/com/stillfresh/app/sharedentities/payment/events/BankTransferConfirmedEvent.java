package com.stillfresh.app.sharedentities.payment.events;

/**
 * Published by payment-service when an admin confirms that a bank transfer has been received.
 * Downstream consumers (notification-service, order-service) can react accordingly.
 */
public class BankTransferConfirmedEvent {

    private Long orderId;
    private Long userId;
    private Long vendorId;
    private String paymentReference;
    private Long grossAmountCents;
    private Long platformFeeCents;
    private Long netAmountCents;
    private String currency;
    private String confirmedBy;
    /** Platform fee percentage applied at confirmation time (e.g. 10.0). */
    private Double feePercentApplied;

    public BankTransferConfirmedEvent() {}

    public BankTransferConfirmedEvent(Long orderId, Long userId, Long vendorId,
                                       String paymentReference, Long grossAmountCents,
                                       Long platformFeeCents, Long netAmountCents,
                                       String currency, String confirmedBy, Double feePercentApplied) {
        this.orderId = orderId;
        this.userId = userId;
        this.vendorId = vendorId;
        this.paymentReference = paymentReference;
        this.grossAmountCents = grossAmountCents;
        this.platformFeeCents = platformFeeCents;
        this.netAmountCents = netAmountCents;
        this.currency = currency;
        this.confirmedBy = confirmedBy;
        this.feePercentApplied = feePercentApplied;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public Long getGrossAmountCents() { return grossAmountCents; }
    public void setGrossAmountCents(Long grossAmountCents) { this.grossAmountCents = grossAmountCents; }

    public Long getPlatformFeeCents() { return platformFeeCents; }
    public void setPlatformFeeCents(Long platformFeeCents) { this.platformFeeCents = platformFeeCents; }

    public Long getNetAmountCents() { return netAmountCents; }
    public void setNetAmountCents(Long netAmountCents) { this.netAmountCents = netAmountCents; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(String confirmedBy) { this.confirmedBy = confirmedBy; }

    public Double getFeePercentApplied() { return feePercentApplied; }
    public void setFeePercentApplied(Double feePercentApplied) { this.feePercentApplied = feePercentApplied; }
}
