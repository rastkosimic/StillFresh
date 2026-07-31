package com.stillfresh.app.orderservice.repository.projections;

public interface OfferBreakdownProjection {
    Long getOfferId();
    Long getUnitsSold();
    Long getVendorEarningsCents();
    Long getPlatformFeeCents();
    Long getGrossRevenueCents();
}
