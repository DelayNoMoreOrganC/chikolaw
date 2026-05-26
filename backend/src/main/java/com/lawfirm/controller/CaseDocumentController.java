package com.lawfirm.controller;

import com.lawfirm.annotation.AuditLog;
import com.lawfirm.dto.CaseDocumentDTO;
import com.lawfirm.service.CaseDocumentService;
import com.lawfirm.service.OfficeDocumentPreviewService;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 案件文档控制器
 */
@Slf4j
@RestController
@RequestMapping("cases/{caseId}/documents")
@RequiredArgsConstructor
public class CaseDocumentController {

    private final CaseDocumentService caseDocumentService;
    private final OfficeDocumentPreviewService officeDocumentPreviewService;
    private final SecurityUtils securityUtils;

    /**
     * 上传案件文档
     * POST /api/cases/{caseId}/documents
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @AuditLog(value = "上传案件文档", operationType = "UPLOAD", logParams = false)
    public Result<CaseDocumentDTO> uploadDocument(
            @PathVariable Long caseId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType,
            @RequestParam(value = "folderPath", required = false) String folderPath) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            CaseDocumentDTO result = caseDocumentService.uploadDocument(
                    caseId, file, documentType, folderPath, userId);
            return Result.success("文档上传成功", result);
        } catch (IllegalArgumentException e) {
            log.error("上传文档失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (IOException e) {
            log.error("上传文档异常", e);
            return Result.error("文档上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取案件文档列表
     * GET /api/cases/{caseId}/documents
     */
    @GetMapping
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public Result<List<CaseDocumentDTO>> getCaseDocuments(@PathVariable Long caseId) {
        try {
            List<CaseDocumentDTO> documents = caseDocumentService.getCaseDocuments(caseId);
            return Result.success(documents);
        } catch (Exception e) {
            log.error("获取案件文档列表异常", e);
            return Result.error("获取案件文档列表失败");
        }
    }

    /**
     * 根据类型获取文档列表
     * GET /api/cases/{caseId}/documents/type/{documentType}
     */
    @GetMapping("/type/{documentType}")
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public Result<List<CaseDocumentDTO>> getDocumentsByType(
            @PathVariable Long caseId,
            @PathVariable String documentType) {
        try {
            List<CaseDocumentDTO> documents = caseDocumentService.getDocumentsByType(documentType);
            return Result.success(documents);
        } catch (Exception e) {
            log.error("按类型获取文档列表异常", e);
            return Result.error("获取文档列表失败");
        }
    }

    /**
     * 获取文档详情
     * GET /api/cases/{caseId}/documents/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public Result<CaseDocumentDTO> getDocument(
            @PathVariable Long caseId,
            @PathVariable Long id) {
        try {
            CaseDocumentDTO document = caseDocumentService.getDocumentById(id);
            return Result.success(document);
        } catch (IllegalArgumentException e) {
            log.error("获取文档详情失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("获取文档详情异常", e);
            return Result.error("获取文档详情失败");
        }
    }

    /**
     * 更新文档信息
     * PUT /api/cases/{caseId}/documents/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    @AuditLog(value = "更新案件文档", operationType = "UPDATE")
    public Result<CaseDocumentDTO> updateDocument(
            @PathVariable Long caseId,
            @PathVariable Long id,
            @RequestBody CaseDocumentDTO dto) {
        try {
            CaseDocumentDTO result = caseDocumentService.updateDocument(id, dto);
            return Result.success("文档更新成功", result);
        } catch (IllegalArgumentException e) {
            log.error("更新文档失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("更新文档异常", e);
            return Result.error("更新文档失败");
        }
    }

    /**
     * 移动文档到其他文件夹
     * PUT /api/cases/{caseId}/documents/{id}/move
     */
    @PutMapping("/{id}/move")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    public Result<CaseDocumentDTO> moveDocument(
            @PathVariable Long caseId,
            @PathVariable Long id,
            @RequestParam String folderPath) {
        try {
            CaseDocumentDTO result = caseDocumentService.moveDocument(id, folderPath);
            return Result.success("文档移动成功", result);
        } catch (IllegalArgumentException e) {
            log.error("移动文档失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("移动文档异常", e);
            return Result.error("移动文档失败");
        }
    }

    /**
     * 删除文档
     * DELETE /api/cases/{caseId}/documents/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    @AuditLog(value = "删除案件文档", operationType = "DELETE")
    public Result<Void> deleteDocument(
            @PathVariable Long caseId,
            @PathVariable Long id) {
        try {
            caseDocumentService.deleteDocument(id);
            return Result.success();
        } catch (IllegalArgumentException e) {
            log.error("删除文档失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("删除文档异常", e);
            return Result.error("删除文档失败");
        }
    }

    /**
     * 下载文档
     * GET /api/cases/{caseId}/documents/{id}/download
     */
    @GetMapping("/{id}/download")
    @PreAuthorize("isAuthenticated()")
    public void downloadDocument(
            @PathVariable Long caseId,
            @PathVariable Long id,
            @RequestParam(value = "inline", defaultValue = "false") boolean inline,
            HttpServletResponse response) throws IOException {
        streamDocument(caseId, id, inline, response);
    }

    /**
     * 在线预览（inline）
     */
    @GetMapping("/{id}/preview")
    @PreAuthorize("isAuthenticated()")
    public void previewDocument(
            @PathVariable Long caseId,
            @PathVariable Long id,
            HttpServletResponse response) throws IOException {
        streamDocument(caseId, id, true, response);
    }

    /**
     * Office 文档局域网预览（服务端转 HTML，适用于 doc/docx/xls/xlsx/ppt/pptx）
     */
    @GetMapping(value = "/{id}/preview-html", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    @PreAuthorize("isAuthenticated()")
    public void previewOfficeHtml(
            @PathVariable Long caseId,
            @PathVariable Long id,
            HttpServletResponse response) throws IOException {
        try {
            CaseDocumentDTO document = caseDocumentService.getDocumentById(id);
            if (!caseId.equals(document.getCaseId())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            if (!officeDocumentPreviewService.supports(document.getDocumentName())) {
                response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                response.setContentType(MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8");
                response.getWriter().write("该文件类型不支持 Office 预览");
                return;
            }
            long size = document.getFileSize() != null ? document.getFileSize() : 0L;
            String html;
            try (java.io.InputStream in = caseDocumentService.openDocumentStream(id)) {
                html = officeDocumentPreviewService.convertToHtml(in, document.getDocumentName(), size);
            }
            response.setContentType(MediaType.TEXT_HTML_VALUE + ";charset=UTF-8");
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
            response.getWriter().write(html);
            response.flushBuffer();
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType(MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8");
            response.getWriter().write(e.getMessage());
        } catch (Exception e) {
            log.error("Office 预览失败: caseId={}, docId={}", caseId, id, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType(MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8");
            response.getWriter().write("预览失败: " + e.getMessage());
        }
    }

    private void streamDocument(Long caseId, Long id, boolean inline, HttpServletResponse response)
            throws IOException {
        try {
            CaseDocumentDTO document = caseDocumentService.getDocumentById(id);
            if (!caseId.equals(document.getCaseId())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            if (document.getFilePath() == null || document.getFilePath().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("文件路径不存在");
                return;
            }

            com.lawfirm.entity.CaseDocument entity = new com.lawfirm.entity.CaseDocument();
            entity.setContentType(document.getContentType());
            entity.setDocumentName(document.getDocumentName());
            String contentType = caseDocumentService.resolvePreviewContentType(entity);
            response.setContentType(contentType);
            String disposition = inline ? "inline" : "attachment";
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    disposition + "; filename=\"" + java.net.URLEncoder.encode(
                            document.getDocumentName(), "UTF-8").replace("+", "%20") + "\"");

            try (java.io.InputStream inputStream = caseDocumentService.openDocumentStream(id)) {
                org.springframework.util.StreamUtils.copy(inputStream, response.getOutputStream());
            }
            response.flushBuffer();
        } catch (Exception e) {
            log.error("文档流输出失败", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("输出失败: " + e.getMessage());
        }
    }
}
