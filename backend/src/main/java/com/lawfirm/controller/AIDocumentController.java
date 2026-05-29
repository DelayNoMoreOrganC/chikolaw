package com.lawfirm.controller;

import com.lawfirm.dto.AIDocumentRecognitionResult;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.service.AIDocumentService;
import com.lawfirm.util.AiDocumentFileSupport;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * AI文档智能识别控制器
 */
@Slf4j
@RestController
@RequestMapping("ai/documents")
@RequiredArgsConstructor
public class AIDocumentController {

    private final AIDocumentService aiDocumentService;
    private final SecurityUtils securityUtils;

    /**
     * 智能识别法院文书
     * POST /api/ai/documents/recognize
     *
     * @param file 上传的文档文件
     * @param caseId 关联的案件ID（可选）
     * @return 识别结果
     */
    @PostMapping("/recognize")
    public Result<AIDocumentRecognitionResult> recognizeLegalDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "caseId", required = false) Long caseId,
            @RequestParam(value = "executeBusinessLogic", defaultValue = "true") boolean executeBusinessLogic) {

        try {
            // 参数校验
            if (file.isEmpty()) {
                return Result.error("文件不能为空");
            }

            if (!AiDocumentFileSupport.isSupported(file)) {
                return Result.error(AiDocumentFileSupport.unsupportedMessage());
            }

            if (AiDocumentFileSupport.exceedsMaxSize(file)) {
                return Result.error(AiDocumentFileSupport.maxSizeMessage());
            }

            // 获取当前用户ID
            Long userId = securityUtils.getCurrentUserId();

            log.info("开始AI文档识别，文件名: {}, 大小: {}, 用户: {}, 关联案件: {}",
                    file.getOriginalFilename(), file.getSize(), userId, caseId);

            // 执行识别
            AIDocumentRecognitionResult result = aiDocumentService.recognizeLegalDocument(
                    file, userId, caseId, executeBusinessLogic);

            log.info("AI文档识别完成，案号: {}, 法院: {}, 文书类型: {}",
                    result.getCaseNumber(), result.getCourtName(), result.getDocumentType());

            return Result.success("文档识别成功", result);

        } catch (Exception e) {
            log.error("文档识别失败", e);
            return Result.error("文档识别失败: " + e.getMessage());
        }
    }

    /**
     * 批量识别文档
     * POST /api/ai/documents/recognize-batch
     *
     * @param files 上传的文档文件列表
     * @param caseId 关联的案件ID（可选）
     * @return 识别结果列表
     */
    @PostMapping("/recognize-batch")
    public Result<java.util.List<AIDocumentRecognitionResult>> recognizeLegalDocumentsBatch(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "caseId", required = false) Long caseId) {

        try {
            if (files == null || files.length == 0) {
                return Result.error("文件列表不能为空");
            }

            // 限制批量处理数量
            if (files.length > 10) {
                return Result.error("单次最多处理10个文件");
            }

            Long userId = securityUtils.getCurrentUserId();
            java.util.List<AIDocumentRecognitionResult> results = new java.util.ArrayList<>();

            for (MultipartFile file : files) {
                try {
                    AIDocumentRecognitionResult result =
                            aiDocumentService.recognizeLegalDocument(file, userId, caseId);
                    results.add(result);
                } catch (Exception e) {
                    log.error("识别文件失败: {}", file.getOriginalFilename(), e);
                    // 继续处理其他文件，不中断批量流程
                }
            }

            log.info("批量文档识别完成，成功: {}/{}", results.size(), files.length);

            return Result.success("批量识别完成", results);

        } catch (Exception e) {
            log.error("批量文档识别失败", e);
            return Result.error("批量识别失败: " + e.getMessage());
        }
    }
}