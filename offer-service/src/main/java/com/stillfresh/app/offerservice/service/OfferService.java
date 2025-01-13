package com.stillfresh.app.offerservice.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stillfresh.app.offerservice.model.Offer;
import com.stillfresh.app.offerservice.repository.OfferRepository;
import com.stillfresh.app.sharedentities.dto.OfferDto;
import com.stillfresh.app.sharedentities.offer.events.OfferCreationEvent;
import com.stillfresh.app.sharedentities.offer.events.OfferUpdateEvent;
import com.stillfresh.app.sharedentities.vendor.events.OfferRelatedVendorDetailsEvent;

@Service
public class OfferService {
	
	private static final Logger logger = LoggerFactory.getLogger(OfferService.class);
	
    @Autowired
    private OfferRepository offerRepository;

    @CacheEvict(value = "activeOffers", allEntries = true)
    public Offer createOffer(OfferCreationEvent event) {
    	Offer offer = new Offer();
    	offer.setVendorId(event.getVendorId());
    	offer.setName(event.getName());
    	offer.setDescription(event.getDescription());
    	offer.setPrice(event.getPrice());
    	offer.setOriginalPrice(event.getOriginalPrice());
    	offer.setQuantityAvailable(event.getQuantityAvailable());
    	offer.setAddress(event.getAddress());
    	offer.setZipCode(event.getZipCode());
    	offer.setLatitude(event.getLatitude());
    	offer.setLongitude(event.getLongitude());
    	offer.setBusinessType(event.getBusinessType());
    	offer.setDietaryInfo(event.getDietaryInfo());
    	offer.setAllergenInfo(event.getAllergenInfo());
    	offer.setPickupStartTime(event.getPickupStartTime());
    	offer.setPickupEndTime(event.getPickupEndTime());
    	offer.setImageUrl(event.getImageUrl());
    	offer.setRating(event.getRating());
    	offer.setReviewsCount(event.getReviewsCount());
    	offer.setExpirationDate(event.getExpirationDate());
    	offer.setActive(true);
    	
        return offerRepository.save(offer);
    }
    
	public Offer updateOffer(OfferUpdateEvent event) {
		Offer offer = getOfferById(event.getOfferId()).get();
		
		offer.setActive(true);
		offer.setLatitude(event.getLatitude());
		offer.setLongitude(event.getLongitude());
		offer.setOriginalPrice(event.getOriginalPrice());
		offer.setPrice(event.getPrice());
		offer.setQuantityAvailable(event.getQuantityAvailable());
		offer.setRating(event.getRating());
		offer.setReviewsCount(event.getReviewsCount());
		offer.setExpirationDate(event.getExpirationDate());
		offer.setPickupStartTime(event.getPickupStartTime());
		offer.setPickupEndTime(event.getPickupEndTime());
		offer.setVendorId(event.getVendorId());
		offer.setAddress(event.getAddress());
		offer.setAllergenInfo(event.getAllergenInfo());
		offer.setBusinessType(event.getBusinessType());
		offer.setDescription(event.getDescription());
		offer.setDietaryInfo(event.getDietaryInfo());
		offer.setImageUrl(event.getImageUrl());
		offer.setName(event.getName());
		offer.setZipCode(event.getZipCode());

        return offerRepository.save(offer);
	}
    
    @Cacheable(value = "activeOffers")
    public List<OfferDto> findActiveOffersForVendor(Long vendorId) {
        List<Offer> activeOffers = offerRepository.findByVendorIdAndActive(vendorId, true);
        return activeOffers.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
	public List<OfferDto> findAllOffersForVendor(Long vendorId) {
        List<Offer> activeOffers = offerRepository.findByVendorId(vendorId);
        return activeOffers.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
	}
    
    
    private OfferDto toDto(Offer offer) {
        return new OfferDto(
            offer.getId(), 
            offer.getName(),
            offer.getDescription(), 
            offer.getPrice(), 
            offer.getOriginalPrice(), 
            offer.getQuantityAvailable(), 
            offer.getDietaryInfo(), 
            offer.getAllergenInfo(), 
            offer.getImageUrl(), 
            offer.getRating(), 
            offer.getReviewsCount(), 
            offer.getExpirationDate(), 
            offer.isActive(), 
            offer.getCreatedAt()
        );
    }

    
    public List<Offer> getAllOffers() {
        return offerRepository.findAll();
    }

    public Optional<Offer> getOfferById(int id) {
        return offerRepository.findById(id);
    }

    public void deleteOffer(int id) {
        offerRepository.deleteById(id);
    }
    
    
    // Manually invalidate an offer
    @CacheEvict(value = "activeOffers", allEntries = true)
    public void invalidateOffer(int offerId) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found with id: " + offerId));
        if (!offer.isActive()) {
            throw new IllegalStateException("Offer is already inactive");
        }
        offer.setActive(false);
        offerRepository.save(offer);
    }
    
    // Invalidate all active offers for a given vendor
    @Transactional
    @CacheEvict(value = "activeOffers", allEntries = true)
    public void invalidateAllOffersByVendor(Long vendorId) {
        logger.info("Invalidating all active offers for vendorId: {}", vendorId);
        try {
            offerRepository.invalidateAllOffersByVendor(vendorId);
            logger.info("Successfully invalidated all active offers for vendorId: {}", vendorId);
        } catch (Exception ex) {
            logger.error("Failed to invalidate offers for vendorId: {}. Reason: {}", vendorId, ex.getMessage());
            throw new RuntimeException("Failed to invalidate offers for vendor with ID " + vendorId, ex);
        }
    }

    // Automatically invalidate expired offers
    @Transactional
    @Scheduled(fixedRate = 3600000) // Run every hour
    @CacheEvict(value = "activeOffers", allEntries = true)
    public void invalidateExpiredOffers() {
        logger.info("Running scheduled task to invalidate expired offers...");
        List<Offer> expiredOffers = offerRepository.findExpiredOffers();
        if (expiredOffers.isEmpty()) {
            logger.info("No expired offers found for invalidation.");
        } else {
            expiredOffers.forEach(offer -> {
                offer.setActive(false);
                offerRepository.save(offer);
                logger.info("Invalidated expired offer: ID={} ExpirationDate={}", offer.getId(), offer.getExpirationDate());
            });
            logger.info("Scheduled task completed. Total invalidated offers: {}", expiredOffers.size());
        }
    }

	public void updateOfferRelatedVendorDetails(OfferRelatedVendorDetailsEvent event) {
        logger.info("Updating offer related vendor's details...");
		try {
			offerRepository.updateOfferRelatedVendorDetails(event.getId(), event.getAddress(), event.getZipCode(), event.getLatitude(), event.getLongitude(), event.getBusinessType(), event.getPickupStartTime(), event.getPickupEndTime(), event.getReviewsCount());
			logger.info("Offer related vendor's details updated successfully");
		} catch (Exception e) {
			logger.info("Offer related vendor's details failed to update: {}", e.getMessage());
		}
	}

}
