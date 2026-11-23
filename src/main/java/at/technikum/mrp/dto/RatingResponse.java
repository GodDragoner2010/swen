package at.technikum.mrp.dto;

public record RatingResponse(
        long id,
        long mediaId,
        String mediaTitle,
        long userId,
        int stars,
        String comment,
        boolean commentConfirmed,
        String createdAt,
        String updatedAt,
        int likeCount,
        boolean likedByMe
) {
}
