package com.lawfirm.controller;

import com.lawfirm.entity.AuditLog;
import com.lawfirm.repository.AuditLogRepository;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志控制器
 * PRD要求：操作审计日志（AOP切面自动记录）
 */
@Slf4j
@RestController
@RequestMapping("audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    /**
     * 分页查询审计日志
     * GET /api/audit-logs?page=1&size=20&module=&operation=&userId=
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTOR', 'MANAGER')")
    public Page<AuditLog> getAuditLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) Long userId) {

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 基础查询 - 所有日志按时间倒序
        if (!StringUtils.hasText(module) && !StringUtils.hasText(operation) && userId == null) {
            return auditLogRepository.findAll(pageable);
        }

        // 如果有筛选条件，使用简化实现 - 获取所有结果后在内存中过滤
        List<AuditLog> allLogs = auditLogRepository.findAll();

        List<AuditLog> filteredLogs = allLogs.stream()
                .filter(log -> {
                    boolean match = true;
                    if (StringUtils.hasText(module) && !module.equals(log.getModule())) {
                        match = false;
                    }
                    if (StringUtils.hasText(operation) && !operation.equals(log.getOperation())) {
                        match = false;
                    }
                    if (userId != null && !userId.equals(log.getUserId())) {
                        match = false;
                    }
                    return match;
                })
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(java.util.stream.Collectors.toList());

        // 手动分页
        int start = (page - 1) * size;
        int end = Math.min(start + size, filteredLogs.size());

        return new org.springframework.data.domain.PageImpl<>(
            filteredLogs.subList(start, end),
            pageable,
            filteredLogs.size()
        );
    }

    /**
     * 获取审计日志详情
     * GET /api/audit-logs/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTOR', 'MANAGER')")
    public Result<AuditLog> getAuditLogById(@PathVariable Long id) {
        return auditLogRepository.findById(id)
                .map(Result::success)
                .orElse(Result.error("审计日志不存在"));
    }

    /**
     * 获取模块列表（用于筛选下拉框）
     * GET /api/audit-logs/modules
     */
    @GetMapping("/modules")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTOR', 'MANAGER')")
    public List<String> getModules() {
        return auditLogRepository.findAll().stream()
                .map(AuditLog::getModule)
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 获取操作类型列表（用于筛选下拉框）
     * GET /api/audit-logs/operations
     */
    @GetMapping("/operations")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTOR', 'MANAGER')")
    public List<String> getOperations() {
        return auditLogRepository.findAll().stream()
                .map(AuditLog::getOperation)
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 获取统计信息
     * GET /api/audit-logs/stats
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTOR', 'MANAGER')")
    public Object getAuditLogStats() {
        long totalLogs = auditLogRepository.count();

        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        long todayLogs = auditLogRepository.findAll().stream()
                .filter(log -> log.getCreatedAt().isAfter(todayStart))
                .count();

        long failedLogs = auditLogRepository.findAll().stream()
                .filter(log -> log.getStatus() != null && log.getStatus() != 200)
                .count();

        return new Object() {
            public final long total = totalLogs;
            public final long today = todayLogs;
            public final long failed = failedLogs;
        };
    }
}