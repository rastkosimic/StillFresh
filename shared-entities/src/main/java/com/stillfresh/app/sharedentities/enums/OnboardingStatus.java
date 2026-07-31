package com.stillfresh.app.sharedentities.enums;

/**
 * Represents the onboarding status of a vendor
 * Tracks progress through the vendor onboarding workflow
 */
public enum OnboardingStatus {
    /**
     * Initial registration - vendor submitted basic info, awaiting admin verification
     */
    PENDING_VERIFICATION,
    
    /**
     * Admin has verified the business, vendor can now log in and start onboarding
     */
    VERIFIED,
    
    /**
     * Vendor has selected their type (CHAIN or UNIQUE)
     */
    TYPE_SELECTED,
    
    /**
     * For chains: Headquarters location has been added
     */
    HEADQUARTERS_ADDED,
    
    /**
     * Banking model has been selected (SHARED or INDIVIDUAL)
     */
    BANKING_SETUP,
    
    /**
     * Payment account has been configured and is ready
     */
    PAYMENT_CONFIGURED,
    
    /**
     * Onboarding is complete - vendor can fully use the platform
     */
    COMPLETED
}

