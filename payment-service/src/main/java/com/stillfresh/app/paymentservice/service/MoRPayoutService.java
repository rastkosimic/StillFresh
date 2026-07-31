package com.stillfresh.app.paymentservice.service;

import com.stillfresh.app.paymentservice.publisher.PaymentEventPublisher;
import com.stillfresh.app.sharedentities.payment.events.MoRPayoutDataRequestEvent;
import com.stillfresh.app.sharedentities.payment.events.MoRPayoutDataResponseEvent;
import com.stillfresh.app.sharedentities.payment.events.MoRPayoutStatusUpdateRequestEvent;
import com.stillfresh.app.sharedentities.payment.events.MoRPayoutStatusUpdateResponseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing MoR payouts via Kafka communication with vendor-service
 */
@Service
public class MoRPayoutService {

    private static final Logger logger = LoggerFactory.getLogger(MoRPayoutService.class);
    private static final long TIMEOUT_SECONDS = 10;

    @Autowired
    private PaymentEventPublisher eventPublisher;

    // Store pending requests waiting for responses
    private final Map<String, CompletableFuture<MoRPayoutDataResponseEvent>> pendingDataRequests = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<MoRPayoutStatusUpdateResponseEvent>> pendingStatusUpdates = new ConcurrentHashMap<>();

    /**
     * Request pending payouts from vendor-service via Kafka
     */
    public List<Map<String, Object>> getPendingPayouts() {
        String requestId = UUID.randomUUID().toString();
        MoRPayoutDataRequestEvent event = new MoRPayoutDataRequestEvent(requestId, "PENDING_PAYOUTS");
        
        CompletableFuture<MoRPayoutDataResponseEvent> future = new CompletableFuture<>();
        pendingDataRequests.put(requestId, future);
        
        eventPublisher.publishMoRPayoutDataRequest(event);
        
        try {
            MoRPayoutDataResponseEvent response = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            pendingDataRequests.remove(requestId);
            
            if (response.isSuccess() && response.getData() != null) {
                return response.getData();
            } else {
                throw new RuntimeException("Failed to get pending payouts: " + 
                    (response.getErrorMessage() != null ? response.getErrorMessage() : "Unknown error"));
            }
        } catch (Exception e) {
            pendingDataRequests.remove(requestId);
            logger.error("Error getting pending payouts: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get pending payouts: " + e.getMessage(), e);
        }
    }

    /**
     * Request vendor balances from vendor-service via Kafka
     */
    public List<Map<String, Object>> getVendorBalances() {
        String requestId = UUID.randomUUID().toString();
        MoRPayoutDataRequestEvent event = new MoRPayoutDataRequestEvent(requestId, "VENDOR_BALANCES");
        
        CompletableFuture<MoRPayoutDataResponseEvent> future = new CompletableFuture<>();
        pendingDataRequests.put(requestId, future);
        
        eventPublisher.publishMoRPayoutDataRequest(event);
        
        try {
            MoRPayoutDataResponseEvent response = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            pendingDataRequests.remove(requestId);
            
            if (response.isSuccess() && response.getData() != null) {
                return response.getData();
            } else {
                throw new RuntimeException("Failed to get vendor balances: " + 
                    (response.getErrorMessage() != null ? response.getErrorMessage() : "Unknown error"));
            }
        } catch (Exception e) {
            pendingDataRequests.remove(requestId);
            logger.error("Error getting vendor balances: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get vendor balances: " + e.getMessage(), e);
        }
    }

    /**
     * Request order payments from vendor-service via Kafka
     */
    public List<Map<String, Object>> getOrderPayments(OffsetDateTime fromDate, OffsetDateTime toDate) {
        String requestId = UUID.randomUUID().toString();
        MoRPayoutDataRequestEvent event = new MoRPayoutDataRequestEvent(requestId, "ORDER_PAYMENTS");
        event.setFromDate(fromDate);
        event.setToDate(toDate);
        
        CompletableFuture<MoRPayoutDataResponseEvent> future = new CompletableFuture<>();
        pendingDataRequests.put(requestId, future);
        
        eventPublisher.publishMoRPayoutDataRequest(event);
        
        try {
            MoRPayoutDataResponseEvent response = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            pendingDataRequests.remove(requestId);
            
            if (response.isSuccess() && response.getData() != null) {
                return response.getData();
            } else {
                throw new RuntimeException("Failed to get order payments: " + 
                    (response.getErrorMessage() != null ? response.getErrorMessage() : "Unknown error"));
            }
        } catch (Exception e) {
            pendingDataRequests.remove(requestId);
            logger.error("Error getting order payments: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get order payments: " + e.getMessage(), e);
        }
    }

    /**
     * Request payout summary from vendor-service via Kafka
     */
    public Map<String, Object> getPayoutSummary() {
        String requestId = UUID.randomUUID().toString();
        MoRPayoutDataRequestEvent event = new MoRPayoutDataRequestEvent(requestId, "PAYOUT_SUMMARY");
        
        CompletableFuture<MoRPayoutDataResponseEvent> future = new CompletableFuture<>();
        pendingDataRequests.put(requestId, future);
        
        eventPublisher.publishMoRPayoutDataRequest(event);
        
        try {
            MoRPayoutDataResponseEvent response = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            pendingDataRequests.remove(requestId);
            
            if (response.isSuccess() && response.getSummary() != null) {
                return response.getSummary();
            } else {
                throw new RuntimeException("Failed to get payout summary: " + 
                    (response.getErrorMessage() != null ? response.getErrorMessage() : "Unknown error"));
            }
        } catch (Exception e) {
            pendingDataRequests.remove(requestId);
            logger.error("Error getting payout summary: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get payout summary: " + e.getMessage(), e);
        }
    }

    /**
     * Request vendor payouts from vendor-service via Kafka
     */
    public List<Map<String, Object>> getVendorPayouts(Long vendorId) {
        String requestId = UUID.randomUUID().toString();
        MoRPayoutDataRequestEvent event = new MoRPayoutDataRequestEvent(requestId, "VENDOR_PAYOUTS", vendorId);
        
        CompletableFuture<MoRPayoutDataResponseEvent> future = new CompletableFuture<>();
        pendingDataRequests.put(requestId, future);
        
        eventPublisher.publishMoRPayoutDataRequest(event);
        
        try {
            MoRPayoutDataResponseEvent response = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            pendingDataRequests.remove(requestId);
            
            if (response.isSuccess() && response.getData() != null) {
                return response.getData();
            } else {
                throw new RuntimeException("Failed to get vendor payouts: " + 
                    (response.getErrorMessage() != null ? response.getErrorMessage() : "Unknown error"));
            }
        } catch (Exception e) {
            pendingDataRequests.remove(requestId);
            logger.error("Error getting vendor payouts: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get vendor payouts: " + e.getMessage(), e);
        }
    }

    /**
     * Update payout status via Kafka
     */
    public void updatePayoutStatus(Long payoutId, String status, String transactionReference, String notes) {
        String requestId = UUID.randomUUID().toString();
        MoRPayoutStatusUpdateRequestEvent event = new MoRPayoutStatusUpdateRequestEvent(requestId, payoutId, status);
        event.setTransactionReference(transactionReference);
        event.setNotes(notes);
        
        CompletableFuture<MoRPayoutStatusUpdateResponseEvent> future = new CompletableFuture<>();
        pendingStatusUpdates.put(requestId, future);
        
        eventPublisher.publishMoRPayoutStatusUpdateRequest(event);
        
        try {
            MoRPayoutStatusUpdateResponseEvent response = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            pendingStatusUpdates.remove(requestId);
            
            if (!response.isSuccess()) {
                throw new RuntimeException("Failed to update payout status: " + 
                    (response.getErrorMessage() != null ? response.getErrorMessage() : "Unknown error"));
            }
        } catch (Exception e) {
            pendingStatusUpdates.remove(requestId);
            logger.error("Error updating payout status: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update payout status: " + e.getMessage(), e);
        }
    }

    /**
     * Handle data response from vendor-service
     */
    public void handleDataResponse(MoRPayoutDataResponseEvent response) {
        CompletableFuture<MoRPayoutDataResponseEvent> future = pendingDataRequests.remove(response.getRequestId());
        if (future != null) {
            future.complete(response);
        } else {
            logger.warn("Received data response for unknown request ID: {}", response.getRequestId());
        }
    }

    /**
     * Handle status update response from vendor-service
     */
    public void handleStatusUpdateResponse(MoRPayoutStatusUpdateResponseEvent response) {
        CompletableFuture<MoRPayoutStatusUpdateResponseEvent> future = pendingStatusUpdates.remove(response.getRequestId());
        if (future != null) {
            future.complete(response);
        } else {
            logger.warn("Received status update response for unknown request ID: {}", response.getRequestId());
        }
    }
}

