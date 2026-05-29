package com.lawfirm.config;

import com.lawfirm.entity.AIConfig;
import com.lawfirm.repository.AIConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 确保存在启用的智谱 GLM Coding Plan 配置（纯线上模式）。
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class ZhipuAiConfigEnsurer implements CommandLineRunner {

    private final AIConfigRepository aiConfigRepository;
    private final LLMProperties llmProperties;

    @Override
    public void run(String... args) {
        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("未配置 ZHIPU_API_KEY，卷宗/对话等 AI 功能可能不可用（请在 backend/.env 中配置）");
            return;
        }

        List<AIConfig> zhipuConfigs = aiConfigRepository.findByProviderTypeAndDeletedFalse("zhipu");
        AIConfig target = zhipuConfigs.stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsEnabled()))
                .findFirst()
                .orElse(null);

        if (target == null) {
            target = new AIConfig();
            target.setConfigName("智谱 GLM Coding Plan");
            target.setProviderType("zhipu");
            target.setDeleted(false);
            target.setIsEnabled(true);
            target.setCategory("GENERAL");
            target.setDescription("GLM Coding Plan 线上模型（chat + vision）");
        }

        target.setApiKey(apiKey);
        target.setApiUrl(llmProperties.getZhipu().getBaseUrl());
        target.setModelName(llmProperties.getZhipu().getChatModel());
        target.setTemperature(llmProperties.getZhipu().getTemperature());
        target.setMaxTokens(llmProperties.getZhipu().getMaxTokens());
        target.setTimeoutSeconds(180);
        target.setIsDefault(true);

        aiConfigRepository.findAll().stream()
                .filter(c -> !Boolean.TRUE.equals(c.getDeleted()))
                .filter(c -> Boolean.TRUE.equals(c.getIsDefault()))
                .filter(c -> !"zhipu".equalsIgnoreCase(c.getProviderType()))
                .forEach(c -> {
                    c.setIsDefault(false);
                    aiConfigRepository.save(c);
                });

        aiConfigRepository.save(target);
        log.info("智谱 AI 配置已就绪: model={}, endpoint={}",
                target.getModelName(), target.getApiUrl());
    }

    private String resolveApiKey() {
        String env = System.getenv("ZHIPU_API_KEY");
        if (env != null && !env.isEmpty()) {
            return env;
        }
        env = System.getenv("GLM_CODING_API_KEY");
        if (env != null && !env.isEmpty()) {
            return env;
        }
        return llmProperties.getZhipu().getApiKey();
    }
}
