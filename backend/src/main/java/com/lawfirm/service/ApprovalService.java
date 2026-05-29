package com.lawfirm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.dto.*;
import com.lawfirm.entity.*;
import com.lawfirm.enums.ApprovalStatus;
import com.lawfirm.enums.CaseStatus;
import com.lawfirm.repository.*;
import com.lawfirm.util.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 审批服务
 */
@Slf4j
@Service
public class ApprovalService {

    private final ApprovalRepository approvalRepository;
    private final ApprovalFlowRepository approvalFlowRepository;
    private final UserRepository userRepository;
    private final CaseRepository caseRepository;
    private final NotificationService notificationService;
    private final CaseIntakePendingRepository caseIntakePendingRepository;
    private final ApprovalWorkflowService approvalWorkflowService;
    private final ObjectMapper objectMapper;
    private final CaseIntakePendingService caseIntakePendingService;

    public ApprovalService(
            ApprovalRepository approvalRepository,
            ApprovalFlowRepository approvalFlowRepository,
            UserRepository userRepository,
            CaseRepository caseRepository,
            NotificationService notificationService,
            CaseIntakePendingRepository caseIntakePendingRepository,
            ApprovalWorkflowService approvalWorkflowService,
            ObjectMapper objectMapper,
            @Lazy CaseIntakePendingService caseIntakePendingService) {
        this.approvalRepository = approvalRepository;
        this.approvalFlowRepository = approvalFlowRepository;
        this.userRepository = userRepository;
        this.caseRepository = caseRepository;
        this.notificationService = notificationService;
        this.caseIntakePendingRepository = caseIntakePendingRepository;
        this.approvalWorkflowService = approvalWorkflowService;
        this.objectMapper = objectMapper;
        this.caseIntakePendingService = caseIntakePendingService;
    }

    public static final String TYPE_OFFICIAL_DOC = "OFFICIAL_DOC";
    public static final String TYPE_CASE_TERMINATION = "CASE_TERMINATION";
    public static final String TYPE_OTHER = "OTHER";

    /**
     * 审批类型常量
     */
    public static final String TYPE_SEAL = "SEAL";  // 用印申请
    public static final String TYPE_REIMBURSEMENT = "REIMBURSEMENT";  // 费用报销
    public static final String TYPE_INVOICE = "INVOICE";  // 开票申请
    public static final String TYPE_LEAVE = "LEAVE";  // 请假出差
    public static final String TYPE_PURCHASE = "PURCHASE";  // 采购申请
    public static final String TYPE_LICENSE = "LICENSE";  // 证照借用
    public static final String TYPE_CASE_FILING = "CASE_FILING";  // 立案申请（卷宗录入未匹配）

    /**
     * 创建审批
     */
    @Transactional
    public ApprovalDTO createApproval(ApprovalCreateRequest request, Long currentUserId) {
        Approval approval = new Approval();
        BeanUtils.copyProperties(request, approval);
        approval.setApplicantId(currentUserId);
        approval.setApplyTime(LocalDateTime.now());

        if (request.getCurrentApproverId() != null) {
            approval.setCurrentApproverId(request.getCurrentApproverId());
            approval.setStatus(ApprovalStatus.PENDING.getCode());
        } else if (approvalWorkflowService.isAutoApproveAll(request.getApprovalType())) {
            approval.setStatus(ApprovalStatus.APPROVED.getCode());
            approval.setApprovedTime(LocalDateTime.now());
            approval.setCurrentApproverId(currentUserId);
        } else {
            java.util.Optional<Long> firstApprover =
                    approvalWorkflowService.resolveFirstApproverId(request.getApprovalType());
            if (firstApprover.isPresent()) {
                approval.setCurrentApproverId(firstApprover.get());
                approval.setStatus(ApprovalStatus.PENDING.getCode());
            } else {
                approval.setStatus(ApprovalStatus.APPROVED.getCode());
                approval.setApprovedTime(LocalDateTime.now());
                approval.setCurrentApproverId(currentUserId);
            }
        }

        // 如果关联案件，验证案件是否存在
        if (request.getCaseId() != null) {
            Case caseEntity = caseRepository.findById(request.getCaseId())
                    .orElseThrow(() -> new RuntimeException("案件不存在"));
            // 可以在这里添加更多业务逻辑
        }

        approval = approvalRepository.save(approval);

        // 记录流程
        recordFlow(approval.getId(), currentUserId, "SUBMIT", "提交审批");

        if (TYPE_CASE_FILING.equals(approval.getApprovalType()) && approval.getCaseId() != null) {
            linkFilingApprovalToCase(approval.getCaseId(), approval.getId());
        }

        if (ApprovalStatus.APPROVED.getCode().equals(approval.getStatus())
                && TYPE_CASE_FILING.equals(approval.getApprovalType())) {
            handleCaseFilingApproved(approval);
        }

        if (ApprovalStatus.PENDING.getCode().equals(approval.getStatus())
                && approval.getCurrentApproverId() != null
                && !approval.getCurrentApproverId().equals(currentUserId)) {
            notificationService.sendApprovalPendingNotification(
                    approval.getCurrentApproverId(), approval.getId(), approval.getTitle());
        }

        return toDTO(approval);
    }

    /**
     * 同意审批
     */
    @Transactional
    public void approveApproval(Long approvalId, String comments, Long approverId) {
        Approval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new RuntimeException("审批单不存在"));

        if (approval.getCurrentApproverId() != null && !approval.getCurrentApproverId().equals(approverId)) {
            throw new RuntimeException("您不是当前审批人");
        }

        if (!ApprovalStatus.PENDING.getCode().equals(approval.getStatus())) {
            throw new RuntimeException("审批单状态不正确");
        }

        approval.setStatus(ApprovalStatus.APPROVED.getCode());
        approval.setApprovedTime(LocalDateTime.now());
        approval.setApprovalNotes(comments);

        approvalRepository.save(approval);

        // 记录流程
        recordFlow(approvalId, approverId, "APPROVE", comments);

        boolean filingNotified = false;
        if (TYPE_CASE_FILING.equals(approval.getApprovalType())) {
            filingNotified = handleCaseFilingApproved(approval);
        }

        if (approval.getApplicantId() != null && !approval.getApplicantId().equals(approverId) && !filingNotified) {
            notificationService.sendApprovalResultNotification(
                    approval.getApplicantId(), approvalId, approval.getTitle(), true);
        }
    }

    /**
     * 驳回审批
     */
    @Transactional
    public void rejectApproval(Long approvalId, String comments, Long approverId) {
        Approval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new RuntimeException("审批单不存在"));

        if (approval.getCurrentApproverId() != null && !approval.getCurrentApproverId().equals(approverId)) {
            throw new RuntimeException("您不是当前审批人");
        }

        if (!ApprovalStatus.PENDING.getCode().equals(approval.getStatus())) {
            throw new RuntimeException("审批单状态不正确");
        }

        approval.setStatus(ApprovalStatus.REJECTED.getCode());
        approval.setApprovedTime(LocalDateTime.now());
        approval.setApprovalNotes(comments);

        approvalRepository.save(approval);

        // 记录流程
        recordFlow(approvalId, approverId, "REJECT", comments);

        if (approval.getApplicantId() != null && !approval.getApplicantId().equals(approverId)) {
            notificationService.sendApprovalResultNotification(
                    approval.getApplicantId(), approvalId, approval.getTitle(), false);
        }
    }

    /**
     * 转审
     */
    @Transactional
    public void transferApproval(Long approvalId, Long newApproverId, String comments, Long currentApproverId) {
        Approval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new RuntimeException("审批单不存在"));

        if (approval.getCurrentApproverId() != null
                && !approval.getCurrentApproverId().equals(currentApproverId)) {
            throw new RuntimeException("您不是当前审批人");
        }

        // 验证新审批人
        if (!userRepository.existsById(newApproverId)) {
            throw new RuntimeException("新审批人不存在");
        }

        // 验证状态
        if (!ApprovalStatus.PENDING.getCode().equals(approval.getStatus())) {
            throw new RuntimeException("审批单状态不正确");
        }

        Long oldApproverId = approval.getCurrentApproverId();

        // 更新审批单
        approval.setCurrentApproverId(newApproverId);
        approval.setStatus(ApprovalStatus.TRANSFERRED.getCode());

        approvalRepository.save(approval);

        // 记录流程
        recordFlow(approvalId, currentApproverId, "TRANSFER",
                "转给" + getUserName(newApproverId) + "，备注：" + comments);

        // 重置为待审批状态
        approval.setStatus(ApprovalStatus.PENDING.getCode());
        approvalRepository.save(approval);
    }

    /**
     * 撤回审批
     */
    @Transactional
    public void withdrawApproval(Long approvalId, Long applicantId) {
        Approval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new RuntimeException("审批单不存在"));

        // 验证申请人
        if (!approval.getApplicantId().equals(applicantId)) {
            throw new RuntimeException("您不是申请人，无法撤回");
        }

        // 验证状态
        if (!ApprovalStatus.PENDING.getCode().equals(approval.getStatus())) {
            throw new RuntimeException("只能撤回待审批的单据");
        }

        // 更新状态
        approval.setStatus(ApprovalStatus.WITHDRAWN.getCode());
        approvalRepository.save(approval);

        // 记录流程
        recordFlow(approvalId, applicantId, "WITHDRAW", "申请人撤回");
    }

    /**
     * 催办
     */
    @Transactional
    public void urgeApproval(Long approvalId, Long applicantId) {
        Approval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new RuntimeException("审批单不存在"));

        // 验证申请人
        if (!approval.getApplicantId().equals(applicantId)) {
            throw new RuntimeException("您不是申请人，无法催办");
        }

        // 验证状态
        if (!ApprovalStatus.PENDING.getCode().equals(approval.getStatus())) {
            throw new RuntimeException("只能催办待审批的单据");
        }

        // 记录流程（作为催办记录）
        recordFlow(approvalId, applicantId, "URGE", "申请人催办");

        // 发送催办通知
        User applicant = userRepository.findById(applicantId).orElse(null);
        String applicantName = applicant != null ? applicant.getRealName() : "申请人";
        String title = "审批催办提醒";
        String content = String.format("审批单「%s」被 %s 催办，请及时处理。", approval.getTitle(), applicantName);

        notificationService.sendNotification(
                approval.getCurrentApproverId(),
                title,
                content,
                NotificationService.CATEGORY_APPROVAL,
                approvalId,
                "Approval"
        );

        log.info("审批单 {} 已催办，通知审批人：{}", approvalId, approval.getCurrentApproverId());
    }

    /**
     * 获取审批列表
     */
    public PageResult<ApprovalDTO> getApprovalList(ApprovalQueryRequest request, Long currentUserId) {
        Pageable pageable = PageRequest.of(
                request.getPage() - 1,
                request.getSize(),
                Sort.by(Sort.Direction.fromString(request.getSortDirection()), request.getSortField())
        );

        Specification<Approval> spec = (root, query, cb) -> {
            List<javax.persistence.criteria.Predicate> predicates = new ArrayList<>();

            // 基础条件
            if (request.getApprovalType() != null) {
                predicates.add(cb.equal(root.get("approvalType"), request.getApprovalType()));
            }

            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            }

            // 权限控制：只能看到自己申请的或自己需要审批的
            if (request.getApplicantId() != null) {
                predicates.add(cb.equal(root.get("applicantId"), request.getApplicantId()));
            } else if (request.getCurrentApproverId() != null) {
                predicates.add(cb.equal(root.get("currentApproverId"), request.getCurrentApproverId()));
            } else {
                // 默认：查看自己相关的
                javax.persistence.criteria.Predicate applicantCondition =
                        cb.equal(root.get("applicantId"), currentUserId);
                javax.persistence.criteria.Predicate approverCondition =
                        cb.equal(root.get("currentApproverId"), currentUserId);
                predicates.add(cb.or(applicantCondition, approverCondition));
            }

            if (request.getCaseId() != null) {
                predicates.add(cb.equal(root.get("caseId"), request.getCaseId()));
            }

            if (request.getKeyword() != null) {
                String keyword = "%" + request.getKeyword() + "%";
                javax.persistence.criteria.Predicate titleCondition =
                        cb.like(root.get("title"), keyword);
                javax.persistence.criteria.Predicate contentCondition =
                        cb.like(root.get("content"), keyword);
                predicates.add(cb.or(titleCondition, contentCondition));
            }

            // 排除已删除
            predicates.add(cb.equal(root.get("deleted"), false));

            return cb.and(predicates.toArray(new javax.persistence.criteria.Predicate[0]));
        };

        Page<Approval> page = approvalRepository.findAll(spec, pageable);

        List<ApprovalDTO> records = page.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResult.of(
                (long) request.getPage(),
                (long) request.getSize(),
                page.getTotalElements(),
                records
        );
    }

    /**
     * 获取审批详情
     */
    public ApprovalDTO getApprovalDetail(Long approvalId) {
        Approval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new RuntimeException("审批单不存在"));
        return toDTO(approval);
    }

    /**
     * 获取审批流程记录
     */
    public List<ApprovalFlow> getApprovalFlow(Long approvalId) {
        return approvalFlowRepository.findByApprovalIdOrderByActionTimeAsc(approvalId);
    }

    /**
     * 获取审批类型列表
     */
    public List<Map<String, String>> getApprovalTypes() {
        List<Map<String, String>> types = new ArrayList<>();
        types.add(createTypeItem(TYPE_SEAL, "用印申请"));
        types.add(createTypeItem(TYPE_REIMBURSEMENT, "费用报销"));
        types.add(createTypeItem(TYPE_INVOICE, "开票申请"));
        types.add(createTypeItem(TYPE_LEAVE, "请假出差"));
        types.add(createTypeItem(TYPE_PURCHASE, "采购申请"));
        types.add(createTypeItem(TYPE_LICENSE, "证照借用"));
        types.add(createTypeItem(TYPE_CASE_FILING, "立案申请"));
        types.add(createTypeItem(TYPE_OFFICIAL_DOC, "公文流转"));
        types.add(createTypeItem(TYPE_CASE_TERMINATION, "终止委托"));
        types.add(createTypeItem(TYPE_OTHER, "其他"));
        return types;
    }

    // 辅助方法

    private void recordFlow(Long approvalId, Long approverId, String action, String comments) {
        ApprovalFlow flow = new ApprovalFlow();
        flow.setApprovalId(approvalId);
        flow.setApproverId(approverId);
        flow.setAction(action);
        flow.setComments(comments);
        flow.setActionTime(LocalDateTime.now());
        approvalFlowRepository.save(flow);
    }

    private ApprovalDTO toDTO(Approval approval) {
        ApprovalDTO dto = new ApprovalDTO();
        BeanUtils.copyProperties(approval, dto);

        dto.setApplicantName(getUserName(approval.getApplicantId()));
        dto.setCurrentApproverName(getUserName(approval.getCurrentApproverId()));
        dto.setStatusDesc(getStatusDesc(approval.getStatus()));

        if (approval.getCaseId() != null) {
            caseRepository.findById(approval.getCaseId()).ifPresent(c -> {
                dto.setCaseName(c.getCaseName());
            });
        }

        return dto;
    }

    private String getUserName(Long userId) {
        return userRepository.findById(userId)
                .map(User::getRealName)
                .orElse("未知");
    }

    private String getStatusDesc(String status) {
        if (status == null) return null;
        switch (status) {
            case "PENDING": return "待审批";
            case "APPROVED": return "已同意";
            case "REJECTED": return "已驳回";
            case "TRANSFERRED": return "已转审";
            case "WITHDRAWN": return "已撤回";
            default: return status;
        }
    }

    private Map<String, String> createTypeItem(String code, String name) {
        Map<String, String> item = new HashMap<>();
        item.put("code", code);
        item.put("name", name);
        return item;
    }

    private boolean handleCaseFilingApproved(Approval approval) {
        Long approvalId = approval.getId();
        Long pendingId = parseIntakePendingId(approval.getAttachments());
        Long targetCaseId = approval.getCaseId();

        if (pendingId != null) {
            markIntakeFilingApproved(pendingId, approvalId);
            try {
                CaseIntakeDraftResultDTO draft = caseIntakePendingService.createDraftCaseAfterFilingApproved(
                        pendingId, approval.getApplicantId());
                if (draft.getDraftCaseId() != null) {
                    targetCaseId = draft.getDraftCaseId();
                    linkFilingApprovalToCase(targetCaseId, approvalId);
                    if (approval.getApplicantId() != null) {
                        notificationService.sendCaseFilingDraftReadyNotification(
                                approval.getApplicantId(), approvalId, approval.getTitle(),
                                draft.getDraftCaseId(), draft.isIntakeAttached());
                        return true;
                    }
                }
            } catch (Exception e) {
                log.warn("立案审批通过后创建草稿案件失败: pendingId={}, approvalId={}, err={}",
                        pendingId, approvalId, e.getMessage());
            }
        }

        if (targetCaseId != null) {
            linkFilingApprovalToCase(targetCaseId, approvalId);
            if (approval.getApplicantId() != null) {
                notificationService.sendCaseFilingDraftReadyNotification(
                        approval.getApplicantId(), approvalId, approval.getTitle(),
                        targetCaseId, pendingId != null);
                return true;
            }
        }
        return false;
    }

    private void linkFilingApprovalToCase(Long caseId, Long approvalId) {
        if (caseId == null || approvalId == null) {
            return;
        }
        caseRepository.findById(caseId).ifPresent(c -> {
            if (CaseStatus.ACTIVE.getCode().equals(c.getStatus())
                    || CaseStatus.CLOSED.getCode().equals(c.getStatus())
                    || CaseStatus.ARCHIVED.getCode().equals(c.getStatus())) {
                return;
            }
            c.setFilingApprovalId(approvalId);
            caseRepository.save(c);
        });
    }

    private void markIntakeFilingApproved(Long pendingId, Long approvalId) {
        caseIntakePendingRepository.findById(pendingId).ifPresent(p -> {
            if ("PENDING".equals(p.getStatus()) || "FILING_APPROVED".equals(p.getStatus())) {
                p.setStatus("FILING_APPROVED");
                if (approvalId != null) {
                    p.setApprovalId(approvalId);
                }
                caseIntakePendingRepository.save(p);
            }
        });
    }

    private Long parseIntakePendingId(String attachments) {
        if (attachments == null || attachments.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(attachments);
            if (node.has("intakePendingId") && !node.get("intakePendingId").isNull()) {
                return node.get("intakePendingId").asLong();
            }
        } catch (Exception e) {
            log.warn("解析立案审批附件失败: {}", attachments);
        }
        return null;
    }
}
