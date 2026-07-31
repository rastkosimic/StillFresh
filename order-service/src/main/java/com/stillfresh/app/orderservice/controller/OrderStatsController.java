package com.stillfresh.app.orderservice.controller;

import com.stillfresh.app.orderservice.model.Order;
import com.stillfresh.app.orderservice.repository.OrderRepository;
import com.stillfresh.app.orderservice.repository.projections.CompletedOrderEarningsProjection;
import com.stillfresh.app.orderservice.repository.projections.DailyRevenueProjection;
import com.stillfresh.app.orderservice.repository.projections.OfferBreakdownProjection;
import com.stillfresh.app.orderservice.repository.projections.VendorTotalsProjection;
import com.stillfresh.app.sharedentities.dto.CompletedOrderEarnings;
import com.stillfresh.app.sharedentities.dto.DailyRevenueStat;
import com.stillfresh.app.sharedentities.dto.OfferSalesBreakdown;
import com.stillfresh.app.sharedentities.dto.VendorStatsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/orders/stats")
public class OrderStatsController {

    @Autowired
    private OrderRepository orderRepository;

    /** Aggregate totals + per-offer breakdown for a vendor over an optional date range. */
    @GetMapping("/vendor/{vendorId}")
    public VendorStatsResponse getVendorStats(
            @PathVariable Long vendorId,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(value = "offerIds", required = false) List<Long> offerIds) {

        VendorTotalsProjection totals = hasOfferFilter(offerIds)
                ? orderRepository.aggregateTotalsByVendorAndOffers(vendorId, offerIds, from, to)
                : orderRepository.aggregateTotalsByVendor(vendorId, from, to);
        List<OfferBreakdownProjection> breakdown = hasOfferFilter(offerIds)
                ? orderRepository.aggregateByOfferAndOffers(vendorId, offerIds, from, to)
                : orderRepository.aggregateByOffer(vendorId, from, to);
        return buildStatsResponse(totals, breakdown, from, to);
    }

    /**
     * Daily earnings trend for the last N days (default 7).
     * Used to power the revenue chart in the vendor dashboard.
     */
    @GetMapping("/vendor/{vendorId}/trend")
    public VendorStatsResponse getVendorTrend(
            @PathVariable Long vendorId,
            @RequestParam(value = "days", defaultValue = "7") int days,
            @RequestParam(value = "offerIds", required = false) List<Long> offerIds) {

        OffsetDateTime from = OffsetDateTime.now().minusDays(days);
        List<DailyRevenueProjection> raw = hasOfferFilter(offerIds)
                ? orderRepository.aggregateByDayAndOffers(vendorId, offerIds, from)
                : orderRepository.aggregateByDay(vendorId, from);

        List<DailyRevenueStat> trend = raw.stream()
                .map(r -> new DailyRevenueStat(
                        r.getDate(),
                        r.getUnitsSold() != null ? r.getUnitsSold() : 0L,
                        r.getVendorEarningsCents() != null ? r.getVendorEarningsCents() : 0L,
                        r.getPlatformFeeCents() != null ? r.getPlatformFeeCents() : 0L,
                        r.getGrossRevenueCents() != null ? r.getGrossRevenueCents() : 0L
                ))
                .collect(Collectors.toList());

        VendorStatsResponse resp = new VendorStatsResponse(
                trend.stream().mapToLong(DailyRevenueStat::getUnitsSold).sum(),
                trend.stream().mapToLong(DailyRevenueStat::getVendorEarningsCents).sum(),
                trend.stream().mapToLong(DailyRevenueStat::getPlatformFeeCents).sum(),
                trend.stream().mapToLong(DailyRevenueStat::getGrossRevenueCents).sum(),
                List.of(), from, OffsetDateTime.now()
        );
        resp.setDailyTrend(trend);
        return resp;
    }

    /**
     * Recent completed orders with per-order platform fee and vendor earnings.
     */
    @GetMapping("/vendor/{vendorId}/recent-completed")
    public List<CompletedOrderEarnings> getRecentCompletedOrders(
            @PathVariable Long vendorId,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "offerIds", required = false) List<Long> offerIds) {

        List<CompletedOrderEarningsProjection> raw = hasOfferFilter(offerIds)
                ? orderRepository.findRecentCompletedOrdersByOffers(vendorId, offerIds, from, limit)
                : orderRepository.findRecentCompletedOrders(vendorId, from, limit);

        return raw.stream()
                .map(this::toCompletedOrderEarnings)
                .collect(Collectors.toList());
    }

    /**
     * Active orders for a vendor: CONFIRMED, PROCESSING, or READY, ordered by pickup deadline.
     */
    @GetMapping("/vendor/{vendorId}/active-orders")
    public ResponseEntity<List<Order>> getActiveOrders(
            @PathVariable Long vendorId,
            @RequestParam(value = "offerIds", required = false) List<Long> offerIds) {
        List<Order> orders = hasOfferFilter(offerIds)
                ? orderRepository.findActiveOrdersByVendorAndOffers(vendorId, offerIds)
                : orderRepository.findActiveOrdersByVendor(vendorId);
        return ResponseEntity.ok(orders);
    }

    private static boolean hasOfferFilter(List<Long> offerIds) {
        return offerIds != null && !offerIds.isEmpty();
    }

    public static VendorStatsResponse buildStatsResponse(
            VendorTotalsProjection totals,
            List<OfferBreakdownProjection> breakdown,
            OffsetDateTime from,
            OffsetDateTime to) {

        long totalUnits = totals != null && totals.getUnitsSold() != null ? totals.getUnitsSold() : 0L;
        long vendorEarnings = totals != null && totals.getTotalVendorEarningsCents() != null
                ? totals.getTotalVendorEarningsCents() : 0L;
        long platformFee = totals != null && totals.getTotalPlatformFeeCents() != null
                ? totals.getTotalPlatformFeeCents() : 0L;
        long gross = totals != null && totals.getTotalGrossRevenueCents() != null
                ? totals.getTotalGrossRevenueCents() : 0L;

        List<OfferSalesBreakdown> items = breakdown.stream()
                .map(b -> new OfferSalesBreakdown(
                        b.getOfferId(),
                        b.getUnitsSold() != null ? b.getUnitsSold() : 0L,
                        b.getVendorEarningsCents() != null ? b.getVendorEarningsCents() : 0L,
                        b.getPlatformFeeCents() != null ? b.getPlatformFeeCents() : 0L,
                        b.getGrossRevenueCents() != null ? b.getGrossRevenueCents() : 0L
                ))
                .collect(Collectors.toList());

        return new VendorStatsResponse(totalUnits, vendorEarnings, platformFee, gross, items, from, to);
    }

    private CompletedOrderEarnings toCompletedOrderEarnings(CompletedOrderEarningsProjection p) {
        return new CompletedOrderEarnings(
                p.getOrderId(),
                p.getOfferId(),
                p.getOfferName(),
                p.getQuantity() != null ? p.getQuantity() : 0,
                p.getGrossAmountCents() != null ? p.getGrossAmountCents() : 0L,
                p.getPlatformFeeCents() != null ? p.getPlatformFeeCents() : 0L,
                p.getNetAmountCents() != null ? p.getNetAmountCents() : 0L,
                p.getFeePercentApplied(),
                p.getCurrency(),
                p.getSettledAt()
        );
    }
}
