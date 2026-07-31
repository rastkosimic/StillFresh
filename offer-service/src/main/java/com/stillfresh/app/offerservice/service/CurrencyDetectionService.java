package com.stillfresh.app.offerservice.service;

import com.stillfresh.app.sharedentities.enums.Currency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for detecting currency based on country code.
 * Maps ISO 2-letter country codes to appropriate currencies.
 */
@Service
public class CurrencyDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(CurrencyDetectionService.class);
    
    // Country code to Currency mapping
    private static final Map<String, Currency> COUNTRY_TO_CURRENCY = new HashMap<>();
    
    static {
        // Eurozone countries
        COUNTRY_TO_CURRENCY.put("AT", Currency.EUR); // Austria
        COUNTRY_TO_CURRENCY.put("BE", Currency.EUR); // Belgium
        COUNTRY_TO_CURRENCY.put("CY", Currency.EUR); // Cyprus
        COUNTRY_TO_CURRENCY.put("EE", Currency.EUR); // Estonia
        COUNTRY_TO_CURRENCY.put("FI", Currency.EUR); // Finland
        COUNTRY_TO_CURRENCY.put("FR", Currency.EUR); // France
        COUNTRY_TO_CURRENCY.put("DE", Currency.EUR); // Germany
        COUNTRY_TO_CURRENCY.put("GR", Currency.EUR); // Greece
        COUNTRY_TO_CURRENCY.put("IE", Currency.EUR); // Ireland
        COUNTRY_TO_CURRENCY.put("IT", Currency.EUR); // Italy
        COUNTRY_TO_CURRENCY.put("LV", Currency.EUR); // Latvia
        COUNTRY_TO_CURRENCY.put("LT", Currency.EUR); // Lithuania
        COUNTRY_TO_CURRENCY.put("LU", Currency.EUR); // Luxembourg
        COUNTRY_TO_CURRENCY.put("MT", Currency.EUR); // Malta
        COUNTRY_TO_CURRENCY.put("NL", Currency.EUR); // Netherlands
        COUNTRY_TO_CURRENCY.put("PT", Currency.EUR); // Portugal
        COUNTRY_TO_CURRENCY.put("SK", Currency.EUR); // Slovakia
        COUNTRY_TO_CURRENCY.put("SI", Currency.EUR); // Slovenia
        COUNTRY_TO_CURRENCY.put("ES", Currency.EUR); // Spain
        COUNTRY_TO_CURRENCY.put("BG", Currency.EUR); // Bulgaria
        
        // Other European countries
        COUNTRY_TO_CURRENCY.put("GB", Currency.GBP); // United Kingdom
        COUNTRY_TO_CURRENCY.put("CH", Currency.CHF); // Switzerland
        COUNTRY_TO_CURRENCY.put("LI", Currency.CHF); // Liechtenstein
        COUNTRY_TO_CURRENCY.put("SE", Currency.SEK); // Sweden
        COUNTRY_TO_CURRENCY.put("NO", Currency.NOK); // Norway
        COUNTRY_TO_CURRENCY.put("DK", Currency.DKK); // Denmark
        COUNTRY_TO_CURRENCY.put("PL", Currency.PLN); // Poland
        COUNTRY_TO_CURRENCY.put("HU", Currency.HUF); // Hungary
        COUNTRY_TO_CURRENCY.put("CZ", Currency.CZK); // Czech Republic
        COUNTRY_TO_CURRENCY.put("RO", Currency.RON); // Romania
        COUNTRY_TO_CURRENCY.put("IS", Currency.ISK); // Iceland
        
        // Eastern Europe and Balkans
        COUNTRY_TO_CURRENCY.put("RS", Currency.RSD); // Serbia
        COUNTRY_TO_CURRENCY.put("BA", Currency.EUR); // Bosnia and Herzegovina (uses EUR in practice)
        COUNTRY_TO_CURRENCY.put("AL", Currency.ALL); // Albania
        COUNTRY_TO_CURRENCY.put("MK", Currency.MKD); // North Macedonia
        COUNTRY_TO_CURRENCY.put("ME", Currency.EUR); // Montenegro (uses EUR)
        COUNTRY_TO_CURRENCY.put("HR", Currency.EUR); // Croatia (uses EUR)
        COUNTRY_TO_CURRENCY.put("MD", Currency.MDL); // Moldova
        COUNTRY_TO_CURRENCY.put("UA", Currency.UAH); // Ukraine
        COUNTRY_TO_CURRENCY.put("RU", Currency.RUB); // Russia
        COUNTRY_TO_CURRENCY.put("TR", Currency.TRY); // Turkey
        COUNTRY_TO_CURRENCY.put("GE", Currency.GEL); // Georgia
    }

    /**
     * Determines currency based on country code.
     * 
     * @param countryCode ISO 2-letter country code (e.g., "RS", "DE", "US")
     * @return Currency enum value, defaults to EUR if country cannot be determined
     */
    public Currency getCurrencyForCountry(String countryCode) {
        if (countryCode == null || countryCode.isEmpty()) {
            logger.warn("Empty country code provided. Defaulting to EUR");
            return Currency.EUR;
        }
        
        String upperCountryCode = countryCode.toUpperCase();
        Currency currency = COUNTRY_TO_CURRENCY.get(upperCountryCode);
        
        if (currency != null) {
            logger.debug("Mapped country {} to currency {}", upperCountryCode, currency);
            return currency;
        } else {
            logger.warn("No currency mapping found for country code: {}. Defaulting to EUR", upperCountryCode);
            return Currency.EUR;
        }
    }
}
