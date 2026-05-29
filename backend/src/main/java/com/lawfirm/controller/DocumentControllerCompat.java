package com.lawfirm.controller;

import com.lawfirm.dto.CaseDocumentDTO;
import com.lawfirm.service.CaseDocumentService;
import com.lawfirm.util.PageResult;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档中心：跨案件文档聚合检索
 */
@Slf4j
@RestController
@RequestMapping("documents")
@RequiredArgsConstructor
public class DocumentControllerCompat {

    private final CaseDocumentService caseDocumentService;

    /**
     * GET /api/documents?page=1&size=20&caseId=&documentType=&keyword=
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> listDocuments(
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) Long caseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            PageResult<CaseDocumentDTO> pageResult = caseDocumentService.searchDocuments(
                    caseId, documentType, keyword, page, size);
            Map<String, Object> body = new HashMap<>();
            body.put("stats", caseDocumentService.getDocumentCenterStats());
            body.put("page", pageResult);
            return Result.success(body);
        } catch (Exception e) {
            log.error("获取文档列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 兼容旧前端：无分页参数时返回全量列表
     */
    @GetMapping("/all")
    @PreAuthorize("isAuthenticated()")
    public Result<List<CaseDocumentDTO>> getAllDocumentsLegacy(
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) Long caseId) {
        try {
            List<CaseDocumentDTO> documents;
            if (caseId != null) {
                documents = caseDocumentService.getCaseDocuments(caseId);
            } else if (documentType != null && !documentType.isBlank()) {
                documents = caseDocumentService.getDocumentsByType(documentType);
            } else {
                documents = caseDocumentService.getAllDocuments();
            }
            return Result.success(documents);
        } catch (Exception e) {
            log.error("获取文档列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<CaseDocumentDTO> getDocument(@PathVariable Long id) {
        try {
            return Result.success(caseDocumentService.getDocumentById(id));
        } catch (Exception e) {
            log.error("获取文档详情失败", e);
            return Result.error(e.getMessage());
        }
    }
}
