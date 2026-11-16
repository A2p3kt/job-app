package dev.toluwalase.jobapp.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests validating PBKDF2 password hashing and verification in SecurityConfig.
 */
public class SecurityConfigTest {

    @Test
    @DisplayName("hashPassword creates non-null, salted hash containing delimiter")
    void testHashPassword() {
        String hash = SecurityConfig.hashPassword("SuperSecret123!");
        assertNotNull(hash);
        assertTrue(hash.contains(":"));
    }

    @Test
    @DisplayName("verifyPassword returns true for matching password and false for wrong candidate")
    void testVerifyPassword() {
        String hash = SecurityConfig.hashPassword("MySecurePass#2026");

        assertTrue(SecurityConfig.verifyPassword("MySecurePass#2026", hash));
        assertFalse(SecurityConfig.verifyPassword("WrongPassword", hash));
        assertFalse(SecurityConfig.verifyPassword("", hash));
        assertFalse(SecurityConfig.verifyPassword(null, hash));
        assertFalse(SecurityConfig.verifyPassword("MySecurePass#2026", "corrupted-hash"));
    }

    @Test
    @DisplayName("hashPassword throws IllegalArgumentException on blank or null inputs")
    void testHashPasswordRejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> SecurityConfig.hashPassword(""));
        assertThrows(IllegalArgumentException.class, () -> SecurityConfig.hashPassword(null));
    }
}
