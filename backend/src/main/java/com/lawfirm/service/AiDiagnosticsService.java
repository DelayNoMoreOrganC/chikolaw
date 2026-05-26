package com.lawfirm.service;

import com.lawfirm.config.LLMProperties;
import com.lawfirm.entity.AIConfig;
import com.lawfirm.enums.AIModelUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 路由与最近 LLM 调用诊断（不含密钥）。
 */
@Service
@RequiredArgsConstructor
public class AiDiagnosticsService {

    private final AIModelRoutingService aimodelRoutingService;
    private final LLMProperties llmProperties;
    private final LLMApiService llmApiService;

    public Map<String, Object> snapshot() {
        Map<String, Object> root = new LinkedHashMap<>();

        Map<String, Object> routing = new LinkedHashMap<>();
        for (AIModelUseCase u : AIModelUseCase.values()) {
            try {
                AIConfig c = aimodelRoutingService.resolveForUseCase(u);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("ok", true);
                row.put("providerType", c.getProviderType());
                row.put("configId", c.getId());
                row.put("modelName", c.getModelName());
                routing.put(u.name(), row);
            } catch (Exception e) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("ok", false);
                row.put("error", e.getMessage());
                routing.put(u.name(), row);
            }
        }
        root.put("routing", routing);

        Map<String, Object> fb = new LinkedHashMap<>();
        if (llmProperties.getFallback() != null) {
            fb.put("enabled", llmProperties.getFallback().isEnabled());
            fb.put("provider", llmProperties.getFallback().getProvider());
        }
        root.put("fallback", fb);

        LLMProperties.RoutingConfig r = llmProperties.getRouting();
        if (r != null) {
            Map<String, Object> rc = new LinkedHashMap<>();
            rc.put("legalChat", r.getLegalChat());
            rc.put("rag", r.getRag());
            rc.put("document", r.getDocument());
            rc.put("generalChat", r.getGeneralChat());
            rc.put("extract", r.getExtract());
            rc.put("documentRecognitionExtract", r.getDocumentRecognitionExtract());
            rc.put("legacyDocument", r.getLegacyDocument());
            root.put("routingYml", rc);
        }

        root.put("recentLlmCalls", llmApiService.getRecentLlmCallMaps());
        return root;
    }
}
