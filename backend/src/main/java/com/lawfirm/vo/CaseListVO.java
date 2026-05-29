package com.lawfirm.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 案件列表VO
 */
@Data
public class CaseListVO {

    /**
     * 案件ID
     */
    private Long id;

    /**
     * 案件编号
     */
    private String caseNumber;

    /**
     * 案件名称
     */
    private String caseName;

    /**
     * 案件类型
     */
    private String caseType;

    /**
     * 案件类型描述
     */
    private String caseTypeDesc;

    /**
     * 案件状态
     */
    private String status;

    /**
     * 案件状态描述
     */
    private String statusDesc;

    /**
     * 案件等级
     */
    private String level;

    /**
     * 案件等级描述
     */
    private String levelDesc;

    /**
     * 当前阶段
     */
    private String currentStage;

    /**
     * 案由
     */
    private String caseReason;

    /**
     * 管辖法院
     */
    private String court;

    /**
     * 主办律师ID
     */
    private Long ownerId;

    /**
     * 主办律师姓名
     */
    private String ownerName;

    /**
     * 立案日期
     */
    private LocalDate filingDate;

    /**
     * 审限日期
     */
    private LocalDate deadlineDate;

    /**
     * 标的额
     */
    private BigDecimal amount;

    /**
     * 代理费
     */
    private BigDecimal attorneyFee;

    private String npaSubtype;
    private String entrustingBankName;
    private String assetBatchNo;
    private BigDecimal principalBalance;
    private BigDecimal interestBalance;
    private BigDecimal executionRecoveryAmount;
    private String terminationStatus;

    /**
     * 当事人信息（原告 vs 被告）
     */
    private String parties;

    /**
     * 下次开庭时间
     */
    @JsonProperty("nextHearing")
    private LocalDate nextHearingDate;

    /**
     * 标签
     */
    private String tags;

    /**
     * 创建时间
     */
    private LocalDate createdAt;

    /**
     * 更新时间
     */
    private LocalDate updatedAt;

    /**
     * 是否法律援助案件
     */
    private Boolean isLegalAid;

    /**
     * 是否可以编辑
     */
    private Boolean canEdit;

    /**
     * 是否可以删除
     */
    private Boolean canDelete;

    /**
     * 利冲审查状态
     */
    private String conflictCheckStatus;

    /**
     * 阶段卷宗目录是否已初始化
     */
    private Boolean stageFoldersInitialized;
}
