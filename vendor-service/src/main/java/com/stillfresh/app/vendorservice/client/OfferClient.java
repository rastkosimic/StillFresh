package com.stillfresh.app.vendorservice.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.stillfresh.app.sharedentities.dto.OfferDto;

import com.stillfresh.app.vendorservice.config.OfferServiceFeignConfig;

@FeignClient(name = "offer-service", configuration = OfferServiceFeignConfig.class)
public interface OfferClient {
	
    @GetMapping("/offers/{vendorId}/active")
    List<OfferDto> getActiveOffersForVendor(@PathVariable("vendorId") Long vendorId);
    
    @GetMapping("/offers/{vendorId}/all-offers")
	List<OfferDto> getAllOffersForVendor(@PathVariable("vendorId") Long id);

}
