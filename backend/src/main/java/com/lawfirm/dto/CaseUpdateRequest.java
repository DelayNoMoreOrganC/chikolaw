package com.lawfirm.dto;

import com.lawfirm.validation.PercentageSum;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 更新案件请求DTO
 */
@Data
@PercentageSum(message = "案源人、承办部门、律所提留三者比例之和必须为100%")
public class CaseUpdateRequest {

    /**
     * 案件名称
     */
    private String caseName;

    /**
     * 案件类型
     */
    private String caseType;

    /**
     * 案件程序
     */
    private String procedure;

    /**
     * 案由
     */
    private String caseReason;

    /**
     * 管辖法院
     */
    private String court;

    /**
     * 立案时间
     */
    private LocalDate filingDate;

    /**
     * 审限时间
     */
    private LocalDate deadlineDate;

    /**
     * 委托时间
     */
    private LocalDate commissionDate;

    /**
     * 案件标签
     */
    private String tags;

    /**
     * 案件状态
     */
    private String status;

    /**
     * 当前阶段
     */
    private String currentStage;

    /**
     * 案件简述
     */
    private String summary;

    /**
     * 办案策略
     */
    private String strategies;

    /**
     * 案件等级
     */
    private String level;

    /**
     * 主办律师ID
     */
    private Long ownerId;

    /**
     * 协办律师ID列表
     */
    private List<Long> coOwnerIds;

    /**
     * 律师助理ID列表
     */
    private List<Long> assistantIds;

    /**
     * 标的额
     */
    private BigDecimal amount;

    /**
     * 代理费
     */
    private BigDecimal attorneyFee;

    /**
     * 收费方式
     */
    private String feeMethod;

    /**
     * 收费简介
     */
    private String feeDescription;

    /**
     * 收费备注
     */
    private String feeNotes;

    /**
     * 金融不良资产专项字段
     */
    private String npaSubtype;
    private String entrustingBankName;
    private String assetBatchNo;
    private String transferAgreementNo;
    private String loanContractNo;
    private BigDecimal principalBalance;
    private BigDecimal interestBalance;
    private String guaranteeType;
    private String collateralStatus;
    private String preservationStatus;
    private BigDecimal executionRecoveryAmount;
    private String terminationStatus;

    /**
     * 胜诉金额
     */
    private BigDecimal wonAmount;

    /**
     * 实际回款
     */
    private BigDecimal actualReceived;

    /**
     * 结案状态
     */
    private String closeStatus;

    /**
     * 结案日期
     */
    private LocalDate closeDate;

    /**
     * 归档日期
     */
    private LocalDate archiveDate;

    /**
     * 档案保管地
     */
    private String archiveLocation;

    /**
     * 当事人列表（全量更新）
     */
    @Valid
    private List<PartyDTO> parties;

    /**
     * 关联客户ID列表
     */
    private List<Long> clientIds;

    /**
     * 关联案件ID列表
     */
    private List<Long> relatedCaseIds;

    // ==================== 新增字段（对标行政管理要求）====================

    /**
     * 收案日期
     */
    @NotNull(message = "收案日期不能为空")
    private LocalDate acceptanceDate;

    /**
     * 法院案号
     */
    private String courtCaseNumber;

    /**
     * 开庭日期
     */
    private LocalDate hearingDate;

    /**
     * 合同服务开始时间（顾问类）
     */
    private LocalDate contractStartDate;

    /**
     * 合同服务结束时间（顾问类）
     */
    private LocalDate contractEndDate;

    /**
     * 代理类型（刑事：原告/被告）
     */
    private String representationType;

    /**
     * 案源人
     */
    @NotBlank(message = "案源人不能为空")
    private String sourcePerson;

    /**
     * 案源人分配比例（%）
     */
    private BigDecimal sourcePersonPercentage;

    /**
     * 承办部门分配比例（%）
     */
    private BigDecimal departmentPercentage;

    /**
     * 律所提留比例（%）
     */
    private BigDecimal firmPercentage;

    /**
     * 业务类型（根据案件类型的细分）
     */
    private String businessType;

    /**
     * 犯罪嫌疑人（刑事案件专用）
     */
    private String criminalSuspect;

    /**
     * 涉案标的（单位：万元）
     */
    private BigDecimal disputedAmount;

    /**
     * 主办部门（根据主办律师自动关联）
     */
    private String hostDepartment;

    /**
     * 协办部门（根据协办律师自动关联）
     */
    private String coDepartments;

    /**
     * 备注（收费方式备注等）
     */
    private String remark;

    /**
     * 利益冲突审查状态
     */
    private String conflictCheckStatus;

    /**
     * 利益冲突豁免审批ID
     */
    private Long conflictWaiverApprovalId;

    /**
     * 其他委托人（可多选，JSON数组格式）
     */
    private String otherClients;

    /**
     * 审级（可多选，JSON数组格式）
     */
    private String procedureLevels;

    /**
     * 是否法律援助案件
     */
    private Boolean isLegalAid;

    /**
     * 固定费用金额
     */
    private java.math.BigDecimal fixedFee;

    /**
     * 风险比例（%）
     */
    private java.math.BigDecimal riskRatio;

    /**
     * 风险费用金额
     */
    private java.math.BigDecimal riskFee;

    /**
     * 收费方式详细说明
     */
    private String feeRemark;
}
