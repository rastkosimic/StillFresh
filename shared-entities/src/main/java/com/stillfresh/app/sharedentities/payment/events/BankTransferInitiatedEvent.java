package com.stillfresh.app.sharedentities.payment.events;

import java.time.Instant;

/**
 * Published by payment-service after a BankTransferPayment record is created.
 * Notification-service listens to this and sends the customer their payment instructions.
 */
public class BankTransferInitiatedEvent {

    private Long orderId;
    private Long userId;
    /** Unique human-readable reference (e.g. SF-20260314-A1B2C). */
    private String paymentReference;
    /** Platform IBAN the customer must transfer to. */
    private String iban;
    private String bankName;
    private String accountHolder;
    /** Amount to transfer, in minor currency units. */
    private Long amountCents;
    /** ISO 4217 currency code. */
    private String currency;
    /** The exact string the customer must put in the payment description/narration. */
    private String paymentDescription;
    /** When the transfer instruction expires (after which admin may cancel). */
    private Instant expiresAt;

    public BankTransferInitiatedEvent() {}

    public BankTransferInitiatedEvent(Long orderId, Long userId, String paymentReference,
                                       String iban, String bankName, String accountHolder,
                                       Long amountCents, String currency,
                                       String paymentDescription, Instant expiresAt) {
        this.orderId = orderId;
        this.userId = userId;
        this.paymentReference = paymentReference;
        this.iban = iban;
        this.bankName = bankName;
        this.accountHolder = accountHolder;
        this.amountCents = amountCents;
        this.currency = currency;
        this.paymentDescription = paymentDescription;
        this.expiresAt = expiresAt;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getAccountHolder() { return accountHolder; }
    public void setAccountHolder(String accountHolder) { this.accountHolder = accountHolder; }

    public Long getAmountCents() { return amountCents; }
    public void setAmountCents(Long amountCents) { this.amountCents = amountCents; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getPaymentDescription() { return paymentDescription; }
    public void setPaymentDescription(String paymentDescription) { this.paymentDescription = paymentDescription; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
