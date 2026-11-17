package at.technikum.mrp.util;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public final class Format {
    private Format() {
    }

    public static String iso(Instant instant) {
        return instant == null ? null : DateTimeFormatter.ISO_INSTANT.format(instant);
    }
}
