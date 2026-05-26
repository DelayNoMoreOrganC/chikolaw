package com.lawfirm.dto;

import lombok.Data;

/**
 * 立案审批通过后自动创建的草稿案件结果
 */
@Data
public class CaseIntakeDraftResultDTO {
    private Long pendingId;
    private Long draftCaseId;
    private String caseNumber;
    private String caseName;
    /** 卷宗是否已挂接到草稿案件 */
    private boolean intakeAttached;
}
