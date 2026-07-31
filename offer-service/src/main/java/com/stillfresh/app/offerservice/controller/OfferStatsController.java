package com.stillfresh.app.offerservice.controller;

import com.stillfresh.app.offerservice.service.OfferStatsService;
import com.stillfresh.app.sharedentities.dto.OfferSupplyStatsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/offers/stats")
public class OfferStatsController {

    @Autowired
    private OfferStatsService offerStatsService;

    @GetMapping("/vendor/{vendorId}")
    public OfferSupplyStatsResponse getVendorSupplyStats(
            @PathVariable Long vendorId,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(value = "offerIds", required = false) List<Long> offerIds) {
        return offerStatsService.getVendorSupplyStats(vendorId, from, to, offerIds);
    }

    @GetMapping("/vendor/{vendorId}/trend")
    public OfferSupplyStatsResponse getVendorSupplyTrend(
            @PathVariable Long vendorId,
            @RequestParam(value = "days", defaultValue = "7") int days,
            @RequestParam(value = "offerIds", required = false) List<Long> offerIds) {
        return offerStatsService.getVendorSupplyTrend(vendorId, days, offerIds);
    }
}
