package com.stillfresh.app.offerservice.service;

import com.stillfresh.app.sharedentities.logging.LogSanitizer;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stillfresh.app.offerservice.config.OfferMetrics;
import com.stillfresh.app.sharedentities.exceptions.ResourceNotFoundException;
import com.stillfresh.app.offerservice.model.Offer;
import com.stillfresh.app.offerservice.publisher.OfferEventPublisher;
import com.stillfresh.app.offerservice.repository.OfferRepository;
import com.stillfresh.app.offerservice.service.OfferSupplyEventService;
import com.stillfresh.app.sharedentities.dto.OfferDto;
import com.stillfresh.app.sharedentities.enums.Currency;
import com.stillfresh.app.sharedentities.enums.OfferCategory;
import com.stillfresh.app.sharedentities.enums.PickupDaySlot;
import com.stillfresh.app.sharedentities.enums.PickupMealSlot;
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
    
    @Autowired
    private CurrencyDetectionService currencyDetectionService;
    
    @Autowired
    private TimeZoneDetectionService timeZoneDetectionService;

    @Autowired
    private OfferMetrics offerMetrics;

    @Autowired
    private OfferSupplyEventService offerSupplyEventService;

    @CacheEvict(value = "activeOffers", allEntries = true)
    public Offer createOffer(OfferCreationEvent event) {
    	Offer offer = new Offer();
    	offer.setVendorId(event.getVendorId());
    	offer.setLocationName(event.getLocationName());
    	offer.setChainName(event.getChainName());
    	offer.setWebsite(event.getWebsite());
    	offer.setVendorImageUrl(event.getVendorImageUrl());
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
    	// Set category from event, or fallback to mapping from businessType
    	if (event.getCategory() != null) {
    	    offer.setCategory(event.getCategory());
    	} else {
    	    offer.setCategory(mapBusinessTypeToCategory(event.getBusinessType()));
    	    logger.info("Category not provided, mapped businessType '{}' to category '{}'", 
    	               event.getBusinessType(), offer.getCategory());
    	}
    	offer.setDietaryInfo(event.getDietaryInfo());
    	offer.setAllergenInfo(event.getAllergenInfo());
    	// Option B: pickup date + times
    	ZoneId vendorZone = timeZoneDetectionService.getZoneId(event.getLatitude(), event.getLongitude());
    	LocalDate pickupDate = event.getPickupDate() != null ? event.getPickupDate() : LocalDate.now(vendorZone);
    	offer.setPickupDate(pickupDate);
    	offer.setPickupStartTime(event.getPickupStartTime());
    	offer.setPickupEndTime(event.getPickupEndTime());
    	
    	// Validate that pickup date + end time is in the future
    	validatePickupTimeInFuture(pickupDate, event.getPickupEndTime(), vendorZone, "create");
    	offer.setImageUrl(event.getImageUrl());
    	offer.setRating(event.getRating());
    	offer.setReviewsCount(event.getReviewsCount());
    	
    	// Calculate expirationDate from pickupDate + pickupEndTime in vendor's timezone
    	// If expirationDate is not provided from frontend, set it to end of pickup window
    	if (event.getExpirationDate() != null) {
    	    offer.setExpirationDate(event.getExpirationDate());
    	} else {
    	    // Set expiration to end of pickup window (pickupDate + pickupEndTime) in vendor's timezone
    	    LocalTime pickupEndTime = event.getPickupEndTime();
    	    OffsetDateTime expirationInVendorZone = pickupDate.atTime(pickupEndTime)
    	        .atZone(vendorZone)
    	        .toOffsetDateTime();
    	    offer.setExpirationDate(expirationInVendorZone);
    	    logger.info("Calculated expirationDate from pickupDate {} and pickupEndTime {} in vendor timezone {}: {}", 
    	               pickupDate, pickupEndTime, vendorZone, expirationInVendorZone);
    	}
    	
    	offer.setActive(true);
    	
    	// Determine currency based on vendor's country code
    	Currency currency = currencyDetectionService.getCurrencyForCountry(event.getCountry());
    	offer.setCurrency(currency.getIsoCode());
    	logger.info("Determined currency {} for offer from vendor country: {}", 
    	           currency, event.getCountry());
    	
        Offer saved = offerRepository.save(offer);
        offerSupplyEventService.recordCreate(saved, saved.getQuantityAvailable());
        return saved;
    }
    
	public Offer updateOffer(OfferUpdateEvent event) {
		Offer offer = getOfferById(event.getOfferDto().getId()).get();
		
		// Get vendor timezone for calculations
		ZoneId vendorZone = timeZoneDetectionService.getZoneId(
		    event.getOfferDto().getLatitude(), event.getOfferDto().getLongitude());
		
		// Reactivate offer and clear expired/sold-out status (reset logic)
		offer.setActive(true);
		offer.setExpiredAt(null);   // Clear expired status when updating
		offer.setSoldOutAt(null);   // Clear sold-out status when updating
		
		offer.setLatitude(event.getOfferDto().getLatitude());
		offer.setLongitude(event.getOfferDto().getLongitude());
		offer.setOriginalPrice(event.getOfferDto().getOriginalPrice());
		offer.setPrice(event.getOfferDto().getPrice());
		int previousQuantity = offer.getQuantityAvailable();
		int newQuantity = event.getOfferDto().getQuantityAvailable();
		offer.setQuantityAvailable(newQuantity);
		offer.setRating(event.getOfferDto().getRating());
		offer.setReviewsCount(event.getOfferDto().getReviewsCount());
		
		// Option B: pickup date + times
		LocalDate pickupDate = event.getOfferDto().getPickupDate();
		if (pickupDate != null) {
		    offer.setPickupDate(pickupDate);
		} else {
		    // If pickupDate is not provided, use existing or default to today
		    pickupDate = offer.getPickupDate() != null ? offer.getPickupDate() : LocalDate.now(vendorZone);
		}
		offer.setPickupStartTime(event.getOfferDto().getPickupStartTime());
		LocalTime pickupEndTime = event.getOfferDto().getPickupEndTime();
		if (pickupEndTime != null) {
		    offer.setPickupEndTime(pickupEndTime);
		} else {
		    // If pickupEndTime is not provided, use existing
		    pickupEndTime = offer.getPickupEndTime();
		}
		
		// Validate that pickup date + end time is in the future
		validatePickupTimeInFuture(pickupDate, pickupEndTime, vendorZone, "update");
		
		// Recalculate expirationDate from pickupDate + pickupEndTime if not provided
		// (Same logic as createOffer - enables reset of expired offers)
		if (event.getOfferDto().getExpirationDate() != null) {
		    offer.setExpirationDate(event.getOfferDto().getExpirationDate());
		} else {
		    // Recalculate from pickupDate + pickupEndTime in vendor's timezone
		    // Use the pickupEndTime variable already set above
		    if (pickupEndTime != null) {
		        OffsetDateTime expirationInVendorZone = pickupDate.atTime(pickupEndTime)
		            .atZone(vendorZone)
		            .toOffsetDateTime();
		        offer.setExpirationDate(expirationInVendorZone);
		        logger.info("Recalculated expirationDate from pickupDate {} and pickupEndTime {} in vendor timezone {}: {}", 
		                   pickupDate, pickupEndTime, vendorZone, expirationInVendorZone);
		    }
		}
		
		offer.setVendorId(event.getVendorId());
		offer.setAddress(event.getOfferDto().getAddress());
		offer.setAllergenInfo(event.getOfferDto().getAllergenInfo());
		offer.setBusinessType(event.getOfferDto().getBusinessType());
		// Update category if provided, otherwise keep existing or map from businessType
		if (event.getOfferDto().getCategory() != null) {
		    offer.setCategory(event.getOfferDto().getCategory());
		} else if (offer.getCategory() == null) {
		    offer.setCategory(mapBusinessTypeToCategory(event.getOfferDto().getBusinessType()));
		}
		offer.setDescription(event.getOfferDto().getDescription());
		offer.setDietaryInfo(event.getOfferDto().getDietaryInfo());
		offer.setImageUrl(event.getOfferDto().getImageUrl());
		offer.setName(event.getOfferDto().getName());
		offer.setZipCode(event.getOfferDto().getZipCode());
		offer.setLocationName(event.getOfferDto().getLocationName());
		offer.setChainName(event.getOfferDto().getChainName());
		offer.setWebsite(event.getOfferDto().getWebsite());
		offer.setVendorImageUrl(event.getOfferDto().getVendorImageUrl());

		// Currency is determined by vendor's country, which is updated via updateOfferRelatedVendorDetails
		// If currency is not set, keep existing currency or default to EUR
		if (offer.getCurrency() == null || offer.getCurrency().isEmpty()) {
			offer.setCurrency(Currency.EUR.getIsoCode());
			logger.warn("Offer {} has no currency set, defaulting to EUR", offer.getId());
		}

		logger.info("Updated offer ID={}, cleared expiredAt/soldOutAt, reactivated offer, recalculated expirationDate if needed", 
		           offer.getId());
        Offer saved = offerRepository.save(offer);
        offerSupplyEventService.recordReplenishDelta(saved, previousQuantity, newQuantity);
        return saved;
	}
    
    @Cacheable(value = "activeOffers")
    public List<OfferDto> findActiveOffersForVendor(Long vendorId) {
        List<Offer> activeOffers = offerRepository.findByVendorIdAndActive(vendorId, true);
        return activeOffers.stream()
                .map(offer -> {
                    ZoneId vendorZone = timeZoneDetectionService.getZoneId(
                        offer.getLatitude(), offer.getLongitude());
                    return toOfferDto(offer, vendorZone);
                })
                .collect(Collectors.toList());
    }

	public List<OfferDto> findAllOffersForVendor(Long vendorId) {
        List<Offer> activeOffers = offerRepository.findByVendorId(vendorId);
        return activeOffers.stream()
                .map(offer -> {
                    ZoneId vendorZone = timeZoneDetectionService.getZoneId(
                        offer.getLatitude(), offer.getLongitude());
                    return toOfferDto(offer, vendorZone);
                })
                .collect(Collectors.toList());
	}
    
    
    public List<Offer> getAllOffers() {
        return offerRepository.findAll();
    }
    
    public List<OfferDto> getAllOffersAsDto() {
        long startTotal = System.currentTimeMillis();
        long startDb = startTotal;

        List<Offer> visibleOffers = offerRepository.findVisibleOffers();

        long endDb = System.currentTimeMillis();

        List<OfferDto> result = visibleOffers.stream()
                .map(offer -> {
                    ZoneId vendorZone = timeZoneDetectionService.getZoneId(
                        offer.getLatitude(), offer.getLongitude());
                    return toOfferDto(offer, vendorZone);
                })
                .collect(Collectors.toList());

        long endTotal = System.currentTimeMillis();

        long dbMs = endDb - startDb;
        long inMemoryMs = endTotal - endDb;
        logger.info("getAllOffersAsDto - dbMs={}, inMemoryMs={}, totalMs={}, count={}",
                dbMs, inMemoryMs, (endTotal - startTotal), result.size());
        offerMetrics.recordOfferListDb(OfferMetrics.ENDPOINT_ALL, dbMs);
        offerMetrics.recordOfferListInMemory(OfferMetrics.ENDPOINT_ALL, inMemoryMs);

        return result;
    }

    public Optional<Offer> getOfferById(Long id) {
        return offerRepository.findById(id);
    }
    
    public Optional<OfferDto> getOfferByIdAsDto(Long id) {
        return offerRepository.findById(id)
                .map(this::toOfferDto);
    }

    /**
     * Batch fetch offers by IDs and convert them to DTOs.
     * This is used by user-service favorites to avoid N+1 lookups.
     *
     * @param ids list of offer IDs
     * @return list of OfferDto, in unspecified order
     */
    public List<OfferDto> getOffersByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return offerRepository.findAllById(ids).stream()
                .filter(Objects::nonNull)
                .map(this::toOfferDto)
                .collect(Collectors.toList());
    }

    /**
     * Deletes an offer on behalf of the authenticated caller.
     *
     * <p>The role check in {@code WebSecurityConfig} establishes that the caller is a vendor or
     * an admin; this check establishes that a vendor is deleting its own offer. Without it any
     * vendor could delete the entire marketplace one ID at a time.
     *
     * @param id             the offer to delete
     * @param callerVendorId the caller's vendor ID, as stamped by the gateway; ignored for admins
     * @param callerIsAdmin  whether the caller holds ADMIN or SUPER_ADMIN
     */
    @CacheEvict(value = "activeOffers", allEntries = true)
    public void deleteOffer(Long id, Long callerVendorId, boolean callerIsAdmin) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with id: " + id));

        if (!callerIsAdmin) {
            if (callerVendorId == null || !callerVendorId.equals(offer.getVendorId())) {
                logger.warn("Rejected delete of offer {} by vendorId {}: offer belongs to vendorId {}",
                        id, callerVendorId, offer.getVendorId());
                throw new AccessDeniedException("You can only delete your own offers");
            }
        }

        offerRepository.delete(offer);
        logger.info("Deleted offer {} (vendorId {})", id, offer.getVendorId());
    }
    
    
    // Manually invalidate an offer
    @CacheEvict(value = "activeOffers", allEntries = true)
    public void invalidateOffer(Long offerId) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found with id: " + offerId));
        if (!offer.isActive()) {
            throw new IllegalStateException("Offer is already inactive");
        }
        
        // Set expiredAt to today in vendor's timezone (consistent with automatic expiration)
        ZoneId vendorZone = timeZoneDetectionService.getZoneId(
            offer.getLatitude(), offer.getLongitude());
        offer.setExpiredAt(LocalDate.now(vendorZone));
        
        // Clear soldOutAt if it was set (manual invalidation, not sold out)
        offer.setSoldOutAt(null);
        
        offer.setActive(false);
        offerRepository.save(offer);
        
        logger.info("Manually invalidated offer: ID={} ExpiredAt={}", offer.getId(), offer.getExpiredAt());
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
                // Set expiredAt to today in vendor's timezone
                ZoneId vendorZone = timeZoneDetectionService.getZoneId(
                    offer.getLatitude(), offer.getLongitude());
                offer.setExpiredAt(LocalDate.now(vendorZone));
                offer.setActive(false);
                offerRepository.save(offer);
                logger.debug("Invalidated expired offer: ID={} ExpiredAt={} ExpirationDate={}",
                           offer.getId(), offer.getExpiredAt(), offer.getExpirationDate());
            });
            logger.info("Scheduled task completed. Total invalidated offers: {}", expiredOffers.size());
        }
    }

	public void updateOfferRelatedVendorDetails(OfferRelatedVendorDetailsEvent event) {
        logger.info("Updating offer related vendor's details...");
		try {
			// Determine currency based on vendor's country code
			Currency currency = currencyDetectionService.getCurrencyForCountry(event.getCountry());
			
			offerRepository.updateOfferRelatedVendorDetails(
			    event.getId(),
			    event.getLocationName(),
			    event.getChainName(),
			    event.getWebsite(),
			    event.getVendorImageUrl(),
			    event.getAddress(),
			    event.getZipCode(),
			    event.getLatitude(),
			    event.getLongitude(),
			    event.getBusinessType(),
			    event.getReviewsCount(),
			    event.getRating(),
			    currency.getIsoCode()
			);
			logger.info("Offer related vendor's details updated successfully with currency: {} (from country: {})", currency, event.getCountry());
		} catch (Exception e) {
			logger.info("Offer related vendor's details failed to update: {}", e.getMessage());
		}
	}

    /**
     * Handles Kafka OfferRequestEvent: uses the same optimized getNearbyOffers logic as the REST
     * /offers/nearby endpoint (DB bounding-box filter + in-memory distance/sort), then publishes
     * AvailableOffersEvent. No full-table scan.
     */
    public void findNearbyOffers(double userLat, double userLon, double range, String requestId) {
        List<OfferDto> availableOffers = getNearbyOffers(userLat, userLon, range);

        logger.info("findNearbyOffers (event) - count={}", availableOffers.size());

        // Include the requestId in the response event
        eventPublisher.publishAvailableOffers(new AvailableOffersEvent(requestId, availableOffers));
    }

    public List<OfferDto> getNearbyOffers(double userLat, double userLon, double range) {
        return getNearbyOffers(userLat, userLon, range, null);
    }
    
    public List<OfferDto> getNearbyOffers(double userLat, double userLon, double range, String categoryStr) {
        long startTotal = System.currentTimeMillis();

        OfferCategory category = null;
        if (categoryStr != null && !categoryStr.isEmpty()) {
            try {
                category = OfferCategory.valueOf(categoryStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid category provided: {}", categoryStr);
            }
        }

        final OfferCategory filterCategory = category;

        BoundingBox box = calculateBoundingBox(userLat, userLon, range);

        long startDb = System.currentTimeMillis();
        List<Offer> candidates = offerRepository.findVisibleOffersInBoundingBox(
                box.minLat, box.maxLat, box.minLon, box.maxLon);
        long endDb = System.currentTimeMillis();

        List<NearbyOffer> nearbyOffers = candidates.stream()
                .map(offer -> {
                    double distance = calculateDistance(userLat, userLon,
                            offer.getLatitude(), offer.getLongitude());
                    return new NearbyOffer(offer, distance);
                })
                .filter(nearby -> nearby.distance <= range)
                .filter(nearby -> {
                    if (filterCategory == null) {
                        return true;
                    }
                    Offer offer = nearby.offer;
                    OfferCategory effectiveCategory = offer.getCategory() != null
                            ? offer.getCategory()
                            : mapBusinessTypeToCategory(offer.getBusinessType());
                    return effectiveCategory == filterCategory;
                })
                .sorted(Comparator.comparingDouble(o -> o.distance))
                .collect(Collectors.toList());

        List<OfferDto> result = nearbyOffers.stream()
                .map(nearby -> {
                    ZoneId vendorZone = timeZoneDetectionService.getZoneId(
                        nearby.offer.getLatitude(), nearby.offer.getLongitude());
                    return toOfferDto(nearby.offer, vendorZone);
                })
                .collect(Collectors.toList());

        long endTotal = System.currentTimeMillis();

        long dbMs = endDb - startDb;
        long inMemoryMs = endTotal - endDb;
        logger.info("getNearbyOffers - dbMs={}, inMemoryMs={}, totalMs={}, rangeKm={}, lat={}, lon={}, count={}",
                dbMs, inMemoryMs, (endTotal - startTotal), range,
                LogSanitizer.roundCoordinate(userLat), LogSanitizer.roundCoordinate(userLon), result.size());
        offerMetrics.recordOfferListDb(OfferMetrics.ENDPOINT_NEARBY, dbMs);
        offerMetrics.recordOfferListInMemory(OfferMetrics.ENDPOINT_NEARBY, inMemoryMs);

        return result;
    }
    
    public List<OfferDto> getOffersByCategory(OfferCategory category) {
        long startTotal = System.currentTimeMillis();
        long startDb = startTotal;

        // Start from all visible offers (DB filtered), then apply category logic in memory
        List<Offer> visibleOffers = offerRepository.findVisibleOffers();

        long endDb = System.currentTimeMillis();

        List<OfferDto> result = visibleOffers.stream()
                .filter(offer -> {
                    OfferCategory effectiveCategory = offer.getCategory() != null
                            ? offer.getCategory()
                            : mapBusinessTypeToCategory(offer.getBusinessType());
                    return effectiveCategory == category;
                })
                .map(offer -> {
                    ZoneId vendorZone = timeZoneDetectionService.getZoneId(
                        offer.getLatitude(), offer.getLongitude());
                    return toOfferDto(offer, vendorZone);
                })
                .collect(Collectors.toList());

        long endTotal = System.currentTimeMillis();

        long dbMs = endDb - startDb;
        long inMemoryMs = endTotal - endDb;
        logger.info("getOffersByCategory - dbMs={}, inMemoryMs={}, totalMs={}, category={}, count={}",
                dbMs, inMemoryMs, (endTotal - startTotal), category, result.size());
        offerMetrics.recordOfferListDb(OfferMetrics.ENDPOINT_BY_CATEGORY, dbMs);
        offerMetrics.recordOfferListInMemory(OfferMetrics.ENDPOINT_BY_CATEGORY, inMemoryMs);

        return result;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // Earth's radius in kilometers
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private BoundingBox calculateBoundingBox(double latitude, double longitude, double rangeKm) {
        // Approximate conversion: 1 degree latitude ~ 111 km
        double latDelta = rangeKm / 111.0;

        // Avoid division by zero near the poles
        double cosLat = Math.cos(Math.toRadians(latitude));
        double lonDegreeKm = cosLat == 0 ? 1e-6 : 111.0 * cosLat;
        double lonDelta = rangeKm / lonDegreeKm;

        double minLat = latitude - latDelta;
        double maxLat = latitude + latDelta;
        double minLon = longitude - lonDelta;
        double maxLon = longitude + lonDelta;

        // Clamp to valid ranges
        minLat = Math.max(minLat, -90.0);
        maxLat = Math.min(maxLat, 90.0);
        minLon = Math.max(minLon, -180.0);
        maxLon = Math.min(maxLon, 180.0);

        return new BoundingBox(minLat, maxLat, minLon, maxLon);
    }

    private static final class BoundingBox {
        final double minLat;
        final double maxLat;
        final double minLon;
        final double maxLon;

        BoundingBox(double minLat, double maxLat, double minLon, double maxLon) {
            this.minLat = minLat;
            this.maxLat = maxLat;
            this.minLon = minLon;
            this.maxLon = maxLon;
        }
    }

    private static final class NearbyOffer {
        final Offer offer;
        final double distance;

        NearbyOffer(Offer offer, double distance) {
            this.offer = offer;
            this.distance = distance;
        }
    }
    
    /**
     * Converts an offer to DTO using the given vendor timezone.
     * Use this overload in batch flows to reuse a pre-resolved ZoneId and avoid repeated lookups per offer.
     */
    public OfferDto toOfferDto(Offer offer, ZoneId vendorZone) {
        OfferDto dto = new OfferDto(
            offer.getId(),
            offer.getLocationName(),
            offer.getChainName(),
            offer.getWebsite(),
            offer.getVendorImageUrl(),
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
            offer.getCurrency() != null ? offer.getCurrency() : Currency.EUR.getIsoCode(), // Default to EUR if null
            offer.getBusinessType(),
            offer.getCategory() != null ? offer.getCategory() : mapBusinessTypeToCategory(offer.getBusinessType()),
            offer.getPickupDate(),
            offer.getPickupStartTime(),
            offer.getPickupEndTime()
        );
        dto.setVendorId(offer.getVendorId());
        dto.setOriginalQuantity(offer.getOriginalQuantity() > 0 ? offer.getOriginalQuantity() : offer.getQuantityAvailable());

        // Set status flags: true if offer has ever been marked expired/sold out (so favorites can show "Expired" and allow removal)
        boolean isExpired = offer.getExpiredAt() != null;
        boolean isSoldOut = offer.getSoldOutAt() != null;

        dto.setExpired(isExpired);
        dto.setSoldOut(isSoldOut);
        dto.setGreyedOut(isExpired || isSoldOut);
        
        // Derived pickup grouping fields for UI
        LocalDate pickupDate = offer.getPickupDate() != null ? offer.getPickupDate() : LocalDate.now(vendorZone);
        LocalTime start = offer.getPickupStartTime();
        LocalTime end = offer.getPickupEndTime();
        dto.setPickupDaySlot(computePickupDaySlot(pickupDate, vendorZone));
        dto.setPickupMealSlot(computePickupMealSlot(start, end));
        dto.setCollectNow(computeCollectNow(pickupDate, start, end, vendorZone));

        return dto;
    }

    /** Single-offer conversion; resolves vendor timezone internally. */
    public OfferDto toOfferDto(Offer offer) {
        ZoneId vendorZone = timeZoneDetectionService.getZoneId(offer.getLatitude(), offer.getLongitude());
        return toOfferDto(offer, vendorZone);
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

    @CacheEvict(value = "activeOffers", allEntries = true)
    public void updateOfferQuantity(OfferQuantityUpdatedEvent event) {
        Offer offer = offerRepository.findById(event.getOfferId())
                .orElseThrow(() -> new RuntimeException("Offer not found for ID: " + event.getOfferId()));

        int newQuantity = offer.getQuantityAvailable() + event.getQuantityChange();
        if (newQuantity < 0) {
            throw new RuntimeException("Offer quantity cannot be negative");
        }

        offer.setQuantityAvailable(newQuantity);
        if (newQuantity == 0) {
            // Set soldOutAt to today in vendor's timezone
            ZoneId vendorZone = timeZoneDetectionService.getZoneId(
                offer.getLatitude(), offer.getLongitude());
            offer.setSoldOutAt(LocalDate.now(vendorZone));
            offer.setActive(false);
            logger.info("Offer ID {} is now sold out and set to inactive. SoldOutAt={}", 
                       offer.getId(), offer.getSoldOutAt());
        }
        offerRepository.save(offer);
        logger.info("Updated quantity for Offer ID {}: {}", offer.getId(), newQuantity);
    }

    /**
     * Validates that pickup date + pickup end time is in the future (ahead of current time).
     * Throws IllegalArgumentException if the pickup time is in the past.
     * 
     * @param pickupDate The pickup date
     * @param pickupEndTime The pickup end time
     * @param vendorZone The vendor's timezone
     * @param operation The operation being performed ("create" or "update") for error messages
     * @throws IllegalArgumentException if pickup date + end time is in the past
     */
    private void validatePickupTimeInFuture(LocalDate pickupDate, LocalTime pickupEndTime, ZoneId vendorZone, String operation) {
        if (pickupDate == null || pickupEndTime == null) {
            // If either is null, skip validation (will be handled elsewhere)
            return;
        }
        
        // Combine pickup date + end time in vendor's timezone
        OffsetDateTime pickupEndDateTime = pickupDate.atTime(pickupEndTime)
            .atZone(vendorZone)
            .toOffsetDateTime();
        
        // Get current time in vendor's timezone
        OffsetDateTime now = OffsetDateTime.now(vendorZone);
        
        // Check if pickup end time is in the past
        if (pickupEndDateTime.isBefore(now) || pickupEndDateTime.isEqual(now)) {
            String errorMessage = String.format(
                "Cannot %s offer: Pickup end time (%s %s) must be in the future. Current time in vendor timezone: %s",
                operation,
                pickupDate,
                pickupEndTime,
                now.toLocalDate() + " " + now.toLocalTime()
            );
            logger.warn(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        
        logger.debug("Pickup time validation passed: {} {} is in the future (vendor timezone: {})", 
                    pickupDate, pickupEndTime, vendorZone);
    }

    /**
     * Maps business type to default category as fallback.
     * Used when category is not explicitly provided.
     * 
     * @param businessType The business type string
     * @return Default category for the business type, or GROCERIES as default
     */
    private OfferCategory mapBusinessTypeToCategory(String businessType) {
        if (businessType == null || businessType.isEmpty()) {
            return OfferCategory.GROCERIES; // Default fallback
        }
        
        String bt = businessType.toLowerCase().trim();
        
        // Map common business types to categories
        if (bt.contains("restaurant") || bt.contains("cafe") || bt.contains("bistro") || 
            bt.contains("food") || bt.contains("meal")) {
            return OfferCategory.MEALS;
        }
        if (bt.contains("bakery") || bt.contains("bread") || bt.contains("pastry") || 
            bt.contains("patisserie")) {
            return OfferCategory.BREAD_PASTRIES;
        }
        if (bt.contains("supermarket") || bt.contains("grocery") || bt.contains("market") ||
            bt.contains("store") || bt.contains("shop")) {
            return OfferCategory.GROCERIES;
        }
        if (bt.contains("florist") || bt.contains("flower") || bt.contains("plant")) {
            return OfferCategory.FLOWERS_PLANTS;
        }
        if (bt.contains("pet") || bt.contains("animal")) {
            return OfferCategory.PET_FOOD;
        }
        
        // Default fallback
        return OfferCategory.GROCERIES;
    }
    
    private PickupDaySlot computePickupDaySlot(LocalDate pickupDate, ZoneId vendorZone) {
        LocalDate today = LocalDate.now(vendorZone != null ? vendorZone : ZoneId.systemDefault());
        if (pickupDate == null) {
            return PickupDaySlot.TODAY;
        }
        if (pickupDate.isEqual(today)) {
            return PickupDaySlot.TODAY;
        }
        if (pickupDate.isEqual(today.plusDays(1))) {
            return PickupDaySlot.TOMORROW;
        }
        if (pickupDate.isBefore(today)) {
            return PickupDaySlot.PAST;
        }
        return PickupDaySlot.FUTURE;
    }
    
    /**
     * Computes the meal/time-of-day slot from a pickup interval.
     * Uses the midpoint of [start,end] when possible.
     */
    private PickupMealSlot computePickupMealSlot(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            return PickupMealSlot.OTHER;
        }
        // If interval spans midnight or is invalid, fallback
        if (end.isBefore(start)) {
            return PickupMealSlot.OTHER;
        }
        
        int startMin = start.getHour() * 60 + start.getMinute();
        int endMin = end.getHour() * 60 + end.getMinute();
        int mid = (startMin + endMin) / 2;
        int midHour = mid / 60;
        
        // Tunable ranges
        if (midHour >= 5 && midHour < 11) {
            return PickupMealSlot.BREAKFAST;
        }
        if (midHour >= 11 && midHour < 16) {
            return PickupMealSlot.LUNCH;
        }
        if (midHour >= 16 && midHour < 22) {
            return PickupMealSlot.DINNER;
        }
        return PickupMealSlot.OTHER;
    }
    
    private boolean computeCollectNow(LocalDate pickupDate, LocalTime start, LocalTime end, ZoneId vendorZone) {
        if (pickupDate == null || start == null || end == null) {
            return false;
        }
        LocalDate today = LocalDate.now(vendorZone != null ? vendorZone : ZoneId.systemDefault());
        if (!pickupDate.isEqual(today)) {
            return false;
        }
        LocalTime now = LocalTime.now(vendorZone != null ? vendorZone : ZoneId.systemDefault());
        // Inclusive window
        return !now.isBefore(start) && !now.isAfter(end);
    }

    
}
