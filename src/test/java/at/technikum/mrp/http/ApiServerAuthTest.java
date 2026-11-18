package at.technikum.mrp.http;

import at.technikum.mrp.service.AuthService;
import at.technikum.mrp.service.Pbkdf2PasswordHasher;
import at.technikum.mrp.service.TokenService;
import at.technikum.mrp.testutil.InMemoryTokenRepository;
import at.technikum.mrp.testutil.InMemoryUserRepository;
import at.technikum.mrp.testutil.TestClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiServerAuthTest {
    private ApiServer server;
    private TokenService tokenService;
    private HttpClient client;
    private String baseUrl;

    @BeforeEach
    void setUp() throws Exception {
        InMemoryTokenRepository tokens = new InMemoryTokenRepository();
        TestClock clock = new TestClock(Instant.parse("2026-01-15T10:00:00Z"));
        tokenService = new TokenService(tokens, Duration.ofHours(1), clock);
        AuthService authService = new AuthService(
                new InMemoryUserRepository(), new Pbkdf2PasswordHasher(1_000), tokenService, clock);

        Router router = new Router();
        router.add("GET", "/api/public", Router.Access.PUBLIC,
                context -> HttpResponse.ok(Map.of("status", "ok")));
        router.add("GET", "/api/private", Router.Access.PROTECTED,
                context -> HttpResponse.ok(Map.of("userId", context.requireUserId())));

        server = new ApiServer(0, router, authService);
        server.start();
        baseUrl = "http://localhost:" + server.port();
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void publicRouteNeedsNoToken() throws Exception {
        assertEquals(200, get("/api/public", null).statusCode());
    }

    @Test
    void protectedRouteWithoutTokenReturns401WithBearerChallenge() throws Exception {
        java.net.http.HttpResponse<String> response = get("/api/private", null);

        assertEquals(401, response.statusCode());
        assertEquals("Bearer", response.headers().firstValue("WWW-Authenticate").orElse(null));
        assertTrue(response.body().contains("Authorization: Bearer <token> is required"));
    }

    @Test
    void protectedRouteRejectsInvalidToken() throws Exception {
        assertEquals(401, get("/api/private", "Bearer not-a-token").statusCode());
    }

    @Test
    void protectedRouteAcceptsIssuedTokenAndExposesUserId() throws Exception {
        String token = tokenService.issue(42).token();

        java.net.http.HttpResponse<String> response = get("/api/private", "Bearer " + token);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"userId\":42"));
    }

    private java.net.http.HttpResponse<String> get(String path, String authorization) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path)).GET();
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        return client.send(request.build(), java.net.http.HttpResponse.BodyHandlers.ofString());
    }
}
