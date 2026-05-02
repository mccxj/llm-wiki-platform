package com.llmwiki.web.controller;

import com.llmwiki.domain.config.entity.SystemConfig;
import com.llmwiki.domain.config.repository.SystemConfigRepository;
import com.llmwiki.service.maintenance.MaintenanceReport;
import com.llmwiki.service.maintenance.MaintenanceService;
import com.llmwiki.service.maintenance.DuplicateGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理后台控制器
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final SystemConfigRepository configRepo;
    private final MaintenanceService maintenanceService;

    /**
     * 获取所有系统配置
     */
    @GetMapping("/config")
    public ResponseEntity<List<SystemConfig>> getConfig() {
        return ResponseEntity.ok(configRepo.findAll());
    }

    /**
     * 更新系统配置
     */
    @PutMapping("/config/{key}")
    public ResponseEntity<SystemConfig> updateConfig(@PathVariable String key,
                                                      @RequestBody Map<String, String> body) {
        String value = body.get("value");
        if (value == null) {
            return ResponseEntity.badRequest().build();
        }
        SystemConfig config = configRepo.findById(key).orElse(new SystemConfig());
        config.setKey(key);
        config.setValue(value);
        config.setDescription(body.get("description"));
        return ResponseEntity.ok(configRepo.save(config));
    }

    /**
     * 触发孤儿检测
     */
    @PostMapping("/maintenance/orphans")
    public ResponseEntity<List<?>> triggerOrphanCheck() {
        return ResponseEntity.ok(maintenanceService.findOrphans());
    }

    /**
     * 触发过时检测
     */
    @PostMapping("/maintenance/stale")
    public ResponseEntity<List<?>> triggerStaleCheck(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(maintenanceService.findStalePages(days));
    }

    /**
     * 触发重复检测
     */
    @PostMapping("/maintenance/duplicates")
    public ResponseEntity<List<DuplicateGroup>> triggerDuplicateCheck() {
        return ResponseEntity.ok(maintenanceService.findDuplicates());
    }

    /**
     * 触发矛盾检测
     */
    @PostMapping("/maintenance/contradictions")
    public ResponseEntity<List<?>> triggerContradictionCheck() {
        return ResponseEntity.ok(maintenanceService.findContradictions());
    }

    /**
     * 获取维护报告
     */
    @GetMapping("/reports/maintenance")
    public ResponseEntity<MaintenanceReport> getMaintenanceReport() {
        return ResponseEntity.ok(maintenanceService.generateReport());
    }
}
