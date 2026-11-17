package at.technikum.mrp.persistence;

import at.technikum.mrp.domain.User;

public interface UserRepository {
    User create(User user);

    boolean existsByUsername(String username);
}
