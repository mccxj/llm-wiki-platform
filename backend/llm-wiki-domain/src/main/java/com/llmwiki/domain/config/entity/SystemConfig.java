package com.llmwiki.domain.config.entity;

import com.llmwiki.common.util.EncryptionUtil;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "system_config")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SystemConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "config_key", nullable = false, unique = true)
    private String key;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String value;

    private String description;
    private Instant updatedAt;

    /**
     * Transient field holding the encryption key for this config instance.
     * Not persisted to the database. If null, falls back to ENCRYPTION_KEY env var.
     */
    @Transient
    private String encryptionKey;

    @PrePersist
    void prePersist() {
        updatedAt = Instant.now();
        encryptIfSensitive();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    @PostLoad
    void postLoad() {
        decryptIfSensitive();
    }

    private boolean isSensitiveKey() {
        if (key == null) return false;
        String lowerKey = key.toLowerCase();
        return lowerKey.contains("api_key") || lowerKey.contains("secret");
    }

    private String resolveEncryptionKey() {
        if (encryptionKey != null && !encryptionKey.isEmpty()) {
            return encryptionKey;
        }
        return EncryptionUtil.getEncryptionKeyFromEnv();
    }

    private void encryptIfSensitive() {
        if (!isSensitiveKey() || value == null || value.isEmpty()) {
            return;
        }
        // Avoid double-encryption: if the value is already encrypted,
        // attempting to decrypt it would fail. We mark encrypted values
        // with a prefix that we check before encrypting.
        if (value.startsWith("ENC(") && value.endsWith(")")) {
            return;
        }
        String key = resolveEncryptionKey();
        String encrypted = EncryptionUtil.encrypt(value, key);
        if (encrypted != null) {
            value = "ENC(" + encrypted + ")";
        }
    }

    private void decryptIfSensitive() {
        if (!isSensitiveKey() || value == null || value.isEmpty()) {
            return;
        }
        if (value.startsWith("ENC(") && value.endsWith(")")) {
            String encrypted = value.substring(4, value.length() - 1);
            String key = resolveEncryptionKey();
            String decrypted = EncryptionUtil.decrypt(encrypted, key);
            if (decrypted != null) {
                value = decrypted;
            }
        }
    }
}
