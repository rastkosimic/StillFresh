package com.stillfresh.app.userservice.listener;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.sharedentities.shared.events.TokenValidationResponseEvent;

@Component
public class TokenValidationResponseListener {
	
    private static final Logger logger = LoggerFactory.getLogger(TokenValidationResponseListener.class);

    private final ConcurrentHashMap<String, TokenValidationResponseEvent> responseCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CountDownLatch> latchCache = new ConcurrentHashMap<>();

    public void registerLatch(String correlationId, CountDownLatch latch) {
        latchCache.put(correlationId, latch);
    }

    public TokenValidationResponseEvent getResponse(String correlationId) {
        return responseCache.get(correlationId);
    }

    @KafkaListener(topics = "${authorization.topic.name:token-validation-response}", groupId = "authorization-group")
    public void handleTokenValidationResponse(TokenValidationResponseEvent event) {
        logger.debug("Received TokenValidationResponseEvent: {}", event);
        responseCache.put(event.getCorrelationId(), event);
        CountDownLatch latch = latchCache.get(event.getCorrelationId());
        if (latch != null) {
            latch.countDown();
        }
    }

}
