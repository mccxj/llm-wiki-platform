package com.llmwiki.domain.example.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "entity_examples")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EntityExample {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT", name = "example_text")
    private String exampleText;

    @Column(columnDefinition = "TEXT", name = "extraction_data")
    private String extractionData;

    @Column(nullable = false, name = "entity_type")
    private String entityType;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (deleted == null) {
            deleted = false;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
