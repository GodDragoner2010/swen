package at.technikum.mrp.service;

import at.technikum.mrp.domain.AuthToken;
import at.technikum.mrp.testutil.InMemoryTokenRepository;
import at.technikum.mrp.testutil.TestClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenServiceTest {
    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(30);

    private InMemoryTokenRepository tokens;
    private TestClock clock;
    private TokenService service;

    @BeforeEach
    void setUp() {
        tokens = new InMemoryTokenRepository();
        clock = new TestClock(NOW);
        service = new TokenService(tokens, TTL, clock);
    }

    @Test
    void issuesAnUnexpiredTokenStoredForTheUser() {
        AuthToken token = service.issue(42);

        assertFalse(token.token().isBlank());
        assertEquals(42, token.userId());
        assertEquals(NOW, token.createdAt());
        assertEquals(NOW.plus(TTL), token.expiresAt());
        assertTrue(tokens.findByToken(token.token()).isPresent());
    }

    @Test
    void issuesDistinctTokensOnEachIssue() {
        assertNotEquals(service.issue(1).token(), service.issue(1).token());
    }

    @Test
    void validatesAnExistingToken() {
        AuthToken token = service.issue(7);

        assertEquals(Optional.of(7L), service.validate(token.token()));
    }

    @Test
    void rejectsAndDeletesAnExpiredToken() {
        AuthToken token = service.issue(7);
        clock.advance(TTL.plusSeconds(1));

        assertTrue(service.validate(token.token()).isEmpty());
        assertTrue(tokens.findByToken(token.token()).isEmpty());
    }

    @Test
    void rejectsUnknownAndBlankTokens() {
        assertTrue(service.validate("does-not-exist").isEmpty());
        assertTrue(service.validate("").isEmpty());
        assertTrue(service.validate(null).isEmpty());
    }
}
