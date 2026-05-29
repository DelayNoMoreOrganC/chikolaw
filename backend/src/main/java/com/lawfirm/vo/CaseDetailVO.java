package com.lawfirm.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 案件详情VO
 */
@Data
public class CaseDetailVO {

    /**
     * 基本信息
     */
    private Long id;
    private String caseNumber;
    private String caseName;
    private String caseType;
    private String caseTypeDesc;
    private String procedure;
    private String caseReason;
    private String court;
    private LocalDate filingDate;
    private LocalDate deadlineDate;
    private LocalDate commissionDate;
    private LocalDate closeDate;
    private LocalDate archiveDate;
    private String archiveLocation;
    private String summary;
    private String tags;
    private String level;
    private String levelDesc;
    private String status;
    private String statusDesc;
    private String currentStage;
    private BigDecimal amount;
    private BigDecimal attorneyFee;
    private String feeMethod;
    private String feeDescription;
    private String feeNotes;
    private BigDecimal wonAmount;
    private BigDecimal actualReceived;
    private String closeStatus;
    private String closeStatusDesc;
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
     * 新增字段（对标行政管理要求）
     */
    private LocalDate acceptanceDate;
    private String courtCaseNumber;
    private LocalDate hearingDate;
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;
    private String representationType;
    private String sourcePerson;
    private BigDecimal sourcePersonPercentage;
    private BigDecimal departmentPercentage;
    private BigDecimal firmPercentage;
    private String conflictCheckStatus;
    private Long conflictWaiverApprovalId;
    private Long filingApprovalId;
    /** 关联立案审批状态：PENDING / APPROVED / REJECTED */
    private String filingApprovalStatus;
    /** 是否存在待审批的立案申请 */
    private Boolean hasPendingFilingApproval;
    /** 是否可执行确认建案（前端按钮态） */
    private Boolean canConfirmEstablishment;

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
     * 其他委托人（可多选）
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

    /**
     * 团队信息
     */
    private Long ownerId;
    private String ownerName;
    private List<MemberVO> coOwners;
    private List<MemberVO> assistants;

    /**
     * 当事人列表
     */
    private List<PartyVO> parties;

    /**
     * 案件程序列表
     */
    private List<CaseProcedureVO> procedures;

    /**
     * 关联客户ID列表
     */
    private List<Long> clientIds;

    /**
     * 关联案件列表
     */
    private List<RelatedCaseVO> relatedCases;

    /**
     * 办案策略列表
     */
    private String strategies;

    /**
     * 阶段进度
     */
    private List<StageProgressVO> stageProgress;

    /**
     * 权限信息
     */
    private Boolean canEdit;
    private Boolean canDelete;
    private Boolean canArchive;
    private Boolean canChangeStatus;

    /**
     * 成员VO
     */
    @Data
    public static class MemberVO {
        private Long id;
        private String name;
        private String role;
    }

    /**
     * 关联案件VO
     */
    @Data
    public static class RelatedCaseVO {
        private Long id;
        private String caseName;
        private String caseNumber;
        private String caseType;
        private String status;
    }

    /**
     * 阶段进度VO
     */
    @Data
    public static class StageProgressVO {
        private String stageName;
        private Integer stageOrder;
        private String status;
        private String statusDesc;
        private LocalDate startDate;
        private LocalDate endDate;
    }
}
