package at.technikum.mrp.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Duration;

public final class Database {
    private final String url;
    private final String user;
    private final String password;

    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public Connection connect() throws Exception {
        return DriverManager.getConnection(url, user, password);
    }

    public void waitUntilAvailable(int attempts, Duration delay) throws Exception {
        for (int i = 0; i < attempts; i++) {
            try (Connection ignored = connect()) {
                return;
            } catch (Exception e) {
                Thread.sleep(delay.toMillis());
            }
        }
        throw new IllegalStateException("PostgreSQL is not available at " + url);
    }
}
