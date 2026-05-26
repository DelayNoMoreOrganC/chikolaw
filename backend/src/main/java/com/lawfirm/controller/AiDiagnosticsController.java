package com.lawfirm.controller;

import com.lawfirm.service.AiDiagnosticsService;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AI 可观测性：场景路由解析、降级配置、最近 LLM 调用摘要。
 */
@RestController
@RequestMapping("/ai/diagnostics")
@RequiredArgsConstructor
public class AiDiagnosticsController {

    private final AiDiagnosticsService aiDiagnosticsService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DIRECTOR','MANAGER')")
    public Result<Map<String, Object>> snapshot() {
        return Result.success(aiDiagnosticsService.snapshot());
    }
}
