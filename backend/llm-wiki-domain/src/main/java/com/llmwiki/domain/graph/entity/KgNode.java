package com.llmwiki.domain.graph.entity;

import com.llmwiki.common.enums.NodeType;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "kg_nodes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KgNode {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NodeType nodeType;

    @Column(columnDefinition = "TEXT")
    private String description;

    private UUID pageId;
    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); updatedAt = Instant.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
