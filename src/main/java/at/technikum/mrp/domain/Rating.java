package at.technikum.mrp.domain;

import java.time.Instant;

public record Rating(
        long id,
        long userId,
        long mediaId,
        int stars,
        String comment,
        boolean commentConfirmed,
        Instant createdAt,
        Instant updatedAt,
        int likeCount
) {
    public Rating withId(long newId) {
        return new Rating(newId, userId, mediaId, stars, comment, commentConfirmed,
                createdAt, updatedAt, likeCount);
    }

    public Rating withLikeCount(int newLikeCount) {
        return new Rating(id, userId, mediaId, stars, comment, commentConfirmed,
                createdAt, updatedAt, newLikeCount);
    }

    public boolean hasComment() {
        return comment != null && !comment.isBlank();
    }

    public boolean commentVisibleTo(long viewerId) {
        return hasComment() && (commentConfirmed || userId == viewerId);
    }
}
