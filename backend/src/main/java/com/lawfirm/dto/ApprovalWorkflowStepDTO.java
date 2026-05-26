package com.lawfirm.dto;

import lombok.Data;

@Data
public class ApprovalWorkflowStepDTO {
    private Long id;
    private String approvalType;
    private Integer stepOrder;
    private String stepName;
    private Long approverUserId;
    private String approverRoleCode;
    private Boolean autoApprove;
    private Boolean enabled;
}
