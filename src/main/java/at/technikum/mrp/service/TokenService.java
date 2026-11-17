package at.technikum.mrp.service;

import at.technikum.mrp.domain.AuthToken;
import at.technikum.mrp.persistence.TokenRepository;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

public final class TokenService {
    private static final int TOKEN_BYTES = 32;

    private final TokenRepository tokens;
    private final Duration timeToLive;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public TokenService(TokenRepository tokens, Duration timeToLive) {
        this(tokens, timeToLive, Clock.systemUTC());
    }

    public TokenService(TokenRepository tokens, Duration timeToLive, Clock clock) {
        this.tokens = tokens;
        this.timeToLive = timeToLive;
        this.clock = clock;
    }

    public AuthToken issue(long userId) {
        Instant now = clock.instant();
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        AuthToken token = new AuthToken(
                Base64.getUrlEncoder().withoutPadding().encodeToString(bytes),
                userId,
                now,
                now.plus(timeToLive)
        );
        tokens.save(token);
        return token;
    }

    public Optional<Long> validate(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Optional<AuthToken> stored = tokens.findByToken(token);
        if (stored.isEmpty()) {
            return Optional.empty();
        }
        AuthToken authToken = stored.get();
        if (authToken.isExpired(clock.instant())) {
            tokens.delete(token);
            return Optional.empty();
        }
        return Optional.of(authToken.userId());
    }
}
