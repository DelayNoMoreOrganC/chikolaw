package com.lawfirm.controller;

import com.lawfirm.dto.DocumentGenerateRequest;
import com.lawfirm.entity.User;
import com.lawfirm.service.DocumentGenerationService;
import com.lawfirm.util.Result;
import com.lawfirm.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

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
     * 获取支持的文书类型列表
     *
     * @return 文书类型列表
     */
    @GetMapping("/types")
    @PreAuthorize("hasAnyRole('ADMIN', 'LAWYER', 'ASSISTANT')")
    public Result<Object> getDocumentTypes() {
        return Result.success(new Object[]{
                new DocumentType("COMPLAINT", "起诉状", "民事、行政、刑事自诉案件的起诉文书"),
                new DocumentType("DEFENSE_STATEMENT", "答辩状", "被告针对起诉状的答辩文书"),
                new DocumentType("BRIEF", "代理词", "律师在法庭上发表的代理意见"),
                new DocumentType("LEGAL_OPINION", "法律意见书", "就特定法律问题出具的专业意见")
        });
    }

    /**
     * 获取文书类型信息
     */
    public static class DocumentType {
        private String code;
        private String name;
        private String description;

        public DocumentType(String code, String name, String description) {
            this.code = code;
            this.name = name;
            this.description = description;
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
    }
}
