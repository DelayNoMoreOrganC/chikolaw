package com.lawfirm.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 财产保全DTO
 */
@Data
public class PropertyPreservationDTO {

    private Long id;
    private Long caseId;
    private String caseName;

    @NotBlank(message = "被申请人不能为空")
    private String targetPerson;

    @NotBlank(message = "保全标的不能为空")
    private String preservationTarget;

    private BigDecimal amount;

    @NotBlank(message = "管辖法院不能为空")
    private String court;

    @NotNull(message = "保全日期不能为空")
    private LocalDate preservationDate;

    @NotBlank(message = "状态不能为空")
    private String status;

    private String caseNumber;
    private BigDecimal insuranceAmount;
    private String insuranceCompany;
    private String guaranteeType;
    private String registrySearchRecord;
    private String collateralStatus;
    private String seizureSequence;
    private String riskWarning;
    private String remarks;
    private Long createdBy;
    private String createdByName;
}
