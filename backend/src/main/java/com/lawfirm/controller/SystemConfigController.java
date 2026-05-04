package com.lawfirm.controller;

import com.lawfirm.annotation.AuditLog;
import com.lawfirm.entity.SystemConfig;
import com.lawfirm.service.SystemConfigService;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统配置控制器
 */
@Slf4j
@RestController
@RequestMapping("system/config")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    /**
     * 获取聚合配置
     * GET /api/system/config
     */
    @GetMapping
    public Result<Map<String, Object>> getSystemConfig() {
        try {
            Map<String, Object> result = systemConfigService.getAllConfigGroups();
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取系统配置失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 批量更新聚合配置
     * PUT /api/system/config
     */
    @PutMapping
    @AuditLog(value = "更新系统配置", operationType = "UPDATE", logParams = false)
    public Result<Map<String, Object>> updateSystemConfig(@RequestBody Map<String, Object> params) {
        try {
            systemConfigService.saveConfigGroups(params);
            return Result.success(systemConfigService.getAllConfigGroups());
        } catch (Exception e) {
            log.error("更新系统配置失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 创建或更新配置
     * POST /api/system-config
     */
    @PostMapping
    @AuditLog(value = "保存系统配置", operationType = "UPDATE", logParams = false)
    public Result<SystemConfig> saveConfig(@RequestBody Map<String, String> params) {
        try {
            String configKey = params.get("configKey");
            String configValue = params.get("configValue");
            String configType = params.getOrDefault("configType", "STRING");
            String category = params.get("category");
            String description = params.get("description");

            SystemConfig result = systemConfigService.saveConfig(
                    configKey, configValue, configType, category, description);
            return Result.success(result);
        } catch (Exception e) {
            log.error("保存配置失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取配置值
     * GET /api/system-config/value/{key}
     */
    @GetMapping("/value/{key}")
    public Result<String> getConfigValue(@PathVariable String key) {
        try {
            String value = systemConfigService.getConfigValue(key);
            return Result.success(value);
        } catch (Exception e) {
            log.error("获取配置值失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取配置对象
     * GET /api/system-config/{key}
     */
    @GetMapping("/{key}")
    public Result<SystemConfig> getConfig(@PathVariable String key) {
        try {
            SystemConfig config = systemConfigService.getConfig(key);
            return Result.success(config);
        } catch (Exception e) {
            log.error("获取配置失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除配置
     * DELETE /api/system-config/{key}
     */
    @DeleteMapping("/{key}")
    @AuditLog(value = "删除系统配置", operationType = "DELETE", logParams = false)
    public Result<Void> deleteConfig(@PathVariable String key) {
        try {
            systemConfigService.deleteConfig(key);
            return Result.success();
        } catch (Exception e) {
            log.error("删除配置失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取配置列表
     * GET /api/system-config/list
     */
    @GetMapping("/list")
    public Result<List<SystemConfig>> getConfigList(
            @RequestParam(required = false) String category) {
        try {
            List<SystemConfig> result = systemConfigService.getConfigList(category);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取配置列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据分类获取配置
     * GET /api/system-config/category/{category}
     */
    @GetMapping("/category/{category}")
    public Result<Map<String, String>> getConfigsByCategory(@PathVariable String category) {
        try {
            Map<String, String> result = systemConfigService.getConfigsByCategory(category);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取分类配置失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 批量保存配置
     * POST /api/system-config/batch
     */
    @PostMapping("/batch")
    @AuditLog(value = "批量保存系统配置", operationType = "UPDATE", logParams = false)
    public Result<Void> batchSaveConfigs(@RequestBody Map<String, Object> params) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> configs = (Map<String, String>) params.get("configs");
            String category = (String) params.get("category");

            systemConfigService.batchSaveConfigs(configs, category);
            return Result.success();
        } catch (Exception e) {
            log.error("批量保存配置失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取配置分类列表
     * GET /api/system-config/categories
     */
    @GetMapping("/categories")
    public Result<List<Map<String, String>>> getCategories() {
        try {
            List<Map<String, String>> result = systemConfigService.getCategories();
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取配置分类失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 初始化默认配置
     * POST /api/system-config/init
     */
    @PostMapping("/init")
    public Result<Void> initDefaultConfigs() {
        try {
            systemConfigService.initDefaultConfigs();
            return Result.success();
        } catch (Exception e) {
            log.error("初始化配置失败", e);
            return Result.error(e.getMessage());
        }
    }
}
