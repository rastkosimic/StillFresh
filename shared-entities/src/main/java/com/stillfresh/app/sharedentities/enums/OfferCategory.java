package com.stillfresh.app.sharedentities.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum representing offer categories with multi-language support.
 * Categories are stored as enum values in the database (language-agnostic).
 * Translations are provided via getDisplayName(locale) method.
 */
public enum OfferCategory {
    ALL("All"),
    MEALS("Meals"),
    BREAD_PASTRIES("Bread & pastries"),
    GROCERIES("Groceries"),
    FLOWERS_PLANTS("Flowers & plants"),
    PET_FOOD("Pet food");
    
    private final String displayNameEn; // English default
    
    // Translation map: locale code -> category -> translated name
    private static final Map<String, Map<OfferCategory, String>> TRANSLATIONS = new HashMap<>();
    
    static {
        // Serbian (sr) translations
        Map<OfferCategory, String> sr = new HashMap<>();
        sr.put(ALL, "Sve");
        sr.put(MEALS, "Obroci");
        sr.put(BREAD_PASTRIES, "Hleb & Peciva");
        sr.put(GROCERIES, "Namirnice");
        sr.put(FLOWERS_PLANTS, "Cveće & Biljke");
        sr.put(PET_FOOD, "Hrana za kućne ljubimce");
        TRANSLATIONS.put("sr", sr);
        
        // Croatian (hr) translations
        Map<OfferCategory, String> hr = new HashMap<>();
        hr.put(ALL, "Sve");
        hr.put(MEALS, "Obroci");
        hr.put(BREAD_PASTRIES, "Kruh & Peciva");
        hr.put(GROCERIES, "Namirnice");
        hr.put(FLOWERS_PLANTS, "Cvijeće & Biljke");
        hr.put(PET_FOOD, "Hrana za kućne ljubimce");
        TRANSLATIONS.put("hr", hr);
        
        // Montenegrin (sr-ME or hr-ME, using sr as base)
        // Note: Montenegrin is very similar to Serbian, using sr translations
        Map<OfferCategory, String> me = new HashMap<>();
        me.put(ALL, "Sve");
        me.put(MEALS, "Obroci");
        me.put(BREAD_PASTRIES, "Hljeb & Peciva");
        me.put(GROCERIES, "Namirnice");
        me.put(FLOWERS_PLANTS, "Cvijeće & Biljke");
        me.put(PET_FOOD, "Hrana za kućne ljubimce");
        TRANSLATIONS.put("me", me);
        
        // Bosnian (bs) translations
        Map<OfferCategory, String> bs = new HashMap<>();
        bs.put(ALL, "Sve");
        bs.put(MEALS, "Obroci");
        bs.put(BREAD_PASTRIES, "Hljeb & Peciva");
        bs.put(GROCERIES, "Namirnice");
        bs.put(FLOWERS_PLANTS, "Cvijeće & Biljke");
        bs.put(PET_FOOD, "Hrana za kućne ljubimce");
        TRANSLATIONS.put("bs", bs);
        
        // Slovenian (sl) translations
        Map<OfferCategory, String> sl = new HashMap<>();
        sl.put(ALL, "Vse");
        sl.put(MEALS, "Obroki");
        sl.put(BREAD_PASTRIES, "Kruh & Pecivo");
        sl.put(GROCERIES, "Živila");
        sl.put(FLOWERS_PLANTS, "Cvetje & Rastline");
        sl.put(PET_FOOD, "Hrana za hišne ljubljenčke");
        TRANSLATIONS.put("sl", sl);
        
        // Bulgarian (bg) translations
        Map<OfferCategory, String> bg = new HashMap<>();
        bg.put(ALL, "Всички");
        bg.put(MEALS, "Ястия");
        bg.put(BREAD_PASTRIES, "Хляб & Печива");
        bg.put(GROCERIES, "Хранителни стоки");
        bg.put(FLOWERS_PLANTS, "Цветя & Растения");
        bg.put(PET_FOOD, "Храна за домашни любимци");
        TRANSLATIONS.put("bg", bg);
        
        // Romanian (ro) translations
        Map<OfferCategory, String> ro = new HashMap<>();
        ro.put(ALL, "Toate");
        ro.put(MEALS, "Mese");
        ro.put(BREAD_PASTRIES, "Pâine & Produse de patiserie");
        ro.put(GROCERIES, "Alimente");
        ro.put(FLOWERS_PLANTS, "Flori & Plante");
        ro.put(PET_FOOD, "Hrană pentru animale de companie");
        TRANSLATIONS.put("ro", ro);
        
        // Macedonian (mk) translations
        Map<OfferCategory, String> mk = new HashMap<>();
        mk.put(ALL, "Сите");
        mk.put(MEALS, "Оброци");
        mk.put(BREAD_PASTRIES, "Леб & Печива");
        mk.put(GROCERIES, "Намирници");
        mk.put(FLOWERS_PLANTS, "Цвеќе & Растенија");
        mk.put(PET_FOOD, "Храна за домашни миленици");
        TRANSLATIONS.put("mk", mk);
    }
    
    OfferCategory(String displayNameEn) {
        this.displayNameEn = displayNameEn;
    }
    
    /**
     * Returns the translated display name for the given locale.
     * If locale is not supported or translation is missing, returns English name.
     * 
     * @param locale Locale code (e.g., "sr", "hr", "bg", "en"). 
     *               Can be full locale like "sr-RS" or just language code "sr".
     * @return Translated display name or English fallback
     */
    public String getDisplayName(String locale) {
        if (locale == null || locale.isEmpty()) {
            return displayNameEn;
        }
        
        // Extract language code (first 2 characters)
        // Handles both "sr" and "sr-RS" formats
        String lang = locale.toLowerCase();
        if (lang.length() > 2 && lang.contains("-")) {
            lang = lang.substring(0, 2);
        }
        
        Map<OfferCategory, String> translations = TRANSLATIONS.get(lang);
        
        if (translations != null && translations.containsKey(this)) {
            return translations.get(this);
        }
        
        // Fallback to English if locale not supported or translation missing
        return displayNameEn;
    }
    
    /**
     * Returns the English display name.
     * @return English display name
     */
    public String getDisplayName() {
        return displayNameEn;
    }
    
    /**
     * Gets all supported locale codes.
     * @return Array of supported locale codes
     */
    public static String[] getSupportedLocales() {
        return new String[]{"en", "sr", "hr", "me", "bs", "sl", "bg", "ro", "mk"};
    }
    
    /**
     * Checks if a locale is supported.
     * @param locale Locale code to check
     * @return true if locale is supported, false otherwise
     */
    public static boolean isLocaleSupported(String locale) {
        if (locale == null || locale.isEmpty()) {
            return false;
        }
        
        String lang = locale.toLowerCase();
        if (lang.length() > 2 && lang.contains("-")) {
            lang = lang.substring(0, 2);
        }
        
        return TRANSLATIONS.containsKey(lang);
    }
}

