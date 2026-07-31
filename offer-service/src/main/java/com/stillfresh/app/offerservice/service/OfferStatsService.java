package com.stillfresh.app.offerservice.service;

import com.stillfresh.app.offerservice.repository.OfferSupplyEventRepository;
import com.stillfresh.app.offerservice.repository.projections.OfferSupplyBreakdownProjection;
import com.stillfresh.app.offerservice.repository.projections.OfferSupplyDailyProjection;
import com.stillfresh.app.sharedentities.dto.OfferSupplyBreakdown;
import com.stillfresh.app.sharedentities.dto.OfferSupplyDailyStat;
import com.stillfresh.app.sharedentities.dto.OfferSupplyStatsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OfferStatsService {

    @Autowired
    private OfferSupplyEventRepository supplyEventRepository;

    public OfferSupplyStatsResponse getVendorSupplyStats(Long vendorId, OffsetDateTime from, OffsetDateTime to,
                                                         List<Long> offerIds) {
        boolean filtered = offerIds != null && !offerIds.isEmpty();
        Long total = filtered
                ? supplyEventRepository.sumUnitsListedByVendorAndOffers(vendorId, offerIds, from, to)
                : supplyEventRepository.sumUnitsListedByVendor(vendorId, from, to);
        List<OfferSupplyBreakdownProjection> raw = filtered
                ? supplyEventRepository.aggregateByOfferAndOffers(vendorId, offerIds, from, to)
                : supplyEventRepository.aggregateByOffer(vendorId, from, to);

        List<OfferSupplyBreakdown> breakdown = raw.stream()
                .map(b -> new OfferSupplyBreakdown(
                        b.getOfferId(),
                        b.getUnitsListed() != null ? b.getUnitsListed() : 0L))
                .collect(Collectors.toList());

        return new OfferSupplyStatsResponse(
                total != null ? total : 0L,
                breakdown,
                from,
                to
        );
    }

    public OfferSupplyStatsResponse getVendorSupplyTrend(Long vendorId, int days, List<Long> offerIds) {
        OffsetDateTime from = OffsetDateTime.now().minusDays(days);
        OfferSupplyStatsResponse resp = getVendorSupplyStats(vendorId, from, OffsetDateTime.now(), offerIds);

        boolean filtered = offerIds != null && !offerIds.isEmpty();
        List<OfferSupplyDailyProjection> raw = filtered
                ? supplyEventRepository.aggregateByDayAndOffers(vendorId, offerIds, from)
                : supplyEventRepository.aggregateByDay(vendorId, from);
        List<OfferSupplyDailyStat> trend = raw.stream()
                .map(r -> new OfferSupplyDailyStat(
                        r.getDate(),
                        r.getUnitsListed() != null ? r.getUnitsListed() : 0L))
                .collect(Collectors.toList());
        resp.setDailyTrend(trend);
        return resp;
    }
}
