package com.lawfirm.controller;

import com.lawfirm.annotation.AuditLog;
import com.lawfirm.dto.ApprovalDTO;
import com.lawfirm.dto.CaseFileIntakeResult;
import com.lawfirm.dto.CaseIntakePrefillDTO;
import com.lawfirm.entity.CaseIntakePending;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.service.CaseFileIntakeService;
import com.lawfirm.service.CaseIntakePendingService;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 核心能力：主界面文件传入 → AI 分析 → 登记备注 → 案件档案夹。
 */
@Slf4j
@RestController
@RequestMapping("/case-intake")
@RequiredArgsConstructor
public class CaseFileIntakeController {

    private static final long MAX_BYTES = 50L * 1024 * 1024;

    private final CaseFileIntakeService caseFileIntakeService;
    private final CaseIntakePendingService caseIntakePendingService;
    private final SecurityUtils securityUtils;

    /**
     * POST /api/case-intake/process
     */
    @PostMapping("/process")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(value = "卷宗智能录入", operationType = "UPLOAD", logParams = false)
    public Result<CaseFileIntakeResult> process(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "caseId", required = false) Long caseId,
            @RequestParam(value = "remark", required = false) String remark,
            @RequestParam(value = "runAutomation", defaultValue = "false") boolean runAutomation) {

        if (file.getSize() > MAX_BYTES) {
            return Result.error("文件不能超过 50MB");
        }

        Long userId = securityUtils.getCurrentUserId();
        log.info("卷宗录入: file={}, caseId={}, user={}", file.getOriginalFilename(), caseId, userId);

        CaseFileIntakeResult result = caseFileIntakeService.processIntake(
                file, userId, caseId, remark, runAutomation);

        if ("FAILED".equals(result.getStatus())) {
            return Result.error(result.getMessage());
        }
        if ("NEEDS_CASE".equals(result.getStatus())) {
            return Result.success(result.getMessage(), result);
        }
        return Result.success(result.getMessage(), result);
    }

    /**
     * 选定案件后归档（可携带已有识别结果 JSON，由前端在 NEEDS_CASE 后重提）。
     */
    @PostMapping("/attach")
    @PreAuthorize("isAuthenticated()")
    public Result<CaseFileIntakeResult> attach(
            @RequestParam("file") MultipartFile file,
            @RequestParam("caseId") Long caseId,
            @RequestParam(value = "remark", required = false) String remark) {

        Long userId = securityUtils.getCurrentUserId();
        CaseFileIntakeResult result = caseFileIntakeService.attachToCase(
                file, userId, caseId, remark, null);
        return Result.success(result.getMessage(), result);
    }

    @GetMapping("/cases/search")
    @PreAuthorize("isAuthenticated()")
    public Result<List<CaseFileIntakeResult.CaseBriefDTO>> searchCases(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return Result.success(caseFileIntakeService.searchCases(q, Math.min(limit, 50)));
    }

    /**
     * 从暂存记录挂接到指定案件（无需重新上传）。
     */
    @PostMapping("/attach-pending")
    @PreAuthorize("isAuthenticated()")
    public Result<CaseFileIntakeResult> attachFromPending(
            @RequestParam("pendingId") Long pendingId,
            @RequestParam("caseId") Long caseId,
            @RequestParam(value = "remark", required = false) String remark) {
        Long userId = securityUtils.getCurrentUserId();
        CaseFileIntakeResult result = caseIntakePendingService.attachFromPending(
                pendingId, caseId, userId, remark);
        return Result.success(result.getMessage(), result);
    }

    /**
     * 未匹配案件时发起立案审批（关联暂存卷宗）。
     */
    @PostMapping("/filing-application")
    @PreAuthorize("isAuthenticated()")
    public Result<ApprovalDTO> filingApplication(
            @RequestParam("pendingId") Long pendingId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "approverId", required = false) Long approverId) {
        Long userId = securityUtils.getCurrentUserId();
        ApprovalDTO dto = caseIntakePendingService.createCaseFilingApproval(
                pendingId, title, content, approverId, userId);
        return Result.success("立案申请已提交", dto);
    }

    @GetMapping("/pending")
    @PreAuthorize("isAuthenticated()")
    public Result<List<CaseIntakePending>> listPending() {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(caseIntakePendingService.listPending(userId));
    }

    /**
     * 立案审批通过后预填新建案件（GET /api/case-intake/pending/{id}/prefill）
     */
    @GetMapping("/pending/{id}/prefill")
    @PreAuthorize("isAuthenticated()")
    public Result<CaseIntakePrefillDTO> getPrefill(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(caseIntakePendingService.getPrefill(id, userId));
    }
}
