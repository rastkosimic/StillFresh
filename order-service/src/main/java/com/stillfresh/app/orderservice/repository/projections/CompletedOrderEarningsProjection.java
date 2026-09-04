package com.stillfresh.app.orderservice.repository.projections;

import java.time.Instant;

public interface CompletedOrderEarningsProjection {
    Long getOrderId();
    Long getOfferId();
    String getOfferName();
    Integer getQuantity();
    Long getGrossAmountCents();
    Long getPlatformFeeCents();
    Long getNetAmountCents();
    Double getFeePercentApplied();
    String getCurrency();
    Instant getSettledAt();
}
