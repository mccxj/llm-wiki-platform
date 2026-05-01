package com.llmwiki.domain.sync.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "raw_documents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RawDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String sourceId;

    private String sourceName;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private String contentHash;

    @Column(columnDefinition = "TEXT")
    private String sourceUrl;

    private Instant ingestedAt;
    private Instant lastCheckedAt;

    @PrePersist
    void onCreate() { ingestedAt = Instant.now(); }
}
