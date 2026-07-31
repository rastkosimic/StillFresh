package com.stillfresh.app.orderservice.repository.projections;

import java.time.OffsetDateTime;

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
    OffsetDateTime getSettledAt();
}
