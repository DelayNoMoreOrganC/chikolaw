package com.lawfirm.service;

import com.lawfirm.dto.ApprovalWorkflowStepDTO;
import com.lawfirm.entity.ApprovalWorkflowTemplate;
import com.lawfirm.entity.User;
import com.lawfirm.repository.ApprovalWorkflowTemplateRepository;
import com.lawfirm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 审批自定义流程配置（PRD 8.2 最小可行实现）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalWorkflowService {

    public static final String DEFAULT_TYPE = "*";

    private final ApprovalWorkflowTemplateRepository templateRepository;
    private final UserRepository userRepository;

    @PostConstruct
    public void seedDefaultTemplatesIfEmpty() {
        if (templateRepository.count() > 0) {
            return;
        }
        log.info("初始化默认审批流程模板");
        Long defaultApprover = userRepository.findAll().stream()
                .map(User::getId)
                .findFirst()
                .orElse(1L);
        String[] types = {
                "SEAL", "REIMBURSEMENT", "INVOICE", "LEAVE", "PURCHASE", "LICENSE",
                "CASE_FILING", "OFFICIAL_DOC", "CASE_TERMINATION", "OTHER"
        };
        for (String type : types) {
            saveSteps(type, List.of(buildStep(type, 1, "部门主管审批", defaultApprover, false)));
        }
        saveSteps(DEFAULT_TYPE, List.of(buildStep(DEFAULT_TYPE, 1, "默认审批", defaultApprover, false)));
    }

    private ApprovalWorkflowStepDTO buildStep(String type, int order, String name, Long userId, boolean auto) {
        ApprovalWorkflowStepDTO dto = new ApprovalWorkflowStepDTO();
        dto.setApprovalType(type);
        dto.setStepOrder(order);
        dto.setStepName(name);
        dto.setApproverUserId(userId);
        dto.setAutoApprove(auto);
        dto.setEnabled(true);
        return dto;
    }

    public List<ApprovalWorkflowStepDTO> listSteps(String approvalType) {
        List<ApprovalWorkflowTemplate> list = templateRepository
                .findByApprovalTypeAndDeletedFalseOrderByStepOrderAsc(approvalType);
        if (list.isEmpty() && !DEFAULT_TYPE.equals(approvalType)) {
            list = templateRepository.findByApprovalTypeAndDeletedFalseOrderByStepOrderAsc(DEFAULT_TYPE);
        }
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public List<ApprovalWorkflowStepDTO> saveSteps(String approvalType, List<ApprovalWorkflowStepDTO> steps) {
        List<ApprovalWorkflowTemplate> existing = templateRepository
                .findByApprovalTypeAndDeletedFalseOrderByStepOrderAsc(approvalType);
        for (ApprovalWorkflowTemplate t : existing) {
            t.setDeleted(true);
            templateRepository.save(t);
        }
        List<ApprovalWorkflowTemplate> saved = new ArrayList<>();
        int order = 1;
        for (ApprovalWorkflowStepDTO dto : steps) {
            ApprovalWorkflowTemplate entity = new ApprovalWorkflowTemplate();
            entity.setApprovalType(approvalType);
            entity.setStepOrder(dto.getStepOrder() != null ? dto.getStepOrder() : order++);
            entity.setStepName(dto.getStepName());
            entity.setApproverUserId(dto.getApproverUserId());
            entity.setApproverRoleCode(dto.getApproverRoleCode());
            entity.setAutoApprove(Boolean.TRUE.equals(dto.getAutoApprove()));
            entity.setEnabled(dto.getEnabled() == null || dto.getEnabled());
            saved.add(templateRepository.save(entity));
        }
        return saved.stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * 解析首个待办审批人；若首步 autoApprove 返回 empty 表示可直接通过
     */
    public Optional<Long> resolveFirstApproverId(String approvalType) {
        List<ApprovalWorkflowTemplate> steps = loadEnabledSteps(approvalType);
        if (steps.isEmpty()) {
            return userRepository.findAll().stream().map(User::getId).findFirst();
        }
        ApprovalWorkflowTemplate first = steps.get(0);
        if (Boolean.TRUE.equals(first.getAutoApprove())) {
            return Optional.empty();
        }
        if (first.getApproverUserId() != null) {
            return Optional.of(first.getApproverUserId());
        }
        return userRepository.findAll().stream().map(User::getId).findFirst();
    }

    public boolean isAutoApproveAll(String approvalType) {
        List<ApprovalWorkflowTemplate> steps = loadEnabledSteps(approvalType);
        return !steps.isEmpty() && steps.stream().allMatch(s -> Boolean.TRUE.equals(s.getAutoApprove()));
    }

    private List<ApprovalWorkflowTemplate> loadEnabledSteps(String approvalType) {
        List<ApprovalWorkflowTemplate> steps = templateRepository
                .findByApprovalTypeAndEnabledTrueAndDeletedFalseOrderByStepOrderAsc(approvalType);
        if (steps.isEmpty()) {
            steps = templateRepository
                    .findByApprovalTypeAndEnabledTrueAndDeletedFalseOrderByStepOrderAsc(DEFAULT_TYPE);
        }
        return steps;
    }

    private ApprovalWorkflowStepDTO toDto(ApprovalWorkflowTemplate t) {
        ApprovalWorkflowStepDTO dto = new ApprovalWorkflowStepDTO();
        dto.setId(t.getId());
        dto.setApprovalType(t.getApprovalType());
        dto.setStepOrder(t.getStepOrder());
        dto.setStepName(t.getStepName());
        dto.setApproverUserId(t.getApproverUserId());
        dto.setApproverRoleCode(t.getApproverRoleCode());
        dto.setAutoApprove(t.getAutoApprove());
        dto.setEnabled(t.getEnabled());
        return dto;
    }
}
