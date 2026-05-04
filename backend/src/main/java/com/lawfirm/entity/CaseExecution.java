package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 案件执行实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "case_execution")
public class CaseExecution extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "案件ID不能为空")
    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @NotBlank(message = "执行案号不能为空")
    @Column(name = "execution_case_number", nullable = false, length = 100)
    private String executionCaseNumber;

    @NotBlank(message = "管辖法院不能为空")
    @Column(name = "court", nullable = false, length = 200)
    private String court;

    @NotBlank(message = "申请人不能为空")
    @Column(name = "applicant", nullable = false, length = 100)
    private String applicant;

    @NotBlank(message = "被执行人不能为空")
    @Column(name = "respondent", nullable = false, length = 100)
    private String respondent;

    @Column(name = "execution_target", length = 200)
    private String executionTarget;

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    @NotNull(message = "执行日期不能为空")
    @Column(name = "execution_date", nullable = false)
    private LocalDate executionDate;

    @NotBlank(message = "状态不能为空")
    @Column(name = "status", nullable = false, length = 20)
    private String status; // PENDING, IN_PROGRESS, COMPLETED, SUSPENDED, TERMINATED

    @Column(name = "executed_amount", precision = 15, scale = 2)
    private BigDecimal executedAmount;

    @Column(name = "execution_milestone", length = 200)
    private String executionMilestone;

    @Column(name = "next_step", length = 200)
    private String nextStep;

    @Column(name = "valuation_status", length = 100)
    private String valuationStatus;

    @Column(name = "auction_status", length = 100)
    private String auctionStatus;

    @Column(name = "distribution_status", length = 100)
    private String distributionStatus;

    @Column(name = "termination_status", length = 50)
    private String terminationStatus;

    @Column(name = "debt_calculation", precision = 15, scale = 2)
    private BigDecimal debtCalculation;

    @Lob
    private String remarks;

    @NotNull(message = "创建人不能为空")
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
}
