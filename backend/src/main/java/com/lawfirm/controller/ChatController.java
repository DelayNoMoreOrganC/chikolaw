package com.lawfirm.controller;

import com.lawfirm.dto.LegalChatRequest;
import com.lawfirm.dto.RAGChatRequest;
import com.lawfirm.service.LegalChatService;
import com.lawfirm.service.RAGService;
import com.lawfirm.util.Result;
import com.lawfirm.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 聊天控制器
 * 提供法律咨询和RAG检索问答接口
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final LegalChatService legalChatService;
    private final RAGService ragService;
    private final SecurityUtil securityUtil;

    /**
     * 通用法律咨询
     * POST /api/chat/legal
     *
     * @param request 请求参数
     * @return AI回复
     */
    @PostMapping("/legal")
    @PreAuthorize("isAuthenticated()")
    public Result<String> legalConsult(@RequestBody LegalChatRequest request) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            String response = legalChatService.generalConsult(request, userId);
            return Result.success(response);
        } catch (Exception e) {
            log.error("法律咨询失败", e);
            return Result.error("法律咨询失败: " + e.getMessage());
        }
    }

    /**
     * RAG检索问答
     * POST /api/chat/rag
     *
     * @param request 请求参数
     * @return 包含答案和来源的结果
     */
    @PostMapping("/rag")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> ragChat(@RequestBody RAGChatRequest request) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            Map<String, Object> result = ragService.ragChat(request, userId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("RAG检索失败", e);
            return Result.error("RAG检索失败: " + e.getMessage());
        }
    }

    /**
     * 健康检查
     * GET /api/chat/health
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    public Result<Map<String, String>> health() {
        return Result.success(Map.of(
                "status", "ok",
                "service", "Chat Service",
                "version", "1.0.0",
                "features", "legal,rag"
        ));
    }
}
