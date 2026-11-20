package at.technikum.mrp.testutil;

import at.technikum.mrp.domain.MediaEntry;
import at.technikum.mrp.domain.MediaType;
import at.technikum.mrp.domain.User;

import java.time.Instant;
import java.util.List;

public final class TestFixtures {
    public static final Instant NOW = Instant.parse("2025-01-01T12:00:00Z");

    private TestFixtures() {
    }

    public static User user(long id, String username) {
        return new User(id, username, "hash", "salt", null, null, NOW);
    }

    public static MediaEntry media(long id,
                                    String title,
                                    MediaType type,
                                    int releaseYear,
                                    int ageRestriction,
                                    long creatorId,
                                    String... genres) {
        return new MediaEntry(
                id,
                title,
                "Description of " + title,
                type,
                releaseYear,
                List.of(genres),
                ageRestriction,
                creatorId,
                "creator" + creatorId,
                NOW
        );
    }
}
