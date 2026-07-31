package com.stillfresh.app.paymentservice.listener;

import com.stillfresh.app.paymentservice.service.MoRPayoutService;
import com.stillfresh.app.sharedentities.payment.events.MoRPayoutDataResponseEvent;
import com.stillfresh.app.sharedentities.payment.events.MoRPayoutStatusUpdateResponseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listener for MoR payout responses from vendor-service
 */
@Component
public class MoRPayoutResponseListener {

    private static final Logger logger = LoggerFactory.getLogger(MoRPayoutResponseListener.class);

    @Autowired
    private MoRPayoutService morPayoutService;

    @KafkaListener(topics = "${payment.topic.mor-payout-data-response:mor-payout-data-response}", groupId = "payment-service-group")
    public void handleMoRPayoutDataResponse(MoRPayoutDataResponseEvent event) {
        logger.info("Received MoRPayoutDataResponseEvent for request ID: {}, type: {}", 
                   event.getRequestId(), event.getRequestType());
        try {
            morPayoutService.handleDataResponse(event);
        } catch (Exception e) {
            logger.error("Error handling MoR payout data response: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "${payment.topic.mor-payout-status-update-response:mor-payout-status-update-response}", groupId = "payment-service-group")
    public void handleMoRPayoutStatusUpdateResponse(MoRPayoutStatusUpdateResponseEvent event) {
        logger.info("Received MoRPayoutStatusUpdateResponseEvent for request ID: {}, payout ID: {}", 
                   event.getRequestId(), event.getPayoutId());
        try {
            morPayoutService.handleStatusUpdateResponse(event);
        } catch (Exception e) {
            logger.error("Error handling MoR payout status update response: {}", e.getMessage(), e);
        }
    }
}

