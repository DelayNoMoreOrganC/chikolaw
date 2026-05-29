package com.lawfirm.repository;

import com.lawfirm.entity.Approval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 审批Repository
 */
@Repository
public interface ApprovalRepository extends JpaRepository<Approval, Long>, JpaSpecificationExecutor<Approval> {

    /**
     * 根据申请人查找审批列表
     */
    List<Approval> findByApplicantIdOrderByApplyTimeDesc(Long applicantId);

    /**
     * 根据当前审批人查找审批列表
     */
    List<Approval> findByCurrentApproverIdOrderByApplyTimeDesc(Long currentApproverId);

    /**
     * 根据状态查找审批列表
     */
    List<Approval> findByStatusOrderByApplyTimeDesc(String status);

    /**
     * 根据案件ID查找审批列表
     */
    List<Approval> findByCaseIdOrderByApplyTimeDesc(Long caseId);

    java.util.Optional<Approval> findFirstByCaseIdAndApprovalTypeAndStatusOrderByApplyTimeDesc(
            Long caseId, String approvalType, String status);
}
