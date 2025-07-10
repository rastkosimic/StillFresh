package com.stillfresh.app.orderservice.service;

import com.stillfresh.app.orderservice.model.Order;
import com.stillfresh.app.orderservice.publisher.OrderEventPublisher;
import com.stillfresh.app.orderservice.repository.OrderRepository;
import com.stillfresh.app.sharedentities.dto.OfferDto;
import com.stillfresh.app.sharedentities.enums.Currency;
import com.stillfresh.app.sharedentities.offer.events.OfferDetailsRequestedEvent;
import com.stillfresh.app.sharedentities.offer.events.OfferDetailsResponseEvent;
import com.stillfresh.app.sharedentities.offer.events.OfferQuantityUpdatedEvent;
import com.stillfresh.app.sharedentities.order.events.OrderRequestEvent;
import com.stillfresh.app.sharedentities.payment.events.PaymentRequestEvent;
import com.stillfresh.app.sharedentities.order.events.OrderPlacedEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class OrderService {
	private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
	
	@Autowired
    private OrderRepository orderRepository;
	
	@Autowired
	private OrderEventPublisher eventPublisher;
	
	public final ConcurrentHashMap<String, CompletableFuture<OfferDto>> pendingOfferDetailsRequests = new ConcurrentHashMap<>();
	
	public final ConcurrentHashMap<String, CompletableFuture<OrderRequestEvent>> pendingOrderRequests = new ConcurrentHashMap<>();

    public Order createOrder(Order order) {
        return orderRepository.save(order);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<Order> getOrderById(int id) {
        return orderRepository.findById(id);
    }

    public void deleteOrder(int id) {
        orderRepository.deleteById(id);
    }

    public void handleOrderRequest(OrderRequestEvent event) {
        String requestId = UUID.randomUUID().toString();

        // ✅ Store the OfferDetails request
        CompletableFuture<OfferDto> futureOffer = new CompletableFuture<>();
        pendingOfferDetailsRequests.put(requestId, futureOffer);

        // ✅ Store the OrderRequestEvent
        CompletableFuture<OrderRequestEvent> futureOrder = new CompletableFuture<>();
        pendingOrderRequests.put(requestId, futureOrder);  // Store it here

        // Publish the OfferDetailsRequestedEvent to Kafka
        eventPublisher.publishOfferDetailsRequestedEvent(
            new OfferDetailsRequestedEvent(requestId, event.getOfferId())
        );

        // ✅ Process asynchronously when offer details are received
        futureOffer.whenComplete((offerDto, throwable) -> {
            try {
                if (throwable != null) {
                    logger.error("Error fetching offer details for requestId: {}", requestId, throwable);
                    throw new RuntimeException("Error fetching offer details: " + throwable.getMessage());
                }

                // ✅ Validate offer
                if (!offerDto.isActive()) {
                    throw new RuntimeException("The selected offer is no longer active.");
                }
                if (offerDto.getQuantityAvailable() < event.getQuantity()) {
                    throw new RuntimeException("The requested quantity exceeds available stock.");
                }
                if (offerDto.getExpirationDate().isBefore(OffsetDateTime.now())) {
                    throw new RuntimeException("The offer has expired.");
                }

                // ✅ Complete future in pendingOrderRequests with validated order
                futureOrder.complete(event);  // 👈 Ensure it's available for finalizeOrder

                // ✅ Send Payment Request
                PaymentRequestEvent paymentRequest = new PaymentRequestEvent(
                    event.getUserId(), event.getUsername(),
                    convertPriceToCents(offerDto.getPrice()) * event.getQuantity(),
                    event.getOfferId(), requestId, Currency.RSD
                );
                eventPublisher.publishPaymentRequestEvent(paymentRequest);
                
                logger.info("PaymentRequestEvent sent for userId: {}", event.getUserId());

            } catch (Exception e) {
                logger.error("Order processing failed for requestId: {}", requestId, e);
            } finally {
                // Cleanup pending requests map
                pendingOfferDetailsRequests.remove(requestId);
            }
        });

        // ✅ Timeout Mechanism
        futureOffer.orTimeout(10, TimeUnit.SECONDS).exceptionally(throwable -> {
            logger.error("Timeout fetching offer details for requestId: {}", requestId, throwable);
            pendingOfferDetailsRequests.remove(requestId);
            pendingOrderRequests.remove(requestId);
            throw new RuntimeException("Timeout occurred while processing order request.");
        });
    }


    public static Long convertPriceToCents(double price) {
        return BigDecimal.valueOf(price)
                .multiply(BigDecimal.valueOf(100))  // Convert to cents
                .setScale(0, RoundingMode.HALF_UP)  // Round to nearest whole number
                .longValueExact(); // Ensure no data loss
    }
    
    public void handleOfferDetailsResponse(OfferDetailsResponseEvent event) {
        CompletableFuture<OfferDto> future = pendingOfferDetailsRequests.remove(event.getRequestId());
        if (future != null) {
            future.complete(event.getOfferDto());
        } else {
            logger.warn("No pending request found for requestId: {}", event.getRequestId());
        }
    }
    
    public void finalizeOrder(String requestId) {
        logger.info("Finalizing order for requestId: {}", requestId);

        // ✅ Remove pending order request before processing
        CompletableFuture<OrderRequestEvent> futureOrder = pendingOrderRequests.remove(requestId);

        if (futureOrder != null) {
            futureOrder.whenComplete((orderEvent, throwable) -> {
                if (throwable != null) {
                    logger.error("Error finalizing order for requestId: {}", requestId, throwable);
                    return;
                }

                logger.info("Processing order event for userId: {}", orderEvent.getUserId());

                // ✅ Save Order
                Order order = new Order();
                order.setOfferId(orderEvent.getOfferId());
                order.setUserId(orderEvent.getUserId());
                order.setQuantity(orderEvent.getQuantity());
                order.setTotalPrice(convertPriceToCents(orderEvent.getQuantity())); 

                order = orderRepository.save(order);
                logger.info("Order confirmed with ID: {}", order.getId());

                // ✅ Reduce stock quantity
                eventPublisher.publishOfferQuantityUpdatedEvent(
                    new OfferQuantityUpdatedEvent(orderEvent.getOfferId(), -orderEvent.getQuantity())
                );

                // ✅ Publish order placed event
                eventPublisher.publishOrderPlacedEvent(
                    new OrderPlacedEvent(
                        order.getId().toString(),
                        orderEvent.getUserId().toString(),
                        orderEvent.getOfferId().intValue(),
                        orderEvent.getQuantity(),
                        order.getTotalPrice()
                    )
                );

            });
        } else {
            logger.warn("No pending order request found for requestId: {}", requestId);
        }
    }


    public void cancelOrder(String requestId) {
        logger.warn("Cancelling order for requestId: {}", requestId);

        CompletableFuture<OrderRequestEvent> futureOrder = pendingOrderRequests.remove(requestId);

        if (futureOrder != null) {
            futureOrder.whenComplete((orderEvent, throwable) -> {
                if (throwable != null) {
                    logger.error("Error cancelling order for requestId: {}", requestId, throwable);
                    return;
                }

                // ✅ Create and mark order as canceled
                Order order = new Order();
                order.setOfferId(orderEvent.getOfferId());
                order.setUserId(orderEvent.getUserId());
                order.setQuantity(orderEvent.getQuantity());
                order.setTotalPrice(convertPriceToCents(orderEvent.getQuantity()));

                orderRepository.save(order);
                logger.info("Order canceled with ID: {}", order.getId());
            });
        } else {
            logger.warn("No pending order request found for requestId: {}", requestId);
        }
    }

    private void validateAndPlaceOrder(OrderRequestEvent event, OfferDto offer) {
        // Validate the offer
        if (!offer.isActive()) {
            throw new RuntimeException("The selected offer is no longer active.");
        }
        if (offer.getQuantityAvailable() < event.getQuantity()) {
            throw new RuntimeException("The requested quantity exceeds available stock.");
        }
        if (offer.getExpirationDate().isBefore(OffsetDateTime.now())) {
            throw new RuntimeException("The offer has expired.");
        }

        // Calculate total price
        double totalPrice = offer.getPrice() * event.getQuantity();

        // Save the order
        Order order = new Order();
        order.setOfferId(event.getOfferId());
        order.setUserId(event.getUserId());
        order.setQuantity(event.getQuantity());
        order.setTotalPrice(totalPrice);

        orderRepository.save(order);
        logger.info("Order saved successfully with ID: {}", order.getId());

        // Publish OfferQuantityUpdatedEvent
        eventPublisher.publishOfferQuantityUpdatedEvent(
            new OfferQuantityUpdatedEvent(event.getOfferId(), -event.getQuantity())
        );
    }
}
