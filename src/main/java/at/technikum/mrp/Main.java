package at.technikum.mrp;

import at.technikum.mrp.config.AppConfig;
import at.technikum.mrp.http.ApiServer;
import at.technikum.mrp.http.Router;
import at.technikum.mrp.http.controller.AuthController;
import at.technikum.mrp.http.controller.HealthController;
import at.technikum.mrp.http.controller.MediaController;
import at.technikum.mrp.persistence.Database;
import at.technikum.mrp.persistence.MediaRepository;
import at.technikum.mrp.persistence.SchemaInitializer;
import at.technikum.mrp.persistence.TokenRepository;
import at.technikum.mrp.persistence.UserRepository;
import at.technikum.mrp.persistence.jdbc.JdbcMediaRepository;
import at.technikum.mrp.persistence.jdbc.JdbcTokenRepository;
import at.technikum.mrp.persistence.jdbc.JdbcUserRepository;
import at.technikum.mrp.service.AuthService;
import at.technikum.mrp.service.MediaAssembler;
import at.technikum.mrp.service.MediaService;
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
        MediaRepository mediaRepository = new JdbcMediaRepository(database);
        TokenRepository tokenRepository = new JdbcTokenRepository(database);
        PasswordHasher passwordHasher = new Pbkdf2PasswordHasher();
        TokenService tokenService = new TokenService(tokenRepository, config.tokenTtl());
        AuthService authService = new AuthService(userRepository, passwordHasher, tokenService);
        AuthController authController = new AuthController(authService);
        MediaService mediaService = new MediaService(mediaRepository, userRepository, new MediaAssembler());
        MediaController mediaController = new MediaController(mediaService);

        Router router = new Router();
        router.add("GET", "/api/health", Router.Access.PUBLIC, new HealthController()::health);
        router.add("POST", "/api/users/register", Router.Access.PUBLIC, authController::register);
        router.add("POST", "/api/users/login", Router.Access.PUBLIC, authController::login);
        router.add("GET", "/api/media", Router.Access.OPTIONAL_AUTH, mediaController::list);
        router.add("POST", "/api/media", Router.Access.PROTECTED, mediaController::create);
        router.add("GET", "/api/media/{mediaId}", Router.Access.OPTIONAL_AUTH, mediaController::get);
        router.add("PUT", "/api/media/{mediaId}", Router.Access.PROTECTED, mediaController::update);
        router.add("DELETE", "/api/media/{mediaId}", Router.Access.PROTECTED, mediaController::delete);

        ApiServer server = new ApiServer(config.port(), router, authService);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();

        System.out.printf("MRP listening on http://localhost:%d/api%n", server.port());
        System.out.printf("Database: %s (user %s)%n", config.dbUrl(), config.dbUser());
    }
}
