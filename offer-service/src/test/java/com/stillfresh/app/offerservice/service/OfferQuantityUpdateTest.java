package com.stillfresh.app.offerservice.service;

import com.stillfresh.app.offerservice.model.Offer;
import com.stillfresh.app.offerservice.publisher.OfferEventPublisher;
import com.stillfresh.app.offerservice.repository.OfferRepository;
import com.stillfresh.app.offerservice.service.CurrencyDetectionService;
import com.stillfresh.app.sharedentities.offer.events.OfferQuantityUpdatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OfferQuantityUpdateTest {

    @Mock
    private OfferRepository offerRepository;

    @Mock
    private OfferEventPublisher offerEventPublisher;

    @Mock
    @SuppressWarnings("unused") // Required for @InjectMocks to properly inject dependencies into OfferService
    private CurrencyDetectionService currencyDetectionService;

    @InjectMocks
    private OfferService offerService;

    private Offer testOffer;

    @BeforeEach
    void setUp() {
        // Create a test offer with initial quantity
        testOffer = new Offer();
        testOffer.setId(1L);
        testOffer.setVendorId(1L);
        testOffer.setName("Test Offer");
        testOffer.setLocationName("Test Location");
        testOffer.setChainName("Test Chain");
        testOffer.setDescription("Test Description");
        testOffer.setPrice(10.0);
        testOffer.setOriginalPrice(15.0);
        testOffer.setQuantityAvailable(10); // Initial quantity: 10
        testOffer.setAddress("123 Test St");
        testOffer.setZipCode("12345");
        testOffer.setLatitude(40.7128);
        testOffer.setLongitude(-74.0060);
        testOffer.setBusinessType("restaurant");
        testOffer.setPickupStartTime(LocalTime.of(9, 0));
        testOffer.setPickupEndTime(LocalTime.of(17, 0));
        testOffer.setExpirationDate(OffsetDateTime.now().plusDays(1));
        testOffer.setActive(true);
    }

    @Test
    void testOfferQuantityDecreasesWhenOrderIsPlaced() {
        // Given: Offer with initial quantity of 10
        int initialQuantity = testOffer.getQuantityAvailable();
        assertEquals(10, initialQuantity, "Initial quantity should be 10");

        when(offerRepository.findById(1L)).thenReturn(Optional.of(testOffer));
        when(offerRepository.save(any(Offer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Order is placed for 3 items (quantity change: -3)
        int orderQuantity = 3;
        OfferQuantityUpdatedEvent decreaseEvent = new OfferQuantityUpdatedEvent(
            testOffer.getId(),
            -orderQuantity // Negative value decreases quantity
        );
        offerService.updateOfferQuantity(decreaseEvent);

        // Then: Verify offer quantity was decreased by 3
        verify(offerRepository, times(1)).save(argThat(offer ->
            offer.getQuantityAvailable() == 7 // 10 - 3 = 7
        ));
    }

    @Test
    void testOfferQuantityIncreasesWhenOrderIsCancelled() {
        // Given: Offer with initial quantity of 10, and an order was placed (quantity now 7)
        int initialQuantity = testOffer.getQuantityAvailable();
        int orderQuantity = 3;
        
        // Simulate order placement: decrease quantity to 7
        testOffer.setQuantityAvailable(7);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(testOffer));
        when(offerRepository.save(any(Offer.class))).thenAnswer(invocation -> {
            Offer saved = invocation.getArgument(0);
            testOffer.setQuantityAvailable(saved.getQuantityAvailable());
            return saved;
        });

        // When: Order is cancelled (quantity change: +3)
        OfferQuantityUpdatedEvent increaseEvent = new OfferQuantityUpdatedEvent(
            testOffer.getId(),
            orderQuantity // Positive value increases quantity
        );
        offerService.updateOfferQuantity(increaseEvent);

        // Then: Verify offer quantity was increased back to original
        verify(offerRepository, times(1)).save(argThat(offer ->
            offer.getQuantityAvailable() == initialQuantity // Should be 10 again
        ));
    }

    @Test
    void testOfferQuantityCannotGoNegative() {
        // Given: Offer with quantity of 5
        testOffer.setQuantityAvailable(5);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(testOffer));

        // When: Trying to decrease by more than available (e.g., order 10 items)
        OfferQuantityUpdatedEvent invalidEvent = new OfferQuantityUpdatedEvent(
            testOffer.getId(),
            -10 // Trying to decrease by 10 when only 5 available
        );

        // Then: Should throw exception
        assertThrows(RuntimeException.class, () -> {
            offerService.updateOfferQuantity(invalidEvent);
        }, "Should throw exception when quantity would go negative");

        // Verify no save was called
        verify(offerRepository, never()).save(any());
    }

    @Test
    void testOfferBecomesInactiveWhenQuantityReachesZero() {
        // Given: Offer with quantity of 2
        testOffer.setQuantityAvailable(2);
        testOffer.setActive(true);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(testOffer));
        when(offerRepository.save(any(Offer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Order is placed for all remaining items (quantity change: -2)
        OfferQuantityUpdatedEvent decreaseEvent = new OfferQuantityUpdatedEvent(
            testOffer.getId(),
            -2
        );
        offerService.updateOfferQuantity(decreaseEvent);

        // Then: Offer should be inactive and quantity should be 0
        verify(offerRepository, times(1)).save(argThat(offer ->
            offer.getQuantityAvailable() == 0 &&
            !offer.isActive()
        ));
    }

    @Test
    void testMultipleOrderPlacementsAndCancellations() {
        // Given: Offer with initial quantity of 20
        testOffer.setQuantityAvailable(20);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(testOffer));
        when(offerRepository.save(any(Offer.class))).thenAnswer(invocation -> {
            Offer saved = invocation.getArgument(0);
            testOffer.setQuantityAvailable(saved.getQuantityAvailable());
            testOffer.setActive(saved.isActive());
            return saved;
        });

        // Place order 1: 5 items
        offerService.updateOfferQuantity(new OfferQuantityUpdatedEvent(1L, -5));
        verify(offerRepository, atLeastOnce()).save(argThat(offer -> offer.getQuantityAvailable() == 15));

        // Place order 2: 3 items
        offerService.updateOfferQuantity(new OfferQuantityUpdatedEvent(1L, -3));
        verify(offerRepository, atLeastOnce()).save(argThat(offer -> offer.getQuantityAvailable() == 12));

        // Cancel order 1: restore 5 items
        offerService.updateOfferQuantity(new OfferQuantityUpdatedEvent(1L, 5));
        verify(offerRepository, atLeastOnce()).save(argThat(offer -> offer.getQuantityAvailable() == 17));

        // Cancel order 2: restore 3 items
        offerService.updateOfferQuantity(new OfferQuantityUpdatedEvent(1L, 3));
        verify(offerRepository, atLeastOnce()).save(argThat(offer -> offer.getQuantityAvailable() == 20));
    }

    @Test
    void testOfferQuantityUpdateWithZeroChange() {
        // Given: Offer with quantity of 10
        int initialQuantity = testOffer.getQuantityAvailable();
        when(offerRepository.findById(1L)).thenReturn(Optional.of(testOffer));
        when(offerRepository.save(any(Offer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Quantity change is 0 (no change)
        OfferQuantityUpdatedEvent zeroEvent = new OfferQuantityUpdatedEvent(
            testOffer.getId(),
            0
        );
        offerService.updateOfferQuantity(zeroEvent);

        // Then: Quantity should remain unchanged
        verify(offerRepository, times(1)).save(argThat(offer ->
            offer.getQuantityAvailable() == initialQuantity
        ));
    }
}
