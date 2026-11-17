package at.technikum.mrp.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;

public final class Database {
    @FunctionalInterface
    public interface SqlWork<T> {
        T run(Connection connection) throws SQLException;
    }

    private final String url;
    private final String user;
    private final String password;
    private final ThreadLocal<Connection> activeTransaction = new ThreadLocal<>();

    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public <T> T read(SqlWork<T> work) {
        Connection transaction = activeTransaction.get();
        if (transaction != null) {
            try {
                return work.run(transaction);
            } catch (SQLException ex) {
                throw new DataAccessException("Database operation failed", ex);
            }
        }
        try (Connection connection = connect()) {
            return work.run(connection);
        } catch (SQLException ex) {
            throw new DataAccessException("Database operation failed", ex);
        }
    }

    public <T> T inTransaction(SqlWork<T> work) {
        if (activeTransaction.get() != null) {
            return read(work);
        }
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            activeTransaction.set(connection);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (Exception ex) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    ex.addSuppressed(rollbackFailure);
                }
                if (ex instanceof RuntimeException runtime) {
                    throw runtime;
                }
                if (ex instanceof SQLException sql) {
                    throw new DataAccessException("Transaction failed", sql);
                }
                throw new DataAccessException("Transaction failed", ex);
            } finally {
                activeTransaction.remove();
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Could not open database transaction", ex);
        }
    }

    public void waitUntilAvailable(int attempts, Duration delay) throws Exception {
        for (int i = 0; i < attempts; i++) {
            try (Connection ignored = connect()) {
                return;
            } catch (SQLException e) {
                Thread.sleep(delay.toMillis());
            }
        }
        throw new IllegalStateException("PostgreSQL is not available at " + url);
    }
}
