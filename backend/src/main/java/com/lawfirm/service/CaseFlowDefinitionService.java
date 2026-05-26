package com.lawfirm.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 案件流程阶段唯一基准（与 PRD §3、前端 case-lifecycle 对齐）。
 */
@Service
public class CaseFlowDefinitionService {

    private static final Map<String, List<String>> STAGES_BY_TYPE = new LinkedHashMap<>();

    /** 历史模板阶段名 → 规范阶段名 */
    private static final Map<String, String> STAGE_ALIASES = new LinkedHashMap<>();

    static {
        STAGES_BY_TYPE.put("CIVIL", List.of(
                "咨询", "签约", "起草文书", "待立案", "已立案",
                "一审审理中", "一审结案", "执行", "结案归档"
        ));
        STAGES_BY_TYPE.put("CRIMINAL", List.of(
                "咨询", "签约", "会见", "审查起诉", "一审", "二审", "结案归档"
        ));
        STAGES_BY_TYPE.put("ADMINISTRATIVE", List.of(
                "咨询", "签约", "起草文书", "待立案", "已立案", "一审", "二审", "结案归档"
        ));
        STAGES_BY_TYPE.put("COMMERCIAL", List.of(
                "咨询", "签约", "起草文书", "待立案", "已立案", "一审审理中", "一审结案", "执行", "结案归档"
        ));
        STAGES_BY_TYPE.put("ARBITRATION", List.of(
                "咨询", "签约", "起草文书", "申请仲裁", "组庭", "开庭", "裁决", "结案归档"
        ));
        STAGES_BY_TYPE.put("NON_LITIGATION", List.of(
                "咨询", "签约", "尽职调查", "出具文书", "交付", "结案归档"
        ));
        STAGES_BY_TYPE.put("ADVISORY", STAGES_BY_TYPE.get("NON_LITIGATION"));
        STAGES_BY_TYPE.put("ADVISORY", STAGES_BY_TYPE.get("NON_LITIGATION"));

        STAGE_ALIASES.put("起草", "起草文书");
        STAGE_ALIASES.put("立案", "待立案");
        STAGE_ALIASES.put("一审", "一审审理中");
        STAGE_ALIASES.put("一审判决", "一审结案");
        STAGE_ALIASES.put("结案", "结案归档");
        STAGE_ALIASES.put("调解", "一审审理中");
    }

    public List<String> getStageNames(String caseType) {
        if (caseType == null) {
            return STAGES_BY_TYPE.getOrDefault("CIVIL", List.of("咨询", "签约", "办理", "结案归档"));
        }
        return STAGES_BY_TYPE.getOrDefault(caseType,
                List.of("咨询", "签约", "办理", "结案归档"));
    }

    public String getFirstStageName(String caseType) {
        List<String> stages = getStageNames(caseType);
        return stages.isEmpty() ? "咨询" : stages.get(0);
    }

    /**
     * 将模板或前端传入的阶段名规范化为流程基准名。
     */
    public String canonicalStageName(String stageName) {
        if (stageName == null || stageName.isBlank()) {
            return stageName;
        }
        String trimmed = stageName.trim();
        if (STAGE_ALIASES.containsKey(trimmed)) {
            return STAGE_ALIASES.get(trimmed);
        }
        return trimmed;
    }

    /**
     * 查找模板阶段名：先精确匹配，再别名，再包含关系。
     */
    public Optional<String> resolveTemplateStageName(String caseType, String stageName) {
        String canonical = canonicalStageName(stageName);
        List<String> stages = getStageNames(caseType);
        if (stages.contains(canonical)) {
            return Optional.of(canonical);
        }
        for (String s : stages) {
            if (s.contains(canonical) || canonical.contains(s)) {
                return Optional.of(s);
            }
        }
        return Optional.of(canonical);
    }

    public Map<String, String> getStageAliases() {
        return Collections.unmodifiableMap(STAGE_ALIASES);
    }
}
