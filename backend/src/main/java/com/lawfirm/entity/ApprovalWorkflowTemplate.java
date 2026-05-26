package com.lawfirm.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

/**
 * 审批自定义流程模板（按类型配置审批步骤）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "approval_workflow_template")
public class ApprovalWorkflowTemplate extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 审批类型编码，* 表示默认 */
    @Column(name = "approval_type", nullable = false, length = 50)
    private String approvalType;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder = 1;

    @Column(name = "step_name", length = 100)
    private String stepName;

    /** 指定审批人用户ID（优先） */
    @Column(name = "approver_user_id")
    private Long approverUserId;

    /** 审批人角色编码（备用，如 ADMIN、DIRECTOR） */
    @Column(name = "approver_role_code", length = 50)
    private String approverRoleCode;

    /** 本步骤是否自动通过（如行政小额用印） */
    @Column(name = "auto_approve")
    private Boolean autoApprove = false;

    @Column(name = "enabled")
    private Boolean enabled = true;
}
