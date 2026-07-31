package com.stillfresh.app.sharedentities.dto;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

public class VendorStatsResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private long totalUnitsSold;
    /** @deprecated Gross revenue in major currency units. Prefer {@link #getTotalGrossRevenueCents()}. */
    private double totalRevenue;
    private long totalVendorEarningsCents;
    private long totalPlatformFeeCents;
    private long totalGrossRevenueCents;
    private List<OfferSalesBreakdown> breakdown;
    /** Daily revenue trend. Populated only by the /trend endpoint; null otherwise. */
    private List<DailyRevenueStat> dailyTrend;
    /** Recent completed orders with per-order fee breakdown. Populated only by /recent-completed. */
    private List<CompletedOrderEarnings> recentCompletedOrders;

    private OffsetDateTime from;
    private OffsetDateTime to;

    public VendorStatsResponse() {}

    public VendorStatsResponse(long totalUnitsSold, long totalVendorEarningsCents,
                               long totalPlatformFeeCents, long totalGrossRevenueCents,
                               List<OfferSalesBreakdown> breakdown, OffsetDateTime from, OffsetDateTime to) {
        this.totalUnitsSold = totalUnitsSold;
        this.totalVendorEarningsCents = totalVendorEarningsCents;
        this.totalPlatformFeeCents = totalPlatformFeeCents;
        this.totalGrossRevenueCents = totalGrossRevenueCents;
        this.totalRevenue = totalGrossRevenueCents / 100.0;
        this.breakdown = breakdown;
        this.from = from;
        this.to = to;
    }

    public long getTotalUnitsSold() {
        return totalUnitsSold;
    }

    public void setTotalUnitsSold(long totalUnitsSold) {
        this.totalUnitsSold = totalUnitsSold;
    }

    /** @deprecated Gross revenue in major currency units. Prefer {@link #getTotalGrossRevenueCents()}. */
    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public long getTotalVendorEarningsCents() {
        return totalVendorEarningsCents;
    }

    public void setTotalVendorEarningsCents(long totalVendorEarningsCents) {
        this.totalVendorEarningsCents = totalVendorEarningsCents;
    }

    public long getTotalPlatformFeeCents() {
        return totalPlatformFeeCents;
    }

    public void setTotalPlatformFeeCents(long totalPlatformFeeCents) {
        this.totalPlatformFeeCents = totalPlatformFeeCents;
    }

    public long getTotalGrossRevenueCents() {
        return totalGrossRevenueCents;
    }

    public void setTotalGrossRevenueCents(long totalGrossRevenueCents) {
        this.totalGrossRevenueCents = totalGrossRevenueCents;
        this.totalRevenue = totalGrossRevenueCents / 100.0;
    }

    public List<OfferSalesBreakdown> getBreakdown() {
        return breakdown;
    }

    public void setBreakdown(List<OfferSalesBreakdown> breakdown) {
        this.breakdown = breakdown;
    }

    public List<DailyRevenueStat> getDailyTrend() {
        return dailyTrend;
    }

    public void setDailyTrend(List<DailyRevenueStat> dailyTrend) {
        this.dailyTrend = dailyTrend;
    }

    public List<CompletedOrderEarnings> getRecentCompletedOrders() {
        return recentCompletedOrders;
    }

    public void setRecentCompletedOrders(List<CompletedOrderEarnings> recentCompletedOrders) {
        this.recentCompletedOrders = recentCompletedOrders;
    }

    public OffsetDateTime getFrom() {
        return from;
    }

    public void setFrom(OffsetDateTime from) {
        this.from = from;
    }

    public OffsetDateTime getTo() {
        return to;
    }

    public void setTo(OffsetDateTime to) {
        this.to = to;
    }
}
