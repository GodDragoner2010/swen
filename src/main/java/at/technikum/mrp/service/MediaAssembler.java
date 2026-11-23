package at.technikum.mrp.service;

import at.technikum.mrp.domain.MediaEntry;
import at.technikum.mrp.domain.Rating;
import at.technikum.mrp.dto.MediaDetailResponse;
import at.technikum.mrp.dto.MediaSummaryResponse;
import at.technikum.mrp.dto.RatingResponse;
import at.technikum.mrp.util.Format;

import java.util.List;

/**
 * Converts {@link MediaEntry} domain objects into the read DTOs. The rating
 * aggregates ({@code averageScore}, {@code ratingCount}, {@code favoriteCount},
 * {@code favorite}, {@code ratings}) are zero/empty until ratings are
 * implemented.
 */
public final class MediaAssembler {

    public MediaSummaryResponse summary(MediaEntry media) {
        return new MediaSummaryResponse(
                media.id(),
                media.title(),
                media.description(),
                media.mediaType().jsonValue(),
                media.releaseYear(),
                media.genres(),
                media.ageRestriction(),
                media.creatorId(),
                media.creatorUsername(),
                Format.iso(media.createdAt()),
                0.0,
                0,
                0,
                false
        );
    }

    public MediaDetailResponse detail(MediaEntry media) {
        return new MediaDetailResponse(
                media.id(),
                media.title(),
                media.description(),
                media.mediaType().jsonValue(),
                media.releaseYear(),
                media.genres(),
                media.ageRestriction(),
                media.creatorId(),
                media.creatorUsername(),
                Format.iso(media.createdAt()),
                0.0,
                0,
                0,
                false,
                List.of()
        );
    }

    public RatingResponse ratingResponse(Rating rating, MediaEntry media, long viewerId, boolean likedByMe) {
        return new RatingResponse(
                rating.id(),
                rating.mediaId(),
                media == null ? null : media.title(),
                rating.userId(),
                rating.stars(),
                rating.commentVisibleTo(viewerId) ? rating.comment() : null,
                rating.commentConfirmed(),
                Format.iso(rating.createdAt()),
                Format.iso(rating.updatedAt()),
                rating.likeCount(),
                likedByMe
        );
    }
}
