package com.llmwiki.domain.approval.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approval_audits")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID approvalId;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String reviewerId;

    @Column(columnDefinition = "TEXT")
    private String comment;

    private Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }
}
