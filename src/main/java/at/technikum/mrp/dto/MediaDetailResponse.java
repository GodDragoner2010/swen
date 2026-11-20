package at.technikum.mrp.dto;

import java.util.List;

public record MediaDetailResponse(
        long id,
        String title,
        String description,
        String mediaType,
        int releaseYear,
        List<String> genres,
        int ageRestriction,
        long creatorId,
        String creatorUsername,
        String createdAt,
        double averageScore,
        int ratingCount,
        int favoriteCount,
        boolean favorite,
        List<MediaRatingResponse> ratings
) {
}
