package at.technikum.mrp.persistence;

import at.technikum.mrp.domain.Rating;

import java.util.Optional;

public interface RatingRepository {
    Rating create(Rating rating);

    void update(Rating rating);

    Optional<Rating> findById(long id);

    Optional<Rating> findByUserAndMedia(long userId, long mediaId);
}
