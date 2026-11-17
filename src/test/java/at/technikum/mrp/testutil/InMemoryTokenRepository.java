package at.technikum.mrp.testutil;

import at.technikum.mrp.domain.AuthToken;
import at.technikum.mrp.persistence.TokenRepository;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryTokenRepository implements TokenRepository {
    private final Map<String, AuthToken> tokens = new LinkedHashMap<>();

    @Override
    public void save(AuthToken token) {
        tokens.put(token.token(), token);
    }

    @Override
    public Optional<AuthToken> findByToken(String token) {
        return Optional.ofNullable(tokens.get(token));
    }

    @Override
    public void delete(String token) {
        tokens.remove(token);
    }

    @Override
    public void deleteExpired(Instant now) {
        tokens.values().removeIf(token -> token.isExpired(now));
    }
}
