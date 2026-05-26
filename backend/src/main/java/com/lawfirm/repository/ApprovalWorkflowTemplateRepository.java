package com.lawfirm.repository;

import com.lawfirm.entity.ApprovalWorkflowTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalWorkflowTemplateRepository extends JpaRepository<ApprovalWorkflowTemplate, Long> {

    List<ApprovalWorkflowTemplate> findByApprovalTypeAndEnabledTrueAndDeletedFalseOrderByStepOrderAsc(String approvalType);

    List<ApprovalWorkflowTemplate> findByApprovalTypeAndDeletedFalseOrderByStepOrderAsc(String approvalType);

    void deleteByApprovalTypeAndDeletedFalse(String approvalType);
}
