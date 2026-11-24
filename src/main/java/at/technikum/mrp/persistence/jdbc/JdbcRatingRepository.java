package at.technikum.mrp.persistence.jdbc;

import at.technikum.mrp.domain.Rating;
import at.technikum.mrp.persistence.Database;
import at.technikum.mrp.persistence.RatingRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

public final class JdbcRatingRepository implements RatingRepository {
    private static final String SELECT_RATING = """
            SELECT r.id, r.user_id, r.media_id, r.stars, r.comment, r.comment_confirmed,
                   r.created_at, r.updated_at,
                   (SELECT COUNT(*) FROM rating_likes rl WHERE rl.rating_id = r.id) AS like_count
            FROM ratings r
            """;

    private final Database database;

    public JdbcRatingRepository(Database database) {
        this.database = database;
    }

    @Override
    public Rating create(Rating rating) {
        return database.inTransaction(connection -> {
            String sql = """
                    INSERT INTO ratings (user_id, media_id, stars, comment, comment_confirmed, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    RETURNING id
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, rating.userId());
                statement.setLong(2, rating.mediaId());
                statement.setInt(3, rating.stars());
                statement.setString(4, rating.comment());
                statement.setBoolean(5, rating.commentConfirmed());
                statement.setTimestamp(6, Timestamp.from(rating.createdAt()));
                statement.setTimestamp(7, Timestamp.from(rating.updatedAt()));
                try (ResultSet result = statement.executeQuery()) {
                    result.next();
                    return rating.withId(result.getLong("id"));
                }
            }
        });
    }

    @Override
    public void update(Rating rating) {
        database.inTransaction(connection -> {
            String sql = """
                    UPDATE ratings
                    SET comment_confirmed = ?, updated_at = ?
                    WHERE id = ?
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setBoolean(1, rating.commentConfirmed());
                statement.setTimestamp(2, Timestamp.from(rating.updatedAt()));
                statement.setLong(3, rating.id());
                statement.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public Optional<Rating> findById(long id) {
        return database.read(connection -> {
            String sql = SELECT_RATING + " WHERE r.id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, id);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? Optional.of(map(result)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public Optional<Rating> findByUserAndMedia(long userId, long mediaId) {
        return database.read(connection -> {
            String sql = SELECT_RATING + " WHERE r.user_id = ? AND r.media_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, userId);
                statement.setLong(2, mediaId);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? Optional.of(map(result)) : Optional.empty();
                }
            }
        });
    }

    private Rating map(ResultSet result) throws SQLException {
        return new Rating(
                result.getLong("id"),
                result.getLong("user_id"),
                result.getLong("media_id"),
                result.getInt("stars"),
                result.getString("comment"),
                result.getBoolean("comment_confirmed"),
                result.getTimestamp("created_at").toInstant(),
                result.getTimestamp("updated_at").toInstant(),
                result.getInt("like_count")
        );
    }
}
