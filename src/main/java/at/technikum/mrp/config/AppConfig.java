package at.technikum.mrp.config;

import java.time.Duration;

public record AppConfig(
        int port,
        String dbUrl,
        String dbUser,
        String dbPassword,
        Duration tokenTtl
) {
    public static AppConfig fromEnvironment() {
        int port = intEnv("MRP_PORT", 8080);
        String dbUrl = env("MRP_DB_URL", "jdbc:postgresql://localhost:5432/mrp");
        String dbUser = env("MRP_DB_USER", "mrp");
        String dbPassword = env("MRP_DB_PASSWORD", "mrp");
        long ttlMinutes = longEnv("MRP_TOKEN_TTL_MINUTES", 1440);
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("MRP_PORT must be between 0 and 65535");
        }
        if (ttlMinutes < 1) {
            throw new IllegalArgumentException("MRP_TOKEN_TTL_MINUTES must be positive");
        }
        return new AppConfig(port, dbUrl, dbUser, dbPassword, Duration.ofMinutes(ttlMinutes));
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int intEnv(String name, int fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : Integer.parseInt(value.trim());
    }

    private static long longEnv(String name, long fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : Long.parseLong(value.trim());
    }
}
