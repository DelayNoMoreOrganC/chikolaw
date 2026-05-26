package com.lawfirm.service;

import com.lawfirm.dto.*;
import com.lawfirm.entity.*;
import com.lawfirm.enums.CaseStatus;
import com.lawfirm.exception.DuplicateResourceException;
import com.lawfirm.exception.InvalidParameterException;
import com.lawfirm.exception.ResourceNotFoundException;
import com.lawfirm.repository.*;
import com.lawfirm.util.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.lawfirm.vo.CaseDetailVO;
import com.lawfirm.vo.CaseListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 案件服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseService {

    private final CaseRepository caseRepository;
    private final PartyService partyService;
    private final CaseProcedureService caseProcedureService;
    private final CaseRecordService caseRecordService;
    private final CaseTimelineService caseTimelineService;
    private final CaseMemberService caseMemberService;
    private final CaseStageService caseStageService;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final FinanceRecordService financeRecordService;
    private final CaseFlowTemplateRepository caseFlowTemplateRepository;
    private final CaseFlowDefinitionService caseFlowDefinitionService;
    private final CaseStageTodoTemplateRepository caseStageTodoTemplateRepository;
    private final TodoService todoService;
    private final ClientService clientService;
    private final ClientRepository clientRepository;

    /**
     * 创建案件
     */
    @Transactional(rollbackFor = Exception.class)
    public CaseDetailVO createCase(CaseCreateRequest request, Long currentUserId) {
        // 参数校验
        if (request.getCaseType() == null) {
            throw new InvalidParameterException("caseType", "案件类型不能为空");
        }
        if (request.getProcedure() == null) {
            request.setProcedure("FIRST_INSTANCE");
        }
        if (request.getLevel() == null) {
            request.setLevel("GENERAL");
        }
        if (request.getOwnerId() == null) {
            request.setOwnerId(currentUserId);
        }

        // 查重检查
        if (request.getCaseNumber() != null && !request.getCaseNumber().trim().isEmpty()) {
            if (caseRepository.existsByCaseNumberAndDeletedFalse(request.getCaseNumber())) {
                throw new DuplicateResourceException("案件", "caseNumber", request.getCaseNumber());
            }
        }

        // 1. 创建案件基本信息
        Case caseEntity = new Case();

        // 自动生成案件名称
        if (request.getCaseName() == null || request.getCaseName().trim().isEmpty()) {
            caseEntity.setCaseName(autoGenerateName(request.getParties()));
        } else {
            caseEntity.setCaseName(request.getCaseName());
        }

        // 自动生成案件编号
        if (request.getCaseNumber() == null || request.getCaseNumber().trim().isEmpty()) {
            caseEntity.setCaseNumber(autoGenerateNumber(request.getCaseType()));
        } else {
            caseEntity.setCaseNumber(request.getCaseNumber());
        }

        caseEntity.setCaseType(request.getCaseType());
        caseEntity.setProcedure(request.getProcedure());
        caseEntity.setCaseReason(request.getCaseReason());
        caseEntity.setCourt(request.getCourt());
        caseEntity.setFilingDate(request.getFilingDate());
        caseEntity.setDeadlineDate(request.getDeadlineDate());
        caseEntity.setCommissionDate(request.getCommissionDate());
        caseEntity.setTags(request.getTags());
        caseEntity.setSummary(request.getSummary());
        caseEntity.setStrategies(request.getStrategies());
        caseEntity.setLevel(request.getLevel());
        caseEntity.setAmount(request.getAmount());
        caseEntity.setAttorneyFee(request.getAttorneyFee());
        caseEntity.setFeeMethod(request.getFeeMethod());
        applyNpaFields(caseEntity, request);

        // ==================== 新增字段映射（对标行政管理要求）====================
        caseEntity.setAcceptanceDate(request.getAcceptanceDate());
        caseEntity.setCourtCaseNumber(request.getCourtCaseNumber());
        caseEntity.setHearingDate(request.getHearingDate());
        caseEntity.setContractStartDate(request.getContractStartDate());
        caseEntity.setContractEndDate(request.getContractEndDate());
        caseEntity.setRepresentationType(request.getRepresentationType());
        caseEntity.setSourcePerson(request.getSourcePerson());
        caseEntity.setSourcePersonPercentage(request.getSourcePersonPercentage());
        caseEntity.setDepartmentPercentage(request.getDepartmentPercentage());
        caseEntity.setFirmPercentage(request.getFirmPercentage());
        caseEntity.setConflictCheckStatus(request.getConflictCheckStatus());
        caseEntity.setConflictWaiverApprovalId(request.getConflictWaiverApprovalId());

        // 新增字段映射（对标系统问题.xlsx）
        caseEntity.setBusinessType(request.getBusinessType());
        caseEntity.setCriminalSuspect(request.getCriminalSuspect());
        caseEntity.setDisputedAmount(request.getDisputedAmount());
        caseEntity.setHostDepartment(request.getHostDepartment());
        caseEntity.setCoDepartments(request.getCoDepartments());
        caseEntity.setRemark(request.getRemark());

        // 新增字段映射（对标案件登记及系统立结案流程）
        caseEntity.setOtherClients(request.getOtherClients());
        caseEntity.setProcedureLevels(request.getProcedureLevels());
        caseEntity.setIsLegalAid(request.getIsLegalAid() != null ? request.getIsLegalAid() : false);
        caseEntity.setFixedFee(request.getFixedFee());
        caseEntity.setRiskRatio(request.getRiskRatio());
        caseEntity.setRiskFee(request.getRiskFee());
        caseEntity.setFeeRemark(request.getFeeRemark());

        // 设置初始状态（卷宗立案草稿使用 PENDING_FILING）
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            caseEntity.setStatus(request.getStatus().trim());
        } else {
            caseEntity.setStatus(CaseStatus.CONSULTATION.getCode());
        }
        caseEntity.setOwnerId(request.getOwnerId());

        // 保存案件
        caseEntity = caseRepository.save(caseEntity);
        log.info("[DEBUG] Case saved: id={}, deadlineDate={}", caseEntity.getId(), caseEntity.getDeadlineDate());

        // 2. 创建当事人
        List<Party> parties = partyService.batchCreate(request.getParties(), caseEntity.getId());

        // 2.1 同步创建客户（如果勾选了"同步创建客户"）
        syncPartiesToClients(request.getParties(), caseEntity.getId(), currentUserId);

        // 3. 添加团队成员
        caseMemberService.addMember(caseEntity.getId(), request.getOwnerId(), "OWNER");
        if (request.getCoOwnerIds() != null && !request.getCoOwnerIds().isEmpty()) {
            caseMemberService.addMembers(caseEntity.getId(), request.getCoOwnerIds(), "CO_OWNER");
        }
        if (request.getAssistantIds() != null && !request.getAssistantIds().isEmpty()) {
            caseMemberService.addMembers(caseEntity.getId(), request.getAssistantIds(), "ASSISTANT");
        }

        // 4. 创建案件阶段流程
        caseStageService.initializeStages(caseEntity.getId(), request.getCaseType());

        // 5. 自动生成待办事项（根据流程模板）
        generateTodosFromTemplate(caseEntity, request);

        // 5.1 如果有审限时间，自动生成审限届满待办
        log.info("[DEBUG] Checking deadline: caseId={}, deadlineDate={}, isNull={}",
                caseEntity.getId(), caseEntity.getDeadlineDate(), caseEntity.getDeadlineDate() == null);
        if (caseEntity.getDeadlineDate() != null) {
            createDeadlineTodo(caseEntity);
        } else {
            log.info("[DEBUG] deadlineDate is null, skipping deadline todo creation");
        }

        // 6. 记录动态
        caseTimelineService.createSystemTimeline(
                caseEntity.getId(),
                "CASE_CREATED",
                "案件已创建，案号：" + caseEntity.getCaseNumber()
        );

        // 7. 创建应收款记录（如果有）
        if (request.getReceivables() != null && !request.getReceivables().isEmpty()) {
            final Long caseId = caseEntity.getId();
            request.getReceivables().forEach(receivable -> {
                FinanceRecordDTO financeRecordDTO = new FinanceRecordDTO();
                financeRecordDTO.setCaseId(caseId);
                financeRecordDTO.setFinanceType("RECEIVABLE");
                financeRecordDTO.setAmount(receivable.getAmount());
                financeRecordDTO.setTransactionDate(LocalDate.parse(receivable.getDueDate()));
                financeRecordDTO.setDescription(receivable.getName() + (receivable.getNotes() != null ? " - " + receivable.getNotes() : ""));
                financeRecordService.createFinanceRecord(financeRecordDTO, currentUserId);
            });
            log.info("创建应收款记录：{}条", request.getReceivables().size());
        }

        return getCaseDetail(caseEntity.getId());
    }

    /**
     * 更新案件
     */
    @Transactional(rollbackFor = Exception.class)
    public CaseDetailVO updateCase(Long caseId, CaseUpdateRequest request) {
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("案件", caseId));

        // 保存原始状态，用于检测变更
        String originalStatus = caseEntity.getStatus();
        String originalStage = caseEntity.getCurrentStage();

        // 更新基本信息
        if (request.getCaseName() != null) {
            caseEntity.setCaseName(request.getCaseName());
        }
        if (request.getCaseType() != null) {
            caseEntity.setCaseType(request.getCaseType());
        }
        if (request.getProcedure() != null) {
            caseEntity.setProcedure(request.getProcedure());
        }
        if (request.getCaseReason() != null) {
            caseEntity.setCaseReason(request.getCaseReason());
        }
        if (request.getCourt() != null) {
            caseEntity.setCourt(request.getCourt());
        }
        if (request.getFilingDate() != null) {
            caseEntity.setFilingDate(request.getFilingDate());
        }
        if (request.getDeadlineDate() != null) {
            caseEntity.setDeadlineDate(request.getDeadlineDate());
        }
        if (request.getCommissionDate() != null) {
            caseEntity.setCommissionDate(request.getCommissionDate());
        }
        if (request.getTags() != null) {
            caseEntity.setTags(request.getTags());
        }
        if (request.getSummary() != null) {
            caseEntity.setSummary(request.getSummary());
        }
        if (request.getStrategies() != null) {
            caseEntity.setStrategies(request.getStrategies());
        }
        if (request.getLevel() != null) {
            caseEntity.setLevel(request.getLevel());
        }
        if (request.getAmount() != null) {
            caseEntity.setAmount(request.getAmount());
        }
        if (request.getAttorneyFee() != null) {
            caseEntity.setAttorneyFee(request.getAttorneyFee());
        }
        if (request.getFeeMethod() != null) {
            caseEntity.setFeeMethod(request.getFeeMethod());
        }
        applyNpaFields(caseEntity, request);
        if (request.getWonAmount() != null) {
            caseEntity.setWonAmount(request.getWonAmount());
        }
        if (request.getActualReceived() != null) {
            caseEntity.setActualReceived(request.getActualReceived());
        }
        if (request.getCloseStatus() != null) {
            caseEntity.setCloseStatus(request.getCloseStatus());
        }
        if (request.getCloseDate() != null) {
            caseEntity.setCloseDate(request.getCloseDate());
            caseEntity.setStatus(CaseStatus.CLOSED.getCode());
        }
        if (request.getArchiveDate() != null) {
            caseEntity.setArchiveDate(request.getArchiveDate());
            caseEntity.setStatus(CaseStatus.ARCHIVED.getCode());
        }
        if (request.getArchiveLocation() != null) {
            caseEntity.setArchiveLocation(request.getArchiveLocation());
        }

        // 更新主办律师
        if (request.getOwnerId() != null && !request.getOwnerId().equals(caseEntity.getOwnerId())) {
            caseMemberService.updateMemberType(caseId, caseEntity.getOwnerId(), "CO_OWNER");
            caseMemberService.updateMemberType(caseId, request.getOwnerId(), "OWNER");
            caseEntity.setOwnerId(request.getOwnerId());
        }

        // 更新协办律师和助理
        if (request.getCoOwnerIds() != null && !request.getCoOwnerIds().isEmpty()) {
            // 先移除所有协办律师
            List<CaseMember> existingCoOwners = caseMemberService.getByCaseIdAndType(caseEntity.getId(), "CO_OWNER");
            existingCoOwners.forEach(member -> caseMemberService.removeMember(caseEntity.getId(), member.getUserId()));
            // 添加新协办律师
            caseMemberService.addMembers(caseEntity.getId(), request.getCoOwnerIds(), "CO_OWNER");
            log.info("更新协办律师：{}人", request.getCoOwnerIds().size());
        }
        if (request.getAssistantIds() != null && !request.getAssistantIds().isEmpty()) {
            // 先移除所有助理
            List<CaseMember> existingAssistants = caseMemberService.getByCaseIdAndType(caseEntity.getId(), "ASSISTANT");
            existingAssistants.forEach(member -> caseMemberService.removeMember(caseEntity.getId(), member.getUserId()));
            // 添加新助理
            caseMemberService.addMembers(caseEntity.getId(), request.getAssistantIds(), "ASSISTANT");
            log.info("更新助理：{}人", request.getAssistantIds().size());
        }

        caseRepository.save(caseEntity);

        // 检测状态变更，自动生成待办事项
        if (request.getStatus() != null && !request.getStatus().equals(originalStatus)) {
            generateTodosForStatusChange(caseEntity, request.getStatus(), originalStatus);
        }
        if (request.getCurrentStage() != null && !request.getCurrentStage().equals(originalStage)) {
            generateTodosForStageChange(caseEntity, request.getCurrentStage());
        }

        // 更新当事人（全量更新：删除旧的，添加新的）
        if (request.getParties() != null && !request.getParties().isEmpty()) {
            // 获取现有当事人并逻辑删除
            List<com.lawfirm.vo.PartyVO> existingParties = partyService.getByCaseId(caseEntity.getId());
            existingParties.forEach(partyVO -> {
                if (partyVO.getId() != null) {
                    partyService.delete(partyVO.getId());
                }
            });
            // 批量创建新当事人
            partyService.batchCreate(request.getParties(), caseEntity.getId());
            log.info("更新当事人：{}人", request.getParties().size());
        }

        // 记录动态
        caseTimelineService.createSystemTimeline(
                caseId,
                "CASE_UPDATED",
                "案件信息已更新"
        );

        return getCaseDetail(caseId);
    }

    /**
     * 获取案件列表
     */
    @Transactional(readOnly = true)
    public PageResult<CaseListVO> getCaseList(CaseQueryRequest request, Long currentUserId) {
        // 构建查询条件（前端页码从1开始，Spring Data JPA从0开始，需要转换）
        Pageable pageable = PageRequest.of(
                Math.max(0, request.getPage() - 1),  // 修复分页bug：前端从1开始，JPA从0开始
                request.getSize(),
                Sort.by(Sort.Direction.fromString(request.getSortDirection()), request.getSortField())
        );

        // 构建动态查询
        Specification<Case> spec = (root, query, cb) -> {
            List<javax.persistence.criteria.Predicate> predicates = new ArrayList<>();

            // 数据权限隔离：律师看自己，主任看全部
            User currentUser = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("用户", currentUserId));

            boolean isAdmin = hasAdminRole(currentUser);

            if (!isAdmin) {
                // 非管理员只能看到自己作为主办律师的案件
                predicates.add(cb.equal(root.get("ownerId"), currentUserId));
            }

            // 基础条件
            if (request.getCaseType() != null) {
                predicates.add(cb.equal(root.get("caseType"), request.getCaseType()));
            }
            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            }
            if (request.getLevel() != null) {
                predicates.add(cb.equal(root.get("level"), request.getLevel()));
            }
            if (request.getOwnerId() != null) {
                // 管理员可以按ownerId筛选，非管理员已经在上面过滤了
                if (isAdmin) {
                    predicates.add(cb.equal(root.get("ownerId"), request.getOwnerId()));
                }
            }
            if (request.getCourt() != null) {
                predicates.add(cb.like(root.get("court"), "%" + request.getCourt() + "%"));
            }
            if (request.getKeyword() != null) {
                String keyword = "%" + request.getKeyword() + "%";
                javax.persistence.criteria.Predicate nameCondition = cb.like(root.get("caseName"), keyword);
                javax.persistence.criteria.Predicate numberCondition = cb.like(root.get("caseNumber"), keyword);
                predicates.add(cb.or(nameCondition, numberCondition));
            }
            if (request.getTag() != null) {
                predicates.add(cb.like(root.get("tags"), "%" + request.getTag() + "%"));
            }
            if (request.getArchived() != null) {
                if (request.getArchived()) {
                    predicates.add(cb.equal(root.get("status"), CaseStatus.ARCHIVED.getCode()));
                } else {
                    predicates.add(cb.notEqual(root.get("status"), CaseStatus.ARCHIVED.getCode()));
                }
            }
            if (request.getDeleted() != null) {
                predicates.add(cb.equal(root.get("deleted"), request.getDeleted()));
            } else {
                predicates.add(cb.equal(root.get("deleted"), false));
            }

            // 客户筛选：查询该客户关联的案件
            if (request.getClientId() != null) {
                predicates.add(cb.equal(root.get("clientId"), request.getClientId()));
            }

            return cb.and(predicates.toArray(new javax.persistence.criteria.Predicate[0]));
        };

        Page<Case> page = caseRepository.findAll(spec, pageable);

        // 转换为VO
        List<CaseListVO> records = page.getContent().stream()
                .map(this::toListVO)
                .collect(Collectors.toList());

        return PageResult.of(
                (long) request.getPage(),
                (long) request.getSize(),
                page.getTotalElements(),
                records
        );
    }

    /**
     * 判断用户是否有管理员角色
     */
    private boolean hasAdminRole(User user) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(user.getId());
        return userRoles.stream()
                .anyMatch(ur -> {
                    Role role = roleRepository.findById(ur.getRoleId()).orElse(null);
                    if (role == null) return false;
                    // 检查 roleCode 和 roleName，兼容数据库中的不同格式
                    return "ADMIN".equals(role.getRoleCode())
                            || "MANAGER".equals(role.getRoleCode())
                            || "管理员".equals(role.getRoleName())
                            || "主任".equals(role.getRoleName());
                });
    }

    /**
     * 获取案件详情
     */
    @Transactional(readOnly = true)
    public CaseDetailVO getCaseDetail(Long caseId) {
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("案件", caseId));

        CaseDetailVO vo = new CaseDetailVO();
        BeanUtils.copyProperties(caseEntity, vo);

        // 设置描述字段
        vo.setCaseTypeDesc(getCaseTypeDesc(caseEntity.getCaseType()));
        vo.setLevelDesc(getLevelDesc(caseEntity.getLevel()));
        vo.setStatusDesc(getStatusDesc(caseEntity.getStatus()));
        vo.setCloseStatusDesc(getCloseStatusDesc(caseEntity.getCloseStatus()));

        // 设置团队信息
        vo.setOwnerName(getUserName(caseEntity.getOwnerId()));
        vo.setCoOwners(getMembers(caseId, "CO_OWNER"));
        vo.setAssistants(getMembers(caseId, "ASSISTANT"));

        // 设置当事人列表
        vo.setParties(partyService.getByCaseId(caseId));

        // 设置案件程序列表
        vo.setProcedures(caseProcedureService.getByCaseId(caseId));

        // 设置阶段进度
        vo.setStageProgress(caseStageService.getStageProgress(caseId));

        return vo;
    }

    /**
     * 删除案件（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCase(Long caseId) {
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("案件", caseId));

        caseEntity.setDeleted(true);
        caseRepository.save(caseEntity);

        // 记录动态
        caseTimelineService.createSystemTimeline(
                caseId,
                "CASE_DELETED",
                "案件已删除"
        );
    }

    /**
     * 恢复已删除的案件
     */
    @Transactional(rollbackFor = Exception.class)
    public void restoreCase(Long caseId) {
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("案件", caseId));

        if (!caseEntity.getDeleted()) {
            throw new InvalidParameterException("caseId", "该案件未被删除，无需恢复");
        }

        caseEntity.setDeleted(false);
        caseRepository.save(caseEntity);

        // 记录动态
        caseTimelineService.createSystemTimeline(
                caseId,
                "CASE_RESTORED",
                "案件已从回收站恢复"
        );
    }

    /**
     * 归档案件
     */
    @Transactional(rollbackFor = Exception.class)
    public void archiveCase(Long caseId, String archiveLocation) {
        if (archiveLocation == null || archiveLocation.trim().isEmpty()) {
            throw new InvalidParameterException("archiveLocation", "档案保管地不能为空");
        }

        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("案件", caseId));

        caseEntity.setStatus(CaseStatus.ARCHIVED.getCode());
        caseEntity.setArchiveDate(java.time.LocalDate.now());
        caseEntity.setArchiveLocation(archiveLocation);
        caseRepository.save(caseEntity);

        // 记录动态
        caseTimelineService.createSystemTimeline(
                caseId,
                "CASE_ARCHIVED",
                "案件已归档，保管地：" + archiveLocation
        );
    }

    /**
     * 查重检查
     * PRD要求（228行）：按案件名称和案号查重
     */
    public List<CaseListVO> checkDuplicate(String name, String caseNumber) {
        List<Case> duplicates = new ArrayList<>();

        if (name != null && !name.trim().isEmpty()) {
            duplicates.addAll(caseRepository.findByCaseNameContainingAndDeletedFalse(name));
        }

        if (caseNumber != null && !caseNumber.trim().isEmpty()) {
            List<Case> byNumber = caseRepository.findAllByCaseNumberAndDeletedFalse(caseNumber);
            duplicates.addAll(byNumber);
        }

        // 去重
        duplicates = duplicates.stream()
                .distinct()
                .collect(Collectors.toList());

        return duplicates.stream()
                .map(this::toListVO)
                .collect(Collectors.toList());
    }

    /**
     * 搜索法院（用于案件筛选的法院选择器）
     */
    public List<String> searchCourts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            // 如果没有关键词，返回所有不重复的法院
            return caseRepository.findDistinctCourts();
        } else {
            // 如果有关键词，进行模糊搜索
            return caseRepository.findCourtsByKeyword(keyword);
        }
    }

    /**
     * 自动生成案件名称
     * 规则：当事人与对方当事人+案由
     * 支持多个当事人（用"、"连接）
     * 示例：张三、李四 Vs 王五 买卖合同纠纷
     */
    public String autoGenerateName(List<PartyDTO> parties) {
        if (parties == null || parties.isEmpty()) {
            return "未命名案件";
        }

        // 获取所有原告/申请人
        List<String> plaintiffs = parties.stream()
                .filter(p -> "PLAINTIFF".equals(p.getPartyRole()) || "APPLICANT".equals(p.getPartyRole()))
                .map(PartyDTO::getName)
                .filter(name -> name != null && !name.trim().isEmpty())
                .collect(java.util.stream.Collectors.toList());

        // 获取所有被告/被申请人
        List<String> defendants = parties.stream()
                .filter(p -> "DEFENDANT".equals(p.getPartyRole()) || "RESPONDENT".equals(p.getPartyRole()))
                .map(PartyDTO::getName)
                .filter(name -> name != null && !name.trim().isEmpty())
                .collect(java.util.stream.Collectors.toList());

        String plaintiffStr = plaintiffs.isEmpty() ? "原告" : String.join("、", plaintiffs);
        String defendantStr = defendants.isEmpty() ? "被告" : String.join("、", defendants);

        return plaintiffStr + " Vs " + defendantStr;
    }

    /**
     * 自动生成案件名称（带案由）
     * 规则：当事人与对方当事人+案由
     * @param parties 当事人列表
     * @param caseReason 案由
     * @return 案件名称
     */
    public String autoGenerateNameWithReason(List<PartyDTO> parties, String caseReason) {
        String baseName = autoGenerateName(parties);
        if (caseReason != null && !caseReason.trim().isEmpty()) {
            return baseName + " " + caseReason;
        }
        return baseName;
    }

    /**
     * 自动生成案件编号
     * 格式：[年]粤至高[民/刑/行/非/顾]字第XXX号
     * 示例：2025粤至高民字第0001号
     */
    public String autoGenerateNumber(String caseType) {
        String year = String.valueOf(java.time.Year.now().getValue());
        String typeCode = getCaseTypeCodeChinese(caseType);

        // 查询当年该类型的案件数量
        long count = caseRepository.countByCaseTypeAndDeletedFalse(caseType);

        // 生成格式：[年]粤至高[类型]字第XXX号
        return String.format("%s粤至高%s字第%04d号", year, typeCode, count + 1);
    }

    // 辅助方法
    private CaseListVO toListVO(Case caseEntity) {
        CaseListVO vo = new CaseListVO();
        BeanUtils.copyProperties(caseEntity, vo);
        vo.setCaseTypeDesc(getCaseTypeDesc(caseEntity.getCaseType()));
        vo.setStatusDesc(getStatusDesc(caseEntity.getStatus()));
        vo.setLevelDesc(getLevelDesc(caseEntity.getLevel()));
        vo.setOwnerName(getUserName(caseEntity.getOwnerId()));

        // 获取当事人信息
        List<com.lawfirm.vo.PartyVO> parties = partyService.getByCaseId(caseEntity.getId());
        String plaintiff = parties.stream()
                .filter(p -> "PLAINTIFF".equals(p.getPartyRole()))
                .map(com.lawfirm.vo.PartyVO::getName)
                .findFirst()
                .orElse("");
        String defendant = parties.stream()
                .filter(p -> "DEFENDANT".equals(p.getPartyRole()))
                .map(com.lawfirm.vo.PartyVO::getName)
                .findFirst()
                .orElse("");
        vo.setParties(plaintiff + " vs " + defendant);

        return vo;
    }

    private String getUserName(Long userId) {
        return userRepository.findById(userId)
                .map(User::getRealName)
                .orElse("未知");
    }

    private List<CaseDetailVO.MemberVO> getMembers(Long caseId, String memberType) {
        List<CaseMember> members = caseMemberService.getByCaseIdAndType(caseId, memberType);
        return members.stream()
                .map(m -> {
                    CaseDetailVO.MemberVO vo = new CaseDetailVO.MemberVO();
                    vo.setId(m.getUserId());
                    vo.setName(getUserName(m.getUserId()));
                    vo.setRole(memberType);
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private String getCaseTypeDesc(String type) {
        if (type == null) return null;
        switch (type) {
            case "CIVIL": return "民事";
            case "COMMERCIAL": return "商事";
            case "ARBITRATION": return "仲裁";
            case "CRIMINAL": return "刑事";
            case "ADMINISTRATIVE": return "行政";
            case "NON_LITIGATION": return "非诉";
            case "ADVISORY": return "顾问";
            case "FINANCIAL_NPA": return "金融不良资产";
            default: return type;
        }
    }

    private String getLevelDesc(String level) {
        if (level == null) return null;
        switch (level) {
            case "IMPORTANT": return "重要";
            case "GENERAL": return "一般";
            case "MINOR": return "次要";
            default: return level;
        }
    }

    private String getStatusDesc(String status) {
        if (status == null) return null;
        switch (status) {
            case "CONSULTATION": return "咨询";
            case "SIGNED": return "签约";
            case "PENDING_FILING": return "待立案";
            case "ACTIVE": return "审理中";
            case "CLOSED": return "结案";
            case "ARCHIVED": return "归档";
            default: return status;
        }
    }

    private String getCloseStatusDesc(String status) {
        if (status == null) return null;
        switch (status) {
            case "ACHIEVED": return "达成诉求";
            case "PARTIALLY_ACHIEVED": return "部分达成";
            case "NOT_ACHIEVED": return "未达成";
            case "NOT_COMMISSIONED": return "未委托";
            case "TERMINATED": return "终止";
            default: return status;
        }
    }

    private String getCaseTypeCode(String caseType) {
        if (caseType == null) return "X";
        switch (caseType) {
            case "CIVIL": return "M";
            case "COMMERCIAL": return "S";
            case "ARBITRATION": return "A";
            case "CRIMINAL": return "C";
            case "ADMINISTRATIVE": return "A";
            case "NON_LITIGATION": return "N";
            case "ADVISORY": return "G";
            case "FINANCIAL_NPA": return "FNA";
            default: return "X";
        }
    }

    /**
     * 获取案件类型中文字符（用于案号生成）
     * 映射：CIVIL->民, CRIMINAL->刑, ADMINISTRATIVE->行, NON_LITIGATION->非, ADVISORY->顾
     */
    private String getCaseTypeCodeChinese(String caseType) {
        if (caseType == null) return "其";
        switch (caseType) {
            case "CIVIL": return "民";
            case "CRIMINAL": return "刑";
            case "ADMINISTRATIVE": return "行";
            case "NON_LITIGATION": return "非";
            case "ADVISORY": return "顾";
            case "COMMERCIAL": return "商";
            case "ARBITRATION": return "仲";
            case "FINANCIAL_NPA": return "金";
            default: return "其";
        }
    }

    private void applyNpaFields(Case caseEntity, CaseCreateRequest request) {
        caseEntity.setNpaSubtype(request.getNpaSubtype());
        caseEntity.setEntrustingBankName(request.getEntrustingBankName());
        caseEntity.setAssetBatchNo(request.getAssetBatchNo());
        caseEntity.setTransferAgreementNo(request.getTransferAgreementNo());
        caseEntity.setLoanContractNo(request.getLoanContractNo());
        caseEntity.setPrincipalBalance(request.getPrincipalBalance());
        caseEntity.setInterestBalance(request.getInterestBalance());
        caseEntity.setGuaranteeType(request.getGuaranteeType());
        caseEntity.setCollateralStatus(request.getCollateralStatus());
        caseEntity.setPreservationStatus(request.getPreservationStatus());
        caseEntity.setExecutionRecoveryAmount(request.getExecutionRecoveryAmount());
        caseEntity.setTerminationStatus(request.getTerminationStatus());
    }

    private void applyNpaFields(Case caseEntity, CaseUpdateRequest request) {
        if (request.getNpaSubtype() != null) caseEntity.setNpaSubtype(request.getNpaSubtype());
        if (request.getEntrustingBankName() != null) caseEntity.setEntrustingBankName(request.getEntrustingBankName());
        if (request.getAssetBatchNo() != null) caseEntity.setAssetBatchNo(request.getAssetBatchNo());
        if (request.getTransferAgreementNo() != null) caseEntity.setTransferAgreementNo(request.getTransferAgreementNo());
        if (request.getLoanContractNo() != null) caseEntity.setLoanContractNo(request.getLoanContractNo());
        if (request.getPrincipalBalance() != null) caseEntity.setPrincipalBalance(request.getPrincipalBalance());
        if (request.getInterestBalance() != null) caseEntity.setInterestBalance(request.getInterestBalance());
        if (request.getGuaranteeType() != null) caseEntity.setGuaranteeType(request.getGuaranteeType());
        if (request.getCollateralStatus() != null) caseEntity.setCollateralStatus(request.getCollateralStatus());
        if (request.getPreservationStatus() != null) caseEntity.setPreservationStatus(request.getPreservationStatus());
        if (request.getExecutionRecoveryAmount() != null) caseEntity.setExecutionRecoveryAmount(request.getExecutionRecoveryAmount());
        if (request.getTerminationStatus() != null) caseEntity.setTerminationStatus(request.getTerminationStatus());
    }

    /**
     * 根据流程模板自动生成待办事项
     */
    private void generateTodosFromTemplate(Case caseEntity, CaseCreateRequest request) {
        try {
            // 1. 查找对应案件类型的流程模板
            List<CaseFlowTemplate> templates = caseFlowTemplateRepository
                    .findByCaseTypeAndEnabledTrueAndDeletedFalseOrderBySortOrderAsc(
                            caseEntity.getCaseType()
                    );

            if (templates.isEmpty()) {
                log.info("案件类型 {} 没有配置流程模板，跳过自动生成待办", caseEntity.getCaseType());
                return;
            }

            // 2. 使用第一个匹配的模板（如果有多个模板，后续可以增加选择逻辑）
            CaseFlowTemplate template = templates.get(0);

            String firstStage = caseFlowDefinitionService.getFirstStageName(caseEntity.getCaseType());
            List<CaseStageTodoTemplate> todoTemplates = caseStageTodoTemplateRepository
                    .findByFlowTemplateIdAndStageNameAndDeletedFalseOrderBySortOrderAsc(
                            template.getId(), firstStage);

            if (todoTemplates.isEmpty()) {
                log.info("流程模板 {} 首阶段 {} 无待办模板，跳过建案待办", template.getTemplateName(), firstStage);
                return;
            }

            // 仅为首阶段（当前阶段）创建待办，后续阶段在阶段变更时生成
            int createdCount = 0;
            for (CaseStageTodoTemplate todoTemplate : todoTemplates) {
                TodoDTO todoDTO = new TodoDTO();
                todoDTO.setTitle(todoTemplate.getTodoTitle());
                todoDTO.setDescription(todoTemplate.getTodoDescription());
                todoDTO.setPriority(todoTemplate.getPriority());
                todoDTO.setCaseId(caseEntity.getId());
                todoDTO.setStatus("PENDING");

                // 计算截止日期（案件创建时间 + dueDays天）
                if (todoTemplate.getDueDays() != null && todoTemplate.getDueDays() > 0) {
                    LocalDateTime dueDate = caseEntity.getCreatedAt()
                            .plusDays(todoTemplate.getDueDays());
                    todoDTO.setDueDate(dueDate);
                }

                // 根据负责人类型分配待办
                Long assigneeId = determineAssignee(todoTemplate.getAssigneeType(),
                        caseEntity, request);
                if (assigneeId != null) {
                    todoDTO.setAssigneeId(assigneeId);

                    // 创建待办
                    try {
                        todoService.createTodo(todoDTO, assigneeId);
                        createdCount++;
                    } catch (Exception e) {
                        log.error("创建待办失败: {}", e.getMessage());
                    }
                }
            }

            log.info("建案首阶段「{}」自动生成 {} 个待办", firstStage, createdCount);

            if (createdCount > 0) {
                caseTimelineService.createSystemTimeline(
                        caseEntity.getId(),
                        "AUTO_TODO",
                        "建案首阶段「" + firstStage + "」自动生成 " + createdCount + " 个待办"
                );
            }

        } catch (Exception e) {
            log.error("自动生成待办事项失败: {}", e.getMessage(), e);
            // 不抛出异常，避免影响案件创建流程
        }
    }

    /**
     * 状态变更时自动生成待办事项
     */
    private void generateTodosForStatusChange(Case caseEntity, String newStatus, String oldStatus) {
        try {
            log.info("案件状态变更: {} → {}, 案件ID={}", oldStatus, newStatus, caseEntity.getId());

            // 结案状态：生成归档提醒待办
            if ("CLOSED".equals(newStatus)) {
                TodoDTO archiveTodo = new TodoDTO();
                archiveTodo.setTitle(caseEntity.getCaseNumber() + " - 归档提醒");
                archiveTodo.setDescription("案件已结案，请在30日内完成归档工作");
                archiveTodo.setPriority("IMPORTANT");
                archiveTodo.setCaseId(caseEntity.getId());
                archiveTodo.setStatus("PENDING");
                archiveTodo.setAssigneeId(caseEntity.getOwnerId());
                // 结案后30天需要归档
                archiveTodo.setDueDate(LocalDateTime.now().plusDays(30));
                archiveTodo.setReminder(true);

                try {
                    todoService.createTodo(archiveTodo, caseEntity.getOwnerId());
                    log.info("已创建归档提醒待办: {}", archiveTodo.getTitle());

                    // 记录动态
                    caseTimelineService.createSystemTimeline(
                        caseEntity.getId(),
                        "STATUS_CHANGE",
                        "案件状态变更为结案，已自动创建归档提醒待办"
                    );
                } catch (Exception e) {
                    log.error("创建归档待办失败: {}", e.getMessage());
                }
            }

            // 归档状态：生成案件卷宗整理待办
            if ("ARCHIVED".equals(newStatus)) {
                TodoDTO organizeTodo = new TodoDTO();
                organizeTodo.setTitle(caseEntity.getCaseNumber() + " - 卷宗整理");
                organizeTodo.setDescription("案件已归档，请完成卷宗材料的整理和扫描工作");
                organizeTodo.setPriority("NORMAL");
                organizeTodo.setCaseId(caseEntity.getId());
                organizeTodo.setStatus("PENDING");
                organizeTodo.setAssigneeId(caseEntity.getOwnerId());
                // 归档后7天内完成整理
                organizeTodo.setDueDate(LocalDateTime.now().plusDays(7));
                organizeTodo.setReminder(false);

                try {
                    todoService.createTodo(organizeTodo, caseEntity.getOwnerId());
                    log.info("已创建卷宗整理待办: {}", organizeTodo.getTitle());

                    // 记录动态
                    caseTimelineService.createSystemTimeline(
                        caseEntity.getId(),
                        "STATUS_CHANGE",
                        "案件状态变更为归档，已自动创建卷宗整理待办"
                    );
                } catch (Exception e) {
                    log.error("创建卷宗整理待办失败: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("状态变更自动生成待办失败: {}", e.getMessage(), e);
            // 不抛出异常，避免影响案件更新流程
        }
    }

    /**
     * 阶段变更时自动生成待办事项
     */
    private void generateTodosForStageChange(Case caseEntity, String newStage) {
        try {
            log.info("案件阶段变更: 新阶段={}, 案件ID={}", newStage, caseEntity.getId());

            // 根据不同阶段生成对应的待办
            String stageTodoTitle = null;
            String stageTodoDesc = null;
            int dueDays = 3; // 默认3天内完成

            switch (newStage) {
                case "待立案":
                    stageTodoTitle = caseEntity.getCaseNumber() + " - 立案准备";
                    stageTodoDesc = "准备立案材料，包括起诉状、证据清单、授权委托书等";
                    dueDays = 7;
                    break;
                case "已立案":
                    stageTodoTitle = caseEntity.getCaseNumber() + " - 庭前准备";
                    stageTodoDesc = "准备开庭材料，梳理案件争议焦点，准备代理词";
                    dueDays = 5;
                    break;
                case "一审审理中":
                    stageTodoTitle = caseEntity.getCaseNumber() + " - 开庭跟进";
                    stageTodoDesc = "跟进庭审进度，根据庭审情况调整代理策略";
                    dueDays = 3;
                    break;
                case "一审结案":
                    stageTodoTitle = caseEntity.getCaseNumber() + " - 判决书领取";
                    stageTodoDesc = "关注法院判决书送达情况，及时领取并告知当事人";
                    dueDays = 2;
                    break;
                default:
                    // 其他阶段不自动生成待办
                    return;
            }

            if (stageTodoTitle != null) {
                TodoDTO stageTodo = new TodoDTO();
                stageTodo.setTitle(stageTodoTitle);
                stageTodo.setDescription(stageTodoDesc);
                stageTodo.setPriority("NORMAL");
                stageTodo.setCaseId(caseEntity.getId());
                stageTodo.setStatus("PENDING");
                stageTodo.setAssigneeId(caseEntity.getOwnerId());
                stageTodo.setDueDate(LocalDateTime.now().plusDays(dueDays));
                stageTodo.setReminder(false);

                try {
                    todoService.createTodo(stageTodo, caseEntity.getOwnerId());
                    log.info("已创建阶段变更待办: {} (截止: {} 天后)", stageTodoTitle, dueDays);

                    // 记录动态
                    caseTimelineService.createSystemTimeline(
                        caseEntity.getId(),
                        "STAGE_CHANGE",
                        "案件阶段变更为【" + newStage + "】，已自动创建待办：" + stageTodoTitle
                    );
                } catch (Exception e) {
                    log.error("创建阶段待办失败: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("阶段变更自动生成待办失败: {}", e.getMessage(), e);
            // 不抛出异常，避免案件更新流程
        }
    }

    /**
     * 根据负责人类型确定待办分配给谁
     */
    private Long determineAssignee(String assigneeType, Case caseEntity, CaseCreateRequest request) {
        if (assigneeType == null) {
            assigneeType = "OWNER";
        }

        switch (assigneeType) {
            case "OWNER":
                return caseEntity.getOwnerId();
            case "CO_OWNER":
                // 如果有协办律师，分配给第一个协办律师
                if (request.getCoOwnerIds() != null && !request.getCoOwnerIds().isEmpty()) {
                    return request.getCoOwnerIds().get(0);
                }
                // 否则分配给主办律师
                return caseEntity.getOwnerId();
            case "ASSISTANT":
                // 如果有律师助理，分配给第一个助理
                if (request.getAssistantIds() != null && !request.getAssistantIds().isEmpty()) {
                    return request.getAssistantIds().get(0);
                }
                // 否则分配给主办律师
                return caseEntity.getOwnerId();
            default:
                return caseEntity.getOwnerId();
        }
    }

    /**
     * 同步创建客户
     * PRD要求（第246行）：勾选"同步创建客户"复选框后自动在客户库创建
     */
    private void syncPartiesToClients(List<com.lawfirm.dto.PartyDTO> parties, Long caseId, Long userId) {
        if (parties == null || parties.isEmpty()) {
            return;
        }

        for (com.lawfirm.dto.PartyDTO partyDTO : parties) {
            // 只有当isClient=true且syncToClient=true时才同步创建
            if (partyDTO.getIsClient() != null && partyDTO.getIsClient()
                && partyDTO.getSyncToClient() != null && partyDTO.getSyncToClient()) {

                try {
                    // 检查客户是否已存在（根据名称）
                    if (clientRepository.existsByClientNameAndDeletedIsFalse(partyDTO.getName())) {
                        log.info("客户已存在，跳过创建：{}", partyDTO.getName());
                        continue;
                    }

                    // 创建ClientDTO
                    com.lawfirm.dto.ClientDTO clientDTO = new com.lawfirm.dto.ClientDTO();
                    clientDTO.setClientType("INDIVIDUAL".equals(partyDTO.getPartyType()) ? "PERSONAL" : "ENTERPRISE");
                    clientDTO.setClientName(partyDTO.getName());
                    clientDTO.setGender(partyDTO.getGender());
                    clientDTO.setIdCard(partyDTO.getIdCard());
                    clientDTO.setCreditCode(partyDTO.getCreditCode());
                    clientDTO.setPhone(partyDTO.getPhone());
                    clientDTO.setAddress(partyDTO.getAddress());
                    clientDTO.setLegalRepresentative(partyDTO.getLegalRepresentative());
                    clientDTO.setStatus("ACTIVE");
                    clientDTO.setSource("CASE_SYNC");
                    clientDTO.setNotes("从案件ID=" + caseId + "同步创建");

                    // 创建客户
                    com.lawfirm.dto.ClientDTO createdClient = clientService.createClient(clientDTO, userId);
                    log.info("同步创建客户成功：案件ID={}, 客户ID={}, 客户名称={}",
                             caseId, createdClient.getId(), createdClient.getClientName());
                } catch (Exception e) {
                    // 如果客户已存在或其他错误，记录日志但不中断案件创建流程
                    log.warn("同步创建客户失败：案件ID={}, 当事人名称={}, 错误={}",
                             caseId, partyDTO.getName(), e.getMessage());
                }
            }
        }
    }

    /**
     * 创建审限届满待办事项
     * PRD要求：审限时间录入后自动生成审限届满待办
     */
    private void createDeadlineTodo(Case caseEntity) {
        log.info("[DEBUG] 开始创建审限待办：案件ID={}, 案件名={}, 审限={}, ownerId={}",
                caseEntity.getId(), caseEntity.getCaseName(), caseEntity.getDeadlineDate(), caseEntity.getOwnerId());
        try {
            TodoDTO todoDTO = new TodoDTO();
            todoDTO.setTitle("【审限届满】" + caseEntity.getCaseName());
            todoDTO.setDescription("案件审限即将届满，请及时处理相关事宜。案号：" + caseEntity.getCaseNumber());
            todoDTO.setPriority("HIGH");
            todoDTO.setDueDate(caseEntity.getDeadlineDate().atStartOfDay());
            todoDTO.setStatus("PENDING");
            todoDTO.setAssigneeId(caseEntity.getOwnerId());
            todoDTO.setCaseId(caseEntity.getId());
            todoDTO.setReminder(true);

            TodoDTO created = todoService.createTodo(todoDTO, caseEntity.getOwnerId());
            log.info("自动创建审限届满待办成功：案件ID={}, 待办ID={}, 标题={}",
                    caseEntity.getId(), created.getId(), created.getTitle());
        } catch (Exception e) {
            log.error("创建审限届满待办失败：案件ID={}, 错误={}", caseEntity.getId(), e.getMessage(), e);
            throw new RuntimeException("创建审限待办失败: " + e.getMessage(), e);
        }
    }

    // ==================== 批量操作 ====================

    /**
     * 批量结案
     */
    @Transactional
    public void batchCloseCases(List<Long> caseIds, Long operatorId) {
        if (caseIds == null || caseIds.isEmpty()) {
            throw new IllegalArgumentException("Case ID list cannot be empty");
        }

        List<Case> cases = caseRepository.findAllById(caseIds);
        for (Case caseEntity : cases) {
            caseEntity.setStatus("closed");
            caseEntity.setUpdatedAt(LocalDateTime.now());
        }

        caseRepository.saveAll(cases);
        log.info("Batch close cases completed: count={}, operator={}", caseIds.size(), operatorId);
    }

    /**
     * 批量归档
     */
    @Transactional
    public void batchArchiveCases(List<Long> caseIds, Long operatorId) {
        if (caseIds == null || caseIds.isEmpty()) {
            throw new IllegalArgumentException("Case ID list cannot be empty");
        }

        List<Case> cases = caseRepository.findAllById(caseIds);
        for (Case caseEntity : cases) {
            caseEntity.setArchiveDate(LocalDate.now());
            caseEntity.setStatus("ARCHIVED");
        }

        caseRepository.saveAll(cases);
        log.info("Batch archive cases completed: count={}, operator={}", caseIds.size(), operatorId);
    }

    /**
     * 批量删除（软删除）
     */
    @Transactional
    public void batchDeleteCases(List<Long> caseIds, Long operatorId) {
        if (caseIds == null || caseIds.isEmpty()) {
            throw new IllegalArgumentException("Case ID list cannot be empty");
        }

        List<Case> cases = caseRepository.findAllById(caseIds);
        for (Case caseEntity : cases) {
            caseEntity.setDeleted(true);
        }

        caseRepository.saveAll(cases);
        log.info("Batch delete cases completed: count={}, operator={}", caseIds.size(), operatorId);
    }

    /**
     * 批量修改主办律师
     */
    @Transactional
    public void batchChangeOwner(List<Long> caseIds, Long newOwnerId, Long operatorId) {
        if (caseIds == null || caseIds.isEmpty()) {
            throw new IllegalArgumentException("Case ID list cannot be empty");
        }

        if (newOwnerId == null) {
            throw new IllegalArgumentException("Owner ID cannot be empty");
        }

        User newOwner = userRepository.findById(newOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", newOwnerId));

        List<Case> cases = caseRepository.findAllById(caseIds);
        for (Case caseEntity : cases) {
            caseEntity.setOwnerId(newOwnerId);
            caseEntity.setUpdatedAt(LocalDateTime.now());
        }

        caseRepository.saveAll(cases);
        log.info("Batch change owner completed: count={}, newOwner={}, operator={}", caseIds.size(), newOwnerId, operatorId);
    }
}
