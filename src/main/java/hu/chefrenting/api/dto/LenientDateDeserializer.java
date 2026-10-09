package hu.chefrenting.api.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/**
 * Accepts "2025-03-10" and also date-time strings such as "2025-03-10T00:00:00" or
 * "2025-03-10T00:00:00Z"; only the date part is used (rentals are counted in whole days).
 */
public class LenientDateDeserializer extends JsonDeserializer<LocalDate> {
    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getValueAsString();
        if (text == null || text.isBlank()) {
            return null;
        }
        text = text.trim();
        try {
            if (text.length() <= 10) {
                return LocalDate.parse(text);
            }
            try {
                return LocalDateTime.parse(text).toLocalDate();
            } catch (DateTimeParseException e) {
                return OffsetDateTime.parse(text).toLocalDate();
            }
        } catch (DateTimeParseException e) {
            throw ctxt.weirdStringException(text, LocalDate.class, "érvénytelen dátumformátum (várt: yyyy-MM-dd)");
        }
    }
}
