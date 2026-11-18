package at.technikum.mrp.service;

import at.technikum.mrp.domain.User;
import at.technikum.mrp.testutil.InMemoryTokenRepository;
import at.technikum.mrp.testutil.InMemoryUserRepository;
import at.technikum.mrp.testutil.TestClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {
    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    private InMemoryUserRepository users;
    private InMemoryTokenRepository tokens;
    private TestClock clock;
    private PasswordHasher hasher;
    private AuthService service;
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        tokens = new InMemoryTokenRepository();
        hasher = new Pbkdf2PasswordHasher(1_000);
        clock = new TestClock(NOW);
        tokenService = new TokenService(tokens, Duration.ofHours(1), clock);
        service = new AuthService(users, hasher, tokenService, clock);
    }

    @Test
    void registersUserWithTrimmedUsernameAndHashedPassword() {
        User user = service.register("  alice  ", "secret123");

        assertEquals("alice", user.username());
        assertNotEquals("secret123", user.passwordHash());
        assertFalse(user.passwordSalt().isBlank());
        assertTrue(hasher.matches("secret123", user.passwordSalt(), user.passwordHash()));
    }

    @Test
    void assignsIdAndCreatedAtFromClock() {
        User user = service.register("alice", "secret123");

        assertTrue(user.id() > 0);
        assertEquals(NOW, user.createdAt());
    }

    @Test
    void rejectsDuplicateUsername() {
        service.register("alice", "secret123");

        assertThrows(ConflictException.class, () -> service.register("alice", "another1"));
    }

    @Test
    void rejectsDuplicateUsernameWithDifferentWhitespace() {
        service.register("alice", "secret123");

        assertThrows(ConflictException.class, () -> service.register("  alice  ", "another1"));
    }

    @Test
    void rejectsTooShortPassword() {
        assertThrows(ValidationException.class, () -> service.register("alice", "123"));
    }

    @Test
    void rejectsTooLongPassword() {
        assertThrows(ValidationException.class, () -> service.register("alice", "x".repeat(101)));
    }

    @Test
    void rejectsUsernameWithInvalidCharacters() {
        assertThrows(ValidationException.class, () -> service.register("bad user", "secret123"));
    }

    @Test
    void rejectsTooShortUsername() {
        assertThrows(ValidationException.class, () -> service.register("ab", "secret123"));
    }

    @Test
    void rejectsMissingCredentials() {
        assertThrows(ValidationException.class, () -> service.register(null, "secret123"));
        assertThrows(ValidationException.class, () -> service.register("alice", null));
    }

    @Test
    void usesUniqueSaltForEachRegisteredUser() {
        User alice = service.register("alice", "secret123");
        User bob = service.register("bob", "secret123");

        assertNotEquals(alice.passwordSalt(), bob.passwordSalt());
        assertNotEquals(alice.passwordHash(), bob.passwordHash());
    }

    @Test
    void loginReturnsUserAndValidTokenForCorrectCredentials() {
        User registered = service.register("alice", "secret123");

        LoginResult result = service.login("alice", "secret123");

        assertEquals(registered.id(), result.user().id());
        assertEquals("alice", result.user().username());
        assertFalse(result.token().token().isBlank());
        assertEquals(NOW.plus(Duration.ofHours(1)), result.token().expiresAt());
        assertEquals(registered.id(), tokenService.validate(result.token().token()).orElseThrow());
    }

    @Test
    void loginTrimsUsername() {
        service.register("alice", "secret123");

        assertEquals("alice", service.login("  alice  ", "secret123").user().username());
    }

    @Test
    void loginRejectsWrongPassword() {
        service.register("alice", "secret123");

        assertThrows(UnauthorizedException.class, () -> service.login("alice", "wrong123"));
    }

    @Test
    void loginRejectsUnknownUser() {
        assertThrows(UnauthorizedException.class, () -> service.login("nobody", "secret123"));
    }

    @Test
    void loginRejectsMissingCredentials() {
        service.register("alice", "secret123");

        assertThrows(UnauthorizedException.class, () -> service.login(null, "secret123"));
        assertThrows(UnauthorizedException.class, () -> service.login("alice", null));
        assertThrows(UnauthorizedException.class, () -> service.login("  ", "secret123"));
        assertThrows(UnauthorizedException.class, () -> service.login("alice", ""));
    }

    @Test
    void loginTreatsMalformedUsernameAsInvalidCredentials() {
        service.register("alice", "secret123");

        assertThrows(UnauthorizedException.class, () -> service.login("bad user", "secret123"));
    }

    @Test
    void authenticateResolvesUserIdOnlyForValidTokens() {
        User registered = service.register("alice", "secret123");
        String issued = service.login("alice", "secret123").token().token();

        assertEquals(Optional.of(registered.id()), service.authenticate(issued));
        assertEquals(Optional.empty(), service.authenticate("unknown"));
        assertEquals(Optional.empty(), service.authenticate(null));
    }
}
