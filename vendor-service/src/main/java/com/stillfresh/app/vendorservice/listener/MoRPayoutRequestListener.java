package com.stillfresh.app.vendorservice.listener;

import com.stillfresh.app.vendorservice.publisher.VendorEventPublisher;
import com.stillfresh.app.vendorservice.service.VendorService;
import com.stillfresh.app.sharedentities.payment.events.MoRPayoutDataRequestEvent;
import com.stillfresh.app.sharedentities.payment.events.MoRPayoutDataResponseEvent;
import com.stillfresh.app.sharedentities.payment.events.MoRPayoutStatusUpdateRequestEvent;
import com.stillfresh.app.sharedentities.payment.events.MoRPayoutStatusUpdateResponseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Listener for MoR payout requests from payment-service
 */
@Component
public class MoRPayoutRequestListener {

    private static final Logger logger = LoggerFactory.getLogger(MoRPayoutRequestListener.class);

    @Autowired
    private VendorService vendorService;

    @Autowired
    private VendorEventPublisher eventPublisher;

    @KafkaListener(topics = "${payment.topic.mor-payout-data-request:mor-payout-data-request}", groupId = "vendor-service-group")
    public void handleMoRPayoutDataRequest(MoRPayoutDataRequestEvent event) {
        logger.info("Received MoRPayoutDataRequestEvent: requestId={}, type={}, vendorId={}", 
                   event.getRequestId(), event.getRequestType(), event.getVendorId());
        
        try {
            MoRPayoutDataResponseEvent response = new MoRPayoutDataResponseEvent(
                event.getRequestId(), 
                event.getRequestType(), 
                true
            );
            
            switch (event.getRequestType()) {
                case "PENDING_PAYOUTS":
                    List<Map<String, Object>> pendingPayouts = vendorService.getAllMoRPendingPayouts();
                    response.setData(pendingPayouts);
                    break;
                    
                case "VENDOR_BALANCES":
                    List<Map<String, Object>> vendorBalances = vendorService.getAllMoRVendorsWithBalances();
                    response.setData(vendorBalances);
                    break;
                    
                case "ORDER_PAYMENTS":
                    List<Map<String, Object>> orderPayments = vendorService.getAllMoROrderPayments(
                        event.getFromDate(), 
                        event.getToDate()
                    );
                    response.setData(orderPayments);
                    break;
                    
                case "PAYOUT_SUMMARY":
                    Map<String, Object> summary = vendorService.getMoRPayoutSummary();
                    response.setSummary(summary);
                    break;
                    
                case "VENDOR_PAYOUTS":
                    if (event.getVendorId() == null) {
                        response.setSuccess(false);
                        response.setErrorMessage("Vendor ID is required for VENDOR_PAYOUTS request");
                    } else {
                        List<Map<String, Object>> vendorPayouts = vendorService.getVendorPayouts(event.getVendorId());
                        response.setData(vendorPayouts);
                    }
                    break;
                    
                default:
                    response.setSuccess(false);
                    response.setErrorMessage("Unknown request type: " + event.getRequestType());
                    logger.warn("Unknown MoR payout data request type: {}", event.getRequestType());
            }
            
            eventPublisher.publishMoRPayoutDataResponse(response);
            logger.info("Published MoRPayoutDataResponseEvent for requestId: {}", event.getRequestId());
            
        } catch (Exception e) {
            logger.error("Error handling MoR payout data request: {}", e.getMessage(), e);
            MoRPayoutDataResponseEvent errorResponse = new MoRPayoutDataResponseEvent(
                event.getRequestId(), 
                event.getRequestType(), 
                false
            );
            errorResponse.setErrorMessage(e.getMessage());
            eventPublisher.publishMoRPayoutDataResponse(errorResponse);
        }
    }

    @KafkaListener(topics = "${payment.topic.mor-payout-status-update-request:mor-payout-status-update-request}", groupId = "vendor-service-group")
    public void handleMoRPayoutStatusUpdateRequest(MoRPayoutStatusUpdateRequestEvent event) {
        logger.info("Received MoRPayoutStatusUpdateRequestEvent: requestId={}, payoutId={}, status={}", 
                   event.getRequestId(), event.getPayoutId(), event.getStatus());
        
        try {
            vendorService.updatePayoutStatus(
                event.getPayoutId(), 
                event.getStatus(), 
                event.getTransactionReference(), 
                event.getNotes()
            );
            
            MoRPayoutStatusUpdateResponseEvent response = new MoRPayoutStatusUpdateResponseEvent(
                event.getRequestId(), 
                event.getPayoutId(), 
                true
            );
            
            eventPublisher.publishMoRPayoutStatusUpdateResponse(response);
            logger.info("Published MoRPayoutStatusUpdateResponseEvent for requestId: {}", event.getRequestId());
            
        } catch (Exception e) {
            logger.error("Error handling MoR payout status update request: {}", e.getMessage(), e);
            MoRPayoutStatusUpdateResponseEvent errorResponse = new MoRPayoutStatusUpdateResponseEvent(
                event.getRequestId(), 
                event.getPayoutId(), 
                false
            );
            errorResponse.setErrorMessage(e.getMessage());
            eventPublisher.publishMoRPayoutStatusUpdateResponse(errorResponse);
        }
    }
}

