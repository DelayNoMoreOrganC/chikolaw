package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 案件实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "case", indexes = {
    @Index(name = "idx_case_name", columnList = "case_name"),
    @Index(name = "idx_case_status", columnList = "status"),
    @Index(name = "idx_case_owner", columnList = "owner_id"),
    @Index(name = "idx_case_created", columnList = "created_at"),
    @Index(name = "idx_case_deleted", columnList = "deleted")
})
public class Case extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_number", unique = true, length = 50)
    private String caseNumber;

    @NotBlank(message = "案件名称不能为空")
    @Column(name = "case_name", nullable = false)
    private String caseName;

    @NotBlank(message = "案件类型不能为空")
    @Column(name = "case_type", nullable = false, length = 20)
    private String caseType;

    @Column(name = "case_reason", length = 100)
    private String caseReason;

    @Column(length = 20)
    private String procedure;

    @Column(length = 20)
    private String level = "GENERAL";

    @Column(length = 20)
    private String status = "CONSULTATION";

    @Column(name = "current_stage", length = 50)
    private String currentStage;

    @Column(length = 100)
    private String court;

    @Column(name = "filing_date")
    private LocalDate filingDate;

    @Column(name = "deadline_date")
    private LocalDate deadlineDate;

    @Column(name = "commission_date")
    private LocalDate commissionDate;

    @Column(name = "close_date")
    private LocalDate closeDate;

    @Column(name = "close_status", length = 20)
    private String closeStatus;

    @Column(name = "archive_date")
    private LocalDate archiveDate;

    @Column(name = "archive_location")
    private String archiveLocation;

    private String summary;

    @Column(length = 500)
    private String tags;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "attorney_fee", precision = 15, scale = 2)
    private BigDecimal attorneyFee;

    @Column(name = "fee_method", length = 20)
    private String feeMethod;

    @Column(name = "won_amount", precision = 15, scale = 2)
    private BigDecimal wonAmount;

    @Column(name = "actual_received", precision = 15, scale = 2)
    private BigDecimal actualReceived;

    @Lob
    @Column(name = "strategies")
    private String strategies;

    @NotNull(message = "主办律师不能为空")
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /**
     * 客户ID（关联客户）
     */
    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "npa_subtype", length = 30)
    private String npaSubtype;

    @Column(name = "entrusting_bank_name", length = 100)
    private String entrustingBankName;

    @Column(name = "asset_batch_no", length = 100)
    private String assetBatchNo;

    @Column(name = "transfer_agreement_no", length = 100)
    private String transferAgreementNo;

    @Column(name = "loan_contract_no", length = 100)
    private String loanContractNo;

    @Column(name = "principal_balance", precision = 15, scale = 2)
    private BigDecimal principalBalance;

    @Column(name = "interest_balance", precision = 15, scale = 2)
    private BigDecimal interestBalance;

    @Column(name = "guarantee_type", length = 50)
    private String guaranteeType;

    @Column(name = "collateral_status", length = 100)
    private String collateralStatus;

    @Column(name = "preservation_status", length = 50)
    private String preservationStatus;

    @Column(name = "execution_recovery_amount", precision = 15, scale = 2)
    private BigDecimal executionRecoveryAmount;

    @Column(name = "termination_status", length = 50)
    private String terminationStatus;

    // ==================== 新增字段（对标行政管理要求）====================

    /**
     * 收案日期
     */
    @Column(name = "acceptance_date")
    private LocalDate acceptanceDate;

    /**
     * 法院案号
     */
    @Column(name = "court_case_number", length = 100)
    private String courtCaseNumber;

    /**
     * 开庭日期
     */
    @Column(name = "hearing_date")
    private LocalDate hearingDate;

    /**
     * 合同服务开始时间（顾问类）
     */
    @Column(name = "contract_start_date")
    private LocalDate contractStartDate;

    /**
     * 合同服务结束时间（顾问类）
     */
    @Column(name = "contract_end_date")
    private LocalDate contractEndDate;

    /**
     * 代理类型（刑事：原告/被告）
     */
    @Column(name = "representation_type", length = 20)
    private String representationType;

    /**
     * 案源人（支持多人，JSON数组格式存储）
     * 示例：["张律师", "李律师"]
     */
    @Lob
    private String sourcePerson;

    /**
     * 案源人分配比例（%）
     */
    @Column(name = "source_person_percentage", precision = 5, scale = 2)
    private BigDecimal sourcePersonPercentage;

    /**
     * 承办部门分配比例（%）
     */
    @Column(name = "department_percentage", precision = 5, scale = 2)
    private BigDecimal departmentPercentage;

    /**
     * 律所提留比例（%）
     */
    @Column(name = "firm_percentage", precision = 5, scale = 2)
    private BigDecimal firmPercentage;

    /**
     * 利益冲突审查状态
     */
    @Column(name = "conflict_check_status", length = 20)
    private String conflictCheckStatus;

    /**
     * 利益冲突豁免审批ID
     */
    @Column(name = "conflict_waiver_approval_id")
    private Long conflictWaiverApprovalId;

    // ==================== 新增字段（对标系统问题.xlsx）====================

    /**
     * 业务类型（根据案件类型的细分）
     * 民事：婚姻家庭、公司、金融、证券、保险、海事海商、建设工程、劳动、知识产权
     * 刑事：一般代理、当事人自行委托、法律援助、法定通知辩护、扩大通知辩护、刑事附带民事诉讼
     * 行政：一般代理/应诉、行政申诉
     * 非诉：公司、金融、证券、保险、反垄断、建设工程与房地产、劳动、知识产权、税法
     * 顾问：常年法律顾问/专项法律顾问
     */
    @Column(name = "business_type", length = 50)
    private String businessType;

    /**
     * 犯罪嫌疑人（刑事案件专用）
     */
    @Column(name = "criminal_suspect", length = 100)
    private String criminalSuspect;

    /**
     * 涉案标的（单位：万元）
     * 风险代理时必填
     */
    @Column(name = "disputed_amount", precision = 15, scale = 2)
    private BigDecimal disputedAmount;

    /**
     * 主办部门（支持多部门，JSON数组格式存储）
     * 示例：["诉讼一部", "诉讼二部"]
     */
    @Lob
    private String hostDepartment;

    /**
     * 协办部门（支持多部门，根据协办律师自动关联）
     */
    @Lob
    private String coDepartments;

    /**
     * 备注（收费方式备注等）
     */
    @Column(name = "remark", length = 1000)
    private String remark;

    // ==================== 新增字段（对标案件登记及系统立结案流程）====================

    /**
     * 其他委托人（可多选，JSON数组格式存储）
     * 制度要求：多于一名委托人时在"其他委托人"框内选择（可多选）
     * 示例：["委托人2", "委托人3"]
     */
    @Lob
    private String otherClients;

    /**
     * 审级（可多选，JSON数组格式存储）
     * 制度要求：选择代理案件的审级（可多选）
     * 示例：["一审", "二审", "执行"]
     */
    @Lob
    private String procedureLevels;

    /**
     * 是否法律援助案件
     * 制度要求：如案件是法援案件的，在"是否法律援助案件"框内选择"是"
     */
    @Column(name = "is_legal_aid")
    private Boolean isLegalAid = false;

    /**
     * 固定费用金额
     * 制度要求：固定收费案件，在"固定费用"框内直接输入案件收费总金额
     */
    @Column(name = "fixed_fee", precision = 15, scale = 2)
    private BigDecimal fixedFee;

    /**
     * 风险比例（%）
     * 制度要求：按比例收取风险费用的在"风险比例"框内输入比例数字
     */
    @Column(name = "risk_ratio", precision = 5, scale = 2)
    private BigDecimal riskRatio;

    /**
     * 风险费用金额
     * 制度要求：按条件收取具体风险费用金额的在"风险费用"框内输入金额
     */
    @Column(name = "risk_fee", precision = 15, scale = 2)
    private BigDecimal riskFee;

    /**
     * 收费方式详细说明
     * 制度要求：备注-录入收费相关的备注，如有其他审级收费约定、风险费用支付约定细则等
     */
    @Column(name = "fee_remark", length = 1000)
    private String feeRemark;
}
