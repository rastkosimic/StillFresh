package com.stillfresh.app.vendorservice.client;

import com.stillfresh.app.sharedentities.dto.VendorStatsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "order-service", configuration = com.stillfresh.app.vendorservice.config.OrderServiceFeignConfig.class)
public interface OrderClient {

    @GetMapping("/orders/stats/vendor/{vendorId}")
    VendorStatsResponse getVendorStats(
            @PathVariable("vendorId") Long vendorId,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "offerIds", required = false) List<Long> offerIds);

    /** Daily revenue trend for the last N days. */
    @GetMapping("/orders/stats/vendor/{vendorId}/trend")
    VendorStatsResponse getVendorTrend(
            @PathVariable("vendorId") Long vendorId,
            @RequestParam(value = "days", defaultValue = "7") int days,
            @RequestParam(value = "offerIds", required = false) List<Long> offerIds);

    /** Active orders (CONFIRMED/PROCESSING/READY) for a vendor, ordered by pickup deadline. */
    @GetMapping("/orders/stats/vendor/{vendorId}/active-orders")
    List<Map<String, Object>> getActiveOrders(
            @PathVariable("vendorId") Long vendorId,
            @RequestParam(value = "offerIds", required = false) List<Long> offerIds);

    /** Internal: check whether an order is eligible for rating (COMPLETED). */
    @GetMapping("/orders/internal/{orderId}/rating-eligibility")
    OrderRatingEligibilityResponse getRatingEligibility(@PathVariable("orderId") Long orderId);

    /** Recent completed orders with per-order fee and vendor earnings breakdown. */
    @GetMapping("/orders/stats/vendor/{vendorId}/recent-completed")
    List<com.stillfresh.app.sharedentities.dto.CompletedOrderEarnings> getRecentCompletedOrders(
            @PathVariable("vendorId") Long vendorId,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "offerIds", required = false) List<Long> offerIds);
}
