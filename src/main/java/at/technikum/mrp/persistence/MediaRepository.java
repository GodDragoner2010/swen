package at.technikum.mrp.persistence;

import at.technikum.mrp.domain.MediaEntry;

import java.util.List;
import java.util.Optional;

public interface MediaRepository {
    MediaEntry create(MediaEntry media);

    void update(MediaEntry media);

    void delete(long id);

    Optional<MediaEntry> findById(long id);

    List<MediaEntry> findAll();

    boolean exists(long id);
}
