package com.stillfresh.app.sharedentities.payment.events;

/**
 * Fired by payment-service after a PaymentIntent is successfully captured.
 * Carries the full financial breakdown so consumers can update their own records.
 */
public class OrderPaymentSettledEvent {

    private String paymentIntentId;
    private Long userId;
    private Long vendorId;
    private Long offerId;
    /** Total amount charged to the customer, in minor currency units (e.g. cents). */
    private Long grossAmountCents;
    /** Platform fee retained by StillFresh, in minor currency units. */
    private Long platformFeeCents;
    /** Net amount credited to the vendor, in minor currency units. */
    private Long netAmountCents;
    /** ISO 4217 currency code, e.g. "RSD", "EUR". */
    private String currency;
    /** Platform fee percentage applied at settlement time (e.g. 10.0). */
    private Double feePercentApplied;

    public OrderPaymentSettledEvent() {}

    public OrderPaymentSettledEvent(String paymentIntentId, Long userId, Long vendorId, Long offerId,
                                    Long grossAmountCents, Long platformFeeCents, Long netAmountCents,
                                    String currency, Double feePercentApplied) {
        this.paymentIntentId = paymentIntentId;
        this.userId = userId;
        this.vendorId = vendorId;
        this.offerId = offerId;
        this.grossAmountCents = grossAmountCents;
        this.platformFeeCents = platformFeeCents;
        this.netAmountCents = netAmountCents;
        this.currency = currency;
        this.feePercentApplied = feePercentApplied;
    }

    public String getPaymentIntentId() { return paymentIntentId; }
    public void setPaymentIntentId(String paymentIntentId) { this.paymentIntentId = paymentIntentId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public Long getOfferId() { return offerId; }
    public void setOfferId(Long offerId) { this.offerId = offerId; }

    public Long getGrossAmountCents() { return grossAmountCents; }
    public void setGrossAmountCents(Long grossAmountCents) { this.grossAmountCents = grossAmountCents; }

    public Long getPlatformFeeCents() { return platformFeeCents; }
    public void setPlatformFeeCents(Long platformFeeCents) { this.platformFeeCents = platformFeeCents; }

    public Long getNetAmountCents() { return netAmountCents; }
    public void setNetAmountCents(Long netAmountCents) { this.netAmountCents = netAmountCents; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Double getFeePercentApplied() { return feePercentApplied; }
    public void setFeePercentApplied(Double feePercentApplied) { this.feePercentApplied = feePercentApplied; }
}
