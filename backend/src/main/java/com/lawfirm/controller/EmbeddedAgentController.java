package com.lawfirm.controller;

import com.lawfirm.service.EmbeddedAgentService;
import com.lawfirm.service.HermesAgentClient;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 内嵌 Agent 运行时状态（builtin / OpenClaw / Hermes）。
 */
@RestController
@RequestMapping("/agent/runtime")
@RequiredArgsConstructor
public class EmbeddedAgentController {

    private final EmbeddedAgentService embeddedAgentService;
    private final HermesAgentClient hermesAgentClient;

    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> status() {
        return Result.success(embeddedAgentService.runtimeStatus());
    }

    /** 保留 Hermes 专用状态（便于设备侧联调） */
    @GetMapping("/hermes")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> hermesStatus() {
        return Result.success(hermesAgentClient.health());
    }
}
