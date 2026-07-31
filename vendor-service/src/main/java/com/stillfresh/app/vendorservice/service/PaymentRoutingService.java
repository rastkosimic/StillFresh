package com.stillfresh.app.vendorservice.service;

import com.stillfresh.app.sharedentities.enums.PaymentProvider;
import com.stillfresh.app.vendorservice.model.Vendor;
import com.stillfresh.app.vendorservice.service.payment.MoRPayoutProcessor;
import com.stillfresh.app.vendorservice.service.payment.StripePayoutProcessor;
import com.stillfresh.app.vendorservice.service.payment.VendorPayoutProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for routing vendor payouts to the appropriate payment processor
 * Based on vendor's country and payment provider configuration
 */
@Service
public class PaymentRoutingService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentRoutingService.class);
    
    @Autowired
    private PaymentProviderService paymentProviderService;
    
    @Autowired
    private StripePayoutProcessor stripeProcessor;
    
    @Autowired
    private MoRPayoutProcessor morProcessor;
    
    /**
     * Gets the appropriate payout processor for a vendor
     * @param vendor Vendor entity
     * @return VendorPayoutProcessor implementation
     */
    public VendorPayoutProcessor getProcessor(Vendor vendor) {
        PaymentProvider provider = determineProvider(vendor);
        
        switch (provider) {
            case STRIPE:
                logger.debug("Using Stripe processor for vendor: {}", vendor.getId());
                return stripeProcessor;
            case MOR:
                logger.debug("Using MoR processor for vendor: {}", vendor.getId());
                return morProcessor;
            default:
                logger.warn("Unknown payment provider for vendor: {}, defaulting to MoR", vendor.getId());
                return morProcessor;
        }
    }
    
    /**
     * Determines the payment provider for a vendor
     * Uses cached value if available, otherwise determines from country
     * @param vendor Vendor entity
     * @return PaymentProvider
     */
    public PaymentProvider determineProvider(Vendor vendor) {
        // If vendor has explicit payment provider set, use it
        if (vendor.getPaymentProvider() != null) {
            logger.debug("Vendor {} has explicit payment provider: {}", vendor.getId(), vendor.getPaymentProvider());
            return vendor.getPaymentProvider();
        }
        
        // Otherwise, determine from country
        String country = vendor.getCountry();
        if (country == null || country.isEmpty()) {
            logger.warn("Vendor {} has no country set, defaulting to MOR", vendor.getId());
            return PaymentProvider.MOR;
        }
        
        PaymentProvider provider = paymentProviderService.determineProvider(country);
        logger.debug("Determined payment provider for vendor {} (country: {}): {}", 
                    vendor.getId(), country, provider);
        return provider;
    }
    
    /**
     * Processes a payout to a vendor using the appropriate processor
     * @param vendor Vendor entity
     * @param amount Amount in cents
     * @param currency Currency code
     * @param description Payout description
     * @return Transaction ID from the payment processor
     */
    public String processPayout(Vendor vendor, long amount, String currency, String description) {
        logger.info("Processing payout for vendor: {} ({} {} cents)", vendor.getId(), currency, amount);
        
        VendorPayoutProcessor processor = getProcessor(vendor);
        PaymentProvider provider = determineProvider(vendor);
        
        String accountId;
        switch (provider) {
            case STRIPE:
                accountId = vendor.getStripeAccountId();
                if (accountId == null || accountId.isEmpty()) {
                    throw new RuntimeException("Vendor does not have a Stripe account. Please complete Stripe onboarding first.");
                }
                break;
            case MOR:
                // For MoR, use email as account identifier
                accountId = vendor.getEmail();
                if (accountId == null || accountId.isEmpty()) {
                    throw new RuntimeException("Vendor email is required for MoR model.");
                }
                break;
            default:
                throw new RuntimeException("Unknown payment provider for vendor: " + vendor.getId());
        }
        
        try {
            String transactionId = processor.processPayout(accountId, amount, currency, description);
            logger.info("Successfully processed payout for vendor {} via {}: {}", 
                       vendor.getId(), provider, transactionId);
            return transactionId;
        } catch (Exception e) {
            logger.error("Error processing payout for vendor {} via {}: {}", 
                        vendor.getId(), provider, e.getMessage(), e);
            throw new RuntimeException("Failed to process payout: " + e.getMessage());
        }
    }
    
    /**
     * Checks if vendor account is ready to receive payments
     * @param vendor Vendor entity
     * @return true if account is ready, false otherwise
     */
    public boolean isAccountReady(Vendor vendor) {
        PaymentProvider provider = determineProvider(vendor);
        VendorPayoutProcessor processor = getProcessor(vendor);
        
        String accountId;
        switch (provider) {
            case STRIPE:
                accountId = vendor.getStripeAccountId();
                break;
            case MOR:
                // For MoR, check if bank details are provided
                accountId = vendor.getEmail();
                if (accountId == null || accountId.isEmpty()) {
                    return false;
                }
                // A MoR account is ready once the payout has a destination. Either identifier is
                // enough: SEPA transfers use the IBAN, domestic rails use the account number.
                return hasBankDestination(vendor);
            default:
                return false;
        }
        
        if (accountId == null || accountId.isEmpty()) {
            return false;
        }
        
        try {
            return processor.isAccountReady(accountId);
        } catch (Exception e) {
            logger.error("Error checking account readiness for vendor {}: {}", vendor.getId(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * True when a MoR vendor has at least one usable payout identifier.
     */
    public static boolean hasBankDestination(Vendor vendor) {
        return (vendor.getBankIban() != null && !vendor.getBankIban().isBlank())
            || (vendor.getBankAccountNumber() != null && !vendor.getBankAccountNumber().isBlank());
    }
}

