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
import com.stillfresh.app.sharedentities.order.events.VendorOrderNotificationEvent;
import com.stillfresh.app.sharedentities.order.events.OrderCancelledEvent;
import com.stillfresh.app.sharedentities.order.events.FraudFlagEvent;
import com.stillfresh.app.sharedentities.order.events.OrderNoShowEvent;
import com.stillfresh.app.sharedentities.payment.events.PaymentRequestEvent;
import com.stillfresh.app.sharedentities.payment.events.PaymentCancelRequestEvent;
import com.stillfresh.app.sharedentities.payment.events.PaymentCaptureRequestEvent;
import com.stillfresh.app.sharedentities.order.events.BankTransferOrderEvent;
import com.stillfresh.app.sharedentities.order.events.OrderExpiredEvent;
import com.stillfresh.app.sharedentities.order.events.OrderPickupReminderEvent;
import com.stillfresh.app.sharedentities.order.events.OrderPlacedEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class OrderService {
	private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private static final int MAX_PAGE_SIZE = 100;
	
	@Autowired
    private OrderRepository orderRepository;
	
	@Autowired
    private OrderEventPublisher eventPublisher;

	@Autowired
    private GeoFenceService geoFenceService;
	
	public final ConcurrentHashMap<String, CompletableFuture<OfferDto>> pendingOfferDetailsRequests = new ConcurrentHashMap<>();
	
	public final ConcurrentHashMap<String, CompletableFuture<OrderRequestEvent>> pendingOrderRequests = new ConcurrentHashMap<>();

    // Resolved offer details by requestId for use during finalization
    public final ConcurrentHashMap<String, OfferDto> resolvedOfferDetailsByRequestId = new ConcurrentHashMap<>();

    public Order createOrder(Order order) {
        return orderRepository.save(order);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Page<Order> getAllOrders(int page, int size) {
        Pageable pageable = buildPageable(page, size);
        return orderRepository.findAll(pageable);
    }

    /**
     * Returns all orders that have the given status.
     * Used by the mobile app to fetch only specific categories of orders
     * (e.g. CANCELLED, COMPLETED).
     */
    public List<Order> getOrdersByStatus(String status) {
        return orderRepository.findByStatus(status);
    }

    public Page<Order> getOrdersByStatus(String status, int page, int size) {
        Pageable pageable = buildPageable(page, size);
        return orderRepository.findByStatus(status, pageable);
    }

    /**
     * Returns all orders for the given user (customer), newest first.
     */
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Page<Order> getOrdersByUserId(Long userId, int page, int size) {
        Pageable pageable = buildPageable(page, size);
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Returns orders for the given user with the given status, newest first.
     */
    public List<Order> getOrdersByUserIdAndStatus(Long userId, String status) {
        return orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
    }

    public Page<Order> getOrdersByUserIdAndStatus(Long userId, String status, int page, int size) {
        Pageable pageable = buildPageable(page, size);
        return orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable);
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    /**
     * Returns true if the order exists and belongs to the given user.
     */
    public boolean isOrderOwnedByUser(Long orderId, Long userId) {
        return orderRepository.findById(orderId)
                .map(order -> order.getUserId().equals(userId))
                .orElse(false);
    }

    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }

    private Pageable buildPageable(int page, int size) {
        int pageIndex = Math.max(page, 0);
        int pageSize = size <= 0 ? 20 : Math.min(size, MAX_PAGE_SIZE);
        return PageRequest.of(pageIndex, pageSize);
    }

    public Order updateOrderStatus(Long orderId, String status) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new RuntimeException("Order not found with ID: " + orderId);
        }
        
        Order order = orderOpt.get();
        order.setStatus(status);
        return orderRepository.save(order);
    }

    public void handleOrderRequest(OrderRequestEvent event) {
        String requestId = (event.getRequestId() != null && !event.getRequestId().isBlank())
                ? event.getRequestId()
                : UUID.randomUUID().toString();

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
                // expirationDate is optional; if null -> treat as "no expiration"
                if (offerDto.getExpirationDate() != null && offerDto.getExpirationDate().isBefore(OffsetDateTime.now())) {
                    throw new RuntimeException("The offer has expired.");
                }

                // ✅ Complete future in pendingOrderRequests with validated order
                futureOrder.complete(event);  // 👈 Ensure it's available for finalizeOrder

                // ✅ Send Payment Request with vendor information
                Long vendorId = offerDto.getVendorId();
                
                // Determine currency from offer (or default to RSD for backward compatibility)
                Currency paymentCurrency = Currency.RSD; // Default
                if (offerDto.getCurrency() != null && !offerDto.getCurrency().isEmpty()) {
                    // Find Currency enum by ISO code
                    paymentCurrency = findCurrencyByIsoCode(offerDto.getCurrency());
                    if (paymentCurrency == null) {
                        logger.warn("Invalid currency code '{}' in offer {}. Defaulting to RSD.", 
                                   offerDto.getCurrency(), offerDto.getId());
                        paymentCurrency = Currency.RSD;
                    }
                } else {
                    logger.warn("No currency found in offer {}. Defaulting to RSD.", offerDto.getId());
                }
                
                long amountCents = convertPriceToCents(offerDto.getPrice()) * event.getQuantity();
                boolean isBankTransfer = "BANK_TRANSFER".equalsIgnoreCase(event.getPaymentMethod());

                if (isBankTransfer) {
                    // ── Bank transfer flow: save order immediately, no Stripe ──────────
                    Order order = new Order();
                    order.setOfferId(event.getOfferId());
                    order.setUserId(event.getUserId());
                    order.setQuantity(event.getQuantity());
                    order.setUnitPrice(offerDto.getPrice());
                    order.setTotalPrice(offerDto.getPrice() * event.getQuantity());
                    order.setVendorId(vendorId);
                    order.setCurrency(paymentCurrency.getIsoCode());
                    order.setStatus("CONFIRMED");
                    order.setPaymentMethod("BANK_TRANSFER");
                    copyOfferSnapshotToOrder(offerDto, order);

                    if (offerDto.getPickupEndTime() != null || offerDto.getExpirationDate() != null) {
                        OffsetDateTime now = OffsetDateTime.now();
                        LocalTime endTime = offerDto.getPickupEndTime() != null ? offerDto.getPickupEndTime() : LocalTime.of(23, 59);
                        OffsetDateTime pickupBy = now.toLocalDate().atTime(endTime).atOffset(now.getOffset());
                        if (offerDto.getExpirationDate() != null && offerDto.getExpirationDate().isBefore(pickupBy)) {
                            pickupBy = offerDto.getExpirationDate();
                        }
                        order.setPickupBy(pickupBy);
                    }

                    order = orderRepository.save(order);
                    final Long savedOrderId = order.getId();
                    logger.info("Bank transfer order saved: orderId={}, userId={}", savedOrderId, event.getUserId());

                    // Clean up pending maps since finalizeOrder won't be called
                    pendingOrderRequests.remove(requestId);
                    resolvedOfferDetailsByRequestId.remove(requestId);

                    // Publish bank transfer event for payment-service to create instructions
                    eventPublisher.publishBankTransferOrderEvent(new BankTransferOrderEvent(
                        savedOrderId, event.getUserId(), vendorId, event.getOfferId(),
                        amountCents, paymentCurrency.getIsoCode()
                    ));

                    // Reduce stock
                    eventPublisher.publishOfferQuantityUpdatedEvent(
                        new OfferQuantityUpdatedEvent(event.getOfferId(), -event.getQuantity())
                    );

                    // Notify vendor
                    eventPublisher.publishVendorOrderNotificationEvent(
                        new VendorOrderNotificationEvent(vendorId, savedOrderId.toString(),
                            event.getUserId().toString(), event.getOfferId(), event.getQuantity(),
                            order.getTotalPrice(), offerDto.getName(),
                            offerDto.getLocationName(), offerDto.getChainName(), offerDto.getWebsite(),
                            offerDto.getVendorImageUrl(),
                            offerDto.getAddress(), offerDto.getZipCode(), offerDto.getImageUrl())
                    );

                    // Notify user (order placed event)
                    OrderPlacedEvent bankTransferPlacedEvent = new OrderPlacedEvent(
                        savedOrderId.toString(), event.getUserId().toString(),
                        event.getOfferId(), event.getQuantity(), order.getTotalPrice(),
                        offerDto.getLocationName(), offerDto.getChainName(), offerDto.getWebsite(),
                        offerDto.getVendorImageUrl(),
                        offerDto.getAddress(), offerDto.getZipCode(),
                        offerDto.getName(), offerDto.getImageUrl()
                    );
                    bankTransferPlacedEvent.setCustomerEmail(event.getCustomerEmail());
                    eventPublisher.publishOrderPlacedEvent(bankTransferPlacedEvent);

                } else {
                    // ── Stripe flow: publish PaymentRequestEvent, wait for PaymentSuccessEvent ──
                    PaymentRequestEvent paymentRequest = new PaymentRequestEvent(
                        event.getUserId(), event.getUsername(),
                        amountCents,
                        event.getOfferId(), requestId, paymentCurrency,
                        vendorId, null  // stripeAccountId will be fetched by payment service if needed
                    );
                    eventPublisher.publishPaymentRequestEvent(paymentRequest);
                    logger.info("PaymentRequestEvent sent for userId: {}", event.getUserId());
                }

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
        // Keep a copy to enrich finalized orders
        if (event.getOfferDto() != null) {
            resolvedOfferDetailsByRequestId.put(event.getRequestId(), event.getOfferDto());
        }

        CompletableFuture<OfferDto> future = pendingOfferDetailsRequests.remove(event.getRequestId());
        if (future != null) {
            future.complete(event.getOfferDto());
        } else {
            logger.warn("No pending request found for requestId: {}", event.getRequestId());
        }
    }
    
    public void finalizeOrder(String requestId) {
        finalizeOrder(requestId, null);
    }

    public void finalizeOrder(String requestId, String paymentIntentId) {
        logger.info("Finalizing order for requestId: {}, paymentIntentId: {}", requestId, paymentIntentId);

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
                OfferDto offer = resolvedOfferDetailsByRequestId.remove(requestId);
                double unitPrice = (offer != null) ? offer.getPrice() : 0.0;
                Long vendorId = (offer != null) ? offer.getVendorId() : -1L;

                logger.info("Order details - offerId: {}, vendorId: {}, unitPrice: {}, quantity: {}", 
                           orderEvent.getOfferId(), vendorId, unitPrice, orderEvent.getQuantity());

                order.setUnitPrice(unitPrice);
                order.setTotalPrice(unitPrice * orderEvent.getQuantity());
                order.setVendorId(vendorId);
                copyOfferSnapshotToOrder(offer, order);

                // Use currency from offer (or default to RSD for backward compatibility)
                String orderCurrency = Currency.RSD.getIsoCode(); // Default
                if (offer != null && offer.getCurrency() != null && !offer.getCurrency().isEmpty()) {
                    orderCurrency = offer.getCurrency();
                } else {
                    logger.warn("No currency found in offer for order. Defaulting to RSD.");
                }
                order.setCurrency(orderCurrency);
                
                order.setPaymentIntentId(paymentIntentId);  // Store PaymentIntent ID for later capture/cancel
                order.setStatus("CONFIRMED");  // Set status to CONFIRMED after payment

                // Set pickup deadline for expiry and reminder logic (same day + offer pickup end time, or offer expiration)
                if (offer != null) {
                    OffsetDateTime now = OffsetDateTime.now();
                    LocalTime endTime = offer.getPickupEndTime() != null ? offer.getPickupEndTime() : LocalTime.of(23, 59);
                    java.time.OffsetDateTime pickupBy = now.toLocalDate().atTime(endTime).atOffset(now.getOffset());
                    if (offer.getExpirationDate() != null && offer.getExpirationDate().isBefore(pickupBy)) {
                        pickupBy = offer.getExpirationDate();
                    }
                    order.setPickupBy(pickupBy);
                }

                order = orderRepository.save(order);
                logger.info("Order confirmed with ID: {}, PaymentIntent ID: {}", order.getId(), paymentIntentId);

                // ✅ Reduce stock quantity
                eventPublisher.publishOfferQuantityUpdatedEvent(
                    new OfferQuantityUpdatedEvent(orderEvent.getOfferId(), -orderEvent.getQuantity())
                );

                // ✅ Publish order placed event (with offer display fields for notifications)
                OrderPlacedEvent orderPlacedEvent = new OrderPlacedEvent(
                        order.getId().toString(),
                        orderEvent.getUserId().toString(),
                        orderEvent.getOfferId(),
                        orderEvent.getQuantity(),
                        order.getTotalPrice(),
                        offer != null ? offer.getLocationName() : null,
                        offer != null ? offer.getChainName() : null,
                        offer != null ? offer.getWebsite() : null,
                        offer != null ? offer.getVendorImageUrl() : null,
                        offer != null ? offer.getAddress() : null,
                        offer != null ? offer.getZipCode() : null,
                        offer != null ? offer.getName() : null,
                        offer != null ? offer.getImageUrl() : null
                );
                orderPlacedEvent.setCustomerEmail(orderEvent.getCustomerEmail());
                eventPublisher.publishOrderPlacedEvent(orderPlacedEvent);
                
                // ✅ Publish vendor notification event (with offer display fields for notifications)
                eventPublisher.publishVendorOrderNotificationEvent(
                    new VendorOrderNotificationEvent(
                        vendorId,
                        order.getId().toString(),
                        orderEvent.getUserId().toString(),
                        orderEvent.getOfferId(),
                        orderEvent.getQuantity(),
                        order.getTotalPrice(),
                        offer != null ? offer.getName() : null,
                        offer != null ? offer.getLocationName() : null,
                        offer != null ? offer.getChainName() : null,
                        offer != null ? offer.getWebsite() : null,
                        offer != null ? offer.getVendorImageUrl() : null,
                        offer != null ? offer.getAddress() : null,
                        offer != null ? offer.getZipCode() : null,
                        offer != null ? offer.getImageUrl() : null
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

    /**
     * Cancels an existing order by orderId and cancels the associated PaymentIntent if it exists.
     * This implements the Too Good To Go style payment flow where payment authorization
     * is released when an order is cancelled.
     * 
     * @param orderId The ID of the order to cancel
     * @param cancelledBy "CUSTOMER" or "VENDOR" - who cancelled the order
     * @param reason Optional reason for cancellation
     * @return true if order was cancelled successfully, false otherwise
     */
    public boolean cancelOrderById(Long orderId, String cancelledBy, String reason) {
        return cancelOrderById(orderId, cancelledBy, reason, null, null);
    }

    /**
     * Cancels an order, optionally running the anti-bypass geo-fence check for customer cancellations.
     * When the customer's device coordinates are supplied and they are within
     * {@link GeoFenceService#BYPASS_THRESHOLD_METERS} of the pickup location while still inside the
     * active pickup window, the cancellation is flagged as a potential user-vendor bypass and a
     * {@link FraudFlagEvent} is published. The cancellation itself always proceeds.
     */
    public boolean cancelOrderById(Long orderId, String cancelledBy, String reason, Double userLat, Double userLon) {
        logger.warn("Cancelling order by ID: {}, cancelled by: {}, reason: {}", orderId, cancelledBy, reason);
        
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            logger.warn("Order not found with ID: {}", orderId);
            return false;
        }
        
        Order order = orderOpt.get();
        
        // Check if order is already in a terminal state
        if ("CANCELLED".equals(order.getStatus())) {
            logger.warn("Order {} is already cancelled", orderId);
            return false;
        }
        if ("EXPIRED".equals(order.getStatus())) {
            logger.warn("Order {} is already expired", orderId);
            return false;
        }
        if ("COMPLETED".equals(order.getStatus())) {
            logger.warn("Cannot cancel order {} - it is already completed", orderId);
            return false;
        }

        // Anti-bypass fraud check (customer cancellations only). Does not block the cancellation.
        evaluateBypassFraud(order, cancelledBy, userLat, userLon);
        
        // Update order status to CANCELLED
        order.setStatus("CANCELLED");
        order = orderRepository.save(order);
        logger.info("Order {} status updated to CANCELLED", orderId);
        
        // Restore offer quantity (increase by cancelled quantity)
        eventPublisher.publishOfferQuantityUpdatedEvent(
            new OfferQuantityUpdatedEvent(order.getOfferId(), order.getQuantity())
        );
        logger.info("Published OfferQuantityUpdatedEvent to restore {} units for offer {}", 
                   order.getQuantity(), order.getOfferId());
        
        // If order has a PaymentIntent ID, cancel it to release the hold
        if (order.getPaymentIntentId() != null && !order.getPaymentIntentId().isEmpty()) {
            logger.info("Publishing PaymentCancelRequestEvent for order {} with PaymentIntent {}", 
                       orderId, order.getPaymentIntentId());
            eventPublisher.publishPaymentCancelRequest(
                new PaymentCancelRequestEvent(
                    order.getPaymentIntentId(),
                    orderId,
                    order.getUserId(),
                    "Order cancelled by " + cancelledBy + (reason != null ? ": " + reason : "")
                )
            );
        } else {
            logger.info("Order {} has no PaymentIntent ID, skipping payment cancellation", orderId);
        }
        
        // Publish OrderCancelledEvent for notifications (include snapshot fields from order row)
        eventPublisher.publishOrderCancelledEvent(
            new OrderCancelledEvent(
                order.getId().toString(),
                order.getUserId().toString(),
                order.getVendorId(),
                order.getOfferId(),
                order.getQuantity(),
                order.getTotalPrice(),
                cancelledBy,
                reason,
                order.getLocationName(),
                order.getChainName(),
                order.getWebsite(),
                order.getVendorImageUrl(),
                order.getAddress(),
                order.getZipCode(),
                order.getOfferName(),
                order.getOfferImageUrl()
            )
        );
        logger.info("Published OrderCancelledEvent for order {}", orderId);
        
        return true;
    }
    
    /**
     * Convenience method for customer cancellation
     */
    public boolean cancelOrderByCustomer(Long orderId, String reason) {
        return cancelOrderById(orderId, "CUSTOMER", reason, null, null);
    }

    /**
     * Customer cancellation with optional device coordinates for the anti-bypass geo-fence check.
     */
    public boolean cancelOrderByCustomer(Long orderId, String reason, Double userLat, Double userLon) {
        return cancelOrderById(orderId, "CUSTOMER", reason, userLat, userLon);
    }

    /**
     * Anti-bypass detection: if a customer cancels while physically at the pickup location and still
     * within the active pickup window, flag a potential user-vendor bypass. Best-effort and never
     * throws into the cancellation flow.
     */
    private void evaluateBypassFraud(Order order, String cancelledBy, Double userLat, Double userLon) {
        try {
            if (!"CUSTOMER".equals(cancelledBy) || userLat == null || userLon == null) {
                return;
            }
            if (order.getLatitude() == null || order.getLongitude() == null) {
                return;
            }
            // Only meaningful while the order is still within its active pickup window.
            OffsetDateTime pickupBy = order.getPickupBy();
            if (pickupBy != null && OffsetDateTime.now().isAfter(pickupBy)) {
                return;
            }
            double distance = geoFenceService.distanceMeters(
                    userLat, userLon, order.getLatitude(), order.getLongitude());
            if (distance < GeoFenceService.BYPASS_THRESHOLD_METERS) {
                String reason = String.format(
                        "Cancellation at pickup location (%.1fm) within active pickup window - potential bypass",
                        distance);
                logger.warn("potential_bypass_fraud=true for order {} (user {}, vendor {}): {}",
                        order.getId(), order.getUserId(), order.getVendorId(), reason);
                eventPublisher.publishFraudFlagEvent(new FraudFlagEvent(
                        order.getUserId(), order.getVendorId(), order.getId().toString(), reason));
            }
        } catch (Exception e) {
            logger.error("Bypass fraud evaluation failed for order {} (continuing with cancellation)",
                    order.getId(), e);
        }
    }

    /**
     * Customer confirms pickup (e.g. with vendor staff). Validates ownership and order state, then
     * requests payment capture via Kafka. Provider-neutral: the captured reference is the active
     * provider's authorization id (Stripe PaymentIntent {@code pi_...} or AllSecure preauth UUID);
     * {@code payment-service} routes it through the active {@code PaymentProvider}.
     *
     * @return {@code null} on success; otherwise a stable error code: {@code NOT_FOUND}, {@code FORBIDDEN},
     *         {@code INVALID_STATUS}, {@code NO_PAYMENT_REFERENCE}
     */
    public String requestCustomerPickupCapture(Long orderId, Long userId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return "NOT_FOUND";
        }
        Order order = orderOpt.get();
        if (!order.getUserId().equals(userId)) {
            return "FORBIDDEN";
        }
        String status = order.getStatus();
        if (!("CONFIRMED".equals(status) || "PROCESSING".equals(status) || "READY".equals(status))) {
            logger.warn("Customer pickup confirm rejected for order {}: status={}", orderId, status);
            return "INVALID_STATUS";
        }
        // Bank-transfer orders are settled out-of-band and have no authorization hold to capture.
        if ("BANK_TRANSFER".equalsIgnoreCase(order.getPaymentMethod())) {
            logger.warn("Customer pickup confirm rejected for order {}: bank-transfer order has no hold to capture", orderId);
            return "NO_PAYMENT_REFERENCE";
        }
        String reference = order.getPaymentIntentId();
        if (reference == null || reference.isBlank()) {
            logger.warn("Customer pickup confirm rejected for order {}: no payment authorization reference", orderId);
            return "NO_PAYMENT_REFERENCE";
        }
        eventPublisher.publishPaymentCaptureRequest(
                new PaymentCaptureRequestEvent(reference, orderId, userId));
        logger.info("Published PaymentCaptureRequestEvent for order {} (customer {}), reference {}", orderId, userId, reference);
        return null;
    }
    
    /**
     * Convenience method for vendor rejection
     */
    public boolean rejectOrderByVendor(Long orderId, String reason) {
        return cancelOrderById(orderId, "VENDOR", reason);
    }

    /**
     * Finds orders whose pickup deadline has passed and marks them as EXPIRED.
     * Also expires legacy orders with null pickupBy that are older than 24 hours.
     * Restores offer quantity, cancels PaymentIntent (no charge), and publishes OrderExpiredEvent for notifications.
     */
    public void processExpiredOrders() {
        OffsetDateTime now = OffsetDateTime.now();
        List<Order> toExpire = new java.util.ArrayList<>(orderRepository.findOrdersToExpire(now));
        // Legacy orders: null pickupBy (created before we set it) — expire if older than 24h
        toExpire.addAll(orderRepository.findOrdersToExpireWithNullPickupBy(now.minusHours(24)));
        for (Order order : toExpire) {
            try {
                order.setStatus("EXPIRED");
                orderRepository.save(order);

                eventPublisher.publishOfferQuantityUpdatedEvent(
                    new OfferQuantityUpdatedEvent(order.getOfferId(), order.getQuantity())
                );

                if (order.getPaymentIntentId() != null && !order.getPaymentIntentId().isEmpty()) {
                    eventPublisher.publishPaymentCancelRequest(
                        new PaymentCancelRequestEvent(
                            order.getPaymentIntentId(),
                            order.getId(),
                            order.getUserId(),
                            "Order expired (pickup window passed)"
                        )
                    );
                }

                eventPublisher.publishOrderExpiredEvent(
                    new OrderExpiredEvent(
                        order.getId().toString(),
                        order.getUserId().toString(),
                        order.getVendorId(),
                        order.getOfferId(),
                        order.getQuantity(),
                        order.getTotalPrice(),
                        order.getOfferName(),
                        order.getPickupBy(),
                        order.getLocationName(),
                        order.getChainName(),
                        order.getWebsite(),
                        order.getVendorImageUrl(),
                        order.getAddress(),
                        order.getZipCode(),
                        order.getOfferName(),
                        order.getOfferImageUrl()
                    )
                );
                // No-show: the customer failed to pick up within the window. Increment their strike count.
                eventPublisher.publishOrderNoShowEvent(new OrderNoShowEvent(
                        order.getUserId(), order.getVendorId(), order.getId().toString()));

                logger.info("Order {} marked EXPIRED; quantity restored, payment cancelled, no-show recorded", order.getId());
            } catch (Exception e) {
                logger.error("Error expiring order {}", order.getId(), e);
            }
        }
    }

    /**
     * Finds orders that are within 1 hour of pickup deadline and sends a reminder (once per order).
     */
    public void processPickupReminders() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime reminderCutoff = now.plusHours(1); // Remind when pickup is within 1 hour
        List<Order> toRemind = orderRepository.findOrdersForPickupReminder(now, reminderCutoff);
        for (Order order : toRemind) {
            try {
                order.setPickupReminderSent(true);
                orderRepository.save(order);

                eventPublisher.publishOrderPickupReminderEvent(
                    new OrderPickupReminderEvent(
                        order.getId().toString(),
                        order.getUserId().toString(),
                        order.getOfferId(),
                        order.getPickupBy(),
                        order.getOfferName(),
                        order.getLocationName(),
                        order.getChainName(),
                        order.getWebsite(),
                        order.getVendorImageUrl(),
                        order.getAddress(),
                        order.getZipCode(),
                        order.getOfferName(),
                        order.getOfferImageUrl()
                    )
                );
                logger.info("Pickup reminder sent for order {}", order.getId());
            } catch (Exception e) {
                logger.error("Error sending pickup reminder for order {}", order.getId(), e);
            }
        }
    }

    /**
     * Copies the vendor/offer display snapshot from an OfferDto into an Order so that
     * the order row is self-sufficient and order history/detail views do not need
     * cross-service calls even if the underlying offer/vendor later changes or is deleted.
     */
    private void copyOfferSnapshotToOrder(OfferDto offer, Order order) {
        if (offer == null || order == null) {
            return;
        }
        order.setLocationName(offer.getLocationName());
        order.setChainName(offer.getChainName());
        order.setWebsite(offer.getWebsite());
        order.setVendorImageUrl(offer.getVendorImageUrl());
        order.setOfferName(offer.getName());
        order.setOfferImageUrl(offer.getImageUrl());
        order.setAddress(offer.getAddress());
        order.setZipCode(offer.getZipCode());
        order.setLatitude(offer.getLatitude());
        order.setLongitude(offer.getLongitude());
    }

    /**
     * Helper method to find Currency enum by ISO code
     */
    private Currency findCurrencyByIsoCode(String isoCode) {
        if (isoCode == null || isoCode.isEmpty()) {
            return null;
        }
        for (Currency currency : Currency.values()) {
            if (currency.getIsoCode().equalsIgnoreCase(isoCode)) {
                return currency;
            }
        }
        return null;
    }

}
