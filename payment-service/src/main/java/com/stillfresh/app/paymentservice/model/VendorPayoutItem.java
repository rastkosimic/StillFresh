package com.stillfresh.app.paymentservice.model;

import com.stillfresh.app.sharedentities.enums.PayoutStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * One vendor's payout within a PayoutBatch.
 * idempotencyKey is set once at creation and never changed — it prevents the
 * executor from issuing a duplicate bank transfer if executeBatch is called
 * more than once (e.g. after a crash mid-batch).
 */
@Entity
@Table(name = "vendor_payout_items",
       indexes = {
           @Index(name = "idx_vpi_batch_id",       columnList = "batch_id"),
           @Index(name = "idx_vpi_vendor_id",      columnList = "vendor_id"),
           @Index(name = "idx_vpi_status",         columnList = "status"),
           @Index(name = "idx_vpi_idempotency_key",columnList = "idempotency_key")
       })
public class VendorPayoutItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;

    /** Amount to be paid out in minor currency units. */
    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;

    @Column(name = "currency", nullable = false, length = 8)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private PayoutStatus status = PayoutStatus.SCHEDULED;

    /**
     * Unique key sent to the bank executor. Set once at item creation, never
     * mutated. Guarantees exactly-once execution even if executeBatch retries.
     */
    @Column(name = "idempotency_key", nullable = false, unique = true, updatable = false, length = 36)
    private String idempotencyKey = UUID.randomUUID().toString();

    /** Snapshot of vendor's IBAN captured at scheduling time. */
    @Column(name = "target_iban", length = 64)
    private String targetIban;

    @Column(name = "target_bank_name", length = 255)
    private String targetBankName;

    @Column(name = "target_account_holder", length = 255)
    private String targetAccountHolder;

    /** Domestic account number snapshot (e.g. Serbian bank account format). */
    @Column(name = "target_account_number", length = 34)
    private String targetAccountNumber;

    /** Bank code / BIC-SWIFT snapshot for domestic rails. */
    @Column(name = "target_bank_code", length = 16)
    private String targetBankCode;

    /** Reference / confirmation ID returned by the bank executor. */
    @Column(name = "external_reference", length = 255)
    private String externalReference;

    /** Bank-assigned message ID (pain.001 MsgId) for async rails; used to poll pain.002. */
    @Column(name = "bank_message_id", length = 64)
    private String bankMessageId;

    /** Which payout rail submitted this item (STUB, SEPA-XML, CMIPLUS, ...). */
    @Column(name = "rail_type", length = 24)
    private String railType;

    /** When the item was accepted by the bank rail (transition to SUBMITTED). */
    @Column(name = "submitted_at")
    private Instant submittedAt;

    /** How many execution attempts have been made (including retries). */
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    /** Error message from the last failed attempt, if any. */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public VendorPayoutItem() {}

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public Long getAmountCents() { return amountCents; }
    public void setAmountCents(Long amountCents) { this.amountCents = amountCents; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public PayoutStatus getStatus() { return status; }
    public void setStatus(PayoutStatus status) { this.status = status; }

    public String getIdempotencyKey() { return idempotencyKey; }

    public String getTargetIban() { return targetIban; }
    public void setTargetIban(String targetIban) { this.targetIban = targetIban; }

    public String getTargetBankName() { return targetBankName; }
    public void setTargetBankName(String targetBankName) { this.targetBankName = targetBankName; }

    public String getTargetAccountHolder() { return targetAccountHolder; }
    public void setTargetAccountHolder(String targetAccountHolder) { this.targetAccountHolder = targetAccountHolder; }

    public String getTargetAccountNumber() { return targetAccountNumber; }
    public void setTargetAccountNumber(String targetAccountNumber) { this.targetAccountNumber = targetAccountNumber; }

    public String getTargetBankCode() { return targetBankCode; }
    public void setTargetBankCode(String targetBankCode) { this.targetBankCode = targetBankCode; }

    public String getExternalReference() { return externalReference; }
    public void setExternalReference(String externalReference) { this.externalReference = externalReference; }

    public String getBankMessageId() { return bankMessageId; }
    public void setBankMessageId(String bankMessageId) { this.bankMessageId = bankMessageId; }

    public String getRailType() { return railType; }
    public void setRailType(String railType) { this.railType = railType; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }

    public Instant getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(Instant lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getCreatedAt() { return createdAt; }
}
