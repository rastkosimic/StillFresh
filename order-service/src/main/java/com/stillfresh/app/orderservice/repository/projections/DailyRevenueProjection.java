package com.stillfresh.app.orderservice.repository.projections;

public interface DailyRevenueProjection {
    String getDate();
    Long getUnitsSold();
    Long getVendorEarningsCents();
    Long getPlatformFeeCents();
    Long getGrossRevenueCents();
}
