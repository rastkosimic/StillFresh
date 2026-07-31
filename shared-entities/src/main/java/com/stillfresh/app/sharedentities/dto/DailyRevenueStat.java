package com.stillfresh.app.sharedentities.dto;

import java.io.Serializable;

/** One day's completed-order totals — used in the vendor dashboard revenue trend chart. */
public class DailyRevenueStat implements Serializable {
    private static final long serialVersionUID = 1L;

    /** ISO date string: "yyyy-MM-dd" */
    private String date;
    private long unitsSold;
    /** @deprecated Gross revenue in major currency units. Prefer {@link #getGrossRevenueCents()}. */
    private double revenue;
    private long vendorEarningsCents;
    private long platformFeeCents;
    private long grossRevenueCents;

    public DailyRevenueStat() {}

    public DailyRevenueStat(String date, long unitsSold, long vendorEarningsCents,
                            long platformFeeCents, long grossRevenueCents) {
        this.date = date;
        this.unitsSold = unitsSold;
        this.vendorEarningsCents = vendorEarningsCents;
        this.platformFeeCents = platformFeeCents;
        this.grossRevenueCents = grossRevenueCents;
        this.revenue = grossRevenueCents / 100.0;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public long getUnitsSold() { return unitsSold; }
    public void setUnitsSold(long unitsSold) { this.unitsSold = unitsSold; }

    /** @deprecated Gross revenue in major currency units. Prefer {@link #getGrossRevenueCents()}. */
    public double getRevenue() { return revenue; }
    public void setRevenue(double revenue) { this.revenue = revenue; }

    public long getVendorEarningsCents() { return vendorEarningsCents; }
    public void setVendorEarningsCents(long vendorEarningsCents) { this.vendorEarningsCents = vendorEarningsCents; }

    public long getPlatformFeeCents() { return platformFeeCents; }
    public void setPlatformFeeCents(long platformFeeCents) { this.platformFeeCents = platformFeeCents; }

    public long getGrossRevenueCents() { return grossRevenueCents; }
    public void setGrossRevenueCents(long grossRevenueCents) {
        this.grossRevenueCents = grossRevenueCents;
        this.revenue = grossRevenueCents / 100.0;
    }
}
