package com.stillfresh.app.paymentservice.model;

import com.stillfresh.app.sharedentities.enums.PayoutStatus;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * One batch of vendor payouts, created by the daily scheduler.
 * Lifecycle: PENDING → APPROVED (admin) → IN_PROGRESS → COMPLETED | PARTIALLY_COMPLETED | FAILED
 */
@Entity
@Table(name = "payout_batches")
public class PayoutBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private PayoutStatus status = PayoutStatus.PENDING;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    /** Set when an admin approves the batch for execution. */
    @Column(name = "approved_at")
    private Instant approvedAt;

    /** Username / ID of the admin who approved the batch. */
    @Column(name = "approved_by", length = 128)
    private String approvedBy;

    @Column(name = "processed_at")
    private Instant processedAt;

    /** Sum of all vendor payout amounts in this batch, in minor currency units. */
    @Column(name = "total_amount_cents", nullable = false)
    private Long totalAmountCents = 0L;

    /** ISO 4217 currency code (batches are single-currency for simplicity). */
    @Column(name = "currency", nullable = false, length = 8)
    private String currency;

    @Column(name = "item_count", nullable = false)
    private int itemCount = 0;

    @Column(name = "completed_count", nullable = false)
    private int completedCount = 0;

    @Column(name = "failed_count", nullable = false)
    private int failedCount = 0;

    public PayoutBatch() {}

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }

    public PayoutStatus getStatus() { return status; }
    public void setStatus(PayoutStatus status) { this.status = status; }

    public Instant getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(Instant scheduledAt) { this.scheduledAt = scheduledAt; }

    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }

    public Long getTotalAmountCents() { return totalAmountCents; }
    public void setTotalAmountCents(Long totalAmountCents) { this.totalAmountCents = totalAmountCents; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public int getItemCount() { return itemCount; }
    public void setItemCount(int itemCount) { this.itemCount = itemCount; }

    public int getCompletedCount() { return completedCount; }
    public void setCompletedCount(int completedCount) { this.completedCount = completedCount; }

    public int getFailedCount() { return failedCount; }
    public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
}
