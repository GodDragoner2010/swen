package at.technikum.mrp.testutil;

import at.technikum.mrp.domain.Rating;
import at.technikum.mrp.persistence.RatingRepository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryRatingRepository implements RatingRepository {
    private final Map<Long, Rating> byId = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public Rating create(Rating rating) {
        Rating saved = rating.withId(nextId++);
        byId.put(saved.id(), saved);
        return saved;
    }

    @Override
    public void delete(long id) {
        byId.remove(id);
    }

    @Override
    public Optional<Rating> findByUserAndMedia(long userId, long mediaId) {
        return byId.values().stream()
                .filter(rating -> rating.userId() == userId && rating.mediaId() == mediaId)
                .findFirst();
    }

    @Override
    public Optional<Rating> findById(long id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public void update(Rating rating) {
        byId.put(rating.id(), rating);
    }
}
