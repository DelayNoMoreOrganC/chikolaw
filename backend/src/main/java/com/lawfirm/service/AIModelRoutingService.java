package com.lawfirm.service;

import com.lawfirm.config.LLMProperties;
import com.lawfirm.entity.AIConfig;
import com.lawfirm.enums.AIModelUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 按业务场景选择首选 AI 配置（与 llm.routing.* 对齐），找不到时回退到「默认 AI 配置」。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIModelRoutingService {

    private final AIConfigService aiConfigService;
    private final LLMProperties llmProperties;

    public AIConfig resolveForUseCase(AIModelUseCase useCase) {
        String provider = providerForUseCase(useCase);
        Optional<AIConfig> byProvider = aiConfigService.findFirstEnabledByProviderIgnoreCase(provider);
        if (byProvider.isPresent()) {
            log.debug("场景 {} 使用 provider={} 的配置 id={}", useCase, provider, byProvider.get().getId());
            return byProvider.get();
        }

        Optional<AIConfig> def = aiConfigService.findDefaultConfigOptional();
        if (def.isPresent()) {
            log.warn("场景 {} 未找到启用的 provider={}，回退到默认 AI 配置 id={}",
                    useCase, provider, def.get().getId());
            return def.get();
        }

        throw new RuntimeException(String.format(
                "未配置场景 %s 的模型：需要一条已启用且 providerType=%s 的 AI 配置，或设置一条默认 AI",
                useCase, provider));
    }

    private String providerForUseCase(AIModelUseCase useCase) {
        LLMProperties.RoutingConfig r = llmProperties.getRouting();
        if (r == null) {
            return "lmstudio";
        }
        switch (useCase) {
            case LEGAL_CHAT:
                return emptyToDefault(r.getLegalChat());
            case RAG:
                return emptyToDefault(r.getRag());
            case DOCUMENT:
                return emptyToDefault(r.getDocument());
            case GENERAL_CHAT:
                return emptyToDefault(r.getGeneralChat());
            case EXTRACT:
                return emptyToDefault(r.getExtract());
            case DOCUMENT_RECOGNITION_EXTRACT:
                return emptyToDefault(r.getDocumentRecognitionExtract());
            case LEGACY_DOCUMENT:
                return emptyToDefault(r.getLegacyDocument());
            default:
                return "lmstudio";
        }
    }

    private static String emptyToDefault(String s) {
        return (s == null || s.isBlank()) ? "lmstudio" : s.trim();
    }
}
