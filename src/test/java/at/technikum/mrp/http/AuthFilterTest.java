package at.technikum.mrp.http;

import at.technikum.mrp.service.AuthService;
import at.technikum.mrp.service.Pbkdf2PasswordHasher;
import at.technikum.mrp.service.TokenService;
import at.technikum.mrp.service.UnauthorizedException;
import at.technikum.mrp.testutil.InMemoryTokenRepository;
import at.technikum.mrp.testutil.InMemoryUserRepository;
import at.technikum.mrp.testutil.TestClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthFilterTest {
    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    private static final Duration TTL = Duration.ofHours(1);

    private InMemoryTokenRepository tokens;
    private TestClock clock;
    private AuthFilter filter;
    private String token;

    @BeforeEach
    void setUp() {
        tokens = new InMemoryTokenRepository();
        clock = new TestClock(NOW);
        TokenService tokenService = new TokenService(tokens, TTL, clock);
        AuthService authService = new AuthService(
                new InMemoryUserRepository(), new Pbkdf2PasswordHasher(1_000), tokenService, clock);
        filter = new AuthFilter(authService);
        token = tokenService.issue(42).token();
    }

    @Test
    void protectedRouteAcceptsValidBearerToken() {
        assertEquals(42L, filter.authenticate("Bearer " + token, Router.Access.PROTECTED));
    }

    @Test
    void bearerSchemeIsCaseInsensitiveAndTokenIsTrimmed() {
        assertEquals(42L, filter.authenticate("bearer   " + token + "  ", Router.Access.PROTECTED));
    }

    @Test
    void protectedRouteRequiresABearerHeader() {
        UnauthorizedException missing = assertThrows(UnauthorizedException.class,
                () -> filter.authenticate(null, Router.Access.PROTECTED));
        assertEquals("Authorization: Bearer <token> is required", missing.getMessage());

        assertThrows(UnauthorizedException.class,
                () -> filter.authenticate("", Router.Access.PROTECTED));
        assertThrows(UnauthorizedException.class,
                () -> filter.authenticate("Basic abc", Router.Access.PROTECTED));
        assertThrows(UnauthorizedException.class,
                () -> filter.authenticate("Bearer", Router.Access.PROTECTED));
        assertThrows(UnauthorizedException.class,
                () -> filter.authenticate("Bearer   ", Router.Access.PROTECTED));
    }

    @Test
    void protectedRouteRejectsUnknownToken() {
        UnauthorizedException failure = assertThrows(UnauthorizedException.class,
                () -> filter.authenticate("Bearer not-a-token", Router.Access.PROTECTED));

        assertEquals("Invalid or expired token", failure.getMessage());
    }

    @Test
    void protectedRouteRejectsExpiredToken() {
        clock.advance(TTL.plusSeconds(1));

        assertThrows(UnauthorizedException.class,
                () -> filter.authenticate("Bearer " + token, Router.Access.PROTECTED));
        assertTrue(tokens.findByToken(token).isEmpty());
    }

    @Test
    void publicRouteIgnoresAuthorizationHeader() {
        assertNull(filter.authenticate(null, Router.Access.PUBLIC));
        assertNull(filter.authenticate("Bearer not-a-token", Router.Access.PUBLIC));
    }

    @Test
    void optionalAuthResolvesValidTokenAndRejectsInvalidOne() {
        assertEquals(42L, filter.authenticate("Bearer " + token, Router.Access.OPTIONAL_AUTH));
        assertNull(filter.authenticate(null, Router.Access.OPTIONAL_AUTH));
        assertThrows(UnauthorizedException.class,
                () -> filter.authenticate("Bearer not-a-token", Router.Access.OPTIONAL_AUTH));
    }
}
