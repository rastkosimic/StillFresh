package com.stillfresh.app.sharedentities.order.events;

/**
 * Published by order-service after saving a bank-transfer order.
 * Payment-service listens to this, creates a BankTransferPayment record, and
 * publishes BankTransferInitiatedEvent with the payment instructions.
 */
public class BankTransferOrderEvent {

    private Long orderId;
    private Long userId;
    private Long vendorId;
    private Long offerId;
    /** Total amount the customer must transfer, in minor currency units (e.g. cents). */
    private Long grossAmountCents;
    /** ISO 4217 currency code. */
    private String currency;

    public BankTransferOrderEvent() {}

    public BankTransferOrderEvent(Long orderId, Long userId, Long vendorId, Long offerId,
                                   Long grossAmountCents, String currency) {
        this.orderId = orderId;
        this.userId = userId;
        this.vendorId = vendorId;
        this.offerId = offerId;
        this.grossAmountCents = grossAmountCents;
        this.currency = currency;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public Long getOfferId() { return offerId; }
    public void setOfferId(Long offerId) { this.offerId = offerId; }

    public Long getGrossAmountCents() { return grossAmountCents; }
    public void setGrossAmountCents(Long grossAmountCents) { this.grossAmountCents = grossAmountCents; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
