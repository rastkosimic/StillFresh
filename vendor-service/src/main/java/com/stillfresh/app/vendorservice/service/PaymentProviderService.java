package com.stillfresh.app.vendorservice.service;

import com.stillfresh.app.sharedentities.enums.PaymentProvider;
import com.stillfresh.app.sharedentities.enums.PayoutModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class PaymentProviderService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentProviderService.class);
    
    // Stripe supported countries (as of 2024)
    // Source: https://stripe.com/global
    private static final Set<String> STRIPE_SUPPORTED_COUNTRIES = Set.of(
        // North America
        "US", "CA", "MX",
        // Europe
        "GB", "IE", "FR", "DE", "IT", "ES", "NL", "BE", "AT", "CH", 
        "SE", "NO", "DK", "FI", "PL", "PT", "GR", "CZ", "HU", "RO", 
        "BG", "HR", "SI", "SK", "LT", "LV", "EE", "LU", "MT", "CY",
        // Asia Pacific
        "AU", "NZ", "SG", "JP", "HK", "MY", "TH", "PH", "ID", "VN", "IN",
        // Latin America
        "BR", "AR", "CL", "CO", "PE", "UY"
    );
    
    // Countries requiring MoR (Merchant of Record) model (Balkan region and others not supported by Stripe)
    private static final Set<String> MOR_REQUIRED_COUNTRIES = Set.of(
        // Balkan countries
        "RS", // Serbia
        "BA", // Bosnia and Herzegovina
        "AL", // Albania
        "MK", // North Macedonia
        "ME", // Montenegro
        "XK"  // Kosovo
    );
    
    /**
     * Determines the appropriate payment provider for a given country
     * @param countryCode ISO 2-letter country code (e.g., "US", "RS", "DE")
     * @return PaymentProvider (STRIPE or MOR)
     */
    public PaymentProvider determineProvider(String countryCode) {
        if (countryCode == null || countryCode.isEmpty()) {
            logger.warn("Country code is null or empty, defaulting to MOR");
            return PaymentProvider.MOR;
        }
        
        String upperCountryCode = countryCode.toUpperCase();
        
        if (STRIPE_SUPPORTED_COUNTRIES.contains(upperCountryCode)) {
            logger.debug("Country {} is supported by Stripe", upperCountryCode);
            return PaymentProvider.STRIPE;
        } else {
            // For unsupported countries, use MoR model
            logger.debug("Country {} requires MoR model", upperCountryCode);
            return PaymentProvider.MOR;
        }
    }
    
    /**
     * Determines the payout model for a given country
     * @param countryCode ISO 2-letter country code
     * @return PayoutModel (CONNECT or MOR)
     */
    public PayoutModel determinePayoutModel(String countryCode) {
        if (countryCode == null || countryCode.isEmpty()) {
            logger.warn("Country code is null or empty, defaulting to MOR");
            return PayoutModel.MOR;
        }
        
        String upperCountryCode = countryCode.toUpperCase();
        
        if (STRIPE_SUPPORTED_COUNTRIES.contains(upperCountryCode)) {
            logger.debug("Country {} uses Stripe Connect", upperCountryCode);
            return PayoutModel.CONNECT;
        } else {
            logger.debug("Country {} uses MoR model", upperCountryCode);
            return PayoutModel.MOR;
        }
    }
    
    /**
     * Checks if a country is supported by Stripe
     * @param countryCode ISO 2-letter country code
     * @return true if Stripe is supported, false otherwise
     */
    public boolean isStripeSupported(String countryCode) {
        if (countryCode == null || countryCode.isEmpty()) {
            return false;
        }
        return STRIPE_SUPPORTED_COUNTRIES.contains(countryCode.toUpperCase());
    }
    
    /**
     * Checks if a country requires MoR model
     * @param countryCode ISO 2-letter country code
     * @return true if MoR is required, false otherwise
     */
    public boolean requiresMoR(String countryCode) {
        if (countryCode == null || countryCode.isEmpty()) {
            return true; // Default to MoR for unknown countries
        }
        return !STRIPE_SUPPORTED_COUNTRIES.contains(countryCode.toUpperCase());
    }
    
    /**
     * Gets all Stripe supported countries
     * @return Set of country codes
     */
    public Set<String> getStripeSupportedCountries() {
        return Set.copyOf(STRIPE_SUPPORTED_COUNTRIES);
    }
    
    /**
     * Gets all MoR required countries (countries not in Stripe supported list)
     * @return Set of country codes
     */
    public Set<String> getMoRRequiredCountries() {
        return Set.copyOf(MOR_REQUIRED_COUNTRIES);
    }
}

