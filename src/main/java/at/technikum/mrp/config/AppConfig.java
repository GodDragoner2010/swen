package at.technikum.mrp.config;

public final class AppConfig {
    private final int port;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    private AppConfig(int port, String dbUrl, String dbUser, String dbPassword) {
        this.port = port;
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    public static AppConfig fromEnvironment() {
        int port = Integer.parseInt(env("MRP_PORT", "8080"));
        String dbUrl = env("MRP_DB_URL", "jdbc:postgresql://localhost:5432/mrp");
        String dbUser = env("MRP_DB_USER", "mrp");
        String dbPassword = env("MRP_DB_PASSWORD", "mrp");
        return new AppConfig(port, dbUrl, dbUser, dbPassword);
    }

    private static String env(String name, String def) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? def : value;
    }

    public int port() {
        return port;
    }

    public String dbUrl() {
        return dbUrl;
    }

    public String dbUser() {
        return dbUser;
    }

    public String dbPassword() {
        return dbPassword;
    }
}
