package com.stillfresh.app.orderservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.stillfresh.app.orderservice.model.Order;
import com.stillfresh.app.orderservice.repository.projections.CompletedOrderEarningsProjection;
import com.stillfresh.app.orderservice.repository.projections.DailyRevenueProjection;
import com.stillfresh.app.orderservice.repository.projections.OfferBreakdownProjection;
import com.stillfresh.app.orderservice.repository.projections.VendorTotalsProjection;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query(value = "SELECT COALESCE(SUM(o.quantity),0) AS unitsSold, " +
                   "COALESCE(SUM(o.net_amount_cents),0) AS totalVendorEarningsCents, " +
                   "COALESCE(SUM(o.platform_fee_cents),0) AS totalPlatformFeeCents, " +
                   "COALESCE(SUM(o.gross_amount_cents),0) AS totalGrossRevenueCents " +
                   "FROM orders o WHERE o.vendor_id = :vendorId " +
                   "AND o.status = 'COMPLETED' AND o.net_amount_cents IS NOT NULL AND o.settled_at IS NOT NULL " +
                   "AND o.settled_at >= COALESCE(CAST(:fromDate AS timestamptz), '-infinity'::timestamptz) " +
                   "AND o.settled_at <= COALESCE(CAST(:toDate AS timestamptz), 'infinity'::timestamptz)",
           nativeQuery = true)
    VendorTotalsProjection aggregateTotalsByVendor(@Param("vendorId") Long vendorId,
                                                  @Param("fromDate") OffsetDateTime from,
                                                  @Param("toDate") OffsetDateTime to);

    @Query(value = "SELECT o.offer_id AS offerId, COALESCE(SUM(o.quantity),0) AS unitsSold, " +
                   "COALESCE(SUM(o.net_amount_cents),0) AS vendorEarningsCents, " +
                   "COALESCE(SUM(o.platform_fee_cents),0) AS platformFeeCents, " +
                   "COALESCE(SUM(o.gross_amount_cents),0) AS grossRevenueCents " +
                   "FROM orders o WHERE o.vendor_id = :vendorId " +
                   "AND o.status = 'COMPLETED' AND o.net_amount_cents IS NOT NULL AND o.settled_at IS NOT NULL " +
                   "AND o.settled_at >= COALESCE(CAST(:fromDate AS timestamptz), '-infinity'::timestamptz) " +
                   "AND o.settled_at <= COALESCE(CAST(:toDate AS timestamptz), 'infinity'::timestamptz) " +
                   "GROUP BY o.offer_id",
           nativeQuery = true)
    List<OfferBreakdownProjection> aggregateByOffer(@Param("vendorId") Long vendorId,
                                                    @Param("fromDate") OffsetDateTime from,
                                                    @Param("toDate") OffsetDateTime to);

    @Query(value = "SELECT COALESCE(SUM(o.quantity),0) AS unitsSold, " +
                   "COALESCE(SUM(o.net_amount_cents),0) AS totalVendorEarningsCents, " +
                   "COALESCE(SUM(o.platform_fee_cents),0) AS totalPlatformFeeCents, " +
                   "COALESCE(SUM(o.gross_amount_cents),0) AS totalGrossRevenueCents " +
                   "FROM orders o WHERE o.vendor_id = :vendorId " +
                   "AND o.status = 'COMPLETED' AND o.net_amount_cents IS NOT NULL AND o.settled_at IS NOT NULL " +
                   "AND o.offer_id IN (:offerIds) " +
                   "AND o.settled_at >= COALESCE(CAST(:fromDate AS timestamptz), '-infinity'::timestamptz) " +
                   "AND o.settled_at <= COALESCE(CAST(:toDate AS timestamptz), 'infinity'::timestamptz)",
           nativeQuery = true)
    VendorTotalsProjection aggregateTotalsByVendorAndOffers(@Param("vendorId") Long vendorId,
                                                            @Param("offerIds") List<Long> offerIds,
                                                            @Param("fromDate") OffsetDateTime from,
                                                            @Param("toDate") OffsetDateTime to);

    @Query(value = "SELECT o.offer_id AS offerId, COALESCE(SUM(o.quantity),0) AS unitsSold, " +
                   "COALESCE(SUM(o.net_amount_cents),0) AS vendorEarningsCents, " +
                   "COALESCE(SUM(o.platform_fee_cents),0) AS platformFeeCents, " +
                   "COALESCE(SUM(o.gross_amount_cents),0) AS grossRevenueCents " +
                   "FROM orders o WHERE o.vendor_id = :vendorId " +
                   "AND o.status = 'COMPLETED' AND o.net_amount_cents IS NOT NULL AND o.settled_at IS NOT NULL " +
                   "AND o.offer_id IN (:offerIds) " +
                   "AND o.settled_at >= COALESCE(CAST(:fromDate AS timestamptz), '-infinity'::timestamptz) " +
                   "AND o.settled_at <= COALESCE(CAST(:toDate AS timestamptz), 'infinity'::timestamptz) " +
                   "GROUP BY o.offer_id",
           nativeQuery = true)
    List<OfferBreakdownProjection> aggregateByOfferAndOffers(@Param("vendorId") Long vendorId,
                                                             @Param("offerIds") List<Long> offerIds,
                                                             @Param("fromDate") OffsetDateTime from,
                                                             @Param("toDate") OffsetDateTime to);

    /**
     * Daily earnings breakdown for completed orders, ordered by date.
     */
    @Query(value = "SELECT TO_CHAR(DATE(o.settled_at), 'YYYY-MM-DD') AS date, " +
                   "COALESCE(SUM(o.quantity),0) AS unitsSold, " +
                   "COALESCE(SUM(o.net_amount_cents),0) AS vendorEarningsCents, " +
                   "COALESCE(SUM(o.platform_fee_cents),0) AS platformFeeCents, " +
                   "COALESCE(SUM(o.gross_amount_cents),0) AS grossRevenueCents " +
                   "FROM orders o WHERE o.vendor_id = :vendorId " +
                   "AND o.status = 'COMPLETED' AND o.net_amount_cents IS NOT NULL AND o.settled_at IS NOT NULL " +
                   "AND o.settled_at >= CAST(:fromDate AS timestamptz) " +
                   "GROUP BY DATE(o.settled_at) ORDER BY DATE(o.settled_at)",
           nativeQuery = true)
    List<DailyRevenueProjection> aggregateByDay(@Param("vendorId") Long vendorId,
                                                @Param("fromDate") OffsetDateTime from);

    @Query(value = "SELECT TO_CHAR(DATE(o.settled_at), 'YYYY-MM-DD') AS date, " +
                   "COALESCE(SUM(o.quantity),0) AS unitsSold, " +
                   "COALESCE(SUM(o.net_amount_cents),0) AS vendorEarningsCents, " +
                   "COALESCE(SUM(o.platform_fee_cents),0) AS platformFeeCents, " +
                   "COALESCE(SUM(o.gross_amount_cents),0) AS grossRevenueCents " +
                   "FROM orders o WHERE o.vendor_id = :vendorId " +
                   "AND o.status = 'COMPLETED' AND o.net_amount_cents IS NOT NULL AND o.settled_at IS NOT NULL " +
                   "AND o.offer_id IN (:offerIds) " +
                   "AND o.settled_at >= CAST(:fromDate AS timestamptz) " +
                   "GROUP BY DATE(o.settled_at) ORDER BY DATE(o.settled_at)",
           nativeQuery = true)
    List<DailyRevenueProjection> aggregateByDayAndOffers(@Param("vendorId") Long vendorId,
                                                         @Param("offerIds") List<Long> offerIds,
                                                         @Param("fromDate") OffsetDateTime from);

    /**
     * Recent completed orders with per-order fee breakdown for vendor dashboard.
     */
    @Query(value = "SELECT o.id AS orderId, o.offer_id AS offerId, o.offer_name AS offerName, " +
                   "o.quantity AS quantity, o.gross_amount_cents AS grossAmountCents, " +
                   "o.platform_fee_cents AS platformFeeCents, o.net_amount_cents AS netAmountCents, " +
                   "o.fee_percent_applied AS feePercentApplied, o.currency AS currency, o.settled_at AS settledAt " +
                   "FROM orders o WHERE o.vendor_id = :vendorId " +
                   "AND o.status = 'COMPLETED' AND o.net_amount_cents IS NOT NULL AND o.settled_at IS NOT NULL " +
                   "AND o.settled_at >= COALESCE(CAST(:fromDate AS timestamptz), '-infinity'::timestamptz) " +
                   "ORDER BY o.settled_at DESC LIMIT :limit",
           nativeQuery = true)
    List<CompletedOrderEarningsProjection> findRecentCompletedOrders(
            @Param("vendorId") Long vendorId,
            @Param("fromDate") OffsetDateTime from,
            @Param("limit") int limit);

    @Query(value = "SELECT o.id AS orderId, o.offer_id AS offerId, o.offer_name AS offerName, " +
                   "o.quantity AS quantity, o.gross_amount_cents AS grossAmountCents, " +
                   "o.platform_fee_cents AS platformFeeCents, o.net_amount_cents AS netAmountCents, " +
                   "o.fee_percent_applied AS feePercentApplied, o.currency AS currency, o.settled_at AS settledAt " +
                   "FROM orders o WHERE o.vendor_id = :vendorId " +
                   "AND o.status = 'COMPLETED' AND o.net_amount_cents IS NOT NULL AND o.settled_at IS NOT NULL " +
                   "AND o.offer_id IN (:offerIds) " +
                   "AND o.settled_at >= COALESCE(CAST(:fromDate AS timestamptz), '-infinity'::timestamptz) " +
                   "ORDER BY o.settled_at DESC LIMIT :limit",
           nativeQuery = true)
    List<CompletedOrderEarningsProjection> findRecentCompletedOrdersByOffers(
            @Param("vendorId") Long vendorId,
            @Param("offerIds") List<Long> offerIds,
            @Param("fromDate") OffsetDateTime from,
            @Param("limit") int limit);

    /**
     * Active orders for a vendor: CONFIRMED, PROCESSING, or READY.
     */
    @Query("SELECT o FROM Order o WHERE o.vendorId = :vendorId AND o.status IN ('CONFIRMED', 'PROCESSING', 'READY') ORDER BY o.pickupBy ASC NULLS LAST")
    List<Order> findActiveOrdersByVendor(@Param("vendorId") Long vendorId);

    @Query("SELECT o FROM Order o WHERE o.vendorId = :vendorId AND o.offerId IN :offerIds AND o.status IN ('CONFIRMED', 'PROCESSING', 'READY') ORDER BY o.pickupBy ASC NULLS LAST")
    List<Order> findActiveOrdersByVendorAndOffers(@Param("vendorId") Long vendorId,
                                                  @Param("offerIds") List<Long> offerIds);

    /**
     * Find all orders that match a single status.
     * This is used by the mobile app to fetch, for example, only CANCELLED or COMPLETED orders.
     */
    List<Order> findByStatus(String status);

    /**
     * Paginated orders by status.
     */
    Page<Order> findByStatus(String status, Pageable pageable);

    /**
     * Find all orders for a given user (customer).
     */
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Paginated orders for a given user (customer).
     */
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find all orders for a given user with a specific status.
     */
    List<Order> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

    /**
     * Paginated orders for a given user with a specific status.
     */
    Page<Order> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status, Pageable pageable);

    /**
     * Find an order by its PaymentIntent ID.
     * Used to update order status when payment is captured.
     */
    Optional<Order> findByPaymentIntentId(String paymentIntentId);

    /**
     * Find orders that are past their pickup deadline and should be marked EXPIRED.
     * Status must be CONFIRMED, PROCESSING, or READY; pickupBy must be non-null and in the past.
     */
    @Query("SELECT o FROM Order o WHERE o.status IN ('CONFIRMED', 'PROCESSING', 'READY') AND o.pickupBy IS NOT NULL AND o.pickupBy < :now")
    List<Order> findOrdersToExpire(@Param("now") OffsetDateTime now);

    /**
     * Find orders with null pickupBy (legacy or missing data) that are old enough to expire.
     * Use a time threshold so e.g. orders created before we set pickupBy get expired after 24h.
     */
    @Query("SELECT o FROM Order o WHERE o.status IN ('CONFIRMED', 'PROCESSING', 'READY') AND o.pickupBy IS NULL AND o.createdAt < :createdBefore")
    List<Order> findOrdersToExpireWithNullPickupBy(@Param("createdBefore") OffsetDateTime createdBefore);

    /**
     * Find orders that are within the reminder window (e.g. 1 hour before pickup) and have not yet had a reminder sent.
     */
    @Query("SELECT o FROM Order o WHERE o.status IN ('CONFIRMED', 'PROCESSING', 'READY') AND o.pickupBy IS NOT NULL AND o.pickupBy > :now AND o.pickupBy <= :reminderCutoff AND o.pickupReminderSent = false")
    List<Order> findOrdersForPickupReminder(@Param("now") OffsetDateTime now, @Param("reminderCutoff") OffsetDateTime reminderCutoff);
}
