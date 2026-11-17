package at.technikum.mrp.persistence;

import at.technikum.mrp.domain.User;

import java.util.Optional;

public interface UserRepository {
    User create(User user);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
