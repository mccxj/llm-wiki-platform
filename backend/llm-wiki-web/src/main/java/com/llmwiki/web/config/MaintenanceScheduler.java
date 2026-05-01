package com.llmwiki.web.config;

import com.llmwiki.domain.maintenance.entity.MaintenanceReportLog;
import com.llmwiki.domain.maintenance.repository.MaintenanceReportLogRepository;
import com.llmwiki.service.maintenance.MaintenanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class MaintenanceScheduler {

    private final MaintenanceService maintenanceService;
    private final MaintenanceReportLogRepository reportLogRepo;

    /**
     * 每日2:00 执行孤儿页面检测
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void detectOrphans() {
        String taskType = "DETECT_ORPHANS";
        log.info("Starting scheduled task: {}", taskType);
        try {
            var orphans = maintenanceService.findOrphans();
            String result = String.format("Found %d orphan pages", orphans.size());
            log.info("Task {} completed: {}", taskType, result);
            saveLog(taskType, result, "COMPLETED");
        } catch (Exception e) {
            log.error("Task {} failed", taskType, e);
            saveLog(taskType, "Error: " + e.getMessage(), "FAILED");
        }
    }

    /**
     * 每周一3:00 执行过时检测 + 矛盾检测
     */
    @Scheduled(cron = "0 0 3 ? * MON")
    public void detectStale() {
        // 过时检测
        String staleTaskType = "DETECT_STALE";
        log.info("Starting scheduled task: {}", staleTaskType);
        try {
            var stalePages = maintenanceService.findStalePages(30);
            String result = String.format("Found %d stale pages (30+ days)", stalePages.size());
            log.info("Task {} completed: {}", staleTaskType, result);
            saveLog(staleTaskType, result, "COMPLETED");
        } catch (Exception e) {
            log.error("Task {} failed", staleTaskType, e);
            saveLog(staleTaskType, "Error: " + e.getMessage(), "FAILED");
        }

        // 矛盾检测
        String contraTaskType = "DETECT_CONTRADICTIONS";
        log.info("Starting scheduled task: {}", contraTaskType);
        try {
            var contradictions = maintenanceService.findContradictions();
            String result = String.format("Found %d contradictory pages", contradictions.size());
            log.info("Task {} completed: {}", contraTaskType, result);
            saveLog(contraTaskType, result, "COMPLETED");
        } catch (Exception e) {
            log.error("Task {} failed", contraTaskType, e);
            saveLog(contraTaskType, "Error: " + e.getMessage(), "FAILED");
        }
    }

    /**
     * 每月1日4:00 执行重复检测 + 拆分建议
     */
    @Scheduled(cron = "0 0 4 1 * ?")
    public void detectDuplicates() {
        // 重复检测
        String dupTaskType = "FIND_DUPLICATES";
        log.info("Starting scheduled task: {}", dupTaskType);
        try {
            var duplicates = maintenanceService.findDuplicates();
            String result = String.format("Found %d duplicate groups", duplicates.size());
            log.info("Task {} completed: {}", dupTaskType, result);
            saveLog(dupTaskType, result, "COMPLETED");
        } catch (Exception e) {
            log.error("Task {} failed", dupTaskType, e);
            saveLog(dupTaskType, "Error: " + e.getMessage(), "FAILED");
        }

        // 拆分建议
        String splitTaskType = "FIND_SPLIT_SUGGESTIONS";
        log.info("Starting scheduled task: {}", splitTaskType);
        try {
            var splitSuggestions = maintenanceService.findSplitSuggestions();
            String result = String.format("Found %d pages suggested for splitting", splitSuggestions.size());
            log.info("Task {} completed: {}", splitTaskType, result);
            saveLog(splitTaskType, result, "COMPLETED");
        } catch (Exception e) {
            log.error("Task {} failed", splitTaskType, e);
            saveLog(splitTaskType, "Error: " + e.getMessage(), "FAILED");
        }
    }

    private void saveLog(String taskType, String result, String status) {
        MaintenanceReportLog log = MaintenanceReportLog.builder()
                .taskType(taskType)
                .result(result)
                .status(status)
                .build();
        reportLogRepo.save(log);
    }
}
