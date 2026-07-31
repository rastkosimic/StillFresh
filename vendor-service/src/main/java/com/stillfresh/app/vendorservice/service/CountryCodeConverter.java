package com.stillfresh.app.vendorservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service to convert country names (in various languages) to ISO 2-letter country codes
 * Supports multiple languages and common variations
 */
@Service
public class CountryCodeConverter {
    
    private static final Logger logger = LoggerFactory.getLogger(CountryCodeConverter.class);
    
    // Map of country names/variations to ISO 2-letter codes
    // Includes English names, native names, common variations, and ISO 3-letter codes
    private static final Map<String, String> COUNTRY_NAME_TO_CODE = new HashMap<>();
    
    static {
        // United States
        addCountry("US", "United States", "USA", "United States of America", "États-Unis", "Estados Unidos", "Vereinigte Staaten");
        
        // Canada
        addCountry("CA", "Canada", "CAN");
        
        // Mexico
        addCountry("MX", "Mexico", "MEX", "México", "Mexique");
        
        // United Kingdom
        addCountry("GB", "United Kingdom", "UK", "Great Britain", "Britain", "Royaume-Uni", "Reino Unido", "Vereinigtes Königreich", "GBR");
        
        // Ireland
        addCountry("IE", "Ireland", "IRE", "Éire", "Irlande", "Irlanda", "Irland");
        
        // France
        addCountry("FR", "France", "FRA", "França", "Frankreich");
        
        // Germany
        addCountry("DE", "Germany", "DEU", "Deutschland", "Allemagne", "Alemania", "Germania");
        
        // Italy
        addCountry("IT", "Italy", "ITA", "Italia", "Italie", "Italien");
        
        // Spain
        addCountry("ES", "Spain", "ESP", "España", "Espagne", "Spanien");
        
        // Netherlands
        addCountry("NL", "Netherlands", "NLD", "Holland", "Pays-Bas", "Países Bajos", "Niederlande", "Olanda");
        
        // Belgium
        addCountry("BE", "Belgium", "BEL", "Belgique", "België", "Belgien", "Belgio");
        
        // Austria
        addCountry("AT", "Austria", "AUT", "Österreich", "Autriche", "Austria", "Austrija");
        
        // Switzerland
        addCountry("CH", "Switzerland", "CHE", "Suisse", "Schweiz", "Svizzera", "Suiza");
        
        // Sweden
        addCountry("SE", "Sweden", "SWE", "Sverige", "Suède", "Suecia", "Schweden");
        
        // Norway
        addCountry("NO", "Norway", "NOR", "Norge", "Norvège", "Noruega", "Norwegen");
        
        // Denmark
        addCountry("DK", "Denmark", "DNK", "Danmark", "Danemark", "Dinamarca", "Dänemark");
        
        // Finland
        addCountry("FI", "Finland", "FIN", "Suomi", "Finlande", "Finlandia", "Finnland");
        
        // Poland
        addCountry("PL", "Poland", "POL", "Polska", "Pologne", "Polonia", "Polen");
        
        // Portugal
        addCountry("PT", "Portugal", "PRT", "Portugal", "Portugal", "Portugal", "Portugal");
        
        // Greece
        addCountry("GR", "Greece", "GRC", "Ελλάδα", "Grèce", "Grecia", "Griechenland", "Grčka");
        
        // Czech Republic
        addCountry("CZ", "Czech Republic", "CZE", "Česká republika", "République tchèque", "República Checa", "Tschechien", "Češka");
        
        // Hungary
        addCountry("HU", "Hungary", "HUN", "Magyarország", "Hongrie", "Hungría", "Ungarn", "Mađarska");
        
        // Romania
        addCountry("RO", "Romania", "ROU", "România", "Roumanie", "Rumania", "Rumänien", "Rumunija");
        
        // Bulgaria
        addCountry("BG", "Bulgaria", "BGR", "България", "Bulgarie", "Bulgaria", "Bulgarien", "Bugarska");
        
        // Croatia
        addCountry("HR", "Croatia", "HRV", "Hrvatska", "Croatie", "Croacia", "Kroatien", "Хрватска");
        
        // Slovenia
        addCountry("SI", "Slovenia", "SVN", "Slovenija", "Slovénie", "Eslovenia", "Slowenien", "Словенија");
        
        // Slovakia
        addCountry("SK", "Slovakia", "SVK", "Slovensko", "Slovaquie", "Eslovaquia", "Slowakei", "Словачка");
        
        // Lithuania
        addCountry("LT", "Lithuania", "LTU", "Lietuva", "Lituanie", "Lituania", "Litauen", "Литванија");
        
        // Latvia
        addCountry("LV", "Latvia", "LVA", "Latvija", "Lettonie", "Letonia", "Lettland", "Летонија");
        
        // Estonia
        addCountry("EE", "Estonia", "EST", "Eesti", "Estonie", "Estonia", "Estland", "Естонија");
        
        // Luxembourg
        addCountry("LU", "Luxembourg", "LUX", "Luxemburg", "Luxemburgo", "Lussemburgo");
        
        // Malta
        addCountry("MT", "Malta", "MLT");
        
        // Cyprus
        addCountry("CY", "Cyprus", "CYP", "Κύπρος", "Chypre", "Chipre", "Zypern", "Кипар");
        
        // Australia
        addCountry("AU", "Australia", "AUS", "Australie", "Australia", "Australien");
        
        // New Zealand
        addCountry("NZ", "New Zealand", "NZL", "Nouvelle-Zélande", "Nueva Zelanda", "Neuseeland", "Novi Zeland");
        
        // Singapore
        addCountry("SG", "Singapore", "SGP", "Singapour", "Singapur");
        
        // Japan
        addCountry("JP", "Japan", "JPN", "Japon", "Japón", "Japan", "Japanska");
        
        // Hong Kong
        addCountry("HK", "Hong Kong", "HKG", "Hong-Kong", "Xianggang");
        
        // Malaysia
        addCountry("MY", "Malaysia", "MYS", "Malaisie", "Malasia", "Malaysien");
        
        // Thailand
        addCountry("TH", "Thailand", "THA", "Thaïlande", "Tailandia", "Thailand");
        
        // Philippines
        addCountry("PH", "Philippines", "PHL", "Philippinen", "Filipinas", "Filipini");
        
        // Indonesia
        addCountry("ID", "Indonesia", "IDN", "Indonésie", "Indonesia", "Indonesien");
        
        // Vietnam
        addCountry("VN", "Vietnam", "VNM", "Việt Nam", "Vietnam", "Vietnam", "Vijetnam");
        
        // India
        addCountry("IN", "India", "IND", "Inde", "India", "Indien", "Indija");
        
        // Brazil
        addCountry("BR", "Brazil", "BRA", "Brasil", "Brésil", "Brasilien");
        
        // Argentina
        addCountry("AR", "Argentina", "ARG", "Argentine", "Argentinien");
        
        // Chile
        addCountry("CL", "Chile", "CHL", "Chili", "Cile");
        
        // Colombia
        addCountry("CO", "Colombia", "COL", "Colombie", "Kolumbien");
        
        // Peru
        addCountry("PE", "Peru", "PER", "Pérou", "Perù", "Peruan");
        
        // Uruguay
        addCountry("UY", "Uruguay", "URY", "Uruguay", "Urugvaj");
        
        // Serbia
        addCountry("RS", "Serbia", "SRB", "Srbija", "Serbie", "Serbien", "Србија");
        
        // Bosnia and Herzegovina
        addCountry("BA", "Bosnia and Herzegovina", "BIH", "Bosna i Hercegovina", "Bosnie-Herzégovine", "Bosnien und Herzegowina", "Босна и Херцеговина");
        
        // Albania
        addCountry("AL", "Albania", "ALB", "Shqipëria", "Albanie", "Albanien", "Албанија");
        
        // North Macedonia
        addCountry("MK", "North Macedonia", "MKD", "Macedonia", "Северна Македонија", "Macédoine du Nord", "Mazedonien", "Makedonija");
        
        // Montenegro
        addCountry("ME", "Montenegro", "MNE", "Crna Gora", "Monténégro", "Montenegro", "Црна Гора");
        
        // Kosovo
        addCountry("XK", "Kosovo", "KOS", "Kosova", "Kosovo", "Косово");
    }
    
    /**
     * Helper method to add multiple variations for a country code
     */
    private static void addCountry(String code, String... names) {
        for (String name : names) {
            if (name != null && !name.trim().isEmpty()) {
                COUNTRY_NAME_TO_CODE.put(name.trim().toLowerCase(), code);
            }
        }
    }
    
    /**
     * Converts a country name (in any language) to ISO 2-letter country code
     * 
     * @param countryInput Country name, ISO code, or variation (case-insensitive)
     * @return ISO 2-letter country code, or null if not found
     */
    public String convertToIsoCode(String countryInput) {
        if (countryInput == null || countryInput.trim().isEmpty()) {
            return null;
        }
        
        String normalized = countryInput.trim();
        
        // If already a 2-letter code (uppercase), return as-is
        if (normalized.length() == 2 && normalized.matches("[A-Z]{2}")) {
            logger.debug("Country input '{}' is already a valid ISO 2-letter code", normalized);
            return normalized;
        }
        
        // If already a 2-letter code (lowercase), return uppercase
        if (normalized.length() == 2 && normalized.matches("[a-z]{2}")) {
            logger.debug("Country input '{}' converted to uppercase ISO code", normalized);
            return normalized.toUpperCase();
        }
        
        // Look up in the mapping (case-insensitive)
        String code = COUNTRY_NAME_TO_CODE.get(normalized.toLowerCase());
        
        if (code != null) {
            logger.debug("Converted country '{}' to ISO code '{}'", countryInput, code);
            return code;
        }
        
        // If not found, log warning and return null
        logger.warn("Could not convert country name '{}' to ISO code. Returning null.", countryInput);
        return null;
    }
    
    /**
     * Converts a country name to ISO 2-letter code, with fallback
     * If conversion fails, returns the original input (assumes it might already be a code)
     * 
     * @param countryInput Country name or code
     * @return ISO 2-letter country code, or original input if conversion fails
     */
    public String convertToIsoCodeWithFallback(String countryInput) {
        String code = convertToIsoCode(countryInput);
        return code != null ? code : countryInput;
    }
    
    /**
     * Checks if a country name/code is valid
     * 
     * @param countryInput Country name or code
     * @return true if valid, false otherwise
     */
    public boolean isValidCountry(String countryInput) {
        return convertToIsoCode(countryInput) != null;
    }
}

