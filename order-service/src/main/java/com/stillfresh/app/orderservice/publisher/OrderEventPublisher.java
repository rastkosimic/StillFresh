package com.stillfresh.app.orderservice.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.stillfresh.app.sharedentities.order.events.OrderPlacedEvent;
import com.stillfresh.app.sharedentities.payment.events.PaymentRequestEvent;
import com.stillfresh.app.sharedentities.offer.events.OfferDetailsRequestedEvent;
import com.stillfresh.app.sharedentities.offer.events.OfferQuantityUpdatedEvent;

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
    private String paymentRequestTopc;

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

	public void publishPaymentRequestEvent(PaymentRequestEvent event) {
		try {
			logger.info("Publishing PaymentRequestEvent to Kafka topic '{}': {}", paymentRequestTopc, event);
			kafkaTemplate.send(paymentRequestTopc, event);
		} catch (Exception e) {
			logger.error("Failed to publish PaymentRequestEvent to Kafka", e);
		}
	}
}
