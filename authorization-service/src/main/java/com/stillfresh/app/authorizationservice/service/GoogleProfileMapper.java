package com.stillfresh.app.authorizationservice.service;

import java.util.Map;

/**
 * Maps Google ID token / OAuth2 user info claims into StillFresh profile fields.
 */
final class GoogleProfileMapper {

    private GoogleProfileMapper() {
    }

    record GoogleProfile(String firstName, String lastName, String country) {
    }

    static GoogleProfile fromGoogleUserInfo(Map<String, Object> googleUserInfo) {
        String givenName = asNonBlankString(googleUserInfo.get("given_name"));
        String familyName = asNonBlankString(googleUserInfo.get("family_name"));
        String fullName = asNonBlankString(googleUserInfo.get("name"));
        String locale = asNonBlankString(googleUserInfo.get("locale"));

        String firstName = givenName;
        String lastName = familyName;

        if (firstName == null && fullName != null) {
            int space = fullName.indexOf(' ');
            if (space > 0) {
                firstName = fullName.substring(0, space).trim();
                if (lastName == null) {
                    lastName = fullName.substring(space + 1).trim();
                }
            } else {
                firstName = fullName;
            }
        }

        return new GoogleProfile(firstName, lastName, localeToCountryCode(locale));
    }

    private static String asNonBlankString(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * Extracts a region/country code from Google's locale claim (e.g. {@code en-US} -> {@code US}).
     */
    static String localeToCountryCode(String locale) {
        if (locale == null || locale.isBlank()) {
            return null;
        }
        String normalized = locale.trim().replace('-', '_');
        int separator = normalized.lastIndexOf('_');
        if (separator >= 0 && separator < normalized.length() - 1) {
            String region = normalized.substring(separator + 1);
            if (region.length() == 2 && region.chars().allMatch(Character::isLetter)) {
                return region.toUpperCase();
            }
        }
        return null;
    }
}
