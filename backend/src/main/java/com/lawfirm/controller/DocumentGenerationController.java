package com.lawfirm.controller;

import com.lawfirm.dto.DocumentGenerateRequest;
import com.lawfirm.dto.TextToDocxRequest;
import com.lawfirm.entity.User;
import com.lawfirm.service.DocumentGenerationService;
import com.lawfirm.service.LegalDocumentDocxService;
import com.lawfirm.enums.DocumentTemplateType;
import com.lawfirm.util.DocumentTypeAliasResolver;
import com.lawfirm.util.Result;
import com.lawfirm.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * AI文书生成控制器
 * 提供法律文书生成接口
 */
@Slf4j
@RestController
@RequestMapping("/ai/documents")
@RequiredArgsConstructor
public class DocumentGenerationController {

    private final DocumentGenerationService documentGenerationService;
    private final LegalDocumentDocxService legalDocumentDocxService;
    private final SecurityUtil securityUtil;

    /**
     * 生成法律文书
     *
     * @param request 文书生成请求
     * @return 生成的文书内容
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'LAWYER')")
    public Result<String> generateDocument(@Valid @RequestBody DocumentGenerateRequest request) {
        try {
            request.setDocumentType(DocumentTypeAliasResolver.normalize(request.getDocumentType()));
            log.info("收到文书生成请求: 案件ID={}, 文书类型={}",
                    request.getCaseId(), request.getDocumentType());

            // 验证请求完整性
            documentGenerationService.validateRequest(request);

            // 获取当前用户ID
            Long userId = securityUtil.getCurrentUserId();

            // 生成文书
            String documentContent = documentGenerationService.generateDocument(request, userId);

            log.info("文书生成成功: 案件ID={}, 文书类型={}",
                    request.getCaseId(), request.getDocumentType());

            return Result.success(documentContent);

        } catch (Exception e) {
            log.error("文书生成失败: 案件ID={}, 文书类型={}, 错误={}",
                    request.getCaseId(), request.getDocumentType(), e.getMessage(), e);
            return Result.error("文书生成失败: " + e.getMessage());
        }
    }

    /**
     * 将文书正文导出为 Word (.docx)
     */
    @PostMapping("/export-docx")
    @PreAuthorize("hasAnyRole('ADMIN', 'LAWYER', 'ASSISTANT')")
    public ResponseEntity<byte[]> exportDocx(@Valid @RequestBody TextToDocxRequest request) {
        byte[] docx = legalDocumentDocxService.textToDocx(request.getTitle(), request.getContent());
        String name = request.getFileName();
        if (name == null || name.isBlank()) {
            name = (request.getTitle() != null ? request.getTitle() : "法律文书") + ".docx";
        } else if (!name.toLowerCase().endsWith(".docx")) {
            name = name + ".docx";
        }
        String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(docx);
    }

    /**
     * 获取支持的文书类型列表
     *
     * @return 文书类型列表
     */
    @GetMapping("/types")
    @PreAuthorize("hasAnyRole('ADMIN', 'LAWYER', 'ASSISTANT')")
    public Result<Object> getDocumentTypes() {
        java.util.List<DocumentType> types = new java.util.ArrayList<>();
        for (DocumentTemplateType t : DocumentTemplateType.values()) {
            String route = DocumentTypeAliasResolver.isLegacyDocumentType(t.name())
                    ? "LEGACY_DOCUMENT" : "DOCUMENT";
            types.add(new DocumentType(t.name(), t.getDescription(), t.getDescription(), route));
        }
        return Result.success(types);
    }

    /**
     * 获取文书类型信息
     */
    public static class DocumentType {
        private String code;
        private String name;
        private String description;
        private String route;

        public DocumentType(String code, String name, String description, String route) {
            this.code = code;
            this.name = name;
            this.description = description;
            this.route = route;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getRoute() {
            return route;
        }
    }
}
