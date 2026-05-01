package com.llmwiki.service.audit;

import com.llmwiki.domain.audit.entity.AuditLog;
import com.llmwiki.domain.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 审计日志服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepo;

    /**
     * 记录操作日志
     */
    public void log(String action, String entityType, UUID entityId, String operator, String detail) {
        AuditLog audit = AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .operator(operator)
                .detail(detail)
                .build();
        auditLogRepo.save(audit);
        log.debug("Audit log: {} {} by {}", action, entityType, operator);
    }

    /**
     * 记录变更日志（含 before/after）
     */
    public void logChange(String action, String entityType, UUID entityId, String operator,
                          String beforeValue, String afterValue, String detail) {
        AuditLog audit = AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .operator(operator)
                .beforeValue(beforeValue)
                .afterValue(afterValue)
                .detail(detail)
                .build();
        auditLogRepo.save(audit);
        log.debug("Audit change log: {} {} by {}", action, entityType, operator);
    }

    /**
     * 查询实体的操作历史
     */
    public List<AuditLog> getEntityHistory(String entityType, UUID entityId) {
        return auditLogRepo.findByEntityTypeAndEntityId(entityType, entityId);
    }

    /**
     * 查询最近操作记录
     */
    public List<AuditLog> getRecentLogs() {
        return auditLogRepo.findTop100ByOrderByCreatedAtDesc();
    }
}
