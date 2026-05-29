package com.lawfirm.dto;

import com.lawfirm.validation.PercentageSum;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.List;

@Data
@PercentageSum(message = "案源人、承办部门、律所提留三者比例之和必须为100%")
public class CaseCreateRequest {
    // 基本信息
    private String caseNumber;

    @NotBlank(message = "案件名称不能为空")
    private String caseName;

    @NotBlank(message = "案件类型不能为空")
    private String caseType;

    private String procedure;

    private String level;

    private Long ownerId;

    // 团队成员
    private List<Long> coOwnerIds;
    private List<Long> assistantIds;

    // 案件详情
    private String caseReason;
    private String court;
    private LocalDate filingDate;
    private LocalDate deadlineDate;
    private LocalDate commissionDate;

    // 标签和摘要
    private String tags;
    private String summary;

    // 办案策略
    private String strategies;

    // 案件状态
    private String status;

    // 当前阶段
    private String currentStage;

    // 金额相关
    private java.math.BigDecimal amount;
    private java.math.BigDecimal attorneyFee;
    private String feeMethod;

    // 金融不良资产专项字段
    private String npaSubtype;
    private String entrustingBankName;
    private String assetBatchNo;
    private String transferAgreementNo;
    private String loanContractNo;
    private java.math.BigDecimal principalBalance;
    private java.math.BigDecimal interestBalance;
    private String guaranteeType;
    private String collateralStatus;
    private String preservationStatus;
    private java.math.BigDecimal executionRecoveryAmount;
    private String terminationStatus;

    // 当事人信息
    private List<PartyDTO> parties;

    // 应收款信息
    private List<ReceivableRequest> receivables;

    // ==================== 新增字段（对标行政管理要求）====================

    /**
     * 收案日期
     */
    @javax.validation.constraints.NotNull(message = "收案日期不能为空")
    private java.time.LocalDate acceptanceDate;

    /**
     * 法院案号
     */
    private String courtCaseNumber;

    /**
     * 开庭日期
     */
    private java.time.LocalDate hearingDate;

    /**
     * 合同服务开始时间（顾问类）
     */
    private java.time.LocalDate contractStartDate;

    /**
     * 合同服务结束时间（顾问类）
     */
    private java.time.LocalDate contractEndDate;

    /**
     * 代理类型（刑事：原告/被告）
     */
    private String representationType;

    /**
     * 案源人
     */
    @javax.validation.constraints.NotBlank(message = "案源人不能为空")
    private String sourcePerson;

    /**
     * 案源人分配比例（%）
     */
    private java.math.BigDecimal sourcePersonPercentage;

    /**
     * 承办部门分配比例（%）
     */
    private java.math.BigDecimal departmentPercentage;

    /**
     * 律所提留比例（%）
     */
    private java.math.BigDecimal firmPercentage;

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
     * 风险代理时必填
     */
    private java.math.BigDecimal disputedAmount;

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
     * 制度要求：多于一名委托人时在"其他委托人"框内选择（可多选）
     */
    private String otherClients;

    /**
     * 审级（可多选，JSON数组格式）
     * 制度要求：选择代理案件的审级（可多选）
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

    /**
     * 保存为草稿（待立案），不生成正式办案待办
     */
    private Boolean saveAsDraft;

    @Data
    public static class ReceivableRequest {
        private String name;
        private java.math.BigDecimal amount;
        private String dueDate;
        private String notes;
    }
}
