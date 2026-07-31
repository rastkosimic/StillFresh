package com.stillfresh.app.userservice.client;

import com.stillfresh.app.sharedentities.dto.OfferDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * Feign client for communicating with offer-service.
 * Used to fetch offer details when retrieving favorites.
 */
@FeignClient(name = "offer-service")
public interface OfferServiceClient {

    /**
     * Get offer by ID.
     * Used to fetch offer details for favorites.
     * 
     * @param offerId The offer ID
     * @return OfferDto with offer details
     */
    @GetMapping("/offers/{offerId}")
    OfferDto getOfferById(@PathVariable("offerId") Long offerId);

    /**
     * Batch fetch offers by IDs.
     * Used by favorites listing to avoid N+1 calls.
     *
     * @param offerIds list of offer IDs
     * @return list of OfferDto
     */
    @PostMapping("/offers/batch")
    List<OfferDto> getOffersByIds(@RequestBody List<Long> offerIds);
}

