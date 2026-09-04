package com.stillfresh.app.orderservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_user_id", columnList = "user_id"),
    @Index(name = "idx_order_user_status", columnList = "user_id, status"),
    @Index(name = "idx_order_status", columnList = "status")
})
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "offer_id", nullable = false)
    private Long offerId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "total_price", nullable = false)
    private double totalPrice;

    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;

    @Column(name = "unit_price", nullable = false)
    private double unitPrice;

    @Column(name = "currency", nullable = false)
    private String currency; // ISO currency code (e.g., RSD)

    @Column(name = "payment_intent_id")
    private String paymentIntentId; // Stripe PaymentIntent ID for manual capture/cancel

    /** How the customer paid: STRIPE, ALLSECURE, or BANK_TRANSFER (not vendor payout model). */
    @Column(name = "payment_method", length = 20)
    private String paymentMethod = "STRIPE";

    /** Bank transfer reference (e.g. SF-20260314-A1B2C3). Null for Stripe orders. */
    @Column(name = "bank_transfer_reference", length = 32)
    private String bankTransferReference;

    @Column(name = "status")
    private String status = "PENDING"; // Order status: PENDING, CONFIRMED, PROCESSING, READY, COMPLETED, CANCELLED, EXPIRED

    /** Deadline by which the order must be picked up; after this the order can be marked EXPIRED. */
    @Column(name = "pickup_by")
    private java.time.OffsetDateTime pickupBy;

    /** Whether the "pick up by [time]" reminder (e.g. 1 hour before) has been sent. */
    @Column(name = "pickup_reminder_sent", nullable = false)
    private boolean pickupReminderSent = false;

    // ===== Snapshot of vendor/offer display info at the time the order was placed =====
    // Stored so that order history/detail pages remain self-contained even if the
    // underlying vendor or offer is later modified, deleted, or expires.
    @Column(name = "location_name")
    private String locationName;

    @Column(name = "chain_name")
    private String chainName;

    @Column(length = 500)
    private String website;

    @Column(name = "vendor_image_url", length = 500)
    private String vendorImageUrl;

    @Column(name = "offer_name")
    private String offerName;

    @Column(name = "offer_image_url", length = 500)
    private String offerImageUrl;

    @Column(name = "address")
    private String address;

    @Column(name = "zip_code", length = 32)
    private String zipCode;

    /** Vendor/pickup location coordinates, snapshotted from the offer at placement (used for anti-bypass geo checks). */
    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    /** Customer paid amount at settlement, in minor currency units. */
    @Column(name = "gross_amount_cents")
    private Long grossAmountCents;

    /** Platform fee retained at settlement, in minor currency units. */
    @Column(name = "platform_fee_cents")
    private Long platformFeeCents;

    /** Vendor earnings at settlement, in minor currency units. */
    @Column(name = "net_amount_cents")
    private Long netAmountCents;

    /** Platform fee percentage applied at settlement (e.g. 10.0). */
    @Column(name = "fee_percent_applied")
    private Double feePercentApplied;

    /** When payment was captured or bank transfer confirmed. */
    @Column(name = "settled_at")
    private java.time.OffsetDateTime settledAt;

    @jakarta.persistence.Column(nullable = false, updatable = false)
    private java.time.OffsetDateTime createdAt;

    @jakarta.persistence.Column(nullable = true)
    private java.time.OffsetDateTime updatedAt;

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        this.createdAt = java.time.OffsetDateTime.now();
        if (this.status == null) {
            this.status = "PENDING";
        }
    }

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getOfferId() {
		return offerId;
	}

	public void setOfferId(Long offerId) {
		this.offerId = offerId;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentIntentId() {
        return paymentIntentId;
    }

    public void setPaymentIntentId(String paymentIntentId) {
        this.paymentIntentId = paymentIntentId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getBankTransferReference() {
        return bankTransferReference;
    }

    public void setBankTransferReference(String bankTransferReference) {
        this.bankTransferReference = bankTransferReference;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public java.time.OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public java.time.OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(java.time.OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public java.time.OffsetDateTime getPickupBy() {
        return pickupBy;
    }

    public void setPickupBy(java.time.OffsetDateTime pickupBy) {
        this.pickupBy = pickupBy;
    }

    public boolean isPickupReminderSent() {
        return pickupReminderSent;
    }

    public void setPickupReminderSent(boolean pickupReminderSent) {
        this.pickupReminderSent = pickupReminderSent;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getChainName() {
        return chainName;
    }

    public void setChainName(String chainName) {
        this.chainName = chainName;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getVendorImageUrl() {
        return vendorImageUrl;
    }

    public void setVendorImageUrl(String vendorImageUrl) {
        this.vendorImageUrl = vendorImageUrl;
    }

    public String getOfferName() {
        return offerName;
    }

    public void setOfferName(String offerName) {
        this.offerName = offerName;
    }

    public String getOfferImageUrl() {
        return offerImageUrl;
    }

    public void setOfferImageUrl(String offerImageUrl) {
        this.offerImageUrl = offerImageUrl;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Long getGrossAmountCents() {
        return grossAmountCents;
    }

    public void setGrossAmountCents(Long grossAmountCents) {
        this.grossAmountCents = grossAmountCents;
    }

    public Long getPlatformFeeCents() {
        return platformFeeCents;
    }

    public void setPlatformFeeCents(Long platformFeeCents) {
        this.platformFeeCents = platformFeeCents;
    }

    public Long getNetAmountCents() {
        return netAmountCents;
    }

    public void setNetAmountCents(Long netAmountCents) {
        this.netAmountCents = netAmountCents;
    }

    public Double getFeePercentApplied() {
        return feePercentApplied;
    }

    public void setFeePercentApplied(Double feePercentApplied) {
        this.feePercentApplied = feePercentApplied;
    }

    public java.time.OffsetDateTime getSettledAt() {
        return settledAt;
    }

    public void setSettledAt(java.time.OffsetDateTime settledAt) {
        this.settledAt = settledAt;
    }

    @jakarta.persistence.PreUpdate
    protected void onUpdate() {
        this.updatedAt = java.time.OffsetDateTime.now();
    }
}
