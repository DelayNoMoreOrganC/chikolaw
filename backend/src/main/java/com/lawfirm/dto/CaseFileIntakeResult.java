package com.lawfirm.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 主界面卷宗录入（传文件 → AI 分析 → 登记备注 → 归入案件档案夹）统一响应。
 */
@Data
public class CaseFileIntakeResult {

    /** SUCCESS | NEEDS_CASE | PARTIAL | FAILED */
    private String status;

    private String message;

    private Long caseId;

    private String caseName;

    private String caseNumber;

    private Long documentId;

    private String documentName;

    /** 卷宗目录，如 法院文书/传票 */
    private String folderPath;

    private String documentType;

    /** 登记备注（写入案件动态） */
    private String registrationNote;

    private AIDocumentRecognitionResult recognition;

    /** 分析提供方：local | hermes */
    private String analysisProvider;

    /** 可选：自动业务（待办/日程等） */
    private Map<String, Object> automation;

    /** 案号未匹配且未指定案件时的候选列表 */
    private List<CaseBriefDTO> caseCandidates;

    /** NEEDS_CASE 时服务端暂存的文件 ID，用于挂接或立案审批 */
    private Long pendingId;

    @Data
    public static class CaseBriefDTO {
        private Long id;
        private String caseNumber;
        private String caseName;
        private String currentStage;
    }
}
