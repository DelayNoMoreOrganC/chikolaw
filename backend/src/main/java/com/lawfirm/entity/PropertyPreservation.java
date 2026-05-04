package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 财产保全实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "property_preservation")
public class PropertyPreservation extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "案件ID不能为空")
    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @NotBlank(message = "被申请人不能为空")
    @Column(name = "target_person", nullable = false, length = 100)
    private String targetPerson;

    @NotBlank(message = "保全标的不能为空")
    @Column(name = "preservation_target", nullable = false)
    private String preservationTarget;

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    @NotBlank(message = "管辖法院不能为空")
    @Column(name = "court", nullable = false, length = 200)
    private String court;

    @NotNull(message = "保全日期不能为空")
    @Column(name = "preservation_date", nullable = false)
    private LocalDate preservationDate;

    @NotBlank(message = "状态不能为空")
    @Column(name = "status", nullable = false, length = 20)
    private String status; // PENDING, APPROVED, REJECTED, EXECUTED

    @Column(name = "case_number", length = 50)
    private String caseNumber;

    @Column(name = "insurance_amount", precision = 15, scale = 2)
    private BigDecimal insuranceAmount;

    @Column(name = "insurance_company", length = 200)
    private String insuranceCompany;

    @Column(name = "guarantee_type", length = 50)
    private String guaranteeType;

    @Column(name = "registry_search_record", length = 500)
    private String registrySearchRecord;

    @Column(name = "collateral_status", length = 100)
    private String collateralStatus;

    @Column(name = "seizure_sequence", length = 50)
    private String seizureSequence;

    @Column(name = "risk_warning", length = 500)
    private String riskWarning;

    @Lob
    private String remarks;

    @NotNull(message = "创建人不能为空")
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
}
