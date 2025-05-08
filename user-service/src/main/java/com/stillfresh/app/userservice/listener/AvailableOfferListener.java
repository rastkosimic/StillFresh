package com.stillfresh.app.userservice.listener;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.sharedentities.dto.OfferDto;
import com.stillfresh.app.sharedentities.offer.events.AvailableOffersEvent;

@Component
public class AvailableOfferListener {
    private static final Logger logger = LoggerFactory.getLogger(AvailableOfferListener.class);

    private final ConcurrentHashMap<String, CompletableFuture<List<OfferDto>>> pendingRequests = new ConcurrentHashMap<>();

    public void registerPendingRequest(String requestId, CompletableFuture<List<OfferDto>> future) {
        pendingRequests.put(requestId, future);
        logger.info("Registered pending request for requestId: {}", requestId);
    }

    public void removePendingRequest(String requestId) {
        pendingRequests.remove(requestId);
        logger.info("Removed pending request for requestId: {}", requestId);
    }

    @KafkaListener(topics = "${offer.topic.available-offers:available-offers}", groupId = "offer-service-group")
    public void handleAvailableOffersEvent(AvailableOffersEvent event) {
        String requestId = event.getRequestId();
        logger.info("Received AvailableOffersEvent for requestId: {}", requestId);

        CompletableFuture<List<OfferDto>> future = pendingRequests.remove(requestId);
        if (future != null) {
            future.complete(event.getAvailableOffers()); // Complete the future with the response
        } else {
            logger.warn("No pending request found for requestId: {}", requestId);
        }
    }

    public List<OfferDto> getAvailableOffers(String requestId, long timeout) throws InterruptedException, TimeoutException, ExecutionException {
        CompletableFuture<List<OfferDto>> future = pendingRequests.get(requestId);
        if (future == null) {
            throw new RuntimeException("No pending request for requestId: " + requestId);
        }
        return future.get(timeout, TimeUnit.MILLISECONDS); // Wait for the response with a timeout
    }
}

