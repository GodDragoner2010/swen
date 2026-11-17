package at.technikum.mrp.service;

import at.technikum.mrp.domain.User;
import at.technikum.mrp.testutil.InMemoryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {
    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    private InMemoryUserRepository users;
    private PasswordHasher hasher;
    private AuthService service;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        hasher = new Pbkdf2PasswordHasher(1_000);
        service = new AuthService(users, hasher, Clock.fixed(NOW, ZoneOffset.UTC));
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
}
