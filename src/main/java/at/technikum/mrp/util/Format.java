package at.technikum.mrp.util;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public final class Format {
    private Format() {
    }

    public static String iso(Instant instant) {
        return instant == null ? null : DateTimeFormatter.ISO_INSTANT.format(instant);
    }

    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
