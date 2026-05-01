package com.llmwiki.domain.pipeline.repository;

import com.llmwiki.domain.pipeline.entity.DeadLetterQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeadLetterQueueRepository extends JpaRepository<DeadLetterQueue, UUID> {
    List<DeadLetterQueue> findByStatus(String status);
    List<DeadLetterQueue> findByRawDocumentId(UUID rawDocumentId);
}
