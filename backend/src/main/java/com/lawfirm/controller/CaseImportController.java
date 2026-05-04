package com.lawfirm.controller;

import com.lawfirm.annotation.AuditLog;
import com.lawfirm.dto.CaseImportResultDTO;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.service.CaseImportService;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/cases/import")
@RequiredArgsConstructor
public class CaseImportController {

    private final CaseImportService caseImportService;
    private final SecurityUtils securityUtils;

    @PostMapping("/npa")
    @PreAuthorize("hasAuthority('CASE_CREATE')")
    @AuditLog(value = "批量导入金融不良资产案件", operationType = "IMPORT", logParams = false)
    public Result<CaseImportResultDTO> importNpaCases(@RequestParam("file") MultipartFile file) {
        try {
            Long currentUserId = securityUtils.getCurrentUserId();
            CaseImportResultDTO result = caseImportService.importNpaCases(file, currentUserId);
            return Result.success("导入完成", result);
        } catch (Exception e) {
            log.error("批量导入金融不良资产案件失败", e);
            return Result.error("导入失败: " + e.getMessage());
        }
    }
}
