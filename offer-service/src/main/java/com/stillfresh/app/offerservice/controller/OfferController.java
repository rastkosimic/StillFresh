package com.stillfresh.app.offerservice.controller;

import com.stillfresh.app.offerservice.model.Offer;
import com.stillfresh.app.offerservice.service.OfferService;
import com.stillfresh.app.sharedentities.dto.OfferDto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    public List<Offer> getAllOffers() {
        return offerService.getAllOffers();
    }

    @GetMapping("/{id}")
    public Offer getOfferById(@PathVariable int id) {
        return offerService.getOfferById(id)
                .orElseThrow(() -> new RuntimeException("Offer not found"));
    }

    @DeleteMapping("/{id}")
    public String deleteOffer(@PathVariable int id) {
        offerService.deleteOffer(id);
        return "Offer deleted";
    }
}
