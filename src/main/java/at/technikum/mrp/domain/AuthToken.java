package at.technikum.mrp.domain;

import java.time.Instant;

public record AuthToken(
        String token,
        long userId,
        Instant createdAt,
        Instant expiresAt
) {
    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }
}
