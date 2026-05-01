package com.llmwiki.domain.page.entity;

import com.llmwiki.common.enums.EdgeType;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "page_links")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PageLink {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID sourcePageId;

    @Column(nullable = false)
    private UUID targetPageId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EdgeType linkType = EdgeType.RELATED_TO;

    private Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }
}
