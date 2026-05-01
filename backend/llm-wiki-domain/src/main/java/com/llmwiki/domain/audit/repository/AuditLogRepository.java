package com.llmwiki.domain.audit.repository;

import com.llmwiki.domain.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId);
    List<AuditLog> findByOperatorOrderByCreatedAtDesc(String operator);
    List<AuditLog> findTop100ByOrderByCreatedAtDesc();
}
