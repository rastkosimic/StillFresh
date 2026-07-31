package com.stillfresh.app.vendorservice.service.payment;

import com.stillfresh.app.vendorservice.client.PaymentClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Stripe Connect implementation of VendorPayoutProcessor
 * Delegates to existing Stripe Connect service via PaymentClient
 */
@Service
public class StripePayoutProcessor implements VendorPayoutProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(StripePayoutProcessor.class);
    
    @Autowired
    private PaymentClient paymentClient;
    
    @Override
    public String registerVendor(String vendorEmail, String vendorName, String country) throws Exception {
        logger.info("Registering vendor with Stripe: {} ({})", vendorName, vendorEmail);
        
        if (paymentClient == null) {
            throw new RuntimeException("Payment client is not available");
        }
        
        try {
            PaymentClient.StripeConnectResponse response = paymentClient.createConnectAccount(vendorEmail, vendorName);
            if (response != null && response.getValue() != null && !response.getValue().isEmpty()) {
                String accountId = response.getValue().trim();
                logger.info("Successfully registered vendor with Stripe: {}", accountId);
                return accountId;
            } else {
                throw new RuntimeException("Failed to create Stripe account. Empty response.");
            }
        } catch (Exception e) {
            logger.error("Error registering vendor with Stripe: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to register vendor with Stripe: " + e.getMessage());
        }
    }
    
    @Override
    public String processPayout(String vendorAccountId, long amount, String currency, String description) throws Exception {
        // Stripe payouts are handled automatically via Connect transfers
        // This method is kept for interface compliance but actual payouts happen during payment processing
        logger.info("Stripe payout processing is handled via Connect transfers during payment processing");
        logger.info("Vendor account: {}, Amount: {} {}, Description: {}", vendorAccountId, amount, currency, description);
        return "stripe_connect_transfer"; // Stripe handles this automatically
    }
    
    @Override
    public boolean isAccountReady(String vendorAccountId) throws Exception {
        logger.info("Checking Stripe account readiness: {}", vendorAccountId);
        
        if (paymentClient == null) {
            throw new RuntimeException("Payment client is not available");
        }
        
        try {
            Boolean isReady = paymentClient.isAccountReady(vendorAccountId);
            return isReady != null && isReady;
        } catch (Exception e) {
            logger.error("Error checking Stripe account readiness: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to check Stripe account readiness: " + e.getMessage());
        }
    }
    
    @Override
    public Map<String, Object> getAccountDetails(String vendorAccountId) throws Exception {
        logger.info("Getting Stripe account details: {}", vendorAccountId);
        
        if (paymentClient == null) {
            throw new RuntimeException("Payment client is not available");
        }
        
        try {
            return paymentClient.getAccountDetails(vendorAccountId);
        } catch (Exception e) {
            logger.error("Error getting Stripe account details: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get Stripe account details: " + e.getMessage());
        }
    }
    
    @Override
    public String createOnboardingLink(String vendorAccountId) throws Exception {
        logger.info("Creating Stripe onboarding link: {}", vendorAccountId);
        
        if (paymentClient == null) {
            throw new RuntimeException("Payment client is not available");
        }
        
        try {
            PaymentClient.StripeConnectResponse response = paymentClient.createAccountLink(vendorAccountId);
            if (response != null && response.getValue() != null && !response.getValue().isEmpty()) {
                return response.getValue();
            } else {
                throw new RuntimeException("Failed to create Stripe onboarding link. Empty response.");
            }
        } catch (Exception e) {
            logger.error("Error creating Stripe onboarding link: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create Stripe onboarding link: " + e.getMessage());
        }
    }
    
    @Override
    public String getProviderName() {
        return "STRIPE";
    }
}

