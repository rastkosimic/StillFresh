package com.stillfresh.app.gateway.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.stillfresh.app.sharedentities.shared.events.TokenRequestEvent;

@Service
public class GatewayEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(GatewayEventPublisher.class);

    @Value("${authorization.topic.name:token-validation-request}")
    private String tokenValidationRequestTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public GatewayEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishTokenValidationRequest(TokenRequestEvent event) {
        try {
            logger.debug("Publishing TokenValidationRequest to Kafka topic '{}' with correlationId: {}", 
                        tokenValidationRequestTopic, event.getCorrelationId());
            kafkaTemplate.send(tokenValidationRequestTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish TokenValidationRequest to Kafka", e);
            throw new RuntimeException("Failed to publish token validation request", e);
        }
    }
}

