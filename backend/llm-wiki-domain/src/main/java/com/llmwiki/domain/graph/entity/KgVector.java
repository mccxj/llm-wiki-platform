package com.llmwiki.domain.graph.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "kg_vectors")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KgVector {
    @Id
    private UUID nodeId;

    @Column(nullable = false, columnDefinition = "VECTOR(1536)")
    private float[] vector;

    private String model;

    private Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }
}
