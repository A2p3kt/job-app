package dev.toluwalase.jobapp.web;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web and API security configuration.
 * Configures CORS policies for /api/v1/** REST endpoints and provides
 * cryptographic password hashing utilities using standard PBKDF2WithHmacSHA256.
 */
@Configuration
public class SecurityConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;

    /**
     * Enables Cross-Origin Resource Sharing (CORS) for all RESTful API endpoints (/api/**).
     *
     * @param registry CorsRegistry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        log.info("Configuring CORS policy for /api/** endpoints.");
        registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    /**
     * Hashes a raw password using PBKDF2 with a cryptographically secure random salt.
     * Output format: saltBase64:hashBase64
     *
     * @param password Plaintext password to hash
     * @return Formatted salted hash string
     */
    public static String hashPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or blank");
        }
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);

            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();

            return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new IllegalStateException("Cryptographic algorithm unavailable", ex);
        }
    }

    /**
     * Verifies a raw candidate password against a stored salted hash.
     *
     * @param password   Candidate plaintext password
     * @param storedHash Stored salt:hash string
     * @return true if credentials match, false otherwise
     */
    public static boolean verifyPassword(String password, String storedHash) {
        if (password == null || storedHash == null || !storedHash.contains(":")) {
            return false;
        }
        try {
            String[] parts = storedHash.split(":", 2);
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[1]);

            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] candidateHash = factory.generateSecret(spec).getEncoded();

            int diff = expectedHash.length ^ candidateHash.length;
            for (int i = 0; i < expectedHash.length && i < candidateHash.length; i++) {
                diff |= expectedHash[i] ^ candidateHash[i];
            }
            return diff == 0;
        } catch (Exception ex) {
            return false;
        }
    }
}
