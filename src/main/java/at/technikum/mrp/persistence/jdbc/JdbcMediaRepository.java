package at.technikum.mrp.persistence.jdbc;

import at.technikum.mrp.domain.MediaEntry;
import at.technikum.mrp.domain.MediaType;
import at.technikum.mrp.persistence.Database;
import at.technikum.mrp.persistence.MediaRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class JdbcMediaRepository implements MediaRepository {
    private static final String SELECT_MEDIA = """
            SELECT m.id, m.title, m.description, m.media_type, m.release_year, m.age_restriction,
                   m.creator_id, u.username AS creator_username, m.created_at
            FROM media m
            JOIN users u ON u.id = m.creator_id
            """;

    private final Database database;

    public JdbcMediaRepository(Database database) {
        this.database = database;
    }

    @Override
    public MediaEntry create(MediaEntry media) {
        return database.inTransaction(connection -> {
            long id;
            String sql = """
                    INSERT INTO media (title, description, media_type, release_year, age_restriction, creator_id, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    RETURNING id
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, media.title());
                statement.setString(2, media.description());
                statement.setString(3, media.mediaType().name());
                statement.setInt(4, media.releaseYear());
                statement.setInt(5, media.ageRestriction());
                statement.setLong(6, media.creatorId());
                statement.setTimestamp(7, Timestamp.from(media.createdAt()));
                try (ResultSet result = statement.executeQuery()) {
                    result.next();
                    id = result.getLong("id");
                }
            }
            insertGenres(connection, id, media.genres());
            return media.withId(id);
        });
    }

    @Override
    public void update(MediaEntry media) {
        database.inTransaction(connection -> {
            String sql = """
                    UPDATE media
                    SET title = ?, description = ?, media_type = ?, release_year = ?, age_restriction = ?
                    WHERE id = ?
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, media.title());
                statement.setString(2, media.description());
                statement.setString(3, media.mediaType().name());
                statement.setInt(4, media.releaseYear());
                statement.setInt(5, media.ageRestriction());
                statement.setLong(6, media.id());
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM media_genres WHERE media_id = ?")) {
                statement.setLong(1, media.id());
                statement.executeUpdate();
            }
            insertGenres(connection, media.id(), media.genres());
            return null;
        });
    }

    @Override
    public void delete(long id) {
        database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM media WHERE id = ?")) {
                statement.setLong(1, id);
                statement.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public Optional<MediaEntry> findById(long id) {
        return database.read(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(SELECT_MEDIA + " WHERE m.id = ?")) {
                statement.setLong(1, id);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        return Optional.empty();
                    }
                    List<String> genres = loadGenres(connection, id);
                    return Optional.of(map(result, genres));
                }
            }
        });
    }

    @Override
    public List<MediaEntry> findAll() {
        return database.read(connection -> {
            Map<Long, List<String>> genres = loadAllGenres(connection);
            List<MediaEntry> media = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(SELECT_MEDIA + " ORDER BY m.id");
                 ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    long id = result.getLong("id");
                    media.add(map(result, genres.getOrDefault(id, List.of())));
                }
            }
            return media;
        });
    }

    @Override
    public boolean exists(long id) {
        return database.read(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM media WHERE id = ?")) {
                statement.setLong(1, id);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next();
                }
            }
        });
    }

    private void insertGenres(Connection connection, long mediaId, List<String> genres) throws SQLException {
        if (genres == null || genres.isEmpty()) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO media_genres (media_id, genre) VALUES (?, ?) ON CONFLICT DO NOTHING")) {
            for (String genre : genres) {
                statement.setLong(1, mediaId);
                statement.setString(2, genre);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private List<String> loadGenres(Connection connection, long mediaId) throws SQLException {
        List<String> genres = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT genre FROM media_genres WHERE media_id = ? ORDER BY genre")) {
            statement.setLong(1, mediaId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    genres.add(result.getString("genre"));
                }
            }
        }
        return genres;
    }

    private Map<Long, List<String>> loadAllGenres(Connection connection) throws SQLException {
        Map<Long, List<String>> genres = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT media_id, genre FROM media_genres ORDER BY media_id, genre");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                genres.computeIfAbsent(result.getLong("media_id"), ignored -> new ArrayList<>())
                        .add(result.getString("genre"));
            }
        }
        return genres;
    }

    private MediaEntry map(ResultSet result, List<String> genres) throws SQLException {
        return new MediaEntry(
                result.getLong("id"),
                result.getString("title"),
                result.getString("description"),
                MediaType.valueOf(result.getString("media_type")),
                result.getInt("release_year"),
                List.copyOf(genres),
                result.getInt("age_restriction"),
                result.getLong("creator_id"),
                result.getString("creator_username"),
                result.getTimestamp("created_at").toInstant()
        );
    }
}
