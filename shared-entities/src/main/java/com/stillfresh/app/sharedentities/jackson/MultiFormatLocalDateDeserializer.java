package com.stillfresh.app.sharedentities.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Accepts multiple date formats and parses them into a LocalDate.
 *
 * Supported:
 * - yyyy-MM-dd (ISO, recommended)
 * - dd-MM-yyyy
 * - dd/MM/yyyy
 * - yyyy/MM/dd
 * - yyyy.MM.dd
 *
 * Note: The canonical JSON output for LocalDate remains ISO yyyy-MM-dd.
 */
public class MultiFormatLocalDateDeserializer extends JsonDeserializer<LocalDate> {

    private static final List<DateTimeFormatter> FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,              // yyyy-MM-dd
            DateTimeFormatter.ofPattern("d-M-uuuu"),       // dd-MM-yyyy
            DateTimeFormatter.ofPattern("d/M/uuuu"),       // dd/MM/yyyy
            DateTimeFormatter.ofPattern("uuuu/M/d"),       // yyyy/MM/dd
            DateTimeFormatter.ofPattern("uuuu.M.d")        // yyyy.MM.dd
    );

    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        // Support Jackson timestamp array form: [yyyy,MM,dd]
        if (p.currentToken() == JsonToken.START_ARRAY) {
            p.nextToken();
            int year = p.getIntValue();
            p.nextToken();
            int month = p.getIntValue();
            p.nextToken();
            int day = p.getIntValue();
            // consume until END_ARRAY
            while (p.currentToken() != JsonToken.END_ARRAY) {
                p.nextToken();
            }
            return LocalDate.of(year, month, day);
        }

        String raw = p.getValueAsString();
        if (raw == null) return null;

        String s = raw.trim();
        if (s.isEmpty()) return null;

        // Accept ISO date-time variants by taking the first 10 chars (yyyy-MM-dd...)
        if (s.length() >= 10 && s.charAt(4) == '-' && s.charAt(7) == '-') {
            String first10 = s.substring(0, 10);
            try {
                return LocalDate.parse(first10, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException ignored) {
                // fall through
            }
        }

        for (DateTimeFormatter f : FORMATTERS) {
            try {
                return LocalDate.parse(s, f);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }

        throw new InvalidFormatException(
                p,
                "Invalid date format. Supported: yyyy-MM-dd, dd-MM-yyyy, dd/MM/yyyy, yyyy/MM/dd, yyyy.MM.dd",
                s,
                LocalDate.class
        );
    }
}


