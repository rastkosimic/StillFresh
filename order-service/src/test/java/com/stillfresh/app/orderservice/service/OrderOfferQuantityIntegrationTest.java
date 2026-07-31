package com.stillfresh.app.orderservice.service;

import com.stillfresh.app.orderservice.model.Order;
import com.stillfresh.app.orderservice.repository.OrderRepository;
import com.stillfresh.app.orderservice.publisher.OrderEventPublisher;
import com.stillfresh.app.orderservice.listener.PaymentSuccessListener;
import com.stillfresh.app.orderservice.listener.PaymentFaliureListener;
import com.stillfresh.app.orderservice.listener.VendorStatsRequestListener;
import com.stillfresh.app.orderservice.listener.OfferDetailsResponseListener;
import com.stillfresh.app.orderservice.listener.OrderRequestListener;
import com.stillfresh.app.sharedentities.dto.OfferDto;
import com.stillfresh.app.sharedentities.enums.Currency;
import com.stillfresh.app.sharedentities.offer.events.OfferQuantityUpdatedEvent;
import com.stillfresh.app.sharedentities.order.events.OrderRequestEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
    "spring.kafka.listener.type=none",
    "eureka.client.enabled=false",
    "spring.cache.type=simple",
    "spring.data.redis.host=localhost"
})
@ActiveProfiles("test")
@Transactional
public class OrderOfferQuantityIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockBean
    private OrderEventPublisher orderEventPublisher;

    @MockBean
    private PaymentSuccessListener paymentSuccessListener;

    @MockBean
    private PaymentFaliureListener paymentFaliureListener;

    @MockBean
    private VendorStatsRequestListener vendorStatsRequestListener;

    @MockBean
    private OfferDetailsResponseListener offerDetailsResponseListener;

    @MockBean
    private OrderRequestListener orderRequestListener;

    private OrderRequestEvent testOrderRequest;
    private OfferDto testOfferDto;

    @BeforeEach
    void setUp() {
        // Setup test order request
        testOrderRequest = new OrderRequestEvent();
        testOrderRequest.setUserId(1L);
        testOrderRequest.setUsername("testuser");
        testOrderRequest.setOfferId(100L);
        testOrderRequest.setQuantity(3);

        // Setup test offer DTO
        testOfferDto = new OfferDto();
        testOfferDto.setId(100L);
        testOfferDto.setVendorId(200L);
        testOfferDto.setName("Test Offer");
        testOfferDto.setPrice(15.0);
        testOfferDto.setQuantityAvailable(10); // Initial quantity: 10
        testOfferDto.setActive(true);
        testOfferDto.setExpirationDate(OffsetDateTime.now().plusDays(1));
        testOfferDto.setCurrency("RSD");

        // Mock Kafka template to do nothing (we're testing the service logic, not Kafka)
        when(kafkaTemplate.send(anyString(), any())).thenReturn(null);
    }

    @Test
    void testOfferQuantityDecreasesWhenOrderIsFinalized() {
        // Given: Order request with quantity 3, offer with quantity 10
        String requestId = "test-request-1";
        
        // Setup: Store the order request in pending requests
        CompletableFuture<OrderRequestEvent> futureOrder = new CompletableFuture<>();
        orderService.pendingOrderRequests.put(requestId, futureOrder);
        futureOrder.complete(testOrderRequest);

        // Store offer details
        orderService.resolvedOfferDetailsByRequestId.put(requestId, testOfferDto);

        // Mock the event publisher to capture the quantity update event
        doAnswer(invocation -> {
            OfferQuantityUpdatedEvent event = invocation.getArgument(0);
            // Verify the event has negative quantity change (decrease)
            assertEquals(testOrderRequest.getOfferId(), event.getOfferId());
            assertEquals(-testOrderRequest.getQuantity(), event.getQuantityChange());
            return null;
        }).when(orderEventPublisher).publishOfferQuantityUpdatedEvent(any(OfferQuantityUpdatedEvent.class));

        // When: Order is finalized
        orderService.finalizeOrder(requestId, "pi_test_123");

        // Then: Verify that OfferQuantityUpdatedEvent was published with negative quantity
        verify(orderEventPublisher, times(1)).publishOfferQuantityUpdatedEvent(argThat(event ->
            event.getOfferId().equals(testOrderRequest.getOfferId()) &&
            event.getQuantityChange() == -testOrderRequest.getQuantity()
        ));
    }

    @Test
    void testOfferQuantityIncreasesWhenOrderIsCancelled() {
        // Given: An existing order that was placed
        Order orderToSave = new Order();
        orderToSave.setOfferId(100L);
        orderToSave.setUserId(1L);
        orderToSave.setQuantity(3);
        orderToSave.setUnitPrice(15.0);
        orderToSave.setTotalPrice(45.0);
        orderToSave.setVendorId(200L);
        orderToSave.setCurrency(Currency.RSD.getIsoCode());
        orderToSave.setStatus("CONFIRMED");
        orderToSave.setPaymentIntentId("pi_test_123");
        final Order savedOrder = orderRepository.save(orderToSave);

        // Mock the event publisher to capture the quantity update event
        doAnswer(invocation -> {
            OfferQuantityUpdatedEvent event = invocation.getArgument(0);
            // Verify the event has positive quantity change (increase/restore)
            assertEquals(savedOrder.getOfferId(), event.getOfferId());
            assertEquals(savedOrder.getQuantity(), event.getQuantityChange());
            return null;
        }).when(orderEventPublisher).publishOfferQuantityUpdatedEvent(any(OfferQuantityUpdatedEvent.class));

        // When: Order is cancelled by customer
        boolean cancelled = orderService.cancelOrderByCustomer(savedOrder.getId(), "Changed my mind");

        // Then: Verify order status is CANCELLED
        assertTrue(cancelled, "Order should be cancelled successfully");
        Order cancelledOrder = orderRepository.findById(savedOrder.getId()).orElseThrow();
        assertEquals("CANCELLED", cancelledOrder.getStatus(), 
            "Order status should be CANCELLED");

        // And: Verify that OfferQuantityUpdatedEvent was published with positive quantity (restore)
        verify(orderEventPublisher, times(1)).publishOfferQuantityUpdatedEvent(argThat(event ->
            event.getOfferId().equals(savedOrder.getOfferId()) &&
            event.getQuantityChange() == savedOrder.getQuantity() // Positive value restores quantity
        ));
    }

    @Test
    void testOfferQuantityIncreasesWhenOrderIsRejectedByVendor() {
        // Given: An existing order that was placed
        Order orderToSave = new Order();
        orderToSave.setOfferId(100L);
        orderToSave.setUserId(1L);
        orderToSave.setQuantity(5);
        orderToSave.setUnitPrice(15.0);
        orderToSave.setTotalPrice(75.0);
        orderToSave.setVendorId(200L);
        orderToSave.setCurrency(Currency.RSD.getIsoCode());
        orderToSave.setStatus("CONFIRMED");
        orderToSave.setPaymentIntentId("pi_test_456");
        final Order savedOrder = orderRepository.save(orderToSave);

        // Mock the event publisher to capture the quantity update event
        doAnswer(invocation -> {
            OfferQuantityUpdatedEvent event = invocation.getArgument(0);
            // Verify the event has positive quantity change (increase/restore)
            assertEquals(savedOrder.getOfferId(), event.getOfferId());
            assertEquals(savedOrder.getQuantity(), event.getQuantityChange());
            return null;
        }).when(orderEventPublisher).publishOfferQuantityUpdatedEvent(any(OfferQuantityUpdatedEvent.class));

        // When: Order is rejected by vendor
        boolean rejected = orderService.rejectOrderByVendor(savedOrder.getId(), "Out of stock");

        // Then: Verify order status is CANCELLED
        assertTrue(rejected, "Order should be rejected successfully");
        Order rejectedOrder = orderRepository.findById(savedOrder.getId()).orElseThrow();
        assertEquals("CANCELLED", rejectedOrder.getStatus(), 
            "Order status should be CANCELLED after rejection");

        // And: Verify that OfferQuantityUpdatedEvent was published with positive quantity (restore)
        verify(orderEventPublisher, times(1)).publishOfferQuantityUpdatedEvent(argThat(event ->
            event.getOfferId().equals(savedOrder.getOfferId()) &&
            event.getQuantityChange() == savedOrder.getQuantity() // Positive value restores quantity
        ));
    }

    @Test
    void testCompleteOrderLifecycleWithQuantityChanges() {
        // Given: Order request with quantity 2, offer with quantity 10
        String requestId = "test-request-2";
        int orderQuantity = 2;
        
        testOrderRequest.setQuantity(orderQuantity);
        testOfferDto.setQuantityAvailable(10);

        // Setup: Store the order request
        CompletableFuture<OrderRequestEvent> futureOrder = new CompletableFuture<>();
        orderService.pendingOrderRequests.put(requestId, futureOrder);
        futureOrder.complete(testOrderRequest);
        orderService.resolvedOfferDetailsByRequestId.put(requestId, testOfferDto);

        // Step 1: Finalize order (should decrease quantity)
        orderService.finalizeOrder(requestId, "pi_test_789");
        
        // Verify decrease event was published
        verify(orderEventPublisher, times(1)).publishOfferQuantityUpdatedEvent(argThat(event ->
            event.getOfferId().equals(testOrderRequest.getOfferId()) &&
            event.getQuantityChange() == -orderQuantity // Negative: decrease
        ));

        // Get the created order
        Order createdOrder = orderRepository.findAll().stream()
            .filter(o -> o.getOfferId().equals(testOrderRequest.getOfferId()))
            .findFirst()
            .orElseThrow();

        // Step 2: Cancel the order (should increase quantity back)
        boolean cancelled = orderService.cancelOrderByCustomer(createdOrder.getId(), "Test cancellation");

        // Verify increase event was published
        verify(orderEventPublisher, times(1)).publishOfferQuantityUpdatedEvent(argThat(event ->
            event.getOfferId().equals(createdOrder.getOfferId()) &&
            event.getQuantityChange() == orderQuantity // Positive: restore
        ));

        // Verify order is cancelled
        assertTrue(cancelled);
        Order cancelledOrder = orderRepository.findById(createdOrder.getId()).orElseThrow();
        assertEquals("CANCELLED", cancelledOrder.getStatus());
    }

    @Test
    void testCannotCancelAlreadyCancelledOrder() {
        // Given: An order that is already cancelled
        Order cancelledOrder = new Order();
        cancelledOrder.setOfferId(100L);
        cancelledOrder.setUserId(1L);
        cancelledOrder.setQuantity(3);
        cancelledOrder.setUnitPrice(15.0);
        cancelledOrder.setTotalPrice(45.0);
        cancelledOrder.setVendorId(200L);
        cancelledOrder.setCurrency(Currency.RSD.getIsoCode());
        cancelledOrder.setStatus("CANCELLED");
        cancelledOrder = orderRepository.save(cancelledOrder);

        // When: Trying to cancel again
        boolean result = orderService.cancelOrderByCustomer(cancelledOrder.getId(), "Try again");

        // Then: Should return false (cannot cancel)
        assertFalse(result, "Should not be able to cancel an already cancelled order");

        // Verify no additional quantity update events were published
        verify(orderEventPublisher, never()).publishOfferQuantityUpdatedEvent(any());
    }

    @Test
    void testCannotCancelCompletedOrder() {
        // Given: An order that is completed
        Order completedOrder = new Order();
        completedOrder.setOfferId(100L);
        completedOrder.setUserId(1L);
        completedOrder.setQuantity(3);
        completedOrder.setUnitPrice(15.0);
        completedOrder.setTotalPrice(45.0);
        completedOrder.setVendorId(200L);
        completedOrder.setCurrency(Currency.RSD.getIsoCode());
        completedOrder.setStatus("COMPLETED");
        completedOrder = orderRepository.save(completedOrder);

        // When: Trying to cancel
        boolean result = orderService.cancelOrderByCustomer(completedOrder.getId(), "Too late");

        // Then: Should return false (cannot cancel)
        assertFalse(result, "Should not be able to cancel a completed order");

        // Verify no quantity update events were published
        verify(orderEventPublisher, never()).publishOfferQuantityUpdatedEvent(any());
    }

    @Test
    void testMultipleOrdersAndCancellations() {
        // Given: Multiple orders for the same offer
        Long offerId = 100L;

        // Create order 1: quantity 5
        Order order1 = createTestOrder(offerId, 5, "CONFIRMED");
        order1 = orderRepository.save(order1);

        // Create order 2: quantity 3
        Order order2 = createTestOrder(offerId, 3, "CONFIRMED");
        order2 = orderRepository.save(order2);

        // Create order 3: quantity 2
        Order order3 = createTestOrder(offerId, 2, "CONFIRMED");
        order3 = orderRepository.save(order3);

        // When: Cancelling order 1 (should restore 5)
        orderService.cancelOrderByCustomer(order1.getId(), "Cancel 1");
        verify(orderEventPublisher, times(1)).publishOfferQuantityUpdatedEvent(argThat(event ->
            event.getOfferId().equals(offerId) && event.getQuantityChange() == 5
        ));

        // When: Cancelling order 2 (should restore 3)
        orderService.cancelOrderByCustomer(order2.getId(), "Cancel 2");
        verify(orderEventPublisher, times(1)).publishOfferQuantityUpdatedEvent(argThat(event ->
            event.getOfferId().equals(offerId) && event.getQuantityChange() == 3
        ));

        // When: Cancelling order 3 (should restore 2)
        orderService.cancelOrderByCustomer(order3.getId(), "Cancel 3");
        verify(orderEventPublisher, times(1)).publishOfferQuantityUpdatedEvent(argThat(event ->
            event.getOfferId().equals(offerId) && event.getQuantityChange() == 2
        ));

        // Verify all orders are cancelled
        assertEquals("CANCELLED", orderRepository.findById(order1.getId()).orElseThrow().getStatus());
        assertEquals("CANCELLED", orderRepository.findById(order2.getId()).orElseThrow().getStatus());
        assertEquals("CANCELLED", orderRepository.findById(order3.getId()).orElseThrow().getStatus());
    }

    // Helper method to create test orders
    private Order createTestOrder(Long offerId, int quantity, String status) {
        Order order = new Order();
        order.setOfferId(offerId);
        order.setUserId(1L);
        order.setQuantity(quantity);
        order.setUnitPrice(15.0);
        order.setTotalPrice(15.0 * quantity);
        order.setVendorId(200L);
        order.setCurrency(Currency.RSD.getIsoCode());
        order.setStatus(status);
        return order;
    }
}

