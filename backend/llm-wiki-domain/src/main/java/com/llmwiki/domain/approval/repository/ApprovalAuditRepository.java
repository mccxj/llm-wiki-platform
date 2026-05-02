package com.llmwiki.domain.approval.repository;

import com.llmwiki.domain.approval.entity.ApprovalAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApprovalAuditRepository extends JpaRepository<ApprovalAudit, UUID> {
    List<ApprovalAudit> findByApprovalId(UUID approvalId);
}
