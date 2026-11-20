package at.technikum.mrp.domain;

import java.time.Instant;
import java.util.List;

public record MediaEntry(
        long id,
        String title,
        String description,
        MediaType mediaType,
        int releaseYear,
        List<String> genres,
        int ageRestriction,
        long creatorId,
        String creatorUsername,
        Instant createdAt
) {
    public MediaEntry withId(long newId) {
        return new MediaEntry(newId, title, description, mediaType, releaseYear, genres,
                ageRestriction, creatorId, creatorUsername, createdAt);
    }
}
