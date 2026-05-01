package com.llmwiki.domain.graph.entity;

import com.llmwiki.common.enums.EdgeType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "kg_edges")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KgEdge {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID sourceNodeId;

    @Column(nullable = false)
    private UUID targetNodeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EdgeType edgeType;

    @Builder.Default
    private BigDecimal weight = BigDecimal.valueOf(0.50);

    private Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }
}
