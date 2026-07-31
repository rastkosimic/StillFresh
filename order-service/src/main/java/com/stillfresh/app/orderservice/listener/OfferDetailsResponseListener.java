package com.stillfresh.app.orderservice.listener;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.sharedentities.dto.OfferDto;
import com.stillfresh.app.sharedentities.offer.events.OfferDetailsResponseEvent;
import com.stillfresh.app.orderservice.service.OrderService;

@Component
public class OfferDetailsResponseListener {

    private static final Logger logger = LoggerFactory.getLogger(OfferDetailsResponseListener.class);

    @Autowired
    private OrderService orderService;

    /**
     * Kafka listener for OfferDetailsResponseEvent.
     *
     * @param event the OfferDetailsResponseEvent containing offer details
     */
    @KafkaListener(topics = "${offer.topic.offer-details-response:offer-details-response}", groupId = "order-service-group")
    public void handleOfferDetailsResponseEvent(OfferDetailsResponseEvent event) {
        logger.info("Received OfferDetailsResponseEvent for requestId: {}", event.getRequestId());
        
        // Store offer details for later use in finalizeOrder and complete the future
        orderService.handleOfferDetailsResponse(event);
    }

}
