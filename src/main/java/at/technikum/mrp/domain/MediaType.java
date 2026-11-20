package at.technikum.mrp.domain;

import java.util.Locale;
import java.util.Optional;

public enum MediaType {
    MOVIE,
    SERIES,
    GAME;

    public String jsonValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<MediaType> fromString(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(MediaType.valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
