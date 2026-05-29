package com.lawfirm.controller;

import com.lawfirm.entity.AILog;
import com.lawfirm.service.AILogService;
import com.lawfirm.util.Result;
import com.lawfirm.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * AI日志控制器
 */
@RestController
@RequestMapping("ai/logs")
@RequiredArgsConstructor
public class AILogController {

    private final AILogService aiLogService;
    private final SecurityUtils securityUtils;

    /**
     * 获取当前用户的AI使用日志
     */
    @GetMapping("/user")
    @PreAuthorize("hasAnyRole('ADMIN', 'LAWYER', 'ASSISTANT')")
    public Result<Page<AILog>> getUserLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = getCurrentUserId();
        Page<AILog> logs = aiLogService.getUserLogs(userId,
                PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return Result.success(logs);
    }

    /**
     * 获取案件的AI使用日志
     */
    @GetMapping("/case/{caseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LAWYER', 'ASSISTANT')")
    public Result<Page<AILog>> getCaseLogs(
            @PathVariable Long caseId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AILog> logs = aiLogService.getCaseLogs(caseId,
                PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return Result.success(logs);
    }

    /**
     * 获取所有AI使用日志（管理员）
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Page<AILog>> getAllLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AILog> logs = aiLogService.getAllLogs(
                PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return Result.success(logs);
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        return securityUtils.getCurrentUserId();
    }
}
