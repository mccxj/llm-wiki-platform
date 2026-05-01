package com.llmwiki.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionUtilTest {

    private static final String TEST_KEY = "llm-wiki-default-key-32chars-long!";

    @Test
    void shouldEncryptAndDecrypt() {
        String plainText = "sk-secret-api-key-12345";
        String encrypted = EncryptionUtil.encrypt(plainText, TEST_KEY);
        assertNotNull(encrypted);
        assertNotEquals(plainText, encrypted);
        String decrypted = EncryptionUtil.decrypt(encrypted, TEST_KEY);
        assertEquals(plainText, decrypted);
    }

    @Test
    void shouldProduceDifferentCipherTextForSamePlaintext() {
        String plainText = "same-text";
        String encrypted1 = EncryptionUtil.encrypt(plainText, TEST_KEY);
        String encrypted2 = EncryptionUtil.encrypt(plainText, TEST_KEY);
        // AES with random IV should produce different ciphertext each time
        assertNotEquals(encrypted1, encrypted2);
        // But both should decrypt to the same plaintext
        assertEquals(plainText, EncryptionUtil.decrypt(encrypted1, TEST_KEY));
        assertEquals(plainText, EncryptionUtil.decrypt(encrypted2, TEST_KEY));
    }

    @Test
    void shouldHandleEmptyString() {
        String encrypted = EncryptionUtil.encrypt("", TEST_KEY);
        assertNotNull(encrypted);
        String decrypted = EncryptionUtil.decrypt(encrypted, TEST_KEY);
        assertEquals("", decrypted);
    }

    @Test
    void shouldHandleUnicodeText() {
        String plainText = "中文密钥 🔑 émojis";
        String encrypted = EncryptionUtil.encrypt(plainText, TEST_KEY);
        String decrypted = EncryptionUtil.decrypt(encrypted, TEST_KEY);
        assertEquals(plainText, decrypted);
    }

    @Test
    void shouldReturnNullForNullInput() {
        assertNull(EncryptionUtil.encrypt(null, TEST_KEY));
        assertNull(EncryptionUtil.decrypt(null, TEST_KEY));
    }

    @Test
    void shouldReturnNullForEmptyKey() {
        assertNull(EncryptionUtil.encrypt("text", ""));
        assertNull(EncryptionUtil.decrypt("text", ""));
    }

    @Test
    void shouldDecryptDefaultKeyEncryptedValue() {
        // Simulate encrypting with the default key (as used in SystemConfig)
        String plainText = "super-secret-value";
        String encrypted = EncryptionUtil.encrypt(plainText, TEST_KEY);
        String decrypted = EncryptionUtil.decrypt(encrypted, TEST_KEY);
        assertEquals(plainText, decrypted);
    }
}
