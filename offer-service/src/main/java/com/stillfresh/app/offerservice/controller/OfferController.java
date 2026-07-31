package com.stillfresh.app.offerservice.controller;

import com.stillfresh.app.offerservice.service.OfferService;
import com.stillfresh.app.sharedentities.dto.OfferDto;
import com.stillfresh.app.sharedentities.enums.OfferCategory;
import com.stillfresh.app.sharedentities.exceptions.ResourceNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/offers")
public class OfferController {

	@Autowired
    private OfferService offerService;


//    @PostMapping
//    public Offer createOffer(@RequestBody Offer offer) {
//        return offerService.createOffer(offer);
//    }
	
    @GetMapping("/{vendorId}/active")
    public List<OfferDto> getActiveOffersForVendor(@PathVariable Long vendorId) {
        return offerService.findActiveOffersForVendor(vendorId);
    }
    
    @GetMapping("/{vendorId}/all-offers")
    public List<OfferDto> getAllOffersForVendor(@PathVariable Long vendorId) {
        return offerService.findAllOffersForVendor(vendorId);
    }

    @GetMapping
    public List<OfferDto> getAllOffers(
            @RequestParam(required = false) String category) {
        if (category != null && !category.isEmpty()) {
            try {
                OfferCategory offerCategory = OfferCategory.valueOf(category.toUpperCase());
                return offerService.getOffersByCategory(offerCategory);
            } catch (IllegalArgumentException e) {
                // Invalid category, return all offers
                return offerService.getAllOffersAsDto();
            }
        }
        return offerService.getAllOffersAsDto();
    }

    @GetMapping("/nearby")
    public List<OfferDto> getNearbyOffers(
            @org.springframework.web.bind.annotation.RequestParam double latitude,
            @org.springframework.web.bind.annotation.RequestParam double longitude,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") double range,
            @RequestParam(required = false) String category) {
        return offerService.getNearbyOffers(latitude, longitude, range, category);
    }

    @GetMapping("/{id}")
    public OfferDto getOfferById(@PathVariable Long id) {
        return offerService.getOfferByIdAsDto(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));
    }

    /**
     * Batch endpoint to fetch multiple offers by their IDs.
     * Used by user-service favorites to avoid N+1 offer lookups.
     */
    @PostMapping("/batch")
    public List<OfferDto> getOffersByIds(@RequestBody List<Long> offerIds) {
        return offerService.getOffersByIds(offerIds);
    }

    @DeleteMapping("/{id}")
    public String deleteOffer(@PathVariable Long id) {
        offerService.deleteOffer(id);
        return "Offer deleted";
    }
    
    @GetMapping("/categories")
    public ResponseEntity<List<Map<String, String>>> getCategories(
            @RequestParam(required = false, defaultValue = "en") String locale) {
        
        List<Map<String, String>> categories = Arrays.stream(OfferCategory.values())
            .filter(cat -> cat != OfferCategory.ALL) // Exclude "ALL" from the list
            .map(cat -> Map.of(
                "value", cat.name(),
                "displayName", cat.getDisplayName(locale)
            ))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(categories);
    }
}
