package at.technikum.mrp.service;

import at.technikum.mrp.domain.MediaEntry;
import at.technikum.mrp.domain.Rating;
import at.technikum.mrp.dto.RatingInput;
import at.technikum.mrp.dto.RatingResponse;
import at.technikum.mrp.persistence.MediaRepository;
import at.technikum.mrp.persistence.RatingRepository;

import java.time.Instant;
import java.util.Objects;

public final class RatingService {
    private static final int COMMENT_MAX_LENGTH = 1000;

    private final MediaRepository media;
    private final RatingRepository ratings;
    private final MediaAssembler assembler;

    public RatingService(MediaRepository media, RatingRepository ratings, MediaAssembler assembler) {
        this.media = media;
        this.ratings = ratings;
        this.assembler = assembler;
    }

    public RatingResponse rate(long userId, long mediaId, RatingInput input) {
        MediaEntry entry = requireMedia(mediaId);
        if (input == null) {
            throw new ValidationException("Request body is required");
        }
        int stars = validateStars(input.stars());
        String comment = normalizeComment(input.comment());
        if (ratings.findByUserAndMedia(userId, mediaId).isPresent()) {
            throw new ConflictException("You have already rated this media entry; update the existing rating");
        }
        Instant now = Instant.now();
        Rating rating = new Rating(
                0,
                userId,
                mediaId,
                stars,
                comment,
                comment == null,
                now,
                now,
                0
        );
        Rating created = ratings.create(rating);
        return assembler.ratingResponse(created, entry, userId, false);
    }

    public RatingResponse update(long userId, long ratingId, RatingInput input) {
        Rating existing = requireRating(ratingId);
        requireOwner(existing, userId, "You may only edit your own ratings");
        if (input == null) {
            throw new ValidationException("Request body is required");
        }
        int stars = validateStars(input.stars());
        String comment = normalizeComment(input.comment());
        boolean commentChanged = !Objects.equals(existing.comment(), comment);
        boolean confirmed = comment == null || (commentChanged ? false : existing.commentConfirmed());
        Rating updated = new Rating(
                existing.id(),
                existing.userId(),
                existing.mediaId(),
                stars,
                comment,
                confirmed,
                existing.createdAt(),
                Instant.now(),
                existing.likeCount()
        );
        ratings.update(updated);
        MediaEntry entry = media.findById(updated.mediaId()).orElse(null);
        return assembler.ratingResponse(updated, entry, userId, false);
    }

    public void delete(long userId, long ratingId) {
        Rating existing = requireRating(ratingId);
        requireOwner(existing, userId, "You may only delete your own ratings");
        ratings.delete(ratingId);
    }

    public RatingResponse confirm(long userId, long ratingId) {
        Rating existing = requireRating(ratingId);
        requireOwner(existing, userId, "You may only confirm your own rating comments");
        if (!existing.hasComment()) {
            throw new ValidationException("Rating has no comment to confirm");
        }
        if (existing.commentConfirmed()) {
            MediaEntry entry = media.findById(existing.mediaId()).orElse(null);
            return assembler.ratingResponse(existing, entry, userId, false);
        }
        Rating updated = new Rating(
                existing.id(),
                existing.userId(),
                existing.mediaId(),
                existing.stars(),
                existing.comment(),
                true,
                existing.createdAt(),
                Instant.now(),
                existing.likeCount()
        );
        ratings.update(updated);
        MediaEntry entry = media.findById(updated.mediaId()).orElse(null);
        return assembler.ratingResponse(updated, entry, userId, false);
    }

    private int validateStars(Integer stars) {
        if (stars == null || stars < 1 || stars > 5) {
            throw new ValidationException("Stars must be an integer between 1 and 5");
        }
        return stars;
    }

    private String normalizeComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }
        String normalized = comment.trim();
        if (normalized.length() > COMMENT_MAX_LENGTH) {
            throw new ValidationException("Comment must not exceed " + COMMENT_MAX_LENGTH + " characters");
        }
        return normalized;
    }

    private MediaEntry requireMedia(long mediaId) {
        return media.findById(mediaId)
                .orElseThrow(() -> new NotFoundException("Media entry not found"));
    }

    private Rating requireRating(long ratingId) {
        return ratings.findById(ratingId)
                .orElseThrow(() -> new NotFoundException("Rating not found"));
    }

    private void requireOwner(Rating rating, long userId, String message) {
        if (rating.userId() != userId) {
            throw new ForbiddenException(message);
        }
    }
}
