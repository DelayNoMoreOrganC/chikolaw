package com.lawfirm.service;

import com.lawfirm.dto.TodoDTO;
import com.lawfirm.entity.Case;
import com.lawfirm.entity.CaseFlowTemplate;
import com.lawfirm.entity.CaseMember;
import com.lawfirm.entity.CaseStage;
import com.lawfirm.entity.CaseStageTodoTemplate;
import com.lawfirm.repository.CaseFlowTemplateRepository;
import com.lawfirm.repository.CaseMemberRepository;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.repository.CaseStageRepository;
import com.lawfirm.repository.CaseStageTodoTemplateRepository;
import com.lawfirm.repository.RoleRepository;
import com.lawfirm.repository.UserRoleRepository;
import com.lawfirm.vo.CaseDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 案件阶段服务（阶段列表以 {@link CaseFlowDefinitionService} 为唯一基准）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseStageService {

    private final CaseStageRepository caseStageRepository;
    private final CaseRepository caseRepository;
    private final CaseTimelineService caseTimelineService;
    private final CaseStageTodoTemplateRepository stageTodoTemplateRepository;
    private final CaseFlowTemplateRepository flowTemplateRepository;
    private final CaseFlowDefinitionService caseFlowDefinitionService;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final CaseMemberRepository caseMemberRepository;
    private final TodoService todoService;

    @Transactional
    public void initializeStages(Long caseId, String caseType) {
        List<String> stages = caseFlowDefinitionService.getStageNames(caseType);

        for (int i = 0; i < stages.size(); i++) {
            CaseStage stage = new CaseStage();
            stage.setCaseId(caseId);
            stage.setStageName(stages.get(i));
            stage.setStageOrder(i + 1);
            stage.setStatus(i == 0 ? "IN_PROGRESS" : "PENDING");
            stage.setDeleted(false);
            caseStageRepository.save(stage);
        }

        Case caseEntity = caseRepository.findById(caseId).orElseThrow();
        caseEntity.setCurrentStage(stages.get(0));
        caseRepository.save(caseEntity);
    }

    @Transactional
    public void changeStatus(Long caseId, String targetStage, String reason, Long operatorId) {
        String canonicalTarget = caseFlowDefinitionService.canonicalStageName(targetStage);

        Optional<CaseStage> currentStageOpt = caseStageRepository.findCurrentStage(caseId);
        if (currentStageOpt.isEmpty()) {
            throw new RuntimeException("未找到当前进行中的阶段");
        }

        CaseStage currentStage = currentStageOpt.get();
        List<CaseStage> allStages = caseStageRepository.findByCaseIdAndDeletedFalseOrderByStageOrder(caseId);
        CaseStage targetStageEntity = allStages.stream()
                .filter(s -> s.getStageName().equals(canonicalTarget))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("目标阶段不存在: " + canonicalTarget));

        if (!isValidForwardTransition(currentStage, targetStageEntity, allStages)) {
            throw new RuntimeException("不允许从当前阶段直接跳转到目标阶段，请使用回退功能或按顺序推进");
        }

        currentStage.setStatus("COMPLETED");
        currentStage.setEndDate(LocalDate.now());
        caseStageRepository.save(currentStage);

        targetStageEntity.setStatus("IN_PROGRESS");
        targetStageEntity.setStartDate(LocalDate.now());
        caseStageRepository.save(targetStageEntity);

        Case caseEntity = caseRepository.findById(caseId).orElseThrow();
        caseEntity.setCurrentStage(canonicalTarget);
        caseRepository.save(caseEntity);

        caseTimelineService.createSystemTimeline(
                caseId,
                "STAGE_CHANGED",
                String.format("案件阶段从「%s」变更为「%s」。原因：%s",
                        currentStage.getStageName(), canonicalTarget,
                        reason != null && !reason.isBlank() ? reason : "正常流转")
        );

        autoCreateTodosFromTemplate(caseId, canonicalTarget);
    }

    @Transactional
    public void autoCreateTodosFromTemplate(Long caseId, String stageName) {
        try {
            Case caseEntity = caseRepository.findById(caseId)
                    .orElseThrow(() -> new RuntimeException("案件不存在"));

            String canonicalStage = caseFlowDefinitionService
                    .resolveTemplateStageName(caseEntity.getCaseType(), stageName)
                    .orElse(stageName);

            List<CaseFlowTemplate> templates = flowTemplateRepository
                    .findByCaseTypeAndEnabledTrueAndDeletedFalseOrderBySortOrderAsc(caseEntity.getCaseType());
            if (templates.isEmpty()) {
                log.info("案件类型 {} 无流程模板，跳过阶段待办", caseEntity.getCaseType());
                return;
            }

            Long flowTemplateId = templates.get(0).getId();
            List<CaseStageTodoTemplate> templatesForStage = stageTodoTemplateRepository
                    .findByFlowTemplateIdAndStageNameAndDeletedFalseOrderBySortOrderAsc(
                            flowTemplateId, canonicalStage);

            if (templatesForStage.isEmpty()) {
                log.info("案件 {} 阶段 {} 无待办模板", caseId, canonicalStage);
                return;
            }

            int createdCount = 0;
            for (CaseStageTodoTemplate template : templatesForStage) {
                TodoDTO todoDTO = new TodoDTO();
                todoDTO.setTitle(template.getTodoTitle());
                todoDTO.setDescription(template.getTodoDescription());
                todoDTO.setPriority(template.getPriority() != null ? template.getPriority() : "MEDIUM");
                todoDTO.setCaseId(caseId);
                todoDTO.setStatus("PENDING");

                if (template.getDueDays() != null && template.getDueDays() > 0) {
                    todoDTO.setDueDate(LocalDateTime.now().plusDays(template.getDueDays()));
                } else {
                    todoDTO.setDueDate(LocalDateTime.now().plusDays(3));
                }

                Long assigneeId = resolveAssignee(caseId, template.getAssigneeType(), caseEntity.getOwnerId());
                if (assigneeId == null) {
                    continue;
                }
                todoDTO.setAssigneeId(assigneeId);
                todoService.createTodo(todoDTO, assigneeId);
                createdCount++;
            }

            log.info("案件 {} 阶段 {} 创建待办 {} 条", caseId, canonicalStage, createdCount);
            if (createdCount > 0) {
                caseTimelineService.createSystemTimeline(
                        caseId,
                        "AUTO_TODO",
                        "阶段「" + canonicalStage + "」自动创建 " + createdCount + " 项待办"
                );
            }
        } catch (Exception e) {
            log.error("自动创建待办失败: caseId={}, stage={}", caseId, stageName, e);
        }
    }

    /** @deprecated 使用 {@link #autoCreateTodosFromTemplate} */
    @Transactional
    public void autoCreateTodos(Long caseId, String stageName) {
        autoCreateTodosFromTemplate(caseId, stageName);
    }

    private Long resolveAssignee(Long caseId, String assigneeType, Long ownerId) {
        String type = assigneeType != null ? assigneeType : "OWNER";
        switch (type) {
            case "CO_OWNER":
                List<CaseMember> co = caseMemberRepository.findByCaseIdAndMemberTypeAndDeletedFalse(caseId, "CO_OWNER");
                if (!co.isEmpty()) {
                    return co.get(0).getUserId();
                }
                return ownerId;
            case "ASSISTANT":
                List<CaseMember> asst = caseMemberRepository.findByCaseIdAndMemberTypeAndDeletedFalse(caseId, "ASSISTANT");
                if (!asst.isEmpty()) {
                    return asst.get(0).getUserId();
                }
                return ownerId;
            case "OWNER":
            default:
                return ownerId;
        }
    }

    public List<StageHistoryVO> getStatusHistory(Long caseId) {
        List<CaseStage> stages = caseStageRepository.findByCaseIdAndDeletedFalseOrderByStageOrder(caseId);
        List<StageHistoryVO> history = new ArrayList<>();
        for (CaseStage stage : stages) {
            StageHistoryVO vo = new StageHistoryVO();
            vo.setStageName(stage.getStageName());
            vo.setStageOrder(stage.getStageOrder());
            vo.setStatus(stage.getStatus());
            vo.setStartDate(stage.getStartDate());
            vo.setEndDate(stage.getEndDate());
            history.add(vo);
        }
        return history;
    }

    @Transactional
    public void rollbackStatus(Long caseId, String targetStage, String reason, Long operatorId) {
        assertRollbackPermission(caseId, operatorId);
        if (reason == null || reason.isBlank()) {
            throw new RuntimeException("回退案件阶段必须填写原因");
        }
        String canonicalTarget = caseFlowDefinitionService.canonicalStageName(targetStage);

        Optional<CaseStage> currentStageOpt = caseStageRepository.findCurrentStage(caseId);
        if (currentStageOpt.isEmpty()) {
            throw new RuntimeException("未找到当前进行中的阶段");
        }

        CaseStage currentStage = currentStageOpt.get();
        List<CaseStage> allStages = caseStageRepository.findByCaseIdAndDeletedFalseOrderByStageOrder(caseId);
        CaseStage targetStageEntity = allStages.stream()
                .filter(s -> s.getStageName().equals(canonicalTarget))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("目标阶段不存在"));

        int currentIndex = allStages.indexOf(currentStage);
        int targetIndex = allStages.indexOf(targetStageEntity);
        if (targetIndex >= currentIndex) {
            throw new RuntimeException("只能回退到更早的阶段");
        }

        currentStage.setStatus("PENDING");
        currentStage.setStartDate(null);
        currentStage.setEndDate(null);
        caseStageRepository.save(currentStage);

        for (int i = targetIndex + 1; i < allStages.size(); i++) {
            CaseStage s = allStages.get(i);
            if (i > targetIndex) {
                s.setStatus("PENDING");
                s.setStartDate(null);
                s.setEndDate(null);
                caseStageRepository.save(s);
            }
        }

        targetStageEntity.setStatus("IN_PROGRESS");
        targetStageEntity.setEndDate(null);
        if (targetStageEntity.getStartDate() == null) {
            targetStageEntity.setStartDate(LocalDate.now());
        }
        caseStageRepository.save(targetStageEntity);

        Case caseEntity = caseRepository.findById(caseId).orElseThrow();
        caseEntity.setCurrentStage(canonicalTarget);
        caseRepository.save(caseEntity);

        caseTimelineService.createSystemTimeline(
                caseId,
                "STAGE_ROLLBACK",
                String.format("案件阶段从「%s」回退到「%s」。原因：%s",
                        currentStage.getStageName(), canonicalTarget, reason)
        );
    }

    private void assertRollbackPermission(Long caseId, Long operatorId) {
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("案件不存在"));
        if (operatorId != null && operatorId.equals(caseEntity.getOwnerId())) {
            return;
        }
        if (operatorId != null
                && !caseMemberRepository.findByCaseIdAndUserId(caseId, operatorId).isEmpty()) {
            return;
        }
        if (operatorId != null && hasManagementRole(operatorId)) {
            return;
        }
        throw new RuntimeException("无权限回退案件阶段：需案件负责人、团队成员或主任/管理员");
    }

    private boolean hasManagementRole(Long userId) {
        for (String code : List.of("DIRECTOR", "ADMIN")) {
            if (roleRepository.findByRoleCode(code)
                    .map(role -> userRoleRepository.findByRoleId(role.getId()).stream()
                            .anyMatch(ur -> userId.equals(ur.getUserId())))
                    .orElse(false)) {
                return true;
            }
        }
        return false;
    }

    public List<CaseDetailVO.StageProgressVO> getStageProgress(Long caseId) {
        List<CaseStage> stages = caseStageRepository.findByCaseIdAndDeletedFalseOrderByStageOrder(caseId);
        return stages.stream()
                .map(stage -> {
                    CaseDetailVO.StageProgressVO vo = new CaseDetailVO.StageProgressVO();
                    vo.setStageName(stage.getStageName());
                    vo.setStageOrder(stage.getStageOrder());
                    vo.setStatus(stage.getStatus());
                    vo.setStatusDesc(getStatusDesc(stage.getStatus()));
                    vo.setStartDate(stage.getStartDate());
                    vo.setEndDate(stage.getEndDate());
                    return vo;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    private boolean isValidForwardTransition(CaseStage current, CaseStage target, List<CaseStage> allStages) {
        int currentIndex = allStages.indexOf(current);
        int targetIndex = allStages.indexOf(target);
        return targetIndex > currentIndex;
    }

    private String getStatusDesc(String status) {
        if (status == null) {
            return null;
        }
        switch (status) {
            case "PENDING":
                return "待开始";
            case "IN_PROGRESS":
                return "进行中";
            case "COMPLETED":
                return "已完成";
            default:
                return status;
        }
    }

    @lombok.Data
    public static class StageHistoryVO {
        private String stageName;
        private Integer stageOrder;
        private String status;
        private String statusDesc;
        private LocalDate startDate;
        private LocalDate endDate;
    }
}
