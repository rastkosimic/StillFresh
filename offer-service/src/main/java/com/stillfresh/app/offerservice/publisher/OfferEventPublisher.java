package com.stillfresh.app.offerservice.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.stillfresh.app.sharedentities.offer.events.AvailableOffersEvent;
import com.stillfresh.app.sharedentities.offer.events.OfferDetailsResponseEvent;
import com.stillfresh.app.sharedentities.shared.events.TokenRequestEvent;

@Service
public class OfferEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OfferEventPublisher.class);
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${authorization.topic.name:token-validation-request}")
    private String tokenValidationRequestTopic;
    
    //--------offer topics-----------
    @Value("${offer.topic.available-offers:available-offers}")
    private String availableOffersTopic;

    @Value("${offer.topic.offer-details-response:offer-details-response}")
	private String offerDetailsResponseTopic;
    
	public void publishTokenValidationRequest(TokenRequestEvent event) {
        try {
        	logger.info("Published TokenValidationEvent to Kafka topic '{}'", tokenValidationRequestTopic);
            kafkaTemplate.send(tokenValidationRequestTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish TokenValidationEvent to Kafka", e);
        }
	}

	public void publishAvailableOffers(AvailableOffersEvent event) {
        try {
        	logger.info("Published AvailableOffersEvent to Kafka topic '{}'", availableOffersTopic);
            kafkaTemplate.send(availableOffersTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish AvailableOffersEvent to Kafka", e);
        }
		
	}

    public void publishOfferDetailsResponseEvent(OfferDetailsResponseEvent event) {
        try {
            logger.info("Publishing OfferDetailsResponseEvent to Kafka topic '{}': {}", offerDetailsResponseTopic, event);
            kafkaTemplate.send(offerDetailsResponseTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish OfferDetailsResponseEvent to Kafka", e);
        }
    }

}
