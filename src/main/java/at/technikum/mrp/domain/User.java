package at.technikum.mrp.domain;

import java.time.Instant;

public record User(
        long id,
        String username,
        String passwordHash,
        String passwordSalt,
        String email,
        String favoriteGenre,
        Instant createdAt
) {
    public User withId(long newId) {
        return new User(newId, username, passwordHash, passwordSalt, email, favoriteGenre, createdAt);
    }
}
