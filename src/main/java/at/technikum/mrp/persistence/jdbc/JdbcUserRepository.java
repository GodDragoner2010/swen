package at.technikum.mrp.persistence.jdbc;

import at.technikum.mrp.domain.User;
import at.technikum.mrp.persistence.Database;
import at.technikum.mrp.persistence.UserRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Optional;

public final class JdbcUserRepository implements UserRepository {
    private final Database database;

    public JdbcUserRepository(Database database) {
        this.database = database;
    }

    @Override
    public User create(User user) {
        return database.inTransaction(connection -> {
            String sql = """
                    INSERT INTO users (username, password_hash, password_salt, email, favorite_genre, created_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    RETURNING id
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, user.username());
                statement.setString(2, user.passwordHash());
                statement.setString(3, user.passwordSalt());
                statement.setString(4, user.email());
                statement.setString(5, user.favoriteGenre());
                statement.setTimestamp(6, Timestamp.from(user.createdAt()));
                try (ResultSet result = statement.executeQuery()) {
                    result.next();
                    return user.withId(result.getLong("id"));
                }
            }
        });
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return database.read(connection -> {
            String sql = """
                    SELECT id, username, password_hash, password_salt, email, favorite_genre, created_at
                    FROM users WHERE username = ?
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, username);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new User(
                            result.getLong("id"),
                            result.getString("username"),
                            result.getString("password_hash"),
                            result.getString("password_salt"),
                            result.getString("email"),
                            result.getString("favorite_genre"),
                            result.getTimestamp("created_at").toInstant()
                    ));
                }
            }
        });
    }

    @Override
    public boolean existsByUsername(String username) {
        return database.read(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT 1 FROM users WHERE username = ?")) {
                statement.setString(1, username);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next();
                }
            }
        });
    }
}
