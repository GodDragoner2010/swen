package at.technikum.mrp.testutil;

import at.technikum.mrp.domain.User;
import at.technikum.mrp.persistence.UserRepository;

import java.util.LinkedHashMap;
import java.util.Map;

public final class InMemoryUserRepository implements UserRepository {
    private final Map<Long, User> byId = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public User create(User user) {
        User saved = user.withId(nextId++);
        byId.put(saved.id(), saved);
        return saved;
    }

    @Override
    public boolean existsByUsername(String username) {
        return byId.values().stream().anyMatch(user -> user.username().equals(username));
    }
}
