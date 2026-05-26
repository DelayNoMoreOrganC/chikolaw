package com.lawfirm.controller;

import com.lawfirm.dto.AIConfigDTO;
import com.lawfirm.entity.AIConfig;
import com.lawfirm.service.AIConfigService;
import com.lawfirm.service.LLMApiService;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI配置控制器
 * 提供AI服务配置管理、API连接测试等功能
 */
@Slf4j
@RestController
@RequestMapping("ai/config")
@RequiredArgsConstructor
public class AIConfigController {

    private final AIConfigService aiConfigService;
    private final LLMApiService llmApiService;

    /**
     * 创建AI配置
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public Result<AIConfig> createConfig(@Valid @RequestBody AIConfigDTO dto) {
        AIConfig config = aiConfigService.createConfig(dto);
        return Result.success(config);
    }

    /**
     * 更新AI配置
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<AIConfig> updateConfig(@PathVariable Long id, @Valid @RequestBody AIConfigDTO dto) {
        AIConfig config = aiConfigService.updateConfig(id, dto);
        return Result.success(config);
    }

    /**
     * 删除AI配置
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> deleteConfig(@PathVariable Long id) {
        aiConfigService.deleteConfig(id);
        return Result.success();
    }

    /**
     * 获取AI配置详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<AIConfig> getConfig(@PathVariable Long id) {
        AIConfig config = aiConfigService.getConfig(id);
        return Result.success(config);
    }

    /**
     * 获取所有AI配置
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<List<AIConfig>> getAllConfigs() {
        List<AIConfig> configs = aiConfigService.getAllConfigs();
        return Result.success(configs);
    }

    /**
     * 获取默认配置
     */
    @GetMapping("/default")
    @PreAuthorize("isAuthenticated()")
    public Result<AIConfig> getDefaultConfig() {
        AIConfig config = aiConfigService.getDefaultConfig();
        return Result.success(config);
    }

    /**
     * 按提供商类型查找配置
     */
    @GetMapping("/provider/{providerType}")
    @PreAuthorize("isAuthenticated()")
    public Result<List<AIConfig>> getConfigsByProvider(@PathVariable String providerType) {
        List<AIConfig> configs = aiConfigService.getConfigsByProvider(providerType);
        return Result.success(configs);
    }

    /**
     * 测试API连接
     */
    @PostMapping("/test/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> testConnection(@PathVariable Long id) {
        AIConfig config = aiConfigService.getConfig(id);
        boolean success = llmApiService.testConnection(config.getProviderType(), config);

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("provider", config.getProviderType());
        result.put("message", success ? "连接成功" : "连接失败");

        return Result.success(result);
    }

    /**
     * 获取可用的AI提供商列表
     */
    @GetMapping("/providers")
    @PreAuthorize("isAuthenticated()")
    public Result<List<Map<String, Object>>> getAvailableProviders() {
        List<Map<String, Object>> providers = List.of(
                Map.of("type", "deepseek", "name", "DeepSeek", "description", "DeepSeek AI - 性价比高，中文支持好",
                        "models", List.of("deepseek-chat", "deepseek-coder", "deepseek-vl")),
                Map.of("type", "qwen", "name", "通义千问", "description", "阿里云通义千问 - 企业级AI服务",
                        "models", List.of("qwen-turbo", "qwen-plus", "qwen-max", "qwen-vl-max")),
                Map.of("type", "openai", "name", "OpenAI 兼容", "description", "OpenAI API 兼容（含 LM Studio 本地服务）",
                        "models", List.of("local-qwen", "gpt-4o-mini", "gpt-4")),
                Map.of("type", "lmstudio", "name", "LM Studio", "description", "本地 LM Studio（OpenAI 兼容，默认 http://127.0.0.1:1234）",
                        "models", List.of("请在 LM Studio 中查看已加载模型的 Model ID")),
                Map.of("type", "ollama", "name", "Ollama", "description", "本地部署的开源大模型",
                        "models", List.of("qwen2.5", "llama3", "mistral", "deepseek-coder"))
        );
        return Result.success(providers);
    }

    /**
     * 获取AI功能配置建议
     */
    @GetMapping("/recommendations")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> getRecommendations() {
        Map<String, Object> recommendations = new HashMap<>();

        // OCR识别推荐配置
        recommendations.put("ocr", Map.of(
                "recommended", "deepseek",
                "reason", "DeepSeek Vision API对中文识别准确，支持法律文书专业术语",
                "alternative", "aliyun",
                "models", List.of("deepseek-vl", "qwen-vl-max")
        ));

        // 文书生成推荐配置
        recommendations.put("document", Map.of(
                "recommended", "deepseek",
                "reason", "DeepSeek Chat API对法律文书生成效果好，成本低",
                "alternative", "qwen",
                "models", List.of("deepseek-chat", "qwen-plus")
        ));

        // 法律问答推荐配置
        recommendations.put("qa", Map.of(
                "recommended", "qwen",
                "reason", "通义千问在知识问答方面表现优秀",
                "alternative", "deepseek",
                "models", List.of("qwen-plus", "deepseek-chat")
        ));

        return Result.success(recommendations);
    }

    /**
     * 批量导入AI配置
     */
    @PostMapping("/batch")
    @PreAuthorize("isAuthenticated()")
    public Result<List<AIConfig>> batchImport(@RequestBody List<AIConfigDTO> dtos) {
        List<AIConfig> configs = dtos.stream()
                .map(aiConfigService::createConfig)
                .collect(Collectors.toList());
        return Result.success(configs);
    }

    /**
     * 设置默认配置
     */
    @PutMapping("/setDefault/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<AIConfig> setDefaultConfig(@PathVariable Long id) {
        AIConfigDTO dto = new AIConfigDTO();
        dto.setIsDefault(true);
        AIConfig config = aiConfigService.updateConfig(id, dto);
        return Result.success(config);
    }

    /**
     * 启用/禁用配置
     */
    @PutMapping("/toggle/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<AIConfig> toggleConfig(@PathVariable Long id, @RequestParam Boolean enabled) {
        AIConfigDTO dto = new AIConfigDTO();
        dto.setIsEnabled(enabled);
        AIConfig config = aiConfigService.updateConfig(id, dto);
        return Result.success(config);
    }
}
