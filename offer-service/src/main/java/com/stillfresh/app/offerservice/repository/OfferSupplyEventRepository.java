package com.stillfresh.app.offerservice.repository;

import com.stillfresh.app.offerservice.model.OfferSupplyEvent;
import com.stillfresh.app.offerservice.repository.projections.OfferSupplyBreakdownProjection;
import com.stillfresh.app.offerservice.repository.projections.OfferSupplyDailyProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface OfferSupplyEventRepository extends JpaRepository<OfferSupplyEvent, Long> {

    @Query(value = "SELECT COALESCE(SUM(e.quantity_units),0) FROM offer_supply_events e " +
                   "WHERE e.vendor_id = :vendorId " +
                   "AND e.recorded_at >= COALESCE(CAST(:fromDate AS timestamptz), '-infinity'::timestamptz) " +
                   "AND e.recorded_at <= COALESCE(CAST(:toDate AS timestamptz), 'infinity'::timestamptz)",
           nativeQuery = true)
    Long sumUnitsListedByVendor(@Param("vendorId") Long vendorId,
                                @Param("fromDate") OffsetDateTime from,
                                @Param("toDate") OffsetDateTime to);

    @Query(value = "SELECT e.offer_id AS offerId, COALESCE(SUM(e.quantity_units),0) AS unitsListed " +
                   "FROM offer_supply_events e WHERE e.vendor_id = :vendorId " +
                   "AND e.recorded_at >= COALESCE(CAST(:fromDate AS timestamptz), '-infinity'::timestamptz) " +
                   "AND e.recorded_at <= COALESCE(CAST(:toDate AS timestamptz), 'infinity'::timestamptz) " +
                   "GROUP BY e.offer_id",
           nativeQuery = true)
    List<OfferSupplyBreakdownProjection> aggregateByOffer(@Param("vendorId") Long vendorId,
                                                          @Param("fromDate") OffsetDateTime from,
                                                          @Param("toDate") OffsetDateTime to);

    @Query(value = "SELECT COALESCE(SUM(e.quantity_units),0) FROM offer_supply_events e " +
                   "WHERE e.vendor_id = :vendorId " +
                   "AND e.offer_id IN (:offerIds) " +
                   "AND e.recorded_at >= COALESCE(CAST(:fromDate AS timestamptz), '-infinity'::timestamptz) " +
                   "AND e.recorded_at <= COALESCE(CAST(:toDate AS timestamptz), 'infinity'::timestamptz)",
           nativeQuery = true)
    Long sumUnitsListedByVendorAndOffers(@Param("vendorId") Long vendorId,
                                         @Param("offerIds") List<Long> offerIds,
                                         @Param("fromDate") OffsetDateTime from,
                                         @Param("toDate") OffsetDateTime to);

    @Query(value = "SELECT e.offer_id AS offerId, COALESCE(SUM(e.quantity_units),0) AS unitsListed " +
                   "FROM offer_supply_events e WHERE e.vendor_id = :vendorId " +
                   "AND e.offer_id IN (:offerIds) " +
                   "AND e.recorded_at >= COALESCE(CAST(:fromDate AS timestamptz), '-infinity'::timestamptz) " +
                   "AND e.recorded_at <= COALESCE(CAST(:toDate AS timestamptz), 'infinity'::timestamptz) " +
                   "GROUP BY e.offer_id",
           nativeQuery = true)
    List<OfferSupplyBreakdownProjection> aggregateByOfferAndOffers(@Param("vendorId") Long vendorId,
                                                                   @Param("offerIds") List<Long> offerIds,
                                                                   @Param("fromDate") OffsetDateTime from,
                                                                   @Param("toDate") OffsetDateTime to);

    @Query(value = "SELECT TO_CHAR(DATE(e.recorded_at), 'YYYY-MM-DD') AS date, " +
                   "COALESCE(SUM(e.quantity_units),0) AS unitsListed " +
                   "FROM offer_supply_events e WHERE e.vendor_id = :vendorId " +
                   "AND e.recorded_at >= CAST(:fromDate AS timestamptz) " +
                   "GROUP BY DATE(e.recorded_at) ORDER BY DATE(e.recorded_at)",
           nativeQuery = true)
    List<OfferSupplyDailyProjection> aggregateByDay(@Param("vendorId") Long vendorId,
                                                    @Param("fromDate") OffsetDateTime from);

    @Query(value = "SELECT TO_CHAR(DATE(e.recorded_at), 'YYYY-MM-DD') AS date, " +
                   "COALESCE(SUM(e.quantity_units),0) AS unitsListed " +
                   "FROM offer_supply_events e WHERE e.vendor_id = :vendorId " +
                   "AND e.offer_id IN (:offerIds) " +
                   "AND e.recorded_at >= CAST(:fromDate AS timestamptz) " +
                   "GROUP BY DATE(e.recorded_at) ORDER BY DATE(e.recorded_at)",
           nativeQuery = true)
    List<OfferSupplyDailyProjection> aggregateByDayAndOffers(@Param("vendorId") Long vendorId,
                                                             @Param("offerIds") List<Long> offerIds,
                                                             @Param("fromDate") OffsetDateTime from);
}
