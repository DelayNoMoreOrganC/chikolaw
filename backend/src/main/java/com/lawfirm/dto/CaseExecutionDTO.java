package com.lawfirm.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 案件执行DTO
 */
@Data
public class CaseExecutionDTO {

    private Long id;
    private Long caseId;
    private String caseName;

    @NotBlank(message = "执行案号不能为空")
    private String executionCaseNumber;

    @NotBlank(message = "管辖法院不能为空")
    private String court;

    @NotBlank(message = "申请人不能为空")
    private String applicant;

    @NotBlank(message = "被执行人不能为空")
    private String respondent;

    private String executionTarget;
    private BigDecimal amount;

    @NotNull(message = "执行日期不能为空")
    private LocalDate executionDate;

    @NotBlank(message = "状态不能为空")
    private String status;

    private BigDecimal executedAmount;
    private String executionMilestone;
    private String nextStep;
    private String valuationStatus;
    private String auctionStatus;
    private String distributionStatus;
    private String terminationStatus;
    private BigDecimal debtCalculation;
    private String remarks;
    private Long createdBy;
    private String createdByName;
}
