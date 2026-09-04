package com.stillfresh.app.vendorservice.client;

import com.stillfresh.app.sharedentities.dto.OfferDto;
import com.stillfresh.app.sharedentities.dto.OfferSupplyStatsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "offer-service",
        configuration = com.stillfresh.app.vendorservice.config.OfferServiceFeignConfig.class)
public interface OfferServiceClient {

    @GetMapping("/offers/{vendorId}/all-offers")
    List<OfferDto> getVendorOffers(@PathVariable("vendorId") Long vendorId);

    @GetMapping("/offers/stats/vendor/{vendorId}")
    OfferSupplyStatsResponse getVendorSupplyStats(
            @PathVariable("vendorId") Long vendorId,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "offerIds", required = false) List<Long> offerIds);

    @GetMapping("/offers/stats/vendor/{vendorId}/trend")
    OfferSupplyStatsResponse getVendorSupplyTrend(
            @PathVariable("vendorId") Long vendorId,
            @RequestParam(value = "days", defaultValue = "7") int days,
            @RequestParam(value = "offerIds", required = false) List<Long> offerIds);
}
