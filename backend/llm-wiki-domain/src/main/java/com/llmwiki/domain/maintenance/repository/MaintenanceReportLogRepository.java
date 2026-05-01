package com.llmwiki.domain.maintenance.repository;

import com.llmwiki.domain.maintenance.entity.MaintenanceReportLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MaintenanceReportLogRepository extends JpaRepository<MaintenanceReportLog, UUID> {
    List<MaintenanceReportLog> findTop10ByTaskTypeOrderByCreatedAtDesc(String taskType);
}
