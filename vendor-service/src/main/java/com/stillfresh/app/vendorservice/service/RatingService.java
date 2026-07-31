package com.stillfresh.app.vendorservice.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stillfresh.app.sharedentities.vendor.events.OfferRelatedVendorDetailsEvent;
import com.stillfresh.app.vendorservice.client.OrderClient;
import com.stillfresh.app.vendorservice.client.OrderRatingEligibilityResponse;
import com.stillfresh.app.vendorservice.dto.RatingRequest;
import com.stillfresh.app.vendorservice.dto.RatingResponse;
import com.stillfresh.app.vendorservice.dto.VendorRatingSummary;
import com.stillfresh.app.vendorservice.exception.RatingValidationException;
import com.stillfresh.app.vendorservice.model.Rating;
import com.stillfresh.app.vendorservice.model.Vendor;
import com.stillfresh.app.vendorservice.publisher.VendorEventPublisher;
import com.stillfresh.app.vendorservice.repository.RatingRepository;
import com.stillfresh.app.vendorservice.repository.VendorRepository;

import feign.FeignException;

@Service
public class RatingService {

    private static final Logger logger = LoggerFactory.getLogger(RatingService.class);

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private OrderClient orderClient;

    @Autowired
    private VendorEventPublisher eventPublisher;

    /**
     * Submit a rating for a vendor tied to a completed order.
     * One rating per order; resubmitting for the same order updates that rating.
     */
    @Transactional
    public RatingResponse submitRating(Long userId, RatingRequest ratingRequest) {
        logger.info("Submitting rating for vendor {} order {} by user {}",
                ratingRequest.getVendorId(), ratingRequest.getOrderId(), userId);

        Optional<Vendor> vendorOpt = vendorRepository.findById(ratingRequest.getVendorId());
        if (vendorOpt.isEmpty()) {
            throw new RatingValidationException(HttpStatus.NOT_FOUND,
                    "Vendor not found with ID: " + ratingRequest.getVendorId());
        }

        OrderRatingEligibilityResponse eligibility = fetchOrderEligibility(ratingRequest.getOrderId());

        if (!userId.equals(eligibility.getUserId())) {
            throw new RatingValidationException(HttpStatus.FORBIDDEN,
                    "Order not found or access denied");
        }

        if (!ratingRequest.getVendorId().equals(eligibility.getVendorId())) {
            throw new RatingValidationException(HttpStatus.BAD_REQUEST,
                    "Vendor ID does not match the order's vendor");
        }

        if (!eligibility.isEligible()) {
            throw new RatingValidationException(HttpStatus.BAD_REQUEST,
                    "Order must be COMPLETED before rating. Current status: " + eligibility.getStatus());
        }

        Optional<Rating> existingRatingOpt = ratingRepository.findByOrderId(ratingRequest.getOrderId());

        Rating rating;
        if (existingRatingOpt.isPresent()) {
            rating = existingRatingOpt.get();
            if (!rating.getUserId().equals(userId)) {
                throw new RatingValidationException(HttpStatus.FORBIDDEN,
                        "Order not found or access denied");
            }
            logger.info("Updating existing rating ID {} for order {}", rating.getId(), ratingRequest.getOrderId());
        } else {
            rating = new Rating();
            rating.setUserId(userId);
            rating.setVendorId(ratingRequest.getVendorId());
            rating.setOrderId(ratingRequest.getOrderId());
        }

        rating.setCollectionProcessRating(ratingRequest.getCollectionProcessRating());
        rating.setQualityRating(ratingRequest.getQualityRating());
        rating.setQuantityRating(ratingRequest.getQuantityRating());
        rating.setVarietyRating(ratingRequest.getVarietyRating());

        rating = ratingRepository.save(rating);

        updateVendorRating(ratingRequest.getVendorId());
        publishOfferRatingSync(vendorOpt.get());

        return convertToResponse(rating);
    }

    private OrderRatingEligibilityResponse fetchOrderEligibility(Long orderId) {
        try {
            OrderRatingEligibilityResponse response = orderClient.getRatingEligibility(orderId);
            if (response == null) {
                throw new RatingValidationException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId);
            }
            return response;
        } catch (FeignException.NotFound e) {
            throw new RatingValidationException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId);
        } catch (FeignException.Forbidden e) {
            throw new RatingValidationException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to validate order eligibility");
        } catch (FeignException e) {
            logger.error("Failed to fetch order eligibility for order {}: {}", orderId, e.getMessage());
            throw new RatingValidationException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to validate order eligibility");
        }
    }

    private void publishOfferRatingSync(Vendor vendor) {
        Vendor refreshed = vendorRepository.findById(vendor.getId()).orElse(vendor);
        eventPublisher.publishOfferRelatedVendorDetailsEvent(new OfferRelatedVendorDetailsEvent(
                refreshed.getId(),
                refreshed.getLocationName(),
                refreshed.getChainName(),
                refreshed.getWebsite(),
                refreshed.getImageUrl(),
                refreshed.getAddress(),
                refreshed.getZipCode(),
                refreshed.getLatitude(),
                refreshed.getLongitude(),
                refreshed.getBusinessType(),
                refreshed.getReviewsCount(),
                refreshed.getAverageRating(),
                refreshed.getCountry()));
    }

    @Transactional
    public void updateVendorRating(Long vendorId) {
        logger.info("Updating vendor rating for vendor ID: {}", vendorId);

        List<Rating> ratings = ratingRepository.findByVendorId(vendorId);

        if (ratings.isEmpty()) {
            Optional<Vendor> vendorOpt = vendorRepository.findById(vendorId);
            if (vendorOpt.isPresent()) {
                Vendor vendor = vendorOpt.get();
                vendor.setAverageRating(0.0);
                vendor.setReviewsCount(0);
                vendorRepository.save(vendor);
            }
            return;
        }

        double totalRatingSum = 0.0;
        int totalRatings = ratings.size();

        for (Rating rating : ratings) {
            totalRatingSum += rating.getTotalRating();
        }

        double averageRating = totalRatingSum / totalRatings;

        Optional<Vendor> vendorOpt = vendorRepository.findById(vendorId);
        if (vendorOpt.isPresent()) {
            Vendor vendor = vendorOpt.get();
            vendor.setAverageRating(averageRating);
            vendor.setReviewsCount(totalRatings);
            vendorRepository.save(vendor);
            logger.info("Updated vendor {} with average rating: {} and review count: {}",
                    vendorId, averageRating, totalRatings);
        }
    }

    public List<RatingResponse> getRatingsByVendorId(Long vendorId) {
        logger.info("Fetching ratings for vendor ID: {}", vendorId);

        List<Rating> ratings = ratingRepository.findByVendorId(vendorId);
        return ratings.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<RatingResponse> getRatingsByUserId(Long userId) {
        logger.info("Fetching ratings by user ID: {}", userId);

        List<Rating> ratings = ratingRepository.findByUserId(userId);
        return ratings.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public VendorRatingSummary getVendorRatingSummary(Long vendorId) {
        logger.info("Fetching rating summary for vendor ID: {}", vendorId);

        List<Rating> ratings = ratingRepository.findByVendorId(vendorId);

        if (ratings.isEmpty()) {
            VendorRatingSummary summary = new VendorRatingSummary();
            summary.setVendorId(vendorId);
            summary.setAverageRating(0.0);
            summary.setTotalRatings(0);
            summary.setAverageCollectionProcessRating(0.0);
            summary.setAverageQualityRating(0.0);
            summary.setAverageQuantityRating(0.0);
            summary.setAverageVarietyRating(0.0);
            return summary;
        }

        double collectionProcessSum = 0.0;
        double qualitySum = 0.0;
        double quantitySum = 0.0;
        double varietySum = 0.0;
        double totalRatingSum = 0.0;
        int count = ratings.size();

        for (Rating rating : ratings) {
            collectionProcessSum += rating.getCollectionProcessRating();
            qualitySum += rating.getQualityRating();
            quantitySum += rating.getQuantityRating();
            varietySum += rating.getVarietyRating();
            totalRatingSum += rating.getTotalRating();
        }

        VendorRatingSummary summary = new VendorRatingSummary();
        summary.setVendorId(vendorId);
        summary.setAverageRating(totalRatingSum / count);
        summary.setTotalRatings(count);
        summary.setAverageCollectionProcessRating(collectionProcessSum / count);
        summary.setAverageQualityRating(qualitySum / count);
        summary.setAverageQuantityRating(quantitySum / count);
        summary.setAverageVarietyRating(varietySum / count);

        return summary;
    }

    public boolean hasOrderBeenRated(Long orderId) {
        return ratingRepository.existsByOrderId(orderId);
    }

    private RatingResponse convertToResponse(Rating rating) {
        RatingResponse response = new RatingResponse();
        response.setId(rating.getId());
        response.setVendorId(rating.getVendorId());
        response.setUserId(rating.getUserId());
        response.setOrderId(rating.getOrderId());
        response.setCollectionProcessRating(rating.getCollectionProcessRating());
        response.setQualityRating(rating.getQualityRating());
        response.setQuantityRating(rating.getQuantityRating());
        response.setVarietyRating(rating.getVarietyRating());
        response.setTotalRating(rating.getTotalRating());
        response.setCreatedAt(rating.getCreatedAt());
        response.setUpdatedAt(rating.getUpdatedAt());
        return response;
    }
}
