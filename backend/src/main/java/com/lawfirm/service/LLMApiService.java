package com.lawfirm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.config.LLMProperties;
import com.lawfirm.dto.AIConfigDTO;
import com.lawfirm.entity.AIConfig;
import com.lawfirm.exception.AIServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
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
        String apiKey = getDeepSeekApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
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

        // 带重试的API调用
        return callApiWithRetry(url, entity, "DeepSeek Vision");
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
        if (config == null) {
            throw new AIServiceException("AI配置不能为空");
        }

        String provider = config.getProviderType();
        if (provider == null || provider.isEmpty()) {
            throw new AIServiceException("AI提供商类型不能为空");
        }

        switch (provider.toLowerCase()) {
            case "deepseek":
                return chatWithDeepSeekByConfig(prompt, config);
            case "qwen":
            case "aliyun":
                return chatWithQwenByConfig(prompt, config);
            case "openai":
                return chatWithOpenAIByConfig(prompt, config);
            case "ollama":
                return chatWithOllamaByConfig(prompt, config);
            default:
                throw new AIServiceException("不支持的AI提供商: " + provider);
        }
    }

    /**
     * 使用配置中的DeepSeek服务进行对话
     */
    private String chatWithDeepSeekByConfig(String prompt, AIConfig config) {
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

        String url = baseUrl + "/v1/chat/completions";

        // 构建请求体
        Map<String, Object> requestBody = buildChatRequest(prompt, null, model);

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 带重试的API调用
        return callApiWithRetry(url, entity, "DeepSeek");
    }

    /**
     * 使用配置中的通义千问服务进行对话
     */
    private String chatWithQwenByConfig(String prompt, AIConfig config) {
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

        String url = baseUrl + "/api/v1/services/aigc/text-generation/generation";

        // 构建请求体
        Map<String, Object> requestBody = buildQwenRequest(prompt, null, model);

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
    private String chatWithOpenAIByConfig(String prompt, AIConfig config) {
        // TODO: 实现OpenAI API调用
        throw new AIServiceException("OpenAI API尚未实现");
    }

    /**
     * 使用配置中的Ollama服务进行对话
     */
    private String chatWithOllamaByConfig(String prompt, AIConfig config) {
        String baseUrl = config.getApiUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "http://localhost:11434";
        }

        String model = config.getModelName();
        if (model == null || model.isEmpty()) {
            model = "qwen2.5";
        }

        String url = baseUrl + "/api/chat";

        // 构建请求体 - Ollama使用特定的API格式
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("stream", false);
        requestBody.put("options", new HashMap<String, Object>() {{
            put("temperature", config.getTemperature() != null ? config.getTemperature() : 0.1);
            put("num_predict", config.getMaxTokens() != null ? config.getMaxTokens() : 2000);
        }});
        requestBody.put("messages", new Object[]{
                new HashMap<String, String>() {{
                    put("role", "user");
                    put("content", prompt);
                }}
        });

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 带重试的API调用
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
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("temperature", llmProperties.getDeepseek().getTemperature());
        requestBody.put("max_tokens", llmProperties.getDeepseek().getMaxTokens());

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
        requestBody.put("messages", messages);

        return requestBody;
    }

    /**
     * 构建视觉请求体（包含图片）
     */
    private Map<String, Object> buildVisionRequest(String prompt, String imageBase64, String model) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("temperature", llmProperties.getDeepseek().getTemperature());
        requestBody.put("max_tokens", llmProperties.getDeepseek().getMaxTokens());

        // 构建包含图片的消息
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");

        // 构建内容数组（文本+图片）
        Map<String, Object>[] content = new Map[]{
                new HashMap<String, Object>() {{
                    put("type", "text");
                    put("text", prompt);
                }},
                new HashMap<String, Object>() {{
                    put("type", "image_url");
                    put("image_url", new HashMap<String, String>() {{
                        put("url", "data:image/jpeg;base64," + imageBase64);
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
