package com.llmwiki.domain.processing.repository;

import com.llmwiki.domain.processing.entity.ProcessingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProcessingLogRepository extends JpaRepository<ProcessingLog, UUID> {
    List<ProcessingLog> findByRawDocumentId(UUID rawDocumentId);
}
