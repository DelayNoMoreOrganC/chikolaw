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
}
