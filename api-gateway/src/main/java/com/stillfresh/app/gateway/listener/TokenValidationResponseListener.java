package com.stillfresh.app.gateway.listener;

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

    public void removeResponse(String correlationId) {
        responseCache.remove(correlationId);
        latchCache.remove(correlationId);
    }

    @KafkaListener(topics = "${authorization.topic.response:token-validation-response}", groupId = "api-gateway-group")
    public void handleTokenValidationResponse(TokenValidationResponseEvent event) {
        if (event == null) {
            logger.error("Received null TokenValidationResponseEvent");
            return;
        }
        
        logger.debug("Received TokenValidationResponseEvent: correlationId={}, valid={}", 
                    event.getCorrelationId(), event.isValid());
        
        String correlationId = event.getCorrelationId();
        if (correlationId == null || correlationId.trim().isEmpty()) {
            logger.error("Received TokenValidationResponseEvent with null or empty correlationId: {}", event);
            return;
        }
        
        responseCache.put(correlationId, event);
        CountDownLatch latch = latchCache.get(correlationId);
        if (latch != null) {
            latch.countDown();
        } else {
            logger.warn("No latch found for correlationId: {}", correlationId);
        }
    }
}

