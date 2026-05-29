package com.lawfirm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 律所 AI 运行模式（v2.2 纯 GLM 云端 / v3.0 可切换本地模型）。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "lawfirm.ai")
public class LawfirmAiProperties {

    /**
     * cloud-glm：全部对话/视觉/OCR 走智谱 GLM（v2.2 默认）。
     * hybrid：按 llm.routing.* 与 ai_config 选择本地（lmstudio/ollama）等（v3.0 预留）。
     */
    private String mode = "cloud-glm";

    public boolean isCloudGlm() {
        return mode == null || mode.isBlank() || "cloud-glm".equalsIgnoreCase(mode.trim());
    }

    public boolean isHybrid() {
        return "hybrid".equalsIgnoreCase(mode != null ? mode.trim() : "");
    }
}
