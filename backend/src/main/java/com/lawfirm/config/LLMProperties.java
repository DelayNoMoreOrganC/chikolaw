package com.lawfirm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * LLM API配置属性
 * 从application.yml读取LLM相关配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "llm")
public class LLMProperties {

    /**
     * DeepSeek配置
     */
    private DeepSeekConfig deepseek = new DeepSeekConfig();

    /**
     * 通义千问配置
     */
    private QwenConfig qwen = new QwenConfig();

    /**
     * LM Studio / 本地 OpenAI 兼容接口（默认 http://127.0.0.1:1234/v1/chat/completions）
     */
    private LmStudioConfig lmstudio = new LmStudioConfig();

    /**
     * 本地模型失败时降级策略
     */
    private FallbackConfig fallback = new FallbackConfig();

    /**
     * 各业务场景首选 provider（与库中 {@code ai_config.provider_type} 对应，如 lmstudio、deepseek）
     */
    private RoutingConfig routing = new RoutingConfig();

    /**
     * 超时配置（毫秒）
     */
    private int timeout = 30000;

    /**
     * 连接超时（毫秒）
     */
    private int connectTimeout = 10000;

    /**
     * 重试次数
     */
    private int retry = 3;

    /**
     * DeepSeek配置
     */
    @Data
    public static class DeepSeekConfig {
        private String apiKey;
        private String baseUrl = "https://api.deepseek.com";
        private String chatModel = "deepseek-chat";
        private String visionModel = "deepseek-vl";
        private int maxTokens = 4000;
        private double temperature = 0.7;
    }

    /**
     * 通义千问配置
     */
    @Data
    public static class QwenConfig {
        private String apiKey;
        private String baseUrl = "https://dashscope.aliyuncs.com";
        private String model = "qwen-plus";
    }

    @Data
    public static class LmStudioConfig {
        /** 服务根地址，勿带 /v1/chat/completions */
        private String baseUrl = "http://127.0.0.1:1234";
        /** LM Studio 对 API Key 不校验时可填任意非空占位 */
        private String apiKey = "lm-studio";
        /** 须与 LM Studio 中已加载模型名称一致 */
        private String chatModel = "";
        private int maxTokens = 8192;
        private double temperature = 0.7;
    }

    @Data
    public static class FallbackConfig {
        /**
         * 启用后，本地推理失败会自动降级到云端。
         */
        private boolean enabled = true;
        /**
         * 云端降级目标（当前建议 deepseek）。
         */
        private String provider = "deepseek";
    }

    @Data
    public static class RoutingConfig {
        private String legalChat = "lmstudio";
        private String rag = "lmstudio";
        private String document = "lmstudio";
        private String generalChat = "lmstudio";
        private String extract = "lmstudio";
        private String documentRecognitionExtract = "lmstudio";
        /** DocGenerateService 等旧入口 */
        private String legacyDocument = "lmstudio";
    }
}
