package com.llmwiki.domain.page.entity;

import com.llmwiki.common.enums.ConfidenceLevel;
import com.llmwiki.common.enums.PageStatus;
import com.llmwiki.common.enums.PageType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PageType pageType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PageStatus status = PageStatus.PENDING_APPROVAL;

    @Enumerated(EnumType.STRING)
    private ConfidenceLevel confidence;

    @Builder.Default
    private Boolean contested = false;

    @Column(name = "score")
    private BigDecimal aiScore;

    @Column(columnDefinition = "TEXT")
    private String aiScoreDetail;

    private UUID sourceDocId;

    private String approvedBy;
    private Instant approvedAt;
    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
