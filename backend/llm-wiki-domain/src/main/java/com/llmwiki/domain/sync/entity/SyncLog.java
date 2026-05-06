package com.llmwiki.domain.sync.entity;

import com.llmwiki.common.enums.SyncStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sync_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SyncLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID sourceId;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant finishedAt;

    @Builder.Default
    private Integer fetchedCount = 0;

    @Builder.Default
    private Integer processedCount = 0;

    @Builder.Default
    private Integer skippedCount = 0;

    @Builder.Default
    private Integer failedCount = 0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SyncStatus status = SyncStatus.RUNNING;

    private String errorMessage;
    private Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }
}
