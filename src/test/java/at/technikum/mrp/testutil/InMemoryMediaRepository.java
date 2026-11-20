package at.technikum.mrp.testutil;

import at.technikum.mrp.domain.MediaEntry;
import at.technikum.mrp.persistence.MediaRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryMediaRepository implements MediaRepository {
    private final Map<Long, MediaEntry> byId = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public MediaEntry create(MediaEntry media) {
        MediaEntry saved = media.withId(nextId++);
        byId.put(saved.id(), saved);
        return saved;
    }

    @Override
    public void update(MediaEntry media) {
        if (byId.containsKey(media.id())) {
            byId.put(media.id(), media);
        }
    }

    @Override
    public void delete(long id) {
        byId.remove(id);
    }

    @Override
    public Optional<MediaEntry> findById(long id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public List<MediaEntry> findAll() {
        return byId.values().stream()
                .sorted(Comparator.comparingLong(MediaEntry::id))
                .toList();
    }

    @Override
    public boolean exists(long id) {
        return byId.containsKey(id);
    }

    public List<MediaEntry> snapshot() {
        return new ArrayList<>(byId.values());
    }
}
