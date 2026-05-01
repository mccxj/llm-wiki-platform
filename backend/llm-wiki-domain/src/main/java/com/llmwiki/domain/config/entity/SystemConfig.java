package com.llmwiki.domain.config.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "system_config")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SystemConfig {
    @Id
    @Column(name = "config_key")
    private String key;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String value;

    private String description;
    private Instant updatedAt;

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
