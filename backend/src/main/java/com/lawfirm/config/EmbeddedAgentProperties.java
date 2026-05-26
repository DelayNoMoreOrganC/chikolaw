package com.lawfirm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 系统内嵌 Agent 运行时：默认内置 LLM；可选对接本机 OpenClaw Gateway 或 Hermes。
 */
@Data
@Component
@ConfigurationProperties(prefix = "agent.runtime")
public class EmbeddedAgentProperties {

    /**
     * 分析引擎：builtin（默认）| openclaw | hermes
     */
    private String provider = "builtin";

    private OpenClaw openclaw = new OpenClaw();

    @Data
    public static class OpenClaw {
        /** OpenClaw Gateway OpenAI 兼容地址 */
        private String baseUrl = "http://127.0.0.1:18789/v1";
        private String apiKey = "openclaw-local";
        private String model = "main";
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 120000;
    }
}
