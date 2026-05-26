package com.lawfirm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 本地 Hermes Agent 接入配置（设备侧已加载 Agent，本系统仅做 HTTP 网关）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "hermes.agent")
public class HermesAgentProperties {

    /** 是否启用 Hermes（关闭时卷宗录入走内置 Vision/LLM） */
    private boolean enabled = false;

    /** Hermes 服务根地址，如 http://127.0.0.1:17888 */
    private String baseUrl = "http://127.0.0.1:17888";

    private int connectTimeoutMs = 5000;

    private int readTimeoutMs = 120000;

    /** 健康检查相对路径 */
    private String healthPath = "/health";

    /** 对话相对路径 */
    private String chatPath = "/v1/chat";

    /** 文档分析相对路径（可选，依 Hermes 实际 API 调整） */
    private String analyzePath = "/v1/analyze";
}
