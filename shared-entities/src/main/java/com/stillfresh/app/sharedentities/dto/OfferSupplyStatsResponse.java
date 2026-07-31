package com.stillfresh.app.sharedentities.dto;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

/** Vendor supply (units listed) stats for retail sell-through rate denominator. */
public class OfferSupplyStatsResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private long totalUnitsListed;
    private List<OfferSupplyBreakdown> breakdown;
    private List<OfferSupplyDailyStat> dailyTrend;
    private OffsetDateTime from;
    private OffsetDateTime to;

    public OfferSupplyStatsResponse() {}

    public OfferSupplyStatsResponse(long totalUnitsListed, List<OfferSupplyBreakdown> breakdown,
                                    OffsetDateTime from, OffsetDateTime to) {
        this.totalUnitsListed = totalUnitsListed;
        this.breakdown = breakdown;
        this.from = from;
        this.to = to;
    }

    public long getTotalUnitsListed() { return totalUnitsListed; }
    public void setTotalUnitsListed(long totalUnitsListed) { this.totalUnitsListed = totalUnitsListed; }

    public List<OfferSupplyBreakdown> getBreakdown() { return breakdown; }
    public void setBreakdown(List<OfferSupplyBreakdown> breakdown) { this.breakdown = breakdown; }

    public List<OfferSupplyDailyStat> getDailyTrend() { return dailyTrend; }
    public void setDailyTrend(List<OfferSupplyDailyStat> dailyTrend) { this.dailyTrend = dailyTrend; }

    public OffsetDateTime getFrom() { return from; }
    public void setFrom(OffsetDateTime from) { this.from = from; }

    public OffsetDateTime getTo() { return to; }
    public void setTo(OffsetDateTime to) { this.to = to; }
}
