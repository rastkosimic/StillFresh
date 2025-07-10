package com.stillfresh.app.offerservice.service;

import java.time.OffsetDateTime;
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
import com.stillfresh.app.offerservice.publisher.OfferEventPublisher;
import com.stillfresh.app.offerservice.repository.OfferRepository;
import com.stillfresh.app.sharedentities.dto.OfferDto;
import com.stillfresh.app.sharedentities.offer.events.AvailableOffersEvent;
import com.stillfresh.app.sharedentities.offer.events.OfferCreationEvent;
import com.stillfresh.app.sharedentities.offer.events.OfferDetailsRequestedEvent;
import com.stillfresh.app.sharedentities.offer.events.OfferDetailsResponseEvent;
import com.stillfresh.app.sharedentities.offer.events.OfferQuantityUpdatedEvent;
import com.stillfresh.app.sharedentities.offer.events.OfferUpdateEvent;
import com.stillfresh.app.sharedentities.vendor.events.OfferRelatedVendorDetailsEvent;

@Service
public class OfferService {
	
	private static final Logger logger = LoggerFactory.getLogger(OfferService.class);
	
    @Autowired
    private OfferRepository offerRepository;
    
    @Autowired
    private OfferEventPublisher eventPublisher;

    @CacheEvict(value = "activeOffers", allEntries = true)
    public Offer createOffer(OfferCreationEvent event) {
    	Offer offer = new Offer();
    	offer.setVendorId(event.getVendorId());
    	offer.setVendorName(event.getVendorName());
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
		Offer offer = getOfferById(event.getOfferDto().getId()).get();
		
		offer.setActive(true);
		offer.setLatitude(event.getOfferDto().getLatitude());
		offer.setLongitude(event.getOfferDto().getLongitude());
		offer.setOriginalPrice(event.getOfferDto().getOriginalPrice());
		offer.setPrice(event.getOfferDto().getPrice());
		offer.setQuantityAvailable(event.getOfferDto().getQuantityAvailable());
		offer.setRating(event.getOfferDto().getRating());
		offer.setReviewsCount(event.getOfferDto().getReviewsCount());
		offer.setExpirationDate(event.getOfferDto().getExpirationDate());
		offer.setPickupStartTime(event.getOfferDto().getPickupStartTime());
		offer.setPickupEndTime(event.getOfferDto().getPickupEndTime());
		offer.setVendorId(event.getVendorId());
		offer.setAddress(event.getOfferDto().getAddress());
		offer.setAllergenInfo(event.getOfferDto().getAllergenInfo());
		offer.setBusinessType(event.getOfferDto().getBusinessType());
		offer.setDescription(event.getOfferDto().getDescription());
		offer.setDietaryInfo(event.getOfferDto().getDietaryInfo());
		offer.setImageUrl(event.getOfferDto().getImageUrl());
		offer.setName(event.getOfferDto().getName());
		offer.setZipCode(event.getOfferDto().getZipCode());
		offer.setVendorName(event.getOfferDto().getVendorName());

        return offerRepository.save(offer);
	}
    
    @Cacheable(value = "activeOffers")
    public List<OfferDto> findActiveOffersForVendor(Long vendorId) {
        List<Offer> activeOffers = offerRepository.findByVendorIdAndActive(vendorId, true);
        return activeOffers.stream()
                .map(this::toOfferDto)
                .collect(Collectors.toList());
    }
    
	public List<OfferDto> findAllOffersForVendor(Long vendorId) {
        List<Offer> activeOffers = offerRepository.findByVendorId(vendorId);
        return activeOffers.stream()
                .map(this::toOfferDto)
                .collect(Collectors.toList());
	}
    
    
    public List<Offer> getAllOffers() {
        return offerRepository.findAll();
    }

    public Optional<Offer> getOfferById(Long id) {
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
			offerRepository.updateOfferRelatedVendorDetails(event.getId(), event.getVendorName(), event.getAddress(), event.getZipCode(), event.getLatitude(), event.getLongitude(), event.getBusinessType(),  event.getReviewsCount());
			logger.info("Offer related vendor's details updated successfully");
		} catch (Exception e) {
			logger.info("Offer related vendor's details failed to update: {}", e.getMessage());
		}
	}

	public void findNearbyOffers(double userLat, double userLon, double range, String requestId) {
	    List<OfferDto> availableOffers = offerRepository.findAll().stream()
	            .filter(offer -> {
	                boolean isActive = offer.isActive();
	                boolean isNotExpired = offer.getExpirationDate().isAfter(OffsetDateTime.now());
	                double distance = calculateDistance(userLat, userLon, offer.getLatitude(), offer.getLongitude());
	                logger.info("Offer ID: {}, Active: {}, NotExpired: {}, Distance: {} km",
	                             offer.getId(), isActive, isNotExpired, distance);
	                return isActive && isNotExpired && distance <= range;
	            })
	            .map(this::toOfferDto)
	            .collect(Collectors.toList());

	    logger.info("SIZE of available offers list: {}", availableOffers.size());

	    // Include the requestId in the response event
	    eventPublisher.publishAvailableOffers(new AvailableOffersEvent(requestId, availableOffers));
	}


    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // Earth's radius in kilometers
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double s = R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        logger.info("CALCULATED RADIUS: {}", s);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
    
    private OfferDto toOfferDto(Offer offer) {
        return new OfferDto(
            offer.getId(),
            offer.getVendorName(),
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
            offer.getCreatedAt(),
            offer.getAddress(),
            offer.getZipCode(),
            offer.getLatitude(),
            offer.getLongitude(),
            offer.getBusinessType(),
            offer.getPickupStartTime(),
            offer.getPickupEndTime()
        );
    }

    public void respondToOfferDetailsRequest(OfferDetailsRequestedEvent event) {
        Optional<Offer> offer = offerRepository.findById(event.getOfferId());
        if (offer.isPresent()) {
            OfferDto offerDto = toOfferDto(offer.get());
            eventPublisher.publishOfferDetailsResponseEvent(
                new OfferDetailsResponseEvent(event.getRequestId(), offerDto)
            );
        } else {
            logger.error("Offer not found for ID: {}", event.getOfferId());
        }
    }

	public void updateOfferQuantity(OfferQuantityUpdatedEvent event) {
	    Offer offer = offerRepository.findById(event.getOfferId())
	            .orElseThrow(() -> new RuntimeException("Offer not found for ID: " + event.getOfferId()));

	        int newQuantity = offer.getQuantityAvailable() + event.getQuantityChange();
	        if (newQuantity < 0) {
	            throw new RuntimeException("Offer quantity cannot be negative");
	        }

	        offer.setQuantityAvailable(newQuantity);
	        offerRepository.save(offer);
	        logger.info("Updated quantity for Offer ID {}: {}", offer.getId(), newQuantity);
	}

    
}
