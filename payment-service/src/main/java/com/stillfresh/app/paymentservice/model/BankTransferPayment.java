package com.stillfresh.app.paymentservice.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Tracks a customer bank transfer payment for a single order.
 * Created when order-service publishes a BankTransferOrderEvent.
 */
@Entity
@Table(name = "bank_transfer_payments",
       indexes = {
           @Index(name = "idx_btp_reference",  columnList = "payment_reference", unique = true),
           @Index(name = "idx_btp_order_id",   columnList = "order_id"),
           @Index(name = "idx_btp_user_id",    columnList = "user_id"),
           @Index(name = "idx_btp_status",     columnList = "status")
       })
public class BankTransferPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique human-readable reference shown to the customer (e.g. SF-20260314-A1B2C). */
    @Column(name = "payment_reference", nullable = false, unique = true, length = 32)
    private String paymentReference;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "vendor_id")
    private Long vendorId;

    @Column(name = "offer_id")
    private Long offerId;

    /** Total amount the customer must transfer, in minor currency units. */
    @Column(name = "gross_amount_cents", nullable = false)
    private Long grossAmountCents;

    /** Platform fee calculated at initiation time, in minor currency units. */
    @Column(name = "platform_fee_cents", nullable = false)
    private Long platformFeeCents;

    /** Net amount to be credited to the vendor, in minor currency units. */
    @Column(name = "net_amount_cents", nullable = false)
    private Long netAmountCents;

    /** Platform fee percentage applied at initiation time (e.g. 10.0). */
    @Column(name = "fee_percent_applied")
    private Double feePercentApplied;

    /** ISO 4217 currency code. */
    @Column(name = "currency", nullable = false, length = 8)
    private String currency;

    /** Platform IBAN the customer transfers to. Snapshot at creation time. */
    @Column(name = "iban", nullable = false, length = 64)
    private String iban;

    /** Human-readable bank name. */
    @Column(name = "bank_name", length = 128)
    private String bankName;

    /** Account holder name on the platform account. */
    @Column(name = "account_holder", length = 128)
    private String accountHolder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BankTransferStatus status = BankTransferStatus.PENDING_TRANSFER;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "confirmed_by", length = 128)
    private String confirmedBy;

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

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

    public Long getPlatformFeeCents() { return platformFeeCents; }
    public void setPlatformFeeCents(Long platformFeeCents) { this.platformFeeCents = platformFeeCents; }

    public Long getNetAmountCents() { return netAmountCents; }
    public void setNetAmountCents(Long netAmountCents) { this.netAmountCents = netAmountCents; }

    public Double getFeePercentApplied() { return feePercentApplied; }
    public void setFeePercentApplied(Double feePercentApplied) { this.feePercentApplied = feePercentApplied; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getAccountHolder() { return accountHolder; }
    public void setAccountHolder(String accountHolder) { this.accountHolder = accountHolder; }

    public BankTransferStatus getStatus() { return status; }
    public void setStatus(BankTransferStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant confirmedAt) { this.confirmedAt = confirmedAt; }

    public String getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(String confirmedBy) { this.confirmedBy = confirmedBy; }
}
