package com.stillfresh.app.vendorservice.service;

import com.stillfresh.app.sharedentities.vendor.events.OfferRelatedVendorDetailsEvent;
import com.stillfresh.app.vendorservice.client.OrderClient;
import com.stillfresh.app.vendorservice.client.OrderRatingEligibilityResponse;
import com.stillfresh.app.vendorservice.dto.RatingRequest;
import com.stillfresh.app.vendorservice.dto.RatingResponse;
import com.stillfresh.app.vendorservice.exception.RatingValidationException;
import com.stillfresh.app.vendorservice.model.Rating;
import com.stillfresh.app.vendorservice.model.Vendor;
import com.stillfresh.app.vendorservice.publisher.VendorEventPublisher;
import com.stillfresh.app.vendorservice.repository.RatingRepository;
import com.stillfresh.app.vendorservice.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private OrderClient orderClient;

    @Mock
    private VendorEventPublisher eventPublisher;

    @InjectMocks
    private RatingService ratingService;

    private Vendor vendor;
    private RatingRequest baseRequest;

    @BeforeEach
    void setUp() {
        vendor = new Vendor();
        vendor.setId(10L);
        vendor.setLocationName("Test Vendor");
        vendor.setLatitude(44.0);
        vendor.setLongitude(20.0);
        vendor.setBusinessType("Bakery");
        vendor.setCountry("RS");

        baseRequest = new RatingRequest();
        baseRequest.setVendorId(10L);
        baseRequest.setOrderId(100L);
        baseRequest.setCollectionProcessRating(5);
        baseRequest.setQualityRating(4);
        baseRequest.setQuantityRating(4);
        baseRequest.setVarietyRating(3);
    }

    @Test
    void submitRating_twoOrdersSameUserVendor_createsTwoRatingsAndAggregates() {
        when(vendorRepository.findById(10L)).thenReturn(Optional.of(vendor));
        when(orderClient.getRatingEligibility(100L))
                .thenReturn(eligibility(100L, 1L, 10L, "COMPLETED", true));
        when(orderClient.getRatingEligibility(200L))
                .thenReturn(eligibility(200L, 1L, 10L, "COMPLETED", true));
        when(ratingRepository.findByOrderId(any())).thenReturn(Optional.empty());

        List<Rating> stored = new ArrayList<>();
        when(ratingRepository.save(any(Rating.class))).thenAnswer(inv -> {
            Rating r = inv.getArgument(0);
            r.setId(r.getOrderId());
            stored.removeIf(existing -> existing.getOrderId().equals(r.getOrderId()));
            stored.add(r);
            return r;
        });
        when(ratingRepository.findByVendorId(10L)).thenAnswer(inv -> List.copyOf(stored));

        baseRequest.setOrderId(100L);
        baseRequest.setQualityRating(4);
        ratingService.submitRating(1L, baseRequest);

        baseRequest.setOrderId(200L);
        baseRequest.setQualityRating(3);
        ratingService.submitRating(1L, baseRequest);

        ArgumentCaptor<Vendor> vendorCaptor = ArgumentCaptor.forClass(Vendor.class);
        verify(vendorRepository, atLeastOnce()).save(vendorCaptor.capture());
        Vendor saved = vendorCaptor.getAllValues().get(vendorCaptor.getAllValues().size() - 1);

        assertEquals(2, saved.getReviewsCount());
        assertEquals(3.875, saved.getAverageRating(), 0.01);
        verify(eventPublisher, times(2)).publishOfferRelatedVendorDetailsEvent(any(OfferRelatedVendorDetailsEvent.class));
    }

    @Test
    void submitRating_sameOrder_updatesExistingRatingWithoutIncreasingCount() {
        Rating existing = new Rating();
        existing.setId(50L);
        existing.setUserId(1L);
        existing.setVendorId(10L);
        existing.setOrderId(100L);
        existing.setCollectionProcessRating(2);
        existing.setQualityRating(2);
        existing.setQuantityRating(2);
        existing.setVarietyRating(2);

        when(vendorRepository.findById(10L)).thenReturn(Optional.of(vendor));
        when(orderClient.getRatingEligibility(100L))
                .thenReturn(eligibility(100L, 1L, 10L, "COMPLETED", true));
        when(ratingRepository.findByOrderId(100L)).thenReturn(Optional.of(existing));
        when(ratingRepository.save(any(Rating.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ratingRepository.findByVendorId(10L)).thenReturn(List.of(existing));

        baseRequest.setQualityRating(5);
        RatingResponse response = ratingService.submitRating(1L, baseRequest);

        assertEquals(4.25, response.getTotalRating(), 0.01);

        ArgumentCaptor<Vendor> vendorCaptor = ArgumentCaptor.forClass(Vendor.class);
        verify(vendorRepository).save(vendorCaptor.capture());
        assertEquals(1, vendorCaptor.getValue().getReviewsCount());
        assertEquals(4.25, vendorCaptor.getValue().getAverageRating(), 0.01);
    }

    @Test
    void submitRating_orderNotCompleted_rejected() {
        when(vendorRepository.findById(10L)).thenReturn(Optional.of(vendor));
        when(orderClient.getRatingEligibility(100L))
                .thenReturn(eligibility(100L, 1L, 10L, "CONFIRMED", false));

        RatingValidationException ex = assertThrows(RatingValidationException.class,
                () -> ratingService.submitRating(1L, baseRequest));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(ratingRepository, never()).save(any());
    }

    @Test
    void submitRating_vendorIdMismatch_rejected() {
        when(vendorRepository.findById(10L)).thenReturn(Optional.of(vendor));
        when(orderClient.getRatingEligibility(100L))
                .thenReturn(eligibility(100L, 1L, 99L, "COMPLETED", true));

        RatingValidationException ex = assertThrows(RatingValidationException.class,
                () -> ratingService.submitRating(1L, baseRequest));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(ratingRepository, never()).save(any());
    }

    @Test
    void submitRating_orderOwnedByDifferentUser_rejected() {
        when(vendorRepository.findById(10L)).thenReturn(Optional.of(vendor));
        when(orderClient.getRatingEligibility(100L))
                .thenReturn(eligibility(100L, 2L, 10L, "COMPLETED", true));

        RatingValidationException ex = assertThrows(RatingValidationException.class,
                () -> ratingService.submitRating(1L, baseRequest));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(ratingRepository, never()).save(any());
    }

    private OrderRatingEligibilityResponse eligibility(Long orderId, Long userId, Long vendorId,
                                                       String status, boolean eligible) {
        OrderRatingEligibilityResponse response = new OrderRatingEligibilityResponse();
        response.setOrderId(orderId);
        response.setUserId(userId);
        response.setVendorId(vendorId);
        response.setStatus(status);
        response.setEligible(eligible);
        return response;
    }
}
