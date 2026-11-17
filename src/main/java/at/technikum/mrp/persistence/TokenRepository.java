package at.technikum.mrp.persistence;

import at.technikum.mrp.domain.AuthToken;

import java.time.Instant;
import java.util.Optional;

public interface TokenRepository {
    void save(AuthToken token);

    Optional<AuthToken> findByToken(String token);

    void delete(String token);

    void deleteExpired(Instant now);
}
