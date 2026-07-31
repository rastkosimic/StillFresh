package com.stillfresh.app.orderservice.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.stillfresh.app.sharedentities.order.events.OrderPlacedEvent;
import com.stillfresh.app.sharedentities.order.events.OrderCancelledEvent;
import com.stillfresh.app.sharedentities.order.events.OrderExpiredEvent;
import com.stillfresh.app.sharedentities.order.events.OrderPickupReminderEvent;
import com.stillfresh.app.sharedentities.order.events.FraudFlagEvent;
import com.stillfresh.app.sharedentities.order.events.OrderNoShowEvent;
import com.stillfresh.app.sharedentities.order.events.VendorOrderNotificationEvent;
import com.stillfresh.app.sharedentities.payment.events.PaymentRequestEvent;
import com.stillfresh.app.sharedentities.payment.events.PaymentCancelRequestEvent;
import com.stillfresh.app.sharedentities.payment.events.PaymentCaptureRequestEvent;
import com.stillfresh.app.sharedentities.order.events.BankTransferOrderEvent;
import com.stillfresh.app.sharedentities.offer.events.OfferDetailsRequestedEvent;
import com.stillfresh.app.sharedentities.offer.events.OfferQuantityUpdatedEvent;
import com.stillfresh.app.sharedentities.order.events.VendorStatsResponseEvent;

@Service
public class OrderEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Topic for requesting offer details
    @Value("${offer.topic.offer-details-request:offer-details-request}")
    private String offerDetailsRequestTopic;

    // Topic for notifying offer quantity updates
    @Value("${offer.topic.offer-quantity-updated:offer-quantity-updated}")
    private String offerQuantityUpdatedTopic;

    // Topic for notifying that an order has been placed
    @Value("${order.topic.order-placed:order-placed}")
    private String orderPlacedTopic;
    
    @Value("${payment.topic.payment-request:payment-request}")
    private String paymentRequestTopic;

    @Value("${payment.topic.bank-transfer-order:bank-transfer-order}")
    private String bankTransferOrderTopic;

    @Value("${kafka.topic.vendor-stats-response:vendor-stats-response}")
    private String vendorStatsResponseTopic;
    
    // Topic for notifying vendors about new orders
    @Value("${order.topic.vendor-order-notification:vendor-order-notification}")
    private String vendorOrderNotificationTopic;
    
    @Value("${payment.topic.payment-cancel-request:payment-cancel-request}")
    private String paymentCancelRequestTopic;

    @Value("${payment.topic.payment-capture-request:payment-capture-request}")
    private String paymentCaptureRequestTopic;
    
    @Value("${order.topic.order-cancelled:order-cancelled}")
    private String orderCancelledTopic;

    @Value("${order.topic.order-expired:order-expired}")
    private String orderExpiredTopic;

    @Value("${order.topic.order-pickup-reminder:order-pickup-reminder}")
    private String orderPickupReminderTopic;

    @Value("${order.topic.fraud-flag:fraud-flag}")
    private String fraudFlagTopic;

    @Value("${order.topic.order-no-show:order-no-show}")
    private String orderNoShowTopic;

    public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes an event to request offer details for a given offer ID.
     *
     * @param event the {@link OfferDetailsRequestedEvent}
     */
    public void publishOfferDetailsRequestedEvent(OfferDetailsRequestedEvent event) {
        try {
            logger.info("Publishing OfferDetailsRequestedEvent to Kafka topic '{}': {}", offerDetailsRequestTopic, event);
            kafkaTemplate.send(offerDetailsRequestTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish OfferDetailsRequestedEvent to Kafka", e);
        }
    }

    /**
     * Publishes an event to update the available quantity of an offer.
     *
     * @param event the {@link OfferQuantityUpdatedEvent}
     */
    public void publishOfferQuantityUpdatedEvent(OfferQuantityUpdatedEvent event) {
        try {
            logger.info("Publishing OfferQuantityUpdatedEvent to Kafka topic '{}': {}", offerQuantityUpdatedTopic, event);
            kafkaTemplate.send(offerQuantityUpdatedTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish OfferQuantityUpdatedEvent to Kafka", e);
        }
    }

    /**
     * Publishes an event to indicate that an order has been successfully placed.
     *
     * @param event the {@link OrderPlacedEvent}
     */
    public void publishOrderPlacedEvent(OrderPlacedEvent event) {
        try {
            logger.info("Publishing OrderPlacedEvent to Kafka topic '{}': {}", orderPlacedTopic, event);
            kafkaTemplate.send(orderPlacedTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish OrderPlacedEvent to Kafka", e);
        }
    }

    public void publishBankTransferOrderEvent(BankTransferOrderEvent event) {
        try {
            logger.info("Publishing BankTransferOrderEvent to Kafka topic '{}': orderId={}", bankTransferOrderTopic, event.getOrderId());
            kafkaTemplate.send(bankTransferOrderTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish BankTransferOrderEvent to Kafka", e);
        }
    }

    public void publishPaymentRequestEvent(PaymentRequestEvent event) {
        try {
            logger.info("Publishing PaymentRequestEvent to Kafka topic '{}': {}", paymentRequestTopic, event);
            kafkaTemplate.send(paymentRequestTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish PaymentRequestEvent to Kafka", e);
        }
    }

    public void publishVendorStatsResponse(VendorStatsResponseEvent event) {
        try {
            logger.info("Publishing VendorStatsResponseEvent to Kafka topic '{}': {}", vendorStatsResponseTopic, event);
            kafkaTemplate.send(vendorStatsResponseTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish VendorStatsResponseEvent to Kafka", e);
        }
    }
    
    /**
     * Publishes an event to notify vendors about new orders.
     *
     * @param event the {@link VendorOrderNotificationEvent}
     */
    public void publishVendorOrderNotificationEvent(VendorOrderNotificationEvent event) {
        try {
            logger.info("Publishing VendorOrderNotificationEvent to Kafka topic '{}': {}", vendorOrderNotificationTopic, event);
            kafkaTemplate.send(vendorOrderNotificationTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish VendorOrderNotificationEvent to Kafka", e);
        }
    }
    
    /**
     * Publishes an event to request cancellation of a PaymentIntent.
     * Used in Too Good To Go style payment flow when an order is cancelled.
     *
     * @param event the {@link PaymentCancelRequestEvent}
     */
    public void publishPaymentCancelRequest(PaymentCancelRequestEvent event) {
        try {
            logger.info("Publishing PaymentCancelRequestEvent to Kafka topic '{}': {}", paymentCancelRequestTopic, event);
            kafkaTemplate.send(paymentCancelRequestTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish PaymentCancelRequestEvent to Kafka", e);
        }
    }

    /**
     * Customer confirmed pickup; request capture of the authorized Stripe PaymentIntent.
     */
    public void publishPaymentCaptureRequest(PaymentCaptureRequestEvent event) {
        try {
            logger.info("Publishing PaymentCaptureRequestEvent to Kafka topic '{}': {}", paymentCaptureRequestTopic, event);
            kafkaTemplate.send(paymentCaptureRequestTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish PaymentCaptureRequestEvent to Kafka", e);
        }
    }
    
    /**
     * Publishes an event to notify that an order has been cancelled.
     *
     * @param event the {@link OrderCancelledEvent}
     */
    public void publishOrderCancelledEvent(OrderCancelledEvent event) {
        try {
            logger.info("Publishing OrderCancelledEvent to Kafka topic '{}': {}", orderCancelledTopic, event);
            kafkaTemplate.send(orderCancelledTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish OrderCancelledEvent to Kafka", e);
        }
    }

    /**
     * Publishes an event when an order is marked EXPIRED (pickup window passed).
     * Notification service sends "Your reservation has expired" to the user.
     */
    public void publishOrderExpiredEvent(OrderExpiredEvent event) {
        try {
            logger.info("Publishing OrderExpiredEvent to Kafka topic '{}': {}", orderExpiredTopic, event);
            kafkaTemplate.send(orderExpiredTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish OrderExpiredEvent to Kafka", e);
        }
    }

    /**
     * Publishes a reminder event (e.g. 1 hour before pickup deadline).
     * Notification service sends "Order must be picked up by [time]" to the user.
     */
    public void publishOrderPickupReminderEvent(OrderPickupReminderEvent event) {
        try {
            logger.info("Publishing OrderPickupReminderEvent to Kafka topic '{}': {}", orderPickupReminderTopic, event);
            kafkaTemplate.send(orderPickupReminderTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish OrderPickupReminderEvent to Kafka", e);
        }
    }

    /**
     * Publishes a potential user-vendor bypass fraud flag (customer cancelled while physically at the
     * pickup location within the active pickup window). Consumed to increment bypass strike counters.
     */
    public void publishFraudFlagEvent(FraudFlagEvent event) {
        try {
            logger.info("Publishing FraudFlagEvent to Kafka topic '{}': {}", fraudFlagTopic, event);
            kafkaTemplate.send(fraudFlagTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish FraudFlagEvent to Kafka", e);
        }
    }

    /**
     * Publishes a no-show event when an order expires unpicked. Consumed to increment the user's
     * no-show strike counter.
     */
    public void publishOrderNoShowEvent(OrderNoShowEvent event) {
        try {
            logger.info("Publishing OrderNoShowEvent to Kafka topic '{}': {}", orderNoShowTopic, event);
            kafkaTemplate.send(orderNoShowTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish OrderNoShowEvent to Kafka", e);
        }
    }
}
