package com.lawfirm.service;

import com.lawfirm.dto.AIConfigDTO;
import com.lawfirm.entity.AIConfig;
import com.lawfirm.repository.AIConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * AI配置服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIConfigService {

    private final AIConfigRepository aiConfigRepository;

    /**
     * 创建AI配置
     */
    @Transactional
    public AIConfig createConfig(AIConfigDTO dto) {
        AIConfig config = new AIConfig();
        BeanUtils.copyProperties(dto, config);

        // 如果设置为默认配置，需要取消其他默认配置
        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            aiConfigRepository.findByIsDefaultTrueAndDeletedFalse()
                    .ifPresent(c -> c.setIsDefault(false));
        }

        return aiConfigRepository.save(config);
    }

    /**
     * 更新AI配置
     */
    @Transactional
    public AIConfig updateConfig(Long id, AIConfigDTO dto) {
        AIConfig config = aiConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("AI配置不存在"));

        BeanUtils.copyProperties(dto, config, "id");

        // 如果设置为默认配置，需要取消其他默认配置
        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            aiConfigRepository.findByIsDefaultTrueAndDeletedFalse()
                    .filter(c -> !c.getId().equals(id))
                    .ifPresent(c -> c.setIsDefault(false));
        }

        return aiConfigRepository.save(config);
    }

    /**
     * 删除AI配置
     */
    @Transactional
    public void deleteConfig(Long id) {
        AIConfig config = aiConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("AI配置不存在"));
        config.setDeleted(true);
        aiConfigRepository.save(config);
    }

    /**
     * 获取AI配置详情
     */
    public AIConfig getConfig(Long id) {
        return aiConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("AI配置不存在"));
    }

    /**
     * 获取所有AI配置
     */
    public List<AIConfig> getAllConfigs() {
        return aiConfigRepository.findByIsEnabledTrueAndDeletedFalse();
    }

    /**
     * 获取默认配置
     */
    public AIConfig getDefaultConfig() {
        return aiConfigRepository.findByIsDefaultTrueAndDeletedFalse()
                .orElseThrow(() -> new RuntimeException("未配置默认AI"));
    }

    /**
     * 默认 AI 配置（可能为空）
     */
    public Optional<AIConfig> findDefaultConfigOptional() {
        return aiConfigRepository.findByIsDefaultTrueAndDeletedFalse();
    }

    /**
     * 在已启用配置中按 provider 匹配（忽略大小写）
     */
    public Optional<AIConfig> findFirstEnabledByProviderIgnoreCase(String providerType) {
        if (providerType == null || providerType.isBlank()) {
            return Optional.empty();
        }
        String want = providerType.trim();
        return getAllConfigs().stream()
                .filter(c -> c.getProviderType() != null && want.equalsIgnoreCase(c.getProviderType()))
                .findFirst();
    }

    /**
     * 按提供商类型查找配置
     */
    public List<AIConfig> getConfigsByProvider(String providerType) {
        return aiConfigRepository.findByProviderTypeAndDeletedFalse(providerType);
    }
}
