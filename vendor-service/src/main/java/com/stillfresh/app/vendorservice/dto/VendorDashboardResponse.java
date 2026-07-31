package com.stillfresh.app.vendorservice.dto;

import com.stillfresh.app.sharedentities.dto.DailyRevenueStat;
import com.stillfresh.app.sharedentities.dto.SellThroughDailyStat;

import java.time.Instant;
import java.util.List;

/**
 * Aggregated vendor dashboard response.
 * Each section is nullable — if a downstream service is unavailable that section
 * is omitted (graceful degradation) rather than failing the whole response.
 */
public class VendorDashboardResponse {

    private Long vendorId;
    private String period;
    private Instant generatedAt = Instant.now();
    /** Null when all offers are included; otherwise the filtered subset. */
    private List<Long> selectedOfferIds;

    private PeriodSummary summary;
    /** All-offer totals for the same period (comparison benchmark). */
    private PeriodSummary periodBenchmark;
    private List<DailyRevenueStat> revenueTrend;
    private List<OfferPerformance> offerPerformance;
    private List<ActiveOrder> activeOrders;
    private List<CompletedOrderSummary> recentCompletedOrders;
    private List<CompletedOrderSummary> completedOrdersInPeriod;
    private List<SellThroughDailyStat> sellThroughTrend;
    private RatingSummary ratings;
    private PayoutSummary payoutBalance;

    // ── Nested types ──────────────────────────────────────────────────────────

    public static class PeriodSummary {
        private long totalUnitsSold;
        /** @deprecated Gross revenue in major currency units. Use totalGrossRevenueCents. */
        private double totalRevenue;
        /** Vendor earnings after platform fee, in minor currency units. */
        private long totalVendorEarningsCents;
        /** Platform fee retained by StillFresh, in minor currency units. */
        private long totalPlatformFeeCents;
        /** Gross customer revenue, in minor currency units. */
        private long totalGrossRevenueCents;
        /** Units listed on marketplace in this period (supply denominator). */
        private long totalUnitsListed;
        private int activeOrderCount;
        /** Retail sell-through: totalUnitsSold / totalUnitsListed (0–1). -1 if not computable. */
        private double sellThroughRate = -1;

        public long getTotalUnitsSold() { return totalUnitsSold; }
        public void setTotalUnitsSold(long totalUnitsSold) { this.totalUnitsSold = totalUnitsSold; }
        public double getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }
        public long getTotalVendorEarningsCents() { return totalVendorEarningsCents; }
        public void setTotalVendorEarningsCents(long totalVendorEarningsCents) { this.totalVendorEarningsCents = totalVendorEarningsCents; }
        public long getTotalPlatformFeeCents() { return totalPlatformFeeCents; }
        public void setTotalPlatformFeeCents(long totalPlatformFeeCents) { this.totalPlatformFeeCents = totalPlatformFeeCents; }
        public long getTotalGrossRevenueCents() { return totalGrossRevenueCents; }
        public void setTotalGrossRevenueCents(long totalGrossRevenueCents) {
            this.totalGrossRevenueCents = totalGrossRevenueCents;
            this.totalRevenue = totalGrossRevenueCents / 100.0;
        }
        public long getTotalUnitsListed() { return totalUnitsListed; }
        public void setTotalUnitsListed(long totalUnitsListed) { this.totalUnitsListed = totalUnitsListed; }
        public int getActiveOrderCount() { return activeOrderCount; }
        public void setActiveOrderCount(int activeOrderCount) { this.activeOrderCount = activeOrderCount; }
        public double getSellThroughRate() { return sellThroughRate; }
        public void setSellThroughRate(double sellThroughRate) { this.sellThroughRate = sellThroughRate; }
    }

    public static class OfferPerformance {
        private Long offerId;
        private String offerName;
        private int originalQuantity;
        private int quantityAvailable;
        private long unitsSold;
        /** @deprecated Gross revenue in major currency units. Use grossRevenueCents. */
        private double revenue;
        private long vendorEarningsCents;
        private long platformFeeCents;
        private long grossRevenueCents;
        /** Units listed in this period for this offer. */
        private long unitsListed;
        /** Retail sell-through: unitsSold / unitsListed (0–1). -1 if unitsListed is 0. */
        private double sellThroughRate;
        private boolean active;

        public Long getOfferId() { return offerId; }
        public void setOfferId(Long offerId) { this.offerId = offerId; }
        public String getOfferName() { return offerName; }
        public void setOfferName(String offerName) { this.offerName = offerName; }
        public int getOriginalQuantity() { return originalQuantity; }
        public void setOriginalQuantity(int originalQuantity) { this.originalQuantity = originalQuantity; }
        public int getQuantityAvailable() { return quantityAvailable; }
        public void setQuantityAvailable(int quantityAvailable) { this.quantityAvailable = quantityAvailable; }
        public long getUnitsSold() { return unitsSold; }
        public void setUnitsSold(long unitsSold) { this.unitsSold = unitsSold; }
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
        public long getUnitsListed() { return unitsListed; }
        public void setUnitsListed(long unitsListed) { this.unitsListed = unitsListed; }
        public double getSellThroughRate() { return sellThroughRate; }
        public void setSellThroughRate(double sellThroughRate) { this.sellThroughRate = sellThroughRate; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

    public static class CompletedOrderSummary {
        private Long orderId;
        private Long offerId;
        private String offerName;
        private int quantity;
        private long grossAmountCents;
        private long platformFeeCents;
        private long netAmountCents;
        private Double feePercentApplied;
        private String currency;
        private String settledAt;

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
        public String getSettledAt() { return settledAt; }
        public void setSettledAt(String settledAt) { this.settledAt = settledAt; }
    }

    public static class ActiveOrder {
        private Long orderId;
        private Long offerId;
        private int quantity;
        private double totalPrice;
        private String currency;
        private String status;
        private String pickupBy;
        private String paymentMethod;

        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
        public Long getOfferId() { return offerId; }
        public void setOfferId(Long offerId) { this.offerId = offerId; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public double getTotalPrice() { return totalPrice; }
        public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getPickupBy() { return pickupBy; }
        public void setPickupBy(String pickupBy) { this.pickupBy = pickupBy; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    }

    public static class RatingSummary {
        private double averageRating;
        private int reviewsCount;

        public double getAverageRating() { return averageRating; }
        public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
        public int getReviewsCount() { return reviewsCount; }
        public void setReviewsCount(int reviewsCount) { this.reviewsCount = reviewsCount; }
    }

    public static class PayoutSummary {
        private long unsettledCents;
        private String currency;
        private Long lastPayoutAmountCents;
        private String lastPayoutAt;

        public long getUnsettledCents() { return unsettledCents; }
        public void setUnsettledCents(long unsettledCents) { this.unsettledCents = unsettledCents; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public Long getLastPayoutAmountCents() { return lastPayoutAmountCents; }
        public void setLastPayoutAmountCents(Long lastPayoutAmountCents) { this.lastPayoutAmountCents = lastPayoutAmountCents; }
        public String getLastPayoutAt() { return lastPayoutAt; }
        public void setLastPayoutAt(String lastPayoutAt) { this.lastPayoutAt = lastPayoutAt; }
    }

    // ── Root getters/setters ──────────────────────────────────────────────────

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public Instant getGeneratedAt() { return generatedAt; }
    public List<Long> getSelectedOfferIds() { return selectedOfferIds; }
    public void setSelectedOfferIds(List<Long> selectedOfferIds) { this.selectedOfferIds = selectedOfferIds; }
    public PeriodSummary getSummary() { return summary; }
    public void setSummary(PeriodSummary summary) { this.summary = summary; }
    public PeriodSummary getPeriodBenchmark() { return periodBenchmark; }
    public void setPeriodBenchmark(PeriodSummary periodBenchmark) { this.periodBenchmark = periodBenchmark; }
    public List<DailyRevenueStat> getRevenueTrend() { return revenueTrend; }
    public void setRevenueTrend(List<DailyRevenueStat> revenueTrend) { this.revenueTrend = revenueTrend; }
    public List<OfferPerformance> getOfferPerformance() { return offerPerformance; }
    public void setOfferPerformance(List<OfferPerformance> offerPerformance) { this.offerPerformance = offerPerformance; }
    public List<ActiveOrder> getActiveOrders() { return activeOrders; }
    public void setActiveOrders(List<ActiveOrder> activeOrders) { this.activeOrders = activeOrders; }
    public List<CompletedOrderSummary> getRecentCompletedOrders() { return recentCompletedOrders; }
    public void setRecentCompletedOrders(List<CompletedOrderSummary> recentCompletedOrders) { this.recentCompletedOrders = recentCompletedOrders; }
    public List<CompletedOrderSummary> getCompletedOrdersInPeriod() { return completedOrdersInPeriod; }
    public void setCompletedOrdersInPeriod(List<CompletedOrderSummary> completedOrdersInPeriod) { this.completedOrdersInPeriod = completedOrdersInPeriod; }
    public List<SellThroughDailyStat> getSellThroughTrend() { return sellThroughTrend; }
    public void setSellThroughTrend(List<SellThroughDailyStat> sellThroughTrend) { this.sellThroughTrend = sellThroughTrend; }
    public RatingSummary getRatings() { return ratings; }
    public void setRatings(RatingSummary ratings) { this.ratings = ratings; }
    public PayoutSummary getPayoutBalance() { return payoutBalance; }
    public void setPayoutBalance(PayoutSummary payoutBalance) { this.payoutBalance = payoutBalance; }
}
