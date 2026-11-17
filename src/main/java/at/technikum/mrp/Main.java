package at.technikum.mrp;

import at.technikum.mrp.config.AppConfig;
import at.technikum.mrp.http.ApiServer;
import at.technikum.mrp.http.Router;
import at.technikum.mrp.http.controller.HealthController;
import at.technikum.mrp.persistence.Database;
import at.technikum.mrp.persistence.SchemaInitializer;

import java.time.Duration;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.fromEnvironment();

        Database database = new Database(config.dbUrl(), config.dbUser(), config.dbPassword());
        database.waitUntilAvailable(30, Duration.ofSeconds(2));
        new SchemaInitializer(database).initialize();

        Router router = new Router();
        router.add("GET", "/api/health", Router.Access.PUBLIC, new HealthController()::health);

        ApiServer server = new ApiServer(config.port(), router);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();

        System.out.printf("MRP listening on http://localhost:%d/api%n", server.port());
        System.out.printf("Database: %s (user %s)%n", config.dbUrl(), config.dbUser());
    }
}
