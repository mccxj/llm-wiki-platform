package com.llmwiki.domain.processing.entity;

import com.llmwiki.common.enums.StepStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processing_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProcessingLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID rawDocumentId;

    @Column(nullable = false)
    private String step;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StepStatus status;

    @Column(columnDefinition = "TEXT")
    private String detail;

    private Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }
}
