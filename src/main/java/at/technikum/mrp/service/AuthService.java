package at.technikum.mrp.service;

import at.technikum.mrp.domain.User;
import at.technikum.mrp.persistence.UserRepository;

import java.time.Clock;
import java.util.Optional;
import java.util.regex.Pattern;

public final class AuthService {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9._-]{3,50}");
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_PASSWORD_LENGTH = 100;

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final TokenService tokenService;
    private final Clock clock;

    public AuthService(UserRepository users, PasswordHasher passwordHasher, TokenService tokenService) {
        this(users, passwordHasher, tokenService, Clock.systemUTC());
    }

    public AuthService(UserRepository users, PasswordHasher passwordHasher, TokenService tokenService, Clock clock) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.tokenService = tokenService;
        this.clock = clock;
    }

    public User register(String rawUsername, String password) {
        String username = normalizeUsername(rawUsername);
        validatePassword(password);
        if (users.existsByUsername(username)) {
            throw new ConflictException("Username is already taken");
        }
        String salt = passwordHasher.newSalt();
        User user = new User(
                0,
                username,
                passwordHasher.hash(password, salt),
                salt,
                null,
                null,
                clock.instant()
        );
        return users.create(user);
    }

    public LoginResult login(String rawUsername, String password) {
        String username = rawUsername == null ? "" : rawUsername.trim();
        if (username.isEmpty() || password == null || password.isEmpty()) {
            throw new UnauthorizedException("Invalid username or password");
        }
        Optional<User> candidate = users.findByUsername(username);
        if (candidate.isEmpty()
                || !passwordHasher.matches(password, candidate.get().passwordSalt(), candidate.get().passwordHash())) {
            throw new UnauthorizedException("Invalid username or password");
        }
        User user = candidate.get();
        return new LoginResult(user, tokenService.issue(user.id()));
    }

    public Optional<Long> authenticate(String token) {
        return tokenService.validate(token);
    }

    private String normalizeUsername(String rawUsername) {
        if (rawUsername == null) {
            throw new ValidationException("Username is required");
        }
        String username = rawUsername.trim();
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new ValidationException(
                    "Username must be 3-50 characters and contain only letters, digits, '.', '_' or '-'");
        }
        return username;
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH
                || password.length() > MAX_PASSWORD_LENGTH) {
            throw new ValidationException("Password must be between " + MIN_PASSWORD_LENGTH + " and "
                    + MAX_PASSWORD_LENGTH + " characters");
        }
    }
}
