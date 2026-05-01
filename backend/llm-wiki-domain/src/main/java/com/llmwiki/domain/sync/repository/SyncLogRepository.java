package com.llmwiki.domain.sync.repository;

import com.llmwiki.domain.sync.entity.SyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface SyncLogRepository extends JpaRepository<SyncLog, UUID> {
}
