package com.lawfirm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.config.LawfirmAiProperties;
import com.lawfirm.config.LLMProperties;
import com.lawfirm.dto.AIConfigDTO;
import com.lawfirm.dto.LlmRecentCallSnapshot;
import com.lawfirm.entity.AIConfig;
import com.lawfirm.exception.AIServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM API客户端服务
 * 封装DeepSeek API、通义千问API等LLM服务调用
 * 支持聊天接口和视觉接口，具备超时和重试机制
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LLMApiService {

    private final AIConfigService aiConfigService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final LLMProperties llmProperties;
    private final LawfirmAiProperties lawfirmAiProperties;

    private static final int MAX_RECENT_LLM_CALLS = 50;
    private final ArrayDeque<LlmRecentCallSnapshot> recentLlmCalls = new ArrayDeque<>();

    /**
     * 最近若干次 LLM 调用摘要（内存环，进程重启后清空），供诊断与排错。
     */
    public List<Map<String, Object>> getRecentLlmCallMaps() {
        synchronized (recentLlmCalls) {
            List<Map<String, Object>> out = new ArrayList<>();
            for (LlmRecentCallSnapshot s : recentLlmCalls) {
                out.add(s.toMap());
            }
            return out;
        }
    }

    private void pushRecentLlm(LlmRecentCallSnapshot snapshot) {
        synchronized (recentLlmCalls) {
            while (recentLlmCalls.size() >= MAX_RECENT_LLM_CALLS) {
                recentLlmCalls.removeLast();
            }
            recentLlmCalls.addFirst(snapshot);
        }
    }

    private static String trimErr(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 240 ? message.substring(0, 240) + "…" : message;
    }

    /**
     * 调用DeepSeek聊天接口
     *
     * @param prompt 用户提示
     * @return AI回复
     */
    public String chatWithDeepSeek(String prompt) {
        return chatWithDeepSeek(prompt, null);
    }

    /**
     * 调用DeepSeek聊天接口（带系统提示）
     *
     * @param prompt 用户提示
     * @param systemPrompt 系统提示
     * @return AI回复
     */
    public String chatWithDeepSeek(String prompt, String systemPrompt) {
        String apiKey = getDeepSeekApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            throw new AIServiceException("DeepSeek API密钥未配置");
        }

        String url = llmProperties.getDeepseek().getBaseUrl() + "/v1/chat/completions";

        // 构建请求体
        Map<String, Object> requestBody = buildChatRequest(prompt, systemPrompt, llmProperties.getDeepseek().getChatModel());

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 带重试的API调用
        return callApiWithRetry(url, entity, "DeepSeek");
    }

    /**
     * 调用DeepSeek视觉接口（图片识别）
     *
     * @param prompt 用户提示
     * @param imageBase64 Base64编码的图片
     * @return AI回复
     */
    public String visionWithDeepSeek(String prompt, String imageBase64) {
        long t0 = System.currentTimeMillis();
        String apiKey = getDeepSeekApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            pushRecentLlm(LlmRecentCallSnapshot.builder()
                    .epochMs(System.currentTimeMillis())
                    .operation("vision")
                    .primaryProvider("deepseek")
                    .fallbackUsed(false)
                    .durationMs(System.currentTimeMillis() - t0)
                    .success(false)
                    .errorHint("DeepSeek API密钥未配置")
                    .modelHint(llmProperties.getDeepseek().getVisionModel())
                    .build());
            throw new AIServiceException("DeepSeek API密钥未配置");
        }

        String url = llmProperties.getDeepseek().getBaseUrl() + "/v1/chat/completions";

        // 构建请求体（包含图片）
        Map<String, Object> requestBody = buildVisionRequest(prompt, imageBase64, llmProperties.getDeepseek().getVisionModel());

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            String text = callApiWithRetry(url, entity, "DeepSeek Vision");
            long ms = System.currentTimeMillis() - t0;
            log.info("LLM vision ok durationMs={} model={}", ms, llmProperties.getDeepseek().getVisionModel());
            pushRecentLlm(LlmRecentCallSnapshot.builder()
                    .epochMs(System.currentTimeMillis())
                    .operation("vision")
                    .primaryProvider("deepseek")
                    .fallbackUsed(false)
                    .durationMs(ms)
                    .success(true)
                    .errorHint(null)
                    .modelHint(llmProperties.getDeepseek().getVisionModel())
                    .build());
            return text;
        } catch (AIServiceException ex) {
            long ms = System.currentTimeMillis() - t0;
            log.warn("LLM vision failed durationMs={} err={}", ms, ex.getMessage());
            pushRecentLlm(LlmRecentCallSnapshot.builder()
                    .epochMs(System.currentTimeMillis())
                    .operation("vision")
                    .primaryProvider("deepseek")
                    .fallbackUsed(false)
                    .durationMs(ms)
                    .success(false)
                    .errorHint(trimErr(ex.getMessage()))
                    .modelHint(llmProperties.getDeepseek().getVisionModel())
                    .build());
            throw ex;
        }
    }

    /**
     * 智谱 GLM 视觉（Coding Plan，OpenAI 兼容多模态）
     */
    public String visionWithZhipu(String prompt, String imageBase64) {
        return visionWithZhipu(prompt, imageBase64, "image/jpeg");
    }

    public String visionWithZhipu(String prompt, String imageBase64, String imageMime) {
        long t0 = System.currentTimeMillis();
        String apiKey = getZhipuApiKey();
        String visionModel = llmProperties.getZhipu().getVisionModel();
        if (apiKey == null || apiKey.isEmpty()) {
            pushRecentLlm(LlmRecentCallSnapshot.builder()
                    .epochMs(System.currentTimeMillis())
                    .operation("vision")
                    .primaryProvider("zhipu")
                    .fallbackUsed(false)
                    .durationMs(System.currentTimeMillis() - t0)
                    .success(false)
                    .errorHint("智谱 API 密钥未配置（ZHIPU_API_KEY）")
                    .modelHint(visionModel)
                    .build());
            throw new AIServiceException("智谱 API 密钥未配置，请设置环境变量 ZHIPU_API_KEY");
        }

        String url = zhipuChatCompletionsUrl(null);
        String fallbackUrl = zhipuChatCompletionsFallbackUrl(url);
        Map<String, Object> requestBody = buildVisionRequest(
                prompt, imageBase64, visionModel,
                llmProperties.getZhipu().getTemperature(),
                llmProperties.getZhipu().getMaxTokens(),
                imageMime);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            String text = callApiWithRetryUrls(url, fallbackUrl, entity, "Zhipu Vision");
            long ms = System.currentTimeMillis() - t0;
            pushRecentLlm(LlmRecentCallSnapshot.builder()
                    .epochMs(System.currentTimeMillis())
                    .operation("vision")
                    .primaryProvider("zhipu")
                    .fallbackUsed(false)
                    .durationMs(ms)
                    .success(true)
                    .errorHint(null)
                    .modelHint(visionModel)
                    .build());
            return text;
        } catch (AIServiceException ex) {
            long ms = System.currentTimeMillis() - t0;
            pushRecentLlm(LlmRecentCallSnapshot.builder()
                    .epochMs(System.currentTimeMillis())
                    .operation("vision")
                    .primaryProvider("zhipu")
                    .fallbackUsed(false)
                    .durationMs(ms)
                    .success(false)
                    .errorHint(trimErr(ex.getMessage()))
                    .modelHint(visionModel)
                    .build());
            throw ex;
        }
    }

    /**
     * 调用通义千问聊天接口
     *
     * @param prompt 用户提示
     * @return AI回复
     */
    public String chatWithQwen(String prompt) {
        return chatWithQwen(prompt, null);
    }

    /**
     * 调用通义千问聊天接口（带系统提示）
     *
     * @param prompt 用户提示
     * @param systemPrompt 系统提示
     * @return AI回复
     */
    public String chatWithQwen(String prompt, String systemPrompt) {
        String apiKey = llmProperties.getQwen().getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            throw new AIServiceException("通义千问API密钥未配置");
        }

        String url = llmProperties.getQwen().getBaseUrl() + "/api/v1/services/aigc/text-generation/generation";

        // 构建请求体
        Map<String, Object> requestBody = buildQwenRequest(prompt, systemPrompt, llmProperties.getQwen().getModel());

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 带重试的API调用
        return callApiWithRetry(url, entity, "通义千问");
    }

    /**
     * 使用配置中的AI服务进行对话
     *
     * @param prompt 用户提示
     * @param config AI配置
     * @return AI回复
     */
    public String chatWithConfig(String prompt, AIConfig config) {
        return chatWithConfig(prompt, null, config);
    }

    /**
     * @param systemPrompt 优先使用；为空时使用 {@link AIConfig#getSystemPrompt()}
     */
    public String chatWithConfig(String prompt, String systemPrompt, AIConfig config) {
        if (config == null) {
            throw new AIServiceException("AI配置不能为空");
        }

        String provider = config.getProviderType();
        if (provider == null || provider.isEmpty()) {
            throw new AIServiceException("AI提供商类型不能为空");
        }

        String mergedSystem = systemPrompt;
        if (mergedSystem == null || mergedSystem.isEmpty()) {
            mergedSystem = config.getSystemPrompt();
        }

        long t0 = System.currentTimeMillis();
        String modelHint = config.getModelName() != null ? config.getModelName() : provider;
        try {
            String result = chatByProvider(provider, prompt, mergedSystem, config);
            long ms = System.currentTimeMillis() - t0;
            log.info("LLM chat ok provider={} durationMs={} model={}", provider, ms, modelHint);
            pushRecentLlm(LlmRecentCallSnapshot.builder()
                    .epochMs(System.currentTimeMillis())
                    .operation("chat")
                    .primaryProvider(provider)
                    .fallbackUsed(false)
                    .durationMs(ms)
                    .success(true)
                    .errorHint(null)
                    .modelHint(modelHint)
                    .build());
            return result;
        } catch (AIServiceException ex) {
            if (shouldFallback(provider)) {
                log.warn("主模型调用失败，准备降级到 {}。provider={}, err={}",
                        llmProperties.getFallback().getProvider(), provider, ex.getMessage());
                try {
                    String result = chatWithFallbackProvider(prompt, mergedSystem);
                    long ms = System.currentTimeMillis() - t0;
                    log.info("LLM chat ok after fallback durationMs={} primary={}", ms, provider);
                    pushRecentLlm(LlmRecentCallSnapshot.builder()
                            .epochMs(System.currentTimeMillis())
                            .operation("chat")
                            .primaryProvider(provider)
                            .fallbackUsed(true)
                            .durationMs(ms)
                            .success(true)
                            .errorHint(trimErr(ex.getMessage()))
                            .modelHint("fallback:" + llmProperties.getFallback().getProvider())
                            .build());
                    return result;
                } catch (Exception ex2) {
                    long ms = System.currentTimeMillis() - t0;
                    log.error("LLM fallback 仍失败 primary={} err={}", provider, ex2.getMessage());
                    pushRecentLlm(LlmRecentCallSnapshot.builder()
                            .epochMs(System.currentTimeMillis())
                            .operation("chat")
                            .primaryProvider(provider)
                            .fallbackUsed(true)
                            .durationMs(ms)
                            .success(false)
                            .errorHint(trimErr(ex2.getMessage()))
                            .modelHint(modelHint)
                            .build());
                    if (ex2 instanceof RuntimeException) {
                        throw (RuntimeException) ex2;
                    }
                    throw new AIServiceException("LLM 调用失败: " + ex2.getMessage(), ex2);
                }
            }
            long ms = System.currentTimeMillis() - t0;
            pushRecentLlm(LlmRecentCallSnapshot.builder()
                    .epochMs(System.currentTimeMillis())
                    .operation("chat")
                    .primaryProvider(provider)
                    .fallbackUsed(false)
                    .durationMs(ms)
                    .success(false)
                    .errorHint(trimErr(ex.getMessage()))
                    .modelHint(modelHint)
                    .build());
            throw ex;
        }
    }

    private String chatByProvider(String provider, String prompt, String systemPrompt, AIConfig config) {
        switch (provider.toLowerCase()) {
            case "zhipu":
            case "glm":
            case "zai":
                return chatWithZhipuByConfig(prompt, systemPrompt, config);
            case "deepseek":
                return chatWithDeepSeekByConfig(prompt, systemPrompt, config);
            case "qwen":
            case "aliyun":
                return chatWithQwenByConfig(prompt, systemPrompt, config);
            case "openai":
            case "lmstudio":
                return chatWithOpenAIByConfig(prompt, systemPrompt, config);
            case "ollama":
                return chatWithOllamaByConfig(prompt, systemPrompt, config);
            default:
                throw new AIServiceException("不支持的AI提供商: " + provider);
        }
    }

    private boolean shouldFallback(String provider) {
        if (lawfirmAiProperties.isCloudGlm()) {
            return false;
        }
        if (!llmProperties.getFallback().isEnabled()) {
            return false;
        }
        if (provider == null) {
            return true;
        }
        String p = provider.toLowerCase();
        return !"zhipu".equals(p) && !"glm".equals(p) && !"zai".equals(p) && !"deepseek".equals(p);
    }

    private String chatWithFallbackProvider(String prompt, String systemPrompt) {
        String fallbackProvider = llmProperties.getFallback().getProvider();
        if (fallbackProvider == null || fallbackProvider.isEmpty()) {
            fallbackProvider = "zhipu";
        }
        String fp = fallbackProvider.toLowerCase();
        if ("zhipu".equals(fp) || "glm".equals(fp) || "zai".equals(fp)) {
            AIConfig zhipuConfig = findEnabledProviderConfig("zhipu");
            if (zhipuConfig != null) {
                return chatWithZhipuByConfig(prompt, systemPrompt, zhipuConfig);
            }
            AIConfig temp = new AIConfig();
            temp.setProviderType("zhipu");
            temp.setApiUrl(llmProperties.getZhipu().getBaseUrl());
            temp.setModelName(llmProperties.getZhipu().getChatModel());
            temp.setTemperature(llmProperties.getZhipu().getTemperature());
            temp.setMaxTokens(llmProperties.getZhipu().getMaxTokens());
            return chatWithZhipuByConfig(prompt, systemPrompt, temp);
        }
        if ("deepseek".equals(fp)) {
            AIConfig deepseekConfig = findEnabledProviderConfig("deepseek");
            if (deepseekConfig != null) {
                return chatWithDeepSeekByConfig(prompt, systemPrompt, deepseekConfig);
            }
            AIConfig temp = new AIConfig();
            temp.setProviderType("deepseek");
            temp.setApiUrl(llmProperties.getDeepseek().getBaseUrl());
            temp.setModelName(llmProperties.getDeepseek().getChatModel());
            temp.setTemperature(llmProperties.getDeepseek().getTemperature());
            temp.setMaxTokens(llmProperties.getDeepseek().getMaxTokens());
            return chatWithDeepSeekByConfig(prompt, systemPrompt, temp);
        }
        throw new AIServiceException("不支持的降级 provider: " + fallbackProvider);
    }

    private AIConfig findEnabledProviderConfig(String providerType) {
        try {
            return aiConfigService.getConfigsByProvider(providerType).stream()
                    .filter(c -> Boolean.TRUE.equals(c.getIsEnabled()))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.debug("读取{}配置失败: {}", providerType, e.getMessage());
            return null;
        }
    }

    /**
     * 使用配置中的DeepSeek服务进行对话
     */
    private String chatWithDeepSeekByConfig(String prompt, String systemPrompt, AIConfig config) {
        String apiKey = config.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = getDeepSeekApiKey();
        }
        if (apiKey == null || apiKey.isEmpty()) {
            throw new AIServiceException("DeepSeek API密钥未配置");
        }

        String baseUrl = config.getApiUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = llmProperties.getDeepseek().getBaseUrl();
        }

        String model = config.getModelName();
        if (model == null || model.isEmpty()) {
            model = llmProperties.getDeepseek().getChatModel();
        }

        String url = baseUrl.replaceAll("/+$", "") + "/v1/chat/completions";

        double temperature = config.getTemperature() != null
                ? config.getTemperature() : llmProperties.getDeepseek().getTemperature();
        int maxTokens = config.getMaxTokens() != null
                ? config.getMaxTokens() : llmProperties.getDeepseek().getMaxTokens();

        Map<String, Object> requestBody = buildChatRequest(prompt, systemPrompt, model, temperature, maxTokens);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        return callApiWithRetry(url, entity, "DeepSeek");
    }

    /**
     * 智谱 GLM Coding Plan（OpenAI 兼容 /chat/completions）
     */
    private String chatWithZhipuByConfig(String prompt, String systemPrompt, AIConfig config) {
        String apiKey = config.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = getZhipuApiKey();
        }
        if (apiKey == null || apiKey.isEmpty()) {
            throw new AIServiceException("智谱 API 密钥未配置");
        }

        String model = config.getModelName();
        if (model == null || model.isEmpty()) {
            model = llmProperties.getZhipu().getChatModel();
        }

        double temperature = config.getTemperature() != null
                ? config.getTemperature() : llmProperties.getZhipu().getTemperature();
        int maxTokens = config.getMaxTokens() != null
                ? config.getMaxTokens() : llmProperties.getZhipu().getMaxTokens();

        Map<String, Object> requestBody = buildChatRequest(prompt, systemPrompt, model, temperature, maxTokens);
        String url = zhipuChatCompletionsUrl(config.getApiUrl());
        String fallbackUrl = zhipuChatCompletionsFallbackUrl(url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        return callApiWithRetryUrls(url, fallbackUrl, entity, "Zhipu");
    }

    private String zhipuChatCompletionsUrl(String apiUrlOverride) {
        String baseUrl = apiUrlOverride;
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = llmProperties.getZhipu().getBaseUrl();
        }
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "https://open.bigmodel.cn/api/coding/paas/v4";
        }
        return baseUrl.replaceAll("/+$", "") + "/chat/completions";
    }

    /**
     * 智谱部分节点会出现 EOF，提供 coding/paas 与 paas 端点互备。
     */
    private String zhipuChatCompletionsFallbackUrl(String primaryUrl) {
        if (primaryUrl == null || primaryUrl.isBlank()) {
            return null;
        }
        if (primaryUrl.contains("/api/coding/paas/v4/")) {
            return primaryUrl.replace("/api/coding/paas/v4/", "/api/paas/v4/");
        }
        if (primaryUrl.contains("/api/paas/v4/")) {
            return primaryUrl.replace("/api/paas/v4/", "/api/coding/paas/v4/");
        }
        return null;
    }

    /**
     * 使用配置中的通义千问服务进行对话
     */
    private String chatWithQwenByConfig(String prompt, String systemPrompt, AIConfig config) {
        String apiKey = config.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = llmProperties.getQwen().getApiKey();
        }
        if (apiKey == null || apiKey.isEmpty()) {
            throw new AIServiceException("通义千问API密钥未配置");
        }

        String baseUrl = config.getApiUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = llmProperties.getQwen().getBaseUrl();
        }

        String model = config.getModelName();
        if (model == null || model.isEmpty()) {
            model = llmProperties.getQwen().getModel();
        }

        String url = baseUrl.replaceAll("/+$", "") + "/api/v1/services/aigc/text-generation/generation";

        Map<String, Object> requestBody = buildQwenRequest(prompt, systemPrompt, model);

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 带重试的API调用
        return callApiWithRetry(url, entity, "通义千问");
    }

    /**
     * 使用配置中的OpenAI服务进行对话
     */
    /**
     * OpenAI 兼容接口：适用于 LM Studio、vLLM、部分本地网关等（/v1/chat/completions）
     */
    private String chatWithOpenAIByConfig(String prompt, String systemPrompt, AIConfig config) {
        String baseUrl = config.getApiUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = llmProperties.getLmstudio().getBaseUrl();
        }

        String model = config.getModelName();
        if (model == null || model.isEmpty()) {
            model = llmProperties.getLmstudio().getChatModel();
        }
        if (model == null || model.isEmpty()) {
            throw new AIServiceException("未配置模型名称：请在 AI 配置中填写与 LM Studio 一致的 model id");
        }

        String apiKey = config.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = llmProperties.getLmstudio().getApiKey();
        }
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = "lm-studio";
        }

        double temperature = config.getTemperature() != null
                ? config.getTemperature() : llmProperties.getLmstudio().getTemperature();
        int maxTokens = config.getMaxTokens() != null
                ? config.getMaxTokens() : llmProperties.getLmstudio().getMaxTokens();

        String url = openAiCompatibleChatCompletionsUrl(baseUrl);
        Map<String, Object> requestBody = buildChatRequest(prompt, systemPrompt, model, temperature, maxTokens);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        return callApiWithRetry(url, entity, "OpenAI-Compatible");
    }

    private static String openAiCompatibleChatCompletionsUrl(String baseUrl) {
        String t = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
        if (t.isEmpty()) {
            t = "http://127.0.0.1:1234";
        }
        if (t.endsWith("/v1")) {
            return t + "/chat/completions";
        }
        return t + "/v1/chat/completions";
    }

    /**
     * 使用配置中的Ollama服务进行对话
     */
    private String chatWithOllamaByConfig(String prompt, String systemPrompt, AIConfig config) {
        String baseUrl = config.getApiUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "http://localhost:11434";
        }

        String model = config.getModelName();
        if (model == null || model.isEmpty()) {
            model = "qwen2.5";
        }

        String url = baseUrl.replaceAll("/+$", "") + "/api/chat";

        java.util.List<Map<String, String>> messages = new java.util.ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(new HashMap<String, String>() {{
                put("role", "system");
                put("content", systemPrompt);
            }});
        }
        messages.add(new HashMap<String, String>() {{
            put("role", "user");
            put("content", prompt);
        }});

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("stream", false);
        requestBody.put("options", new HashMap<String, Object>() {{
            put("temperature", config.getTemperature() != null ? config.getTemperature() : 0.1);
            put("num_predict", config.getMaxTokens() != null ? config.getMaxTokens() : 2000);
        }});
        requestBody.put("messages", messages.toArray());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        return callOllamaApiWithRetry(url, entity, "Ollama");
    }

    /**
     * 带重试机制的API调用
     */
    private String callApiWithRetry(String url, HttpEntity<?> entity, String providerName) {
        int retries = 0;
        Exception lastException = null;

        while (retries < llmProperties.getRetry()) {
            try {
                log.debug("调用{} API，尝试 {}/{}", providerName, retries + 1, llmProperties.getRetry());

                // 发送请求
                ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        String.class
                );

                if (response.getStatusCode() == HttpStatus.OK) {
                    return parseOpenAIStyleResponse(response.getBody());
                } else {
                    throw new AIServiceException(providerName + " API调用失败: " + response.getStatusCode());
                }

            } catch (Exception e) {
                lastException = e;
                retries++;

                if (retries < llmProperties.getRetry()) {
                    // 指数退避
                    long waitTime = (long) (1000 * Math.pow(2, retries - 1));
                    log.warn("{} API调用失败，{}ms后重试 ({}/{})",
                            providerName, waitTime, retries, llmProperties.getRetry(), e);

                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new AIServiceException("API调用被中断", ie);
                    }
                } else {
                    log.error("{} API调用失败，已达最大重试次数", providerName, e);
                }
            }
        }

        throw new AIServiceException(providerName + " API调用失败: " + lastException.getMessage(), lastException);
    }

    private String callApiWithRetryUrls(String primaryUrl, String fallbackUrl, HttpEntity<?> entity, String providerName) {
        try {
            return callApiWithRetry(primaryUrl, entity, providerName);
        } catch (AIServiceException primaryEx) {
            if (fallbackUrl == null || fallbackUrl.isBlank() || fallbackUrl.equals(primaryUrl)) {
                throw primaryEx;
            }
            log.warn("{} 主端点失败，切换备用端点重试。primary={}, fallback={}, err={}",
                    providerName, primaryUrl, fallbackUrl, primaryEx.getMessage());
            try {
                return callApiWithRetry(fallbackUrl, entity, providerName + " (fallback)");
            } catch (AIServiceException fallbackEx) {
                throw new AIServiceException(providerName + " 主备端点均失败: " + fallbackEx.getMessage(), fallbackEx);
            }
        }
    }

    /**
     * 带重试机制的Ollama API调用
     */
    private String callOllamaApiWithRetry(String url, HttpEntity<?> entity, String providerName) {
        int retries = 0;
        Exception lastException = null;

        while (retries < llmProperties.getRetry()) {
            try {
                log.debug("调用{} API，尝试 {}/{}", providerName, retries + 1, llmProperties.getRetry());

                // 发送请求
                ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        String.class
                );

                if (response.getStatusCode() == HttpStatus.OK) {
                    return parseOllamaResponse(response.getBody());
                } else {
                    throw new AIServiceException(providerName + " API调用失败: " + response.getStatusCode());
                }

            } catch (Exception e) {
                lastException = e;
                retries++;

                if (retries < llmProperties.getRetry()) {
                    // 指数退避
                    long waitTime = (long) (1000 * Math.pow(2, retries - 1));
                    log.warn("{} API调用失败，{}ms后重试 ({}/{})",
                            providerName, waitTime, retries, llmProperties.getRetry(), e);

                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new AIServiceException("API调用被中断", ie);
                    }
                } else {
                    log.error("{} API调用失败，已达最大重试次数", providerName, e);
                }
            }
        }

        throw new AIServiceException(providerName + " API调用失败: " + lastException.getMessage(), lastException);
    }

    /**
     * 解析OpenAI风格的API响应
     */
    private String parseOpenAIStyleResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");

            if (choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).path("message");
                return message.path("content").asText();
            }

            throw new AIServiceException("无法解析API响应: " + responseBody);

        } catch (Exception e) {
            log.error("解析API响应失败", e);
            throw new AIServiceException("解析API响应失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析Ollama API响应
     */
    private String parseOllamaResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode message = root.path("message");
            return message.path("content").asText();

        } catch (Exception e) {
            log.error("解析Ollama响应失败", e);
            throw new AIServiceException("解析Ollama响应失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建聊天请求体
     */
    private Map<String, Object> buildChatRequest(String prompt, String systemPrompt, String model) {
        return buildChatRequest(prompt, systemPrompt, model,
                llmProperties.getDeepseek().getTemperature(),
                llmProperties.getDeepseek().getMaxTokens());
    }

    private Map<String, Object> buildChatRequest(String prompt, String systemPrompt, String model,
                                                 double temperature, int maxTokens) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);

        Map<String, Object>[] messages;
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages = new Map[]{
                    new HashMap<String, String>() {{
                        put("role", "system");
                        put("content", systemPrompt);
                    }},
                    new HashMap<String, String>() {{
                        put("role", "user");
                        put("content", prompt);
                    }}
            };
        } else {
            messages = new Map[]{
                    new HashMap<String, String>() {{
                        put("role", "user");
                        put("content", prompt);
                    }}
            };
        }
        requestBody.put("messages", messages);

        return requestBody;
    }

    /**
     * 构建视觉请求体（包含图片）
     */
    private Map<String, Object> buildVisionRequest(String prompt, String imageBase64, String model) {
        return buildVisionRequest(prompt, imageBase64, model,
                llmProperties.getDeepseek().getTemperature(),
                llmProperties.getDeepseek().getMaxTokens(),
                "image/jpeg");
    }

    private Map<String, Object> buildVisionRequest(String prompt, String imageBase64, String model,
                                                   double temperature, int maxTokens, String imageMime) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);

        String mime = imageMime != null && !imageMime.isBlank() ? imageMime : "image/jpeg";
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");

        Map<String, Object>[] content = new Map[]{
                new HashMap<String, Object>() {{
                    put("type", "text");
                    put("text", prompt);
                }},
                new HashMap<String, Object>() {{
                    put("type", "image_url");
                    put("image_url", new HashMap<String, String>() {{
                        put("url", "data:" + mime + ";base64," + imageBase64);
                    }});
                }}
        };

        userMessage.put("content", content);
        requestBody.put("messages", new Map[]{userMessage});

        return requestBody;
    }

    /**
     * 构建通义千问请求体
     */
    private Map<String, Object> buildQwenRequest(String prompt, String systemPrompt, String model) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);

        // 构建输入参数
        Map<String, Object> input = new HashMap<>();

        // 构建消息数组
        Map<String, Object>[] messages;
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages = new Map[]{
                    new HashMap<String, String>() {{
                        put("role", "system");
                        put("content", systemPrompt);
                    }},
                    new HashMap<String, String>() {{
                        put("role", "user");
                        put("content", prompt);
                    }}
            };
        } else {
            messages = new Map[]{
                    new HashMap<String, String>() {{
                        put("role", "user");
                        put("content", prompt);
                    }}
            };
        }
        input.put("messages", messages);

        requestBody.put("input", input);

        // 参数配置
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("temperature", llmProperties.getDeepseek().getTemperature());
        parameters.put("max_tokens", llmProperties.getDeepseek().getMaxTokens());
        requestBody.put("parameters", parameters);

        return requestBody;
    }

    /**
     * 获取DeepSeek API密钥
     * 优先使用环境变量，否则使用配置文件中的值
     */
    private String getZhipuApiKey() {
        String envKey = System.getenv("ZHIPU_API_KEY");
        if (envKey != null && !envKey.isEmpty()) {
            return envKey;
        }
        envKey = System.getenv("GLM_CODING_API_KEY");
        if (envKey != null && !envKey.isEmpty()) {
            return envKey;
        }
        String configKey = llmProperties.getZhipu().getApiKey();
        if (configKey != null && !configKey.isEmpty()) {
            return configKey;
        }
        try {
            AIConfig config = aiConfigService.getDefaultConfig();
            if (config != null && config.getApiKey() != null && !config.getApiKey().isEmpty()
                    && isZhipuProvider(config.getProviderType())) {
                return config.getApiKey();
            }
        } catch (Exception e) {
            log.debug("从数据库读取智谱配置失败: {}", e.getMessage());
        }
        return null;
    }

    private static boolean isZhipuProvider(String providerType) {
        if (providerType == null) {
            return false;
        }
        String p = providerType.toLowerCase();
        return "zhipu".equals(p) || "glm".equals(p) || "zai".equals(p);
    }

    private String getDeepSeekApiKey() {
        // 优先从环境变量读取
        String envKey = System.getenv("DEEPSEEK_API_KEY");
        if (envKey != null && !envKey.isEmpty()) {
            return envKey;
        }

        // 其次使用配置文件中的值
        String configKey = llmProperties.getDeepseek().getApiKey();
        if (configKey != null && !configKey.isEmpty() && !configKey.equals("your-deepseek-api-key")) {
            return configKey;
        }

        // 最后尝试从数据库读取
        try {
            AIConfig config = aiConfigService.getDefaultConfig();
            if (config != null && "deepseek".equalsIgnoreCase(config.getProviderType())) {
                return config.getApiKey();
            }
        } catch (Exception e) {
            log.debug("从数据库读取DeepSeek配置失败: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 测试API连接
     *
     * @param provider 提供商类型
     * @param config AI配置
     * @return 测试结果
     */
    public boolean testConnection(String provider, AIConfig config) {
        try {
            String testPrompt = "你好，请回复'连接成功'来测试API连接。";
            String response = chatWithConfig(testPrompt, config);
            return response != null && !response.isEmpty();
        } catch (Exception e) {
            log.error("测试{} API连接失败", provider, e);
            return false;
        }
    }
}
