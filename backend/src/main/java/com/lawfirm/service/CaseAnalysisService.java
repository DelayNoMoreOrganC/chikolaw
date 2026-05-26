package com.lawfirm.service;

import com.lawfirm.entity.Case;
import com.lawfirm.enums.AIModelUseCase;
import com.lawfirm.exception.BusinessException;
import com.lawfirm.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 案件 AI 分析（LLM 语义化，替代关键词 MVP）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseAnalysisService {

    private final CaseRepository caseRepository;
    private final LLMApiService llmApiService;
    private final AIModelRoutingService routingService;

    public Map<String, Object> analyzeCase(Long caseId) {
        Case c = caseRepository.findById(caseId)
                .orElseThrow(() -> new BusinessException("案件不存在: " + caseId));

        String prompt = buildPrompt(c);
        com.lawfirm.entity.AIConfig config = routingService.resolveForUseCase(AIModelUseCase.GENERAL_CHAT);
        String analysis = llmApiService.chatWithConfig(prompt, config);

        Map<String, Object> result = new HashMap<>();
        result.put("caseId", caseId);
        result.put("caseName", c.getCaseName());
        result.put("provider", config.getProviderType());
        result.put("analysis", analysis);
        result.put("mode", "llm");
        return result;
    }

    private String buildPrompt(Case c) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是律所案件分析助手。请根据以下案件信息，用中文输出结构化分析（要素摘要、争议焦点、程序阶段建议、风险提示），控制在 800 字内。\n\n");
        sb.append("案件名称：").append(nullToEmpty(c.getCaseName())).append("\n");
        sb.append("案号：").append(nullToEmpty(c.getCaseNumber())).append("\n");
        sb.append("类型：").append(nullToEmpty(c.getCaseType())).append("\n");
        sb.append("案由：").append(nullToEmpty(c.getCaseReason())).append("\n");
        sb.append("法院：").append(nullToEmpty(c.getCourt())).append("\n");
        sb.append("当前阶段：").append(nullToEmpty(c.getCurrentStage())).append("\n");
        sb.append("案情简介：").append(nullToEmpty(c.getSummary())).append("\n");
        return sb.toString();
    }

    private String nullToEmpty(String s) {
        return s != null ? s : "";
    }
}
