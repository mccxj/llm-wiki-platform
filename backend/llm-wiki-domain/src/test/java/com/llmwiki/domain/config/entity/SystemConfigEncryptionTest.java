package com.llmwiki.domain.config.entity;

import com.llmwiki.common.util.EncryptionUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemConfigEncryptionTest {

    private static final String ENCRYPTION_KEY = "llm-wiki-default-key-32chars-long!";

    @Test
    void shouldEncryptApiKeyOnPrePersist() {
        SystemConfig config = SystemConfig.builder()
                .key("openai_api_key")
                .value("sk-abcdef123456")
                .description("OpenAI API Key")
                .encryptionKey(ENCRYPTION_KEY)
                .build();

        config.prePersist();

        assertNotEquals("sk-abcdef123456", config.getValue());
        // Value should be wrapped in ENC(...)
        assertTrue(config.getValue().startsWith("ENC("));
        assertTrue(config.getValue().endsWith(")"));
        // Extract and decrypt
        String encrypted = config.getValue().substring(4, config.getValue().length() - 1);
        String decrypted = EncryptionUtil.decrypt(encrypted, ENCRYPTION_KEY);
        assertEquals("sk-abcdef123456", decrypted);
    }

    @Test
    void shouldEncryptSecretOnPrePersist() {
        SystemConfig config = SystemConfig.builder()
                .key("jwt_secret")
                .value("my-super-secret-jwt-key")
                .description("JWT Secret")
                .encryptionKey(ENCRYPTION_KEY)
                .build();

        config.prePersist();

        assertNotEquals("my-super-secret-jwt-key", config.getValue());
        assertTrue(config.getValue().startsWith("ENC("));
        String encrypted = config.getValue().substring(4, config.getValue().length() - 1);
        String decrypted = EncryptionUtil.decrypt(encrypted, ENCRYPTION_KEY);
        assertEquals("my-super-secret-jwt-key", decrypted);
    }

    @Test
    void shouldNotEncryptNonSensitiveKeys() {
        SystemConfig config = SystemConfig.builder()
                .key("scoring.threshold")
                .value("60.0")
                .description("Scoring threshold")
                .encryptionKey(ENCRYPTION_KEY)
                .build();

        config.prePersist();

        assertEquals("60.0", config.getValue());
    }

    @Test
    void shouldDecryptApiKeyOnPostLoad() {
        String originalValue = "sk-abcdef123456";
        String encrypted = EncryptionUtil.encrypt(originalValue, ENCRYPTION_KEY);

        SystemConfig config = SystemConfig.builder()
                .key("openai_api_key")
                .value("ENC(" + encrypted + ")")
                .description("OpenAI API Key")
                .encryptionKey(ENCRYPTION_KEY)
                .build();

        config.postLoad();

        assertEquals(originalValue, config.getValue());
    }

    @Test
    void shouldDecryptSecretOnPostLoad() {
        String originalValue = "my-super-secret-jwt-key";
        String encrypted = EncryptionUtil.encrypt(originalValue, ENCRYPTION_KEY);

        SystemConfig config = SystemConfig.builder()
                .key("db_secret")
                .value("ENC(" + encrypted + ")")
                .description("DB Secret")
                .encryptionKey(ENCRYPTION_KEY)
                .build();

        config.postLoad();

        assertEquals(originalValue, config.getValue());
    }

    @Test
    void shouldNotDecryptNonSensitiveKeys() {
        SystemConfig config = SystemConfig.builder()
                .key("scoring.threshold")
                .value("60.0")
                .description("Scoring threshold")
                .encryptionKey(ENCRYPTION_KEY)
                .build();

        config.postLoad();

        assertEquals("60.0", config.getValue());
    }

    @Test
    void shouldHandleCaseInsensitiveKeyMatching() {
        SystemConfig config = SystemConfig.builder()
                .key("OPENAI_API_KEY")
                .value("sk-uppercase-key")
                .description("Uppercase API key")
                .encryptionKey(ENCRYPTION_KEY)
                .build();

        config.prePersist();
        assertNotEquals("sk-uppercase-key", config.getValue());

        config.postLoad();
        assertEquals("sk-uppercase-key", config.getValue());
    }

    @Test
    void shouldHandleKeyContainingSecret() {
        SystemConfig config = SystemConfig.builder()
                .key("aws_secret_access_key")
                .value("aws-secret-value")
                .description("AWS Secret")
                .encryptionKey(ENCRYPTION_KEY)
                .build();

        config.prePersist();
        assertNotEquals("aws-secret-value", config.getValue());

        config.postLoad();
        assertEquals("aws-secret-value", config.getValue());
    }

    @Test
    void shouldNotDoubleEncryptOnPrePersist() {
        SystemConfig config = SystemConfig.builder()
                .key("api_key")
                .value("sk-original")
                .description("API Key")
                .encryptionKey(ENCRYPTION_KEY)
                .build();

        config.prePersist();
        String encryptedOnce = config.getValue();
        config.prePersist();
        String encryptedTwice = config.getValue();

        // Second prePersist should not re-encrypt already encrypted value
        assertEquals(encryptedOnce, encryptedTwice);
    }
}
