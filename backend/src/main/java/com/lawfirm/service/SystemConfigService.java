package com.lawfirm.service;

import com.lawfirm.entity.SystemConfig;
import com.lawfirm.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统配置服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;

    /**
     * 配置分类常量
     */
    public static final String CATEGORY_SYSTEM = "SYSTEM";  // 系统配置
    public static final String CATEGORY_APPROVAL = "APPROVAL";  // 审批配置
    public static final String CATEGORY_CASE = "CASE";  // 案件配置
    public static final String CATEGORY_AI = "AI";  // AI配置
    public static final String CATEGORY_NOTIFICATION = "NOTIFICATION";  // 通知配置

    /**
     * 创建或更新配置
     */
    @Transactional
    @CacheEvict(value = "systemConfig", key = "#configKey")
    public SystemConfig saveConfig(String configKey, String configValue,
                                  String configType, String category, String description) {
        SystemConfig config = systemConfigRepository.findByConfigKey(configKey)
                .orElse(new SystemConfig());

        config.setConfigKey(configKey);
        config.setConfigValue(configValue);
        config.setConfigType(configType != null ? configType : "STRING");
        config.setCategory(category);
        config.setDescription(description);

        return systemConfigRepository.save(config);
    }

    /**
     * 获取配置值
     */
    @Cacheable(value = "systemConfig", key = "#configKey")
    public String getConfigValue(String configKey) {
        return systemConfigRepository.findByConfigKey(configKey)
                .map(SystemConfig::getConfigValue)
                .orElse("");
    }

    /**
     * 获取配置值（带默认值）
     */
    public String getConfigValue(String configKey, String defaultValue) {
        return systemConfigRepository.findByConfigKey(configKey)
                .map(SystemConfig::getConfigValue)
                .orElse(defaultValue);
    }

    /**
     * 获取配置对象
     */
    @Cacheable(value = "systemConfig", key = "'config:' + #configKey")
    public SystemConfig getConfig(String configKey) {
        return systemConfigRepository.findByConfigKey(configKey)
                .orElseThrow(() -> new IllegalArgumentException("配置项不存在: " + configKey));
    }

    /**
     * 删除配置
     */
    @Transactional
    @CacheEvict(value = "systemConfig", key = "#configKey")
    public void deleteConfig(String configKey) {
        SystemConfig config = systemConfigRepository.findByConfigKey(configKey)
                .orElseThrow(() -> new RuntimeException("配置不存在"));

        config.setDeleted(true);
        systemConfigRepository.save(config);
    }

    /**
     * 获取配置列表
     */
    public List<SystemConfig> getConfigList(String category) {
        if (category != null && !category.isEmpty()) {
            return systemConfigRepository.findByCategory(category);
        }

        return systemConfigRepository.findAll();
    }

    /**
     * 获取前端系统设置页使用的聚合配置。
     */
    public Map<String, Object> getAllConfigGroups() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("system", getConfigsByCategory(CATEGORY_SYSTEM));
        result.put("approval", getConfigsByCategory(CATEGORY_APPROVAL));
        result.put("case", getConfigsByCategory(CATEGORY_CASE));
        result.put("ai", getConfigsByCategory(CATEGORY_AI));
        result.put("notification", getConfigsByCategory(CATEGORY_NOTIFICATION));
        result.put("categories", getCategories());
        return result;
    }

    /**
     * 保存聚合配置，兼容 {system:{...}} 和 {configs:{...},category:"SYSTEM"} 两种格式。
     */
    @Transactional
    public void saveConfigGroups(Map<String, Object> groups) {
        if (groups == null || groups.isEmpty()) {
            return;
        }

        Object configs = groups.get("configs");
        Object category = groups.get("category");
        if (configs instanceof Map && category instanceof String) {
            saveConfigGroup((String) category, (Map<?, ?>) configs);
            return;
        }

        saveConfigGroup(CATEGORY_SYSTEM, groups.get("system"));
        saveConfigGroup(CATEGORY_APPROVAL, groups.get("approval"));
        saveConfigGroup(CATEGORY_CASE, groups.get("case"));
        saveConfigGroup(CATEGORY_AI, groups.get("ai"));
        saveConfigGroup(CATEGORY_NOTIFICATION, groups.get("notification"));
    }

    private void saveConfigGroup(String category, Object values) {
        if (!(values instanceof Map)) {
            return;
        }
        ((Map<?, ?>) values).forEach((key, value) -> {
            if (key != null && value != null) {
                saveConfig(String.valueOf(key), String.valueOf(value), "STRING", category, null);
            }
        });
    }

    /**
     * 根据分类获取配置
     */
    public Map<String, String> getConfigsByCategory(String category) {
        // 使用数据库查询优化
        List<SystemConfig> configs = systemConfigRepository.findByCategory(category);

        Map<String, String> result = new HashMap<>();
        for (SystemConfig config : configs) {
            result.put(config.getConfigKey(), config.getConfigValue());
        }

        return result;
    }

    /**
     * 批量保存配置
     */
    @Transactional
    public void batchSaveConfigs(Map<String, String> configs, String category) {
        configs.forEach((key, value) -> {
            saveConfig(key, value, "STRING", category, null);
        });
    }

    /**
     * 获取系统配置分类列表
     */
    public List<Map<String, String>> getCategories() {
        return List.of(
                Map.of("code", CATEGORY_SYSTEM, "name", "系统配置"),
                Map.of("code", CATEGORY_APPROVAL, "name", "审批配置"),
                Map.of("code", CATEGORY_CASE, "name", "案件配置"),
                Map.of("code", CATEGORY_AI, "name", "AI配置"),
                Map.of("code", CATEGORY_NOTIFICATION, "name", "通知配置")
        );
    }

    /**
     * 初始化默认配置
     */
    @Transactional
    public void initDefaultConfigs() {
        // 系统配置
        saveConfig("system.name", "律所智能案件管理系统", "STRING", CATEGORY_SYSTEM, "系统名称");
        saveConfig("system.version", "1.0.0", "STRING", CATEGORY_SYSTEM, "系统版本");
        saveConfig("system.logo", "", "STRING", CATEGORY_SYSTEM, "系统LOGO");

        // 审批配置
        saveConfig("approval.enable_multi_level", "true", "BOOLEAN", CATEGORY_APPROVAL, "启用多级审批");
        saveConfig("approval.auto_approve_same_level", "false", "BOOLEAN", CATEGORY_APPROVAL, "同级自动审批");

        // 案件配置
        saveConfig("case.auto_generate_number", "true", "BOOLEAN", CATEGORY_CASE, "自动生成案件编号");
        saveConfig("case.auto_generate_name", "true", "BOOLEAN", CATEGORY_CASE, "自动生成案件名称");

        // AI配置
        saveConfig("ai.provider", "deepseek", "STRING", CATEGORY_AI, "AI服务提供商");
        saveConfig("ai.api_key", "", "STRING", CATEGORY_AI, "AI API密钥");
        saveConfig("ai.model", "deepseek-chat", "STRING", CATEGORY_AI, "AI模型");

        // 通知配置
        saveConfig("notification.email_enabled", "false", "BOOLEAN", CATEGORY_NOTIFICATION, "启用邮件通知");
        saveConfig("notification.sms_enabled", "false", "BOOLEAN", CATEGORY_NOTIFICATION, "启用短信通知");

        log.info("系统默认配置初始化完成");
    }
}
