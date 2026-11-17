package at.technikum.mrp.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {
    private final PasswordHasher hasher = new Pbkdf2PasswordHasher(1_000);

    @Test
    void hashesPasswordWithSaltAndVerifiesIt() {
        String salt = hasher.newSalt();
        String hash = hasher.hash("secret123", salt);

        assertNotEquals("secret123", hash);
        assertTrue(hasher.matches("secret123", salt, hash));
        assertFalse(hasher.matches("wrong-password", salt, hash));
    }

    @Test
    void differentSaltsProduceDifferentHashesForTheSamePassword() {
        String firstSalt = hasher.newSalt();
        String secondSalt = hasher.newSalt();

        assertNotEquals(firstSalt, secondSalt);
        assertNotEquals(hasher.hash("secret123", firstSalt), hasher.hash("secret123", secondSalt));
    }

    @Test
    void rejectsMissingCredentialsWhenMatching() {
        String salt = hasher.newSalt();
        String hash = hasher.hash("secret123", salt);

        assertFalse(hasher.matches(null, salt, hash));
        assertFalse(hasher.matches("secret123", null, hash));
        assertFalse(hasher.matches("secret123", salt, null));
    }
}
