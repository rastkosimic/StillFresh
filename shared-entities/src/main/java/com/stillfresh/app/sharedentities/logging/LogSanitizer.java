package com.stillfresh.app.sharedentities.logging;

/**
 * Redacts sensitive values before they are written to application logs.
 */
public final class LogSanitizer {

    private LogSanitizer() {
    }

    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "***";
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    public static String maskIban(String iban) {
        if (iban == null || iban.isBlank()) {
            return "***";
        }
        String normalized = iban.replaceAll("\\s+", "");
        if (normalized.length() < 8) {
            return "***";
        }
        return "***" + normalized.substring(normalized.length() - 4);
    }

    public static String maskPaymentReference(String reference) {
        if (reference == null || reference.isBlank()) {
            return "***";
        }
        if (reference.length() <= 8) {
            return "***";
        }
        return reference.substring(0, 4) + "…" + reference.substring(reference.length() - 4);
    }

    public static String roundCoordinate(double value) {
        return String.format("%.2f", value);
    }
}
