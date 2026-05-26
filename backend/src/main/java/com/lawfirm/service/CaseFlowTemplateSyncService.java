package com.lawfirm.service;

import com.lawfirm.entity.CaseStageTodoTemplate;
import com.lawfirm.repository.CaseStageTodoTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 将历史流程模板中的阶段名同步为 {@link CaseFlowDefinitionService} 规范名。
 */
@Slf4j
@Service
@Order(20)
@RequiredArgsConstructor
public class CaseFlowTemplateSyncService implements ApplicationRunner {

    private final CaseStageTodoTemplateRepository stageTodoTemplateRepository;
    private final CaseFlowDefinitionService caseFlowDefinitionService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<String, String> aliases = caseFlowDefinitionService.getStageAliases();
        List<CaseStageTodoTemplate> all = stageTodoTemplateRepository.findAll();
        int updated = 0;
        for (CaseStageTodoTemplate t : all) {
            if (Boolean.TRUE.equals(t.getDeleted())) {
                continue;
            }
            String canonical = aliases.get(t.getStageName());
            if (canonical != null && !canonical.equals(t.getStageName())) {
                t.setStageName(canonical);
                stageTodoTemplateRepository.save(t);
                updated++;
            }
        }
        if (updated > 0) {
            log.info("已同步 {} 条阶段待办模板的阶段名称", updated);
        }
    }
}
