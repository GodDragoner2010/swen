package at.technikum.mrp;

import at.technikum.mrp.config.AppConfig;
import at.technikum.mrp.http.ApiServer;
import at.technikum.mrp.http.Router;
import at.technikum.mrp.http.controller.AuthController;
import at.technikum.mrp.http.controller.HealthController;
import at.technikum.mrp.persistence.Database;
import at.technikum.mrp.persistence.SchemaInitializer;
import at.technikum.mrp.persistence.TokenRepository;
import at.technikum.mrp.persistence.UserRepository;
import at.technikum.mrp.persistence.jdbc.JdbcTokenRepository;
import at.technikum.mrp.persistence.jdbc.JdbcUserRepository;
import at.technikum.mrp.service.AuthService;
import at.technikum.mrp.service.PasswordHasher;
import at.technikum.mrp.service.Pbkdf2PasswordHasher;
import at.technikum.mrp.service.TokenService;

import java.time.Duration;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.fromEnvironment();

        Database database = new Database(config.dbUrl(), config.dbUser(), config.dbPassword());
        database.waitUntilAvailable(30, Duration.ofSeconds(2));
        new SchemaInitializer(database).initialize();

        UserRepository userRepository = new JdbcUserRepository(database);
        TokenRepository tokenRepository = new JdbcTokenRepository(database);
        PasswordHasher passwordHasher = new Pbkdf2PasswordHasher();
        TokenService tokenService = new TokenService(tokenRepository, config.tokenTtl());
        AuthService authService = new AuthService(userRepository, passwordHasher, tokenService);
        AuthController authController = new AuthController(authService);

        Router router = new Router();
        router.add("GET", "/api/health", Router.Access.PUBLIC, new HealthController()::health);
        router.add("POST", "/api/users/register", Router.Access.PUBLIC, authController::register);
        router.add("POST", "/api/users/login", Router.Access.PUBLIC, authController::login);

        ApiServer server = new ApiServer(config.port(), router);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();

        System.out.printf("MRP listening on http://localhost:%d/api%n", server.port());
        System.out.printf("Database: %s (user %s)%n", config.dbUrl(), config.dbUser());
    }
}
