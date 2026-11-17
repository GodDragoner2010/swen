package at.technikum.mrp.persistence.jdbc;

import at.technikum.mrp.domain.User;
import at.technikum.mrp.persistence.Database;
import at.technikum.mrp.persistence.UserRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

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
