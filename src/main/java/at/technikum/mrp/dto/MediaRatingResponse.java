package at.technikum.mrp.dto;

public record MediaRatingResponse(
        long id,
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
