package com.lawfirm.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 审批创建请求
 */
@Data
public class ApprovalCreateRequest {

    @NotBlank(message = "审批类型不能为空")
    private String approvalType;

    @NotBlank(message = "审批标题不能为空")
    private String title;

    @NotBlank(message = "审批内容不能为空")
    private String content;

    private Long caseId;

    /** 可选；未传时按自定义流程模板解析 */
    private Long currentApproverId;

    private String attachments;
}
