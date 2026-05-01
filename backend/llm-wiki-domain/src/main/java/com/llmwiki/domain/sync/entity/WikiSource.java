package com.llmwiki.domain.sync.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wiki_sources")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WikiSource {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String adapterClass;

    @Column(nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private String config = "{}";

    @Builder.Default
    private String syncCron = "0 */6 * * *";

    private Instant lastSyncAt;

    @Builder.Default
    private Boolean enabled = true;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); updatedAt = Instant.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
