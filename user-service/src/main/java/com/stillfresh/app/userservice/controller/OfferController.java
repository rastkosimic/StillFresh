package com.stillfresh.app.userservice.controller;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stillfresh.app.sharedentities.dto.OfferDto;
import com.stillfresh.app.userservice.service.UserService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/offers")
@Tag(name = "Offer Management", description = "Operations related to order management")
public class OfferController {
	
	private static final Logger logger = LoggerFactory.getLogger(OfferController.class);

    @Autowired
    private UserService userService;
    
    @GetMapping("/nearby")
    public ResponseEntity<List<OfferDto>> getNearbyOffers(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam double range) throws ExecutionException {
    	logger.info("nearby endpoint called");
        List<OfferDto> nearbyOffers = userService.getNearbyOffers(latitude, longitude, range);
        return ResponseEntity.ok(nearbyOffers);
    }
    

}
