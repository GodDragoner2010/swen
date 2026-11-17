package at.technikum.mrp.service;

public interface PasswordHasher {
    String newSalt();

    String hash(String password, String salt);

    boolean matches(String password, String salt, String expectedHash);
}
