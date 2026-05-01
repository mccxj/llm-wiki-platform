package com.llmwiki.domain.approval.repository;

import com.llmwiki.domain.approval.entity.ApprovalQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApprovalQueueRepository extends JpaRepository<ApprovalQueue, UUID> {
    List<ApprovalQueue> findByStatus(String status);
    List<ApprovalQueue> findByStatusOrderByCreatedAtAsc(String status);
    long countByStatus(String status);
    List<ApprovalQueue> findByPageId(UUID pageId);
}
