package com.stillfresh.app.paymentservice.model;

import com.stillfresh.app.sharedentities.enums.LedgerEntryType;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * Immutable double-entry style ledger record.
 * VENDOR_CREDIT entries track money owed to vendors.
 * PLATFORM_FEE_INCOME entries track platform revenue.
 * PAYOUT_DEBIT entries reduce the vendor balance when a payout is executed.
 */
@Entity
@Table(name = "ledger_entries",
       indexes = {
           @Index(name = "idx_le_vendor_id",         columnList = "vendor_id"),
           @Index(name = "idx_le_payment_intent_id",  columnList = "payment_intent_id"),
           @Index(name = "idx_le_payout_batch_id",    columnList = "payout_batch_id"),
           @Index(name = "idx_le_settled_vendor",     columnList = "settled, vendor_id")
       })
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Null for PLATFORM_FEE_INCOME entries. */
    @Column(name = "vendor_id")
    private Long vendorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 32)
    private LedgerEntryType entryType;

    /** Amount in minor currency units (e.g. cents). Always positive. */
    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;

    @Column(name = "currency", nullable = false, length = 8)
    private String currency;

    /** Reference to the Stripe PaymentIntent that generated this entry. */
    @Column(name = "payment_intent_id", length = 255)
    private String paymentIntentId;

    /** Set when the entry is included in a payout batch. */
    @Column(name = "payout_batch_id")
    private Long payoutBatchId;

    /** True once this credit has been included in a completed payout. */
    @Column(name = "settled", nullable = false)
    private boolean settled = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "description", length = 500)
    private String description;

    public LedgerEntry() {}

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public LedgerEntryType getEntryType() { return entryType; }
    public void setEntryType(LedgerEntryType entryType) { this.entryType = entryType; }

    public Long getAmountCents() { return amountCents; }
    public void setAmountCents(Long amountCents) { this.amountCents = amountCents; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getPaymentIntentId() { return paymentIntentId; }
    public void setPaymentIntentId(String paymentIntentId) { this.paymentIntentId = paymentIntentId; }

    public Long getPayoutBatchId() { return payoutBatchId; }
    public void setPayoutBatchId(Long payoutBatchId) { this.payoutBatchId = payoutBatchId; }

    public boolean isSettled() { return settled; }
    public void setSettled(boolean settled) { this.settled = settled; }

    public Instant getCreatedAt() { return createdAt; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
