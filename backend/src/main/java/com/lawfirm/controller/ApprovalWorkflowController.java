package com.lawfirm.controller;

import com.lawfirm.dto.ApprovalWorkflowStepDTO;
import com.lawfirm.service.ApprovalWorkflowService;
import com.lawfirm.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/approval/workflow")
@RequiredArgsConstructor
@Tag(name = "审批流程配置", description = "自定义审批流程模板")
public class ApprovalWorkflowController {

    private final ApprovalWorkflowService approvalWorkflowService;

    @GetMapping
    @Operation(summary = "获取审批流程步骤")
    public Result<List<ApprovalWorkflowStepDTO>> list(@RequestParam String approvalType) {
        return Result.success(approvalWorkflowService.listSteps(approvalType));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "保存审批流程步骤")
    public Result<List<ApprovalWorkflowStepDTO>> save(
            @RequestParam String approvalType,
            @RequestBody List<ApprovalWorkflowStepDTO> steps) {
        return Result.success(approvalWorkflowService.saveSteps(approvalType, steps));
    }
}
