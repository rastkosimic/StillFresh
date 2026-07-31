package com.stillfresh.app.vendorservice.listener;

import com.stillfresh.app.vendorservice.publisher.VendorEventPublisher;
import com.stillfresh.app.vendorservice.service.VendorService;
import com.stillfresh.app.sharedentities.payment.events.VendorPaymentInfoRequestEvent;
import com.stillfresh.app.sharedentities.payment.events.VendorPaymentInfoResponseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Listener for vendor payment info requests from payment-service
 */
@Component
public class VendorPaymentInfoRequestListener {

    private static final Logger logger = LoggerFactory.getLogger(VendorPaymentInfoRequestListener.class);

    @Autowired
    private VendorService vendorService;

    @Autowired
    private VendorEventPublisher eventPublisher;

    @KafkaListener(topics = "${payment.topic.vendor-payment-info-request:vendor-payment-info-request}", groupId = "vendor-service-group")
    public void handleVendorPaymentInfoRequest(VendorPaymentInfoRequestEvent event) {
        logger.info("Received VendorPaymentInfoRequestEvent: requestId={}, vendorId={}", 
                   event.getRequestId(), event.getVendorId());
        
        try {
            if (event.getVendorId() == null) {
                logger.error("Vendor ID is null in VendorPaymentInfoRequestEvent");
                VendorPaymentInfoResponseEvent errorResponse = new VendorPaymentInfoResponseEvent(
                    event.getRequestId(), 
                    null, 
                    false
                );
                errorResponse.setErrorMessage("Vendor ID is required");
                eventPublisher.publishVendorPaymentInfoResponse(errorResponse);
                return;
            }

            // Get vendor by ID
            Optional<com.stillfresh.app.vendorservice.model.Vendor> vendorOpt = vendorService.getVendorById(event.getVendorId());
            
            if (vendorOpt.isEmpty()) {
                logger.warn("Vendor not found for ID: {}", event.getVendorId());
                VendorPaymentInfoResponseEvent errorResponse = new VendorPaymentInfoResponseEvent(
                    event.getRequestId(), 
                    event.getVendorId(), 
                    false
                );
                errorResponse.setErrorMessage("Vendor not found with ID: " + event.getVendorId());
                eventPublisher.publishVendorPaymentInfoResponse(errorResponse);
                return;
            }

            com.stillfresh.app.vendorservice.model.Vendor vendor = vendorOpt.get();

            // On the SHARED banking model every chain location is paid through the account
            // owner (headquarters). This applies to both rails: the Stripe Connect account for
            // CONNECT vendors and the bank details for MoR vendors.
            com.stillfresh.app.vendorservice.model.Vendor accountOwner = vendorService.resolvePayoutAccountOwner(vendor);
            if (!accountOwner.getId().equals(vendor.getId())) {
                logger.info("Vendor {} uses a shared payment account. Resolving payment info from owner vendor {}.",
                           vendor.getId(), accountOwner.getId());
            }

            // Get payout model
            String payoutModel = vendor.getPayoutModel() != null ? vendor.getPayoutModel().toString() : null;

            // Get Stripe account ID (only for CONNECT model)
            String stripeAccountId = null;
            if (vendor.getPayoutModel() == com.stillfresh.app.sharedentities.enums.PayoutModel.CONNECT) {
                stripeAccountId = accountOwner.getStripeAccountId();
            }

            // Create response
            VendorPaymentInfoResponseEvent response = new VendorPaymentInfoResponseEvent(
                event.getRequestId(),
                event.getVendorId(),
                true
            );
            response.setPayoutModel(payoutModel);
            response.setStripeAccountId(stripeAccountId);
            // MoR bank details (used by payout scheduler for outbound transfers)
            response.setIbanNumber(accountOwner.getBankIban());
            response.setBankName(accountOwner.getBankName());
            response.setAccountHolderName(accountOwner.getBankAccountHolderName());
            response.setAccountNumber(accountOwner.getBankAccountNumber());
            response.setBankCode(accountOwner.getBankSwiftCode());

            eventPublisher.publishVendorPaymentInfoResponse(response);
            logger.info("Published VendorPaymentInfoResponseEvent for requestId: {}, vendorId: {}, accountOwnerId: {}, payoutModel: {}, hasIban: {}, hasAccountNumber: {}",
                       event.getRequestId(), event.getVendorId(), accountOwner.getId(), payoutModel,
                       accountOwner.getBankIban() != null, accountOwner.getBankAccountNumber() != null);
            
        } catch (Exception e) {
            logger.error("Error handling vendor payment info request: {}", e.getMessage(), e);
            VendorPaymentInfoResponseEvent errorResponse = new VendorPaymentInfoResponseEvent(
                event.getRequestId(), 
                event.getVendorId(), 
                false
            );
            errorResponse.setErrorMessage(e.getMessage());
            eventPublisher.publishVendorPaymentInfoResponse(errorResponse);
        }
    }
}

