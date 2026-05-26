package com.lawfirm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.dto.AIDocumentRecognitionResult;
import com.lawfirm.dto.ApprovalCreateRequest;
import com.lawfirm.dto.ApprovalDTO;
import com.lawfirm.dto.CaseFileIntakeResult;
import com.lawfirm.dto.CaseIntakePrefillDTO;
import com.lawfirm.entity.CaseIntakePending;
import com.lawfirm.entity.Role;
import com.lawfirm.entity.UserRole;
import com.lawfirm.repository.CaseIntakePendingRepository;
import com.lawfirm.repository.RoleRepository;
import com.lawfirm.repository.UserRoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CaseIntakePendingService {

    private static final String PENDING_DIR = "uploads/intake-pending/";

    private final CaseIntakePendingRepository pendingRepository;
    private final CaseFileIntakeService caseFileIntakeService;
    private final ApprovalService approvalService;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final ObjectMapper objectMapper;

    public CaseIntakePendingService(
            CaseIntakePendingRepository pendingRepository,
            @Lazy CaseFileIntakeService caseFileIntakeService,
            ApprovalService approvalService,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            ObjectMapper objectMapper) {
        this.pendingRepository = pendingRepository;
        this.caseFileIntakeService = caseFileIntakeService;
        this.approvalService = approvalService;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Long savePending(MultipartFile file, Long userId, String remark,
                            AIDocumentRecognitionResult recognition) throws IOException {
        Path dir = Paths.get(PENDING_DIR);
        Files.createDirectories(dir);

        CaseIntakePending pending = new CaseIntakePending();
        pending.setUserId(userId);
        pending.setOriginalFilename(file.getOriginalFilename());
        pending.setFileSize(file.getSize());
        pending.setContentType(file.getContentType());
        pending.setRemark(remark);
        pending.setStatus("PENDING");
        pending.setDeleted(false);
        if (recognition != null) {
            pending.setRecognitionJson(objectMapper.writeValueAsString(recognition));
        }
        pending = pendingRepository.save(pending);

        String safeName = sanitize(file.getOriginalFilename());
        Path target = dir.resolve(pending.getId() + "_" + safeName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        pending.setStoredPath(target.toString());
        pendingRepository.save(pending);

        log.info("卷宗暂存: pendingId={}, file={}", pending.getId(), safeName);
        return pending.getId();
    }

    @Transactional
    public CaseFileIntakeResult attachFromPending(Long pendingId, Long caseId, Long userId, String remark) {
        CaseIntakePending pending = pendingRepository.findByIdAndUserIdAndDeletedFalse(pendingId, userId)
                .orElseThrow(() -> new IllegalArgumentException("暂存记录不存在"));

        if (!"PENDING".equals(pending.getStatus())) {
            throw new IllegalArgumentException("该暂存文件已处理");
        }

        MultipartFileAdapter file = new MultipartFileAdapter(pending);
        AIDocumentRecognitionResult recognition = parseRecognition(pending.getRecognitionJson());

        CaseFileIntakeResult result = caseFileIntakeService.attachToCase(
                file, userId, caseId, remark != null ? remark : pending.getRemark(), recognition);

        pending.setStatus("ATTACHED");
        pending.setCaseId(caseId);
        pendingRepository.save(pending);

        result.setPendingId(pendingId);
        return result;
    }

    @Transactional
    public ApprovalDTO createCaseFilingApproval(Long pendingId, String title, String content,
                                                Long approverId, Long userId) {
        CaseIntakePending pending = pendingRepository.findByIdAndUserIdAndDeletedFalse(pendingId, userId)
                .orElseThrow(() -> new IllegalArgumentException("暂存记录不存在"));

        Long resolvedApprover = approverId != null ? approverId : resolveDefaultApprover();

        ApprovalCreateRequest request = new ApprovalCreateRequest();
        request.setApprovalType(ApprovalService.TYPE_CASE_FILING);
        request.setTitle(title != null && !title.isBlank() ? title : "立案申请 - " + pending.getOriginalFilename());
        request.setContent(content != null && !content.isBlank() ? content
                : buildDefaultFilingContent(pending));
        request.setCurrentApproverId(resolvedApprover);
        request.setAttachments("{\"intakePendingId\":" + pendingId + "}");

        ApprovalDTO dto = approvalService.createApproval(request, userId);
        pending.setApprovalId(dto.getId());
        pendingRepository.save(pending);
        return dto;
    }

    public List<CaseIntakePending> listPending(Long userId) {
        return pendingRepository.findByUserIdAndStatusAndDeletedFalseOrderByCreatedAtDesc(userId, "PENDING");
    }

    /**
     * 立案审批通过后，为新建案件页提供预填字段。
     */
    public CaseIntakePrefillDTO getPrefill(Long pendingId, Long userId) {
        CaseIntakePending pending = pendingRepository.findByIdAndUserIdAndDeletedFalse(pendingId, userId)
                .orElseGet(() -> pendingRepository.findById(pendingId)
                        .filter(p -> !Boolean.TRUE.equals(p.getDeleted()))
                        .orElseThrow(() -> new IllegalArgumentException("暂存记录不存在")));

        AIDocumentRecognitionResult rec = parseRecognition(pending.getRecognitionJson());
        CaseIntakePrefillDTO dto = new CaseIntakePrefillDTO();
        dto.setPendingId(pendingId);
        dto.setRemark(pending.getRemark());
        if (rec != null) {
            dto.setCaseNumber(rec.getCaseNumber());
            dto.setCourtName(rec.getCourtName());
            dto.setCaseReason(rec.getCaseReason());
            dto.setPlaintiffName(rec.getPlaintiffName());
            dto.setDefendantName(rec.getDefendantName());
            dto.setHearingDate(rec.getHearingDate());
            dto.setHearingPlace(rec.getHearingPlace());
            dto.setDocumentType(rec.getDocumentType());
            dto.setSuggestedCaseName(buildSuggestedCaseName(rec));
        }
        return dto;
    }

    @Transactional
    public void markFilingApproved(Long pendingId, Long approvalId) {
        pendingRepository.findById(pendingId).ifPresent(p -> {
            if ("PENDING".equals(p.getStatus()) || "FILING_APPROVED".equals(p.getStatus())) {
                p.setStatus("FILING_APPROVED");
                if (approvalId != null) {
                    p.setApprovalId(approvalId);
                }
                pendingRepository.save(p);
            }
        });
    }

    private String buildSuggestedCaseName(AIDocumentRecognitionResult rec) {
        if (rec.getPlaintiffName() != null && rec.getDefendantName() != null) {
            return rec.getPlaintiffName() + "诉" + rec.getDefendantName()
                    + (rec.getCaseReason() != null ? rec.getCaseReason() + "案" : "案");
        }
        if (rec.getCaseReason() != null) {
            return rec.getCaseReason();
        }
        return null;
    }

    private Long resolveDefaultApprover() {
        for (String code : List.of("DIRECTOR", "ADMIN")) {
            Optional<Role> role = roleRepository.findByRoleCode(code);
            if (role.isPresent()) {
                List<UserRole> links = userRoleRepository.findByRoleId(role.get().getId());
                if (!links.isEmpty()) {
                    return links.get(0).getUserId();
                }
            }
        }
        throw new IllegalStateException("未配置主任/管理员审批人，请在申请中指定审批人");
    }

    private String buildDefaultFilingContent(CaseIntakePending pending) {
        StringBuilder sb = new StringBuilder();
        sb.append("卷宗录入未匹配到已有案件，申请立案。\n");
        sb.append("文件名：").append(pending.getOriginalFilename()).append("\n");
        sb.append("暂存编号：").append(pending.getId()).append("\n");
        if (pending.getRemark() != null) {
            sb.append("备注：").append(pending.getRemark());
        }
        return sb.toString();
    }

    private AIDocumentRecognitionResult parseRecognition(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, AIDocumentRecognitionResult.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String sanitize(String name) {
        if (name == null) {
            return "file";
        }
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    /**
     * 从暂存路径重新构造 MultipartFile，供 attachToCase 使用。
     */
    static class MultipartFileAdapter implements MultipartFile {
        private final CaseIntakePending pending;

        MultipartFileAdapter(CaseIntakePending pending) {
            this.pending = pending;
        }

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return pending.getOriginalFilename();
        }

        @Override
        public String getContentType() {
            return pending.getContentType();
        }

        @Override
        public boolean isEmpty() {
            return pending.getFileSize() == null || pending.getFileSize() == 0;
        }

        @Override
        public long getSize() {
            return pending.getFileSize() != null ? pending.getFileSize() : 0;
        }

        @Override
        public byte[] getBytes() throws IOException {
            return Files.readAllBytes(Paths.get(pending.getStoredPath()));
        }

        @Override
        public java.io.InputStream getInputStream() throws IOException {
            return Files.newInputStream(Paths.get(pending.getStoredPath()));
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException {
            Files.copy(Paths.get(pending.getStoredPath()), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
