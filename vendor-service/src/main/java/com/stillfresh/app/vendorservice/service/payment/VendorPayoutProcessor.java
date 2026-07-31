package com.stillfresh.app.vendorservice.service.payment;

import java.util.Map;

/**
 * Interface for vendor payout processors
 * Allows abstraction over different payment providers (Stripe, Payoneer, etc.)
 */
public interface VendorPayoutProcessor {
    
    /**
     * Registers a vendor with the payment provider
     * @param vendorEmail Vendor's email address
     * @param vendorName Vendor's business/display name
     * @param country ISO 2-letter country code
     * @return Provider-specific account identifier
     * @throws Exception if registration fails
     */
    String registerVendor(String vendorEmail, String vendorName, String country) throws Exception;
    
    /**
     * Processes a payout to a vendor
     * @param vendorAccountId Provider-specific vendor account ID
     * @param amount Amount in cents
     * @param currency Currency code (e.g., "eur", "usd")
     * @param description Optional description for the payout
     * @return Payout transaction ID
     * @throws Exception if payout fails
     */
    String processPayout(String vendorAccountId, long amount, String currency, String description) throws Exception;
    
    /**
     * Checks if vendor account is ready to receive payments
     * @param vendorAccountId Provider-specific vendor account ID
     * @return true if account is ready, false otherwise
     * @throws Exception if check fails
     */
    boolean isAccountReady(String vendorAccountId) throws Exception;
    
    /**
     * Gets account details for a vendor
     * @param vendorAccountId Provider-specific vendor account ID
     * @return Map containing account details
     * @throws Exception if retrieval fails
     */
    Map<String, Object> getAccountDetails(String vendorAccountId) throws Exception;
    
    /**
     * Creates an onboarding/registration link for the vendor
     * @param vendorAccountId Provider-specific vendor account ID
     * @return URL for vendor to complete onboarding
     * @throws Exception if link creation fails
     */
    String createOnboardingLink(String vendorAccountId) throws Exception;
    
    /**
     * Gets the payment provider name
     * @return Provider name (e.g., "STRIPE", "PAYONEER")
     */
    String getProviderName();
}

