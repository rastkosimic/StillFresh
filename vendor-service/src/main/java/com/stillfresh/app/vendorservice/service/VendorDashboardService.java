package com.stillfresh.app.vendorservice.service;

import com.stillfresh.app.sharedentities.dto.CompletedOrderEarnings;
import com.stillfresh.app.sharedentities.dto.DailyRevenueStat;
import com.stillfresh.app.sharedentities.dto.OfferDto;
import com.stillfresh.app.sharedentities.dto.OfferSalesBreakdown;
import com.stillfresh.app.sharedentities.dto.OfferSupplyBreakdown;
import com.stillfresh.app.sharedentities.dto.OfferSupplyDailyStat;
import com.stillfresh.app.sharedentities.dto.OfferSupplyStatsResponse;
import com.stillfresh.app.sharedentities.dto.SellThroughDailyStat;
import com.stillfresh.app.sharedentities.dto.VendorStatsResponse;
import com.stillfresh.app.vendorservice.client.OfferServiceClient;
import com.stillfresh.app.vendorservice.client.OrderClient;
import com.stillfresh.app.vendorservice.client.PaymentClient;
import com.stillfresh.app.vendorservice.dto.VendorDashboardResponse;
import com.stillfresh.app.vendorservice.dto.VendorDashboardResponse.*;
import com.stillfresh.app.vendorservice.model.Vendor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class VendorDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(VendorDashboardService.class);

    @Autowired private OrderClient orderClient;
    @Autowired private OfferServiceClient offerServiceClient;
    @Autowired private PaymentClient paymentClient;
    @Autowired private VendorService vendorService;

    public VendorDashboardResponse buildDashboard(Long vendorId, String period, List<Long> offerIds) {
        List<Long> selectedOfferIds = normalizeOfferIds(offerIds);
        List<OfferDto> allOffers = offerServiceClient.getVendorOffers(vendorId);
        validateOfferIds(selectedOfferIds, allOffers);

        VendorDashboardResponse resp = new VendorDashboardResponse();
        resp.setVendorId(vendorId);
        resp.setPeriod(period);
        resp.setSelectedOfferIds(selectedOfferIds);

        PeriodWindow window = resolvePeriod(period);
        String fromStr = window.from() != null
                ? window.from().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                : null;

        VendorStatsResponse benchmarkStats = fetchOrderStats(vendorId, fromStr, null);
        OfferSupplyStatsResponse benchmarkSupply = fetchSupplyStats(vendorId, fromStr, null);
        List<Map<String, Object>> benchmarkActiveOrders = fetchActiveOrders(vendorId, null);

        PeriodSummary benchmark = buildPeriodSummary(benchmarkStats, benchmarkSupply, benchmarkActiveOrders.size());
        resp.setPeriodBenchmark(benchmark);

        boolean filtered = selectedOfferIds != null;
        List<Long> statsOfferIds = filtered ? selectedOfferIds : null;

        VendorStatsResponse stats = filtered
                ? fetchOrderStats(vendorId, fromStr, statsOfferIds)
                : benchmarkStats;
        OfferSupplyStatsResponse supplyStats = filtered
                ? fetchSupplyStats(vendorId, fromStr, statsOfferIds)
                : benchmarkSupply;

        try {
            VendorStatsResponse trend = orderClient.getVendorTrend(vendorId, window.trendDays(), statsOfferIds);
            resp.setRevenueTrend(trend.getDailyTrend());
            resp.setSellThroughTrend(buildSellThroughTrend(
                    trend.getDailyTrend(), vendorId, window.trendDays(), statsOfferIds));
        } catch (Exception e) {
            logger.warn("Dashboard: revenue trend unavailable for vendor {}: {}", vendorId, e.getMessage());
        }

        List<Map<String, Object>> activeOrders = filtered
                ? fetchActiveOrders(vendorId, statsOfferIds)
                : benchmarkActiveOrders;
        resp.setActiveOrders(toActiveOrderDtos(activeOrders));

        Map<Long, OfferSalesBreakdown> salesMap = stats != null && stats.getBreakdown() != null
                ? stats.getBreakdown().stream().collect(Collectors.toMap(OfferSalesBreakdown::getOfferId, b -> b))
                : Map.of();
        Map<Long, Long> listedMap = supplyStats != null && supplyStats.getBreakdown() != null
                ? supplyStats.getBreakdown().stream().collect(Collectors.toMap(OfferSupplyBreakdown::getOfferId, OfferSupplyBreakdown::getUnitsListed))
                : Map.of();

        List<OfferDto> performanceOffers = filtered
                ? allOffers.stream()
                    .filter(o -> selectedOfferIds.contains(o.getId()))
                    .collect(Collectors.toList())
                : allOffers;
        resp.setOfferPerformance(buildOfferPerformance(performanceOffers, salesMap, listedMap));

        PeriodSummary summary = buildPeriodSummary(stats, supplyStats, activeOrders.size());
        resp.setSummary(summary);

        try {
            List<CompletedOrderSummary> completed = toCompletedOrderSummaries(
                    orderClient.getRecentCompletedOrders(vendorId, fromStr, 50, statsOfferIds));
            resp.setCompletedOrdersInPeriod(completed);
            resp.setRecentCompletedOrders(completed);
        } catch (Exception e) {
            logger.warn("Dashboard: completed orders unavailable for vendor {}: {}", vendorId, e.getMessage());
        }

        try {
            Optional<Vendor> vendorOpt = vendorService.getVendorById(vendorId);
            vendorOpt.ifPresent(v -> {
                RatingSummary rs = new RatingSummary();
                rs.setAverageRating(v.getAverageRating());
                rs.setReviewsCount(v.getReviewsCount());
                resp.setRatings(rs);
            });
        } catch (Exception e) {
            logger.warn("Dashboard: ratings unavailable for vendor {}: {}", vendorId, e.getMessage());
        }

        try {
            PayoutSummary ps = new PayoutSummary();
            Map<String, Object> balance = paymentClient.getVendorLedgerBalance(vendorId);
            if (balance != null) {
                Object cents = balance.get("unsettledBalanceCents");
                ps.setUnsettledCents(cents instanceof Number n ? n.longValue() : 0L);
                ps.setCurrency(balance.getOrDefault("currency", "RSD").toString());
            }
            try {
                Map<String, Object> lastPayout = paymentClient.getLastPayout(vendorId);
                if (lastPayout != null) {
                    Object amount = lastPayout.get("amountCents");
                    ps.setLastPayoutAmountCents(amount instanceof Number n ? n.longValue() : null);
                    Object processedAt = lastPayout.get("processedAt");
                    if (processedAt != null) ps.setLastPayoutAt(processedAt.toString());
                }
            } catch (Exception ignored) { }
            resp.setPayoutBalance(ps);
        } catch (Exception e) {
            logger.warn("Dashboard: payout balance unavailable for vendor {}: {}", vendorId, e.getMessage());
        }

        return resp;
    }

    private static List<Long> normalizeOfferIds(List<Long> offerIds) {
        if (offerIds == null || offerIds.isEmpty()) {
            return null;
        }
        return new ArrayList<>(new LinkedHashSet<>(offerIds));
    }

    private static void validateOfferIds(List<Long> selectedOfferIds, List<OfferDto> vendorOffers) {
        if (selectedOfferIds == null) {
            return;
        }
        Set<Long> vendorOfferIds = vendorOffers.stream()
                .map(OfferDto::getId)
                .collect(Collectors.toSet());
        for (Long offerId : selectedOfferIds) {
            if (!vendorOfferIds.contains(offerId)) {
                throw new IllegalArgumentException("Unknown or unauthorized offer id: " + offerId);
            }
        }
    }

    private VendorStatsResponse fetchOrderStats(Long vendorId, String fromStr, List<Long> offerIds) {
        try {
            return orderClient.getVendorStats(vendorId, fromStr, null, offerIds);
        } catch (Exception e) {
            logger.warn("Dashboard: order stats unavailable for vendor {}: {}", vendorId, e.getMessage());
            return null;
        }
    }

    private OfferSupplyStatsResponse fetchSupplyStats(Long vendorId, String fromStr, List<Long> offerIds) {
        try {
            return offerServiceClient.getVendorSupplyStats(vendorId, fromStr, null, offerIds);
        } catch (Exception e) {
            logger.warn("Dashboard: supply stats unavailable for vendor {}: {}", vendorId, e.getMessage());
            return null;
        }
    }

    private List<Map<String, Object>> fetchActiveOrders(Long vendorId, List<Long> offerIds) {
        try {
            return orderClient.getActiveOrders(vendorId, offerIds);
        } catch (Exception e) {
            logger.warn("Dashboard: active orders unavailable for vendor {}: {}", vendorId, e.getMessage());
            return List.of();
        }
    }

    private static PeriodSummary buildPeriodSummary(VendorStatsResponse stats,
                                                    OfferSupplyStatsResponse supplyStats,
                                                    int activeOrderCount) {
        PeriodSummary summary = new PeriodSummary();
        if (stats != null) {
            summary.setTotalUnitsSold(stats.getTotalUnitsSold());
            summary.setTotalVendorEarningsCents(stats.getTotalVendorEarningsCents());
            summary.setTotalPlatformFeeCents(stats.getTotalPlatformFeeCents());
            summary.setTotalGrossRevenueCents(stats.getTotalGrossRevenueCents());
        }
        if (supplyStats != null) {
            summary.setTotalUnitsListed(supplyStats.getTotalUnitsListed());
        }
        summary.setActiveOrderCount(activeOrderCount);
        summary.setSellThroughRate(computeSellThroughRate(summary.getTotalUnitsSold(), summary.getTotalUnitsListed()));
        return summary;
    }

    private record PeriodWindow(OffsetDateTime from, int trendDays) {}

    private PeriodWindow resolvePeriod(String period) {
        String p = period == null ? "week" : period.toLowerCase();
        return switch (p) {
            case "today" -> new PeriodWindow(OffsetDateTime.now().minusDays(1), 1);
            case "month" -> new PeriodWindow(OffsetDateTime.now().minusDays(30), 30);
            case "all" -> new PeriodWindow(null, 365);
            default -> new PeriodWindow(OffsetDateTime.now().minusDays(7), 7);
        };
    }

    private static double computeSellThroughRate(long unitsSold, long unitsListed) {
        return unitsListed > 0 ? (double) unitsSold / unitsListed : -1;
    }

    private List<SellThroughDailyStat> buildSellThroughTrend(
            List<DailyRevenueStat> soldTrend, Long vendorId, int trendDays, List<Long> offerIds) {
        if (soldTrend == null) {
            soldTrend = List.of();
        }
        Map<String, Long> listedByDate = new HashMap<>();
        try {
            OfferSupplyStatsResponse supplyTrend = offerServiceClient.getVendorSupplyTrend(vendorId, trendDays, offerIds);
            if (supplyTrend.getDailyTrend() != null) {
                for (OfferSupplyDailyStat day : supplyTrend.getDailyTrend()) {
                    listedByDate.put(day.getDate(), day.getUnitsListed());
                }
            }
        } catch (Exception e) {
            logger.warn("Dashboard: supply trend unavailable for vendor {}: {}", vendorId, e.getMessage());
        }

        Map<String, Long> soldByDate = soldTrend.stream()
                .collect(Collectors.toMap(DailyRevenueStat::getDate, DailyRevenueStat::getUnitsSold, Long::sum, LinkedHashMap::new));

        List<String> dates = new ArrayList<>(soldByDate.keySet());
        for (String d : listedByDate.keySet()) {
            if (!dates.contains(d)) {
                dates.add(d);
            }
        }
        dates.sort(String::compareTo);

        return dates.stream()
                .map(date -> {
                    long listed = listedByDate.getOrDefault(date, 0L);
                    long sold = soldByDate.getOrDefault(date, 0L);
                    return new SellThroughDailyStat(date, listed, sold, computeSellThroughRate(sold, listed));
                })
                .collect(Collectors.toList());
    }

    private List<ActiveOrder> toActiveOrderDtos(List<Map<String, Object>> orders) {
        return orders.stream().map(o -> {
            ActiveOrder ao = new ActiveOrder();
            ao.setOrderId(toLong(o.get("id")));
            ao.setOfferId(toLong(o.get("offerId")));
            ao.setQuantity(toInt(o.get("quantity")));
            ao.setTotalPrice(toDouble(o.get("totalPrice")));
            Object cur = o.get("currency");
            ao.setCurrency(cur != null ? cur.toString() : null);
            Object st = o.get("status");
            ao.setStatus(st != null ? st.toString() : null);
            Object pm = o.get("paymentMethod");
            ao.setPaymentMethod(pm != null ? pm.toString() : null);
            Object pb = o.get("pickupBy");
            if (pb != null) ao.setPickupBy(pb.toString());
            return ao;
        }).collect(Collectors.toList());
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }

    private int toInt(Object v) {
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return 0; }
    }

    private double toDouble(Object v) {
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(v.toString()); } catch (Exception e) { return 0.0; }
    }

    private List<OfferPerformance> buildOfferPerformance(
            List<OfferDto> offers,
            Map<Long, OfferSalesBreakdown> salesMap,
            Map<Long, Long> listedMap) {
        List<OfferPerformance> result = new ArrayList<>();
        for (OfferDto offer : offers) {
            OfferPerformance op = new OfferPerformance();
            op.setOfferId(offer.getId());
            op.setOfferName(offer.getName());
            op.setOriginalQuantity(offer.getOriginalQuantity());
            op.setQuantityAvailable(offer.getQuantityAvailable());
            op.setActive(offer.isActive());

            long unitsListed = listedMap.getOrDefault(offer.getId(), 0L);
            op.setUnitsListed(unitsListed);

            OfferSalesBreakdown sales = salesMap.get(offer.getId());
            if (sales != null) {
                op.setUnitsSold(sales.getUnitsSold());
                op.setVendorEarningsCents(sales.getVendorEarningsCents());
                op.setPlatformFeeCents(sales.getPlatformFeeCents());
                op.setGrossRevenueCents(sales.getGrossRevenueCents());
            }

            op.setSellThroughRate(computeSellThroughRate(op.getUnitsSold(), unitsListed));
            result.add(op);
        }
        return result;
    }

    private List<CompletedOrderSummary> toCompletedOrderSummaries(List<CompletedOrderEarnings> orders) {
        if (orders == null) {
            return List.of();
        }
        return orders.stream().map(o -> {
            CompletedOrderSummary s = new CompletedOrderSummary();
            s.setOrderId(o.getOrderId());
            s.setOfferId(o.getOfferId());
            s.setOfferName(o.getOfferName());
            s.setQuantity(o.getQuantity());
            s.setGrossAmountCents(o.getGrossAmountCents());
            s.setPlatformFeeCents(o.getPlatformFeeCents());
            s.setNetAmountCents(o.getNetAmountCents());
            s.setFeePercentApplied(o.getFeePercentApplied());
            s.setCurrency(o.getCurrency());
            if (o.getSettledAt() != null) {
                s.setSettledAt(o.getSettledAt().toString());
            }
            return s;
        }).collect(Collectors.toList());
    }
}
