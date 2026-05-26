package com.lawfirm.controller;

import com.lawfirm.dto.OcrExtractRequest;
import com.lawfirm.dto.DocGenerateRequest;
import com.lawfirm.service.DocGenerateService;
import com.lawfirm.service.LlmExtractService;
import com.lawfirm.service.OcrService;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

import java.util.Map;

/**
 * AI功能控制器 - PRD要求的端点
 * 对应PRD第552行: POST /api/ai/ocr-upload | extract | auto-fill | generate-doc
 *
 * 注意: chat和case-chat端点已存在于AiChatController，不再重复
 */
@Slf4j
@RestController
@RequestMapping("ai")
@RequiredArgsConstructor
public class AIFeaturesController {

    private final LlmExtractService llmExtractService;
    private final DocGenerateService docGenerateService;
    private final OcrService ocrService;
    private final SecurityUtils securityUtils;

    /**
     * @deprecated 遗留模拟 OCR，请使用 POST /api/ai/documents/recognize（AIDocumentService）。
     */
    @Deprecated
    @PostMapping("/ocr-upload")
    public Result<String> ocrUploadLegacy(
            @RequestParam("file") MultipartFile file,
            HttpServletResponse response) {
        response.setHeader(HttpHeaders.WARNING, "299 - \"Deprecated: use POST /api/ai/documents/recognize\"");
        log.warn("调用已废弃接口 /api/ai/ocr-upload，请迁移至 /api/ai/documents/recognize");
        try {
            if (file.isEmpty()) {
                return Result.error("文件不能为空");
            }
            String text = ocrService.recognizeDocument(file, "document");
            return Result.success("识别完成（遗留模拟路径，仅供联调）", text);
        } catch (Exception e) {
            log.error("遗留 OCR 失败", e);
            return Result.error("OCR识别失败: " + e.getMessage());
        }
    }

    /**
     * AI文书提取
     * POST /api/ai/extract
     *
     * 从OCR文本中提取法律要素
     */
    @PostMapping("/extract")
    public Result<Map<String, Object>> extractLegalElements(@RequestBody OcrExtractRequest request) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            log.info("AI文书提取请求，案件ID: {}, 文书类型: {}", request.getCaseId(), request.getDocumentType());

            Map<String, Object> extracted = llmExtractService.extractLegalElements(request, userId);
            return Result.success("提取成功", extracted);

        } catch (Exception e) {
            log.error("AI文书提取失败", e);
            return Result.error("提取失败: " + e.getMessage());
        }
    }

    /**
     * AI自动填充案件信息
     * POST /api/ai/auto-fill/{caseId}
     *
     * 根据案件信息自动生成文书模板填充内容
     */
    @PostMapping("/auto-fill/{caseId}")
    public Result<Map<String, Object>> autoFillCaseInfo(
            @PathVariable Long caseId,
            @RequestBody Map<String, String> request) {

        try {
            Long userId = securityUtils.getCurrentUserId();
            String templateType = request.get("templateType");

            log.info("AI自动填充请求，案件ID: {}, 模板类型: {}", caseId, templateType);

            // 构建DocGenerateRequest
            DocGenerateRequest generateRequest = new DocGenerateRequest();
            generateRequest.setCaseId(caseId);
            generateRequest.setDocumentType(templateType);

            // 调用文档生成服务进行自动填充
            String generatedContent = docGenerateService.generateDocument(generateRequest, userId);

            Map<String, Object> result = Map.of(
                    "caseId", caseId,
                    "templateType", templateType,
                    "content", generatedContent,
                    "generatedAt", java.time.LocalDateTime.now()
            );

            return Result.success("自动填充成功", result);

        } catch (Exception e) {
            log.error("AI自动填充失败", e);
            return Result.error("自动填充失败: " + e.getMessage());
        }
    }
}
