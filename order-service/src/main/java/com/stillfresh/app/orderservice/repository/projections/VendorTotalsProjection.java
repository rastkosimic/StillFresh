package com.stillfresh.app.orderservice.repository.projections;

public interface VendorTotalsProjection {
    Long getUnitsSold();
    Long getTotalVendorEarningsCents();
    Long getTotalPlatformFeeCents();
    Long getTotalGrossRevenueCents();
}
