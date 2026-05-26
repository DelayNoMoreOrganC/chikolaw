package com.lawfirm.dto;

import lombok.Data;

/**
 * 立案审批通过后，新建案件页预填数据（来自卷宗暂存 AI 识别）。
 */
@Data
public class CaseIntakePrefillDTO {

    private Long pendingId;

    private String caseNumber;

    private String courtName;

    private String caseReason;

    private String plaintiffName;

    private String defendantName;

    private String hearingDate;

    private String hearingPlace;

    private String documentType;

    private String remark;

    /** 建议案件名称 */
    private String suggestedCaseName;

    /** 审批通过后已生成的草稿案件 ID（若有） */
    private Long draftCaseId;

    private String draftCaseNumber;
}
