package com.llmwiki.domain.maintenance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "maintenance_report_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MaintenanceReportLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "task_type", nullable = false)
    private String taskType;

    @Column(name = "result", columnDefinition = "text")
    private String result;

    @Column(name = "status")
    @Builder.Default
    private String status = "COMPLETED";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
