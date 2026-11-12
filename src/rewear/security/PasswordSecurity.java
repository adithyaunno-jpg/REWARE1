package rewear.security;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Enterprise-grade cryptographic security for password hashing using PBKDF2WithHmacSHA256.
 * Demonstrates:
 * - Cryptographic Randomness (SecureRandom)
 * - Key Derivation Functions (PBKDF2)
 * - Salt Generation
 * - Constant-time comparison to prevent timing attacks
 */
public class PasswordSecurity {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static class HashResult {
        private final String salt;
        private final String hash;

        public HashResult(String salt, String hash) {
            this.salt = salt;
            this.hash = hash;
        }

        public String getSalt() {
            return salt;
        }

        public String getHash() {
            return hash;
        }
    }

    /**
     * Generates a random cryptographic salt and hashes the plaintext password.
     */
    public static HashResult hashPassword(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        byte[] saltBytes = new byte[SALT_LENGTH];
        RANDOM.nextBytes(saltBytes);
        String saltBase64 = Base64.getEncoder().encodeToString(saltBytes);

        String hashBase64 = hashWithSalt(password, saltBytes);
        return new HashResult(saltBase64, hashBase64);
    }

    /**
     * Verifies an input password against stored salt and expected hash.
     */
    public static boolean verifyPassword(String password, String saltBase64, String expectedHashBase64) {
        if (password == null || saltBase64 == null || expectedHashBase64 == null) {
            return false;
        }
        try {
            byte[] saltBytes = Base64.getDecoder().decode(saltBase64);
            String computedHash = hashWithSalt(password, saltBytes);
            
            // Constant-time comparison prevents timing analysis attacks
            return constantTimeEquals(computedHash, expectedHashBase64);
        } catch (Exception e) {
            return false;
        }
    }

    private static String hashWithSalt(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hashBytes = factory.generateSecret(spec).getEncoded();
            spec.clearPassword();
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error computing PBKDF2 hash", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] aBytes = a.getBytes();
        byte[] bBytes = b.getBytes();
        if (aBytes.length != bBytes.length) return false;
        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        return result == 0;
    }
}

