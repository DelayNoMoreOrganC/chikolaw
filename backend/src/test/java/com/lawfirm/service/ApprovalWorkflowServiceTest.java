package com.lawfirm.service;

import com.lawfirm.dto.ApprovalWorkflowStepDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ApprovalWorkflowServiceTest {

    @Autowired
    private ApprovalWorkflowService approvalWorkflowService;

    @Test
    void listAndResolveApprover_forSealType() {
        List<ApprovalWorkflowStepDTO> steps = approvalWorkflowService.listSteps("SEAL");
        assertNotNull(steps);
        assertFalse(steps.isEmpty());
        Optional<Long> approver = approvalWorkflowService.resolveFirstApproverId("SEAL");
        assertTrue(approver.isPresent() || approvalWorkflowService.isAutoApproveAll("SEAL"));
    }
}
