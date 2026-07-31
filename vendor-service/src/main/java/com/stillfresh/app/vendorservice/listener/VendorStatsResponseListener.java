package com.stillfresh.app.vendorservice.listener;

import com.stillfresh.app.sharedentities.order.events.VendorStatsResponseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

@Component
public class VendorStatsResponseListener {

    private static final Logger logger = LoggerFactory.getLogger(VendorStatsResponseListener.class);

    private final ConcurrentHashMap<String, VendorStatsResponseEvent> responseCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CountDownLatch> latchCache = new ConcurrentHashMap<>();

    public void registerLatch(String correlationId, CountDownLatch latch) {
        latchCache.put(correlationId, latch);
    }

    public VendorStatsResponseEvent getResponse(String correlationId) {
        return responseCache.get(correlationId);
    }

    @KafkaListener(topics = "${kafka.topic.vendor-stats-response:vendor-stats-response}", groupId = "vendor-service-group")
    public void handleVendorStatsResponse(VendorStatsResponseEvent event) {
        logger.debug("Received VendorStatsResponseEvent: {}", event);
        responseCache.put(event.getCorrelationId(), event);
        CountDownLatch latch = latchCache.get(event.getCorrelationId());
        if (latch != null) {
            latch.countDown();
        }
    }
}

