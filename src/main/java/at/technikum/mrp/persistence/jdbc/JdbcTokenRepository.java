package at.technikum.mrp.persistence.jdbc;

import at.technikum.mrp.domain.AuthToken;
import at.technikum.mrp.persistence.Database;
import at.technikum.mrp.persistence.TokenRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

public final class JdbcTokenRepository implements TokenRepository {
    private final Database database;

    public JdbcTokenRepository(Database database) {
        this.database = database;
    }

    @Override
    public void save(AuthToken token) {
        database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO auth_tokens (token, user_id, created_at, expires_at) VALUES (?, ?, ?, ?)")) {
                statement.setString(1, token.token());
                statement.setLong(2, token.userId());
                statement.setTimestamp(3, Timestamp.from(token.createdAt()));
                statement.setTimestamp(4, Timestamp.from(token.expiresAt()));
                statement.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public Optional<AuthToken> findByToken(String token) {
        return database.read(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT token, user_id, created_at, expires_at FROM auth_tokens WHERE token = ?")) {
                statement.setString(1, token);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new AuthToken(
                            result.getString("token"),
                            result.getLong("user_id"),
                            result.getTimestamp("created_at").toInstant(),
                            result.getTimestamp("expires_at").toInstant()
                    ));
                }
            }
        });
    }

    @Override
    public void delete(String token) {
        database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM auth_tokens WHERE token = ?")) {
                statement.setString(1, token);
                statement.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public void deleteExpired(Instant now) {
        database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM auth_tokens WHERE expires_at <= ?")) {
                statement.setTimestamp(1, Timestamp.from(now));
                statement.executeUpdate();
            }
            return null;
        });
    }
}
