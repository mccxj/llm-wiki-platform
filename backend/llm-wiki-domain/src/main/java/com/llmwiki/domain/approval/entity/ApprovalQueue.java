package com.llmwiki.domain.approval.entity;

import com.llmwiki.common.enums.ApprovalAction;
import com.llmwiki.common.enums.ApprovalStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approval_queue")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalQueue {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID pageId;

    @Column(nullable = false)
    private String action;

    private String comment;

    @Column(nullable = false)
    @Builder.Default
    private String status = ApprovalStatus.PENDING.name();

    private String reviewerId;

    private Instant reviewedAt;
    private Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }
}
