package com.llmwiki.common.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256 encryption/decryption utility.
 * Uses AES/CBC/PKCS5Padding with a random 16-byte IV prepended to the ciphertext.
 */
public final class EncryptionUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16;
    private static final int KEY_LENGTH = 32;

    private EncryptionUtil() {
        // utility class
    }

    /**
     * Encrypts the given plainText using AES-256 with the provided key.
     * Returns a Base64-encoded string containing IV + ciphertext.
     *
     * @param plainText the text to encrypt
     * @param key       the encryption key (must be at least 32 characters)
     * @return Base64-encoded IV + ciphertext, or null if input is invalid
     */
    public static String encrypt(String plainText, String key) {
        if (plainText == null || key == null || key.isEmpty()) {
            return null;
        }
        try {
            byte[] keyBytes = normalizeKey(key);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);

            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));

            // Prepend IV to ciphertext
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new EncryptionException("Encryption failed", e);
        }
    }

    /**
     * Decrypts the given cipherText (Base64-encoded IV + ciphertext) using AES-256.
     *
     * @param cipherText Base64-encoded IV + ciphertext
     * @param key        the encryption key
     * @return the decrypted plaintext, or null if input is invalid
     */
    public static String decrypt(String cipherText, String key) {
        if (cipherText == null || key == null || key.isEmpty()) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);

            // Extract IV (first 16 bytes)
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // Extract ciphertext
            byte[] encrypted = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);

            byte[] keyBytes = normalizeKey(key);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            byte[] decrypted = cipher.doFinal(encrypted);

            return new String(decrypted, "UTF-8");
        } catch (Exception e) {
            throw new EncryptionException("Decryption failed", e);
        }
    }

    /**
     * Returns the encryption key from the environment variable ENCRYPTION_KEY,
     * or falls back to the default development key.
     */
    public static String getEncryptionKeyFromEnv() {
        String envKey = System.getenv("ENCRYPTION_KEY");
        if (envKey != null && !envKey.isEmpty()) {
            return envKey;
        }
        return "llm-wiki-default-key-32chars-long!";
    }

    private static byte[] normalizeKey(String key) {
        // Ensure key is exactly 32 bytes (AES-256)
        byte[] keyBytes = new byte[KEY_LENGTH];
        byte[] inputBytes = key.getBytes();
        System.arraycopy(inputBytes, 0, keyBytes, 0, Math.min(inputBytes.length, KEY_LENGTH));
        return keyBytes;
    }

    public static class EncryptionException extends RuntimeException {
        public EncryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
