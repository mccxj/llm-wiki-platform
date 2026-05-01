package com.llmwiki.domain.audit.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * 审计日志实体
 */
@Entity
@Table(name = "audit_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * 操作类型 (CREATE, UPDATE, DELETE, APPROVE, REJECT, LOGIN, LOGOUT, etc.)
     */
    @Column(nullable = false)
    private String action;

    /**
     * 操作对象类型 (Page, Node, Edge, Config, etc.)
     */
    private String entityType;

    /**
     * 操作对象ID
     */
    private UUID entityId;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 操作详情 (JSON)
     */
    @Column(columnDefinition = "TEXT")
    private String detail;

    /**
     * 操作前值 (JSON)
     */
    @Column(columnDefinition = "TEXT")
    private String beforeValue;

    /**
     * 操作后值 (JSON)
     */
    @Column(columnDefinition = "TEXT")
    private String afterValue;

    /**
     * IP地址
     */
    private String ipAddress;

    private Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }
}
