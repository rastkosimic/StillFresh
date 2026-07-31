package com.stillfresh.app.sharedentities.dto;

import java.io.Serializable;
import java.time.OffsetDateTime;

/** Per completed order earnings breakdown for vendor dashboard / order history. */
public class CompletedOrderEarnings implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long orderId;
    private Long offerId;
    private String offerName;
    private int quantity;
    private long grossAmountCents;
    private long platformFeeCents;
    private long netAmountCents;
    private Double feePercentApplied;
    private String currency;
    private OffsetDateTime settledAt;

    public CompletedOrderEarnings() {}

    public CompletedOrderEarnings(Long orderId, Long offerId, String offerName, int quantity,
                                  long grossAmountCents, long platformFeeCents, long netAmountCents,
                                  Double feePercentApplied, String currency, OffsetDateTime settledAt) {
        this.orderId = orderId;
        this.offerId = offerId;
        this.offerName = offerName;
        this.quantity = quantity;
        this.grossAmountCents = grossAmountCents;
        this.platformFeeCents = platformFeeCents;
        this.netAmountCents = netAmountCents;
        this.feePercentApplied = feePercentApplied;
        this.currency = currency;
        this.settledAt = settledAt;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getOfferId() { return offerId; }
    public void setOfferId(Long offerId) { this.offerId = offerId; }

    public String getOfferName() { return offerName; }
    public void setOfferName(String offerName) { this.offerName = offerName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public long getGrossAmountCents() { return grossAmountCents; }
    public void setGrossAmountCents(long grossAmountCents) { this.grossAmountCents = grossAmountCents; }

    public long getPlatformFeeCents() { return platformFeeCents; }
    public void setPlatformFeeCents(long platformFeeCents) { this.platformFeeCents = platformFeeCents; }

    public long getNetAmountCents() { return netAmountCents; }
    public void setNetAmountCents(long netAmountCents) { this.netAmountCents = netAmountCents; }

    public Double getFeePercentApplied() { return feePercentApplied; }
    public void setFeePercentApplied(Double feePercentApplied) { this.feePercentApplied = feePercentApplied; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public OffsetDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(OffsetDateTime settledAt) { this.settledAt = settledAt; }
}
