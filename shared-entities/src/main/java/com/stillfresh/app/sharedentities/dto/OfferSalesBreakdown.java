package com.stillfresh.app.sharedentities.dto;

import java.io.Serializable;

public class OfferSalesBreakdown implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long offerId;
    private long unitsSold;
    /** @deprecated Use grossRevenueCents / 100.0 — gross customer revenue, not vendor earnings. */
    private double revenue;
    private long vendorEarningsCents;
    private long platformFeeCents;
    private long grossRevenueCents;

    public OfferSalesBreakdown() {}

    public OfferSalesBreakdown(Long offerId, long unitsSold, long vendorEarningsCents,
                               long platformFeeCents, long grossRevenueCents) {
        this.offerId = offerId;
        this.unitsSold = unitsSold;
        this.vendorEarningsCents = vendorEarningsCents;
        this.platformFeeCents = platformFeeCents;
        this.grossRevenueCents = grossRevenueCents;
        this.revenue = grossRevenueCents / 100.0;
    }

    public Long getOfferId() {
        return offerId;
    }

    public void setOfferId(Long offerId) {
        this.offerId = offerId;
    }

    public long getUnitsSold() {
        return unitsSold;
    }

    public void setUnitsSold(long unitsSold) {
        this.unitsSold = unitsSold;
    }

    /** @deprecated Gross revenue in major currency units. Prefer {@link #getGrossRevenueCents()}. */
    public double getRevenue() {
        return revenue;
    }

    public void setRevenue(double revenue) {
        this.revenue = revenue;
    }

    public long getVendorEarningsCents() {
        return vendorEarningsCents;
    }

    public void setVendorEarningsCents(long vendorEarningsCents) {
        this.vendorEarningsCents = vendorEarningsCents;
    }

    public long getPlatformFeeCents() {
        return platformFeeCents;
    }

    public void setPlatformFeeCents(long platformFeeCents) {
        this.platformFeeCents = platformFeeCents;
    }

    public long getGrossRevenueCents() {
        return grossRevenueCents;
    }

    public void setGrossRevenueCents(long grossRevenueCents) {
        this.grossRevenueCents = grossRevenueCents;
        this.revenue = grossRevenueCents / 100.0;
    }
}
