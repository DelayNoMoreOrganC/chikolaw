package com.lawfirm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 最近 LLM 调用摘要（不含密钥，供诊断接口展示）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmRecentCallSnapshot {
    private long epochMs;
    private String operation;
    private String primaryProvider;
    private boolean fallbackUsed;
    private long durationMs;
    private boolean success;
    private String errorHint;
    private String modelHint;

    public Map<String, Object> toMap() {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("epochMs", epochMs);
        m.put("operation", operation);
        m.put("primaryProvider", primaryProvider);
        m.put("fallbackUsed", fallbackUsed);
        m.put("durationMs", durationMs);
        m.put("success", success);
        m.put("errorHint", errorHint);
        m.put("modelHint", modelHint);
        return m;
    }
}
