package com.lawfirm.controller;

import com.lawfirm.service.HermesAgentClient;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Hermes Agent 接入端口：健康检查、对话、通用 JSON 网关。
 * 设备侧 Hermes 地址见 {@code hermes.agent.base-url}。
 */
@RestController
@RequestMapping("/hermes")
@RequiredArgsConstructor
public class HermesAgentController {

    private final HermesAgentClient hermesAgentClient;

    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> status() {
        return Result.success(hermesAgentClient.health());
    }

    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, Object> body) {
        if (!hermesAgentClient.isEnabled()) {
            return Result.error("Hermes Agent 未启用，请在配置中设置 hermes.agent.enabled=true");
        }
        return Result.success(hermesAgentClient.chat(body));
    }

    /**
     * 通用网关：POST /api/hermes/gateway/v1/xxx 转发至 {baseUrl}/v1/xxx
     */
    @PostMapping("/gateway/**")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> gatewayPost(HttpServletRequest request,
                                             @RequestBody(required = false) String body) {
        if (!hermesAgentClient.isEnabled()) {
            return ResponseEntity.badRequest().body("{\"error\":\"Hermes Agent 未启用\"}");
        }
        String path = extractGatewayPath(request);
        return hermesAgentClient.forward(path, HttpMethod.POST, body, null);
    }

    private String extractGatewayPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String marker = "/hermes/gateway";
        int idx = uri.indexOf(marker);
        if (idx < 0) {
            return "/";
        }
        String suffix = uri.substring(idx + marker.length());
        return suffix.isEmpty() ? "/" : suffix;
    }
}
