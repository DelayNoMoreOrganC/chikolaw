package com.lawfirm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.dto.AIDocumentRecognitionResult;
import com.lawfirm.dto.CaseDocumentDTO;
import com.lawfirm.dto.CaseFileIntakeResult;
import com.lawfirm.entity.Case;
import com.lawfirm.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 系统核心链路：主界面传入文件 → AI 分析 → 登记备注 → 存入案件档案文件夹。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseFileIntakeService {

    private final CaseDocumentService caseDocumentService;
    private final CaseTimelineService caseTimelineService;
    private final CaseRepository caseRepository;
    private final EmbeddedAgentService embeddedAgentService;
    private final CaseIntakePendingService caseIntakePendingService;
    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    public CaseFileIntakeResult processIntake(MultipartFile file,
                                              Long userId,
                                              Long caseId,
                                              String userRemark,
                                              boolean runAutomation) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        AIDocumentRecognitionResult recognition = null;
        String analysisProvider = "builtin";

        try {
            EmbeddedAgentService.AnalysisResult analysis =
                    embeddedAgentService.analyzeDocument(file, userId, caseId, userRemark);
            recognition = analysis.getRecognition();
            analysisProvider = analysis.getProvider();

            Long resolvedCaseId = resolveCaseId(caseId, recognition);
            if (resolvedCaseId == null) {
                CaseFileIntakeResult pending = new CaseFileIntakeResult();
                pending.setStatus("NEEDS_CASE");
                pending.setMessage(buildNoMatchMessage(recognition));
                pending.setRecognition(recognition);
                pending.setAnalysisProvider(analysisProvider);
                pending.setCaseCandidates(findCaseCandidates(recognition));
                try {
                    Long pendingId = caseIntakePendingService.savePending(
                            file, userId, userRemark, recognition);
                    pending.setPendingId(pendingId);
                    pending.setMessage(pending.getMessage()
                            + "\n（文件已暂存，编号 " + pendingId + "，可选择案件挂接或发起立案审批）");
                } catch (Exception e) {
                    log.warn("卷宗暂存失败（仍可手动选案件后重传归档）: {}", e.getMessage());
                    pending.setMessage(pending.getMessage()
                            + "\n（文件暂存失败：" + e.getMessage() + "，请先在上方选择案件后再次上传，或联系管理员修复数据库表）");
                }
                return pending;
            }

            Case caseEntity = caseRepository.findById(resolvedCaseId)
                    .orElseThrow(() -> new IllegalArgumentException("案件不存在"));

            String folderPath = resolveFolderPath(caseEntity, recognition);
            String documentType = resolveDocumentType(recognition);

            CaseDocumentDTO savedDoc = caseDocumentService.uploadDocument(
                    resolvedCaseId, file, documentType, folderPath, userId);

            String ocrJson = toJson(recognition);
            CaseDocumentDTO patch = new CaseDocumentDTO();
            patch.setOcrResult(ocrJson);
            patch.setTags(buildTags(userRemark, recognition));
            caseDocumentService.updateDocument(savedDoc.getId(), patch);

            String registrationNote = buildRegistrationNote(recognition, userRemark, file.getOriginalFilename());
            caseTimelineService.createSystemTimeline(
                    resolvedCaseId, "FILE_INTAKE", registrationNote);

            // 不在此流程新建案件；runAutomation 仅保留扩展点（默认关闭）
            Map<String, Object> automation = null;
            if (runAutomation) {
                automation = Map.of("skipped", true, "reason", "建案须审批，仅对已匹配案件归档");
            }

            CaseFileIntakeResult result = new CaseFileIntakeResult();
            result.setStatus("SUCCESS");
            result.setMessage("已分析并归入案件卷宗");
            result.setCaseId(resolvedCaseId);
            result.setCaseName(caseEntity.getCaseName());
            result.setCaseNumber(caseEntity.getCaseNumber());
            result.setDocumentId(savedDoc.getId());
            result.setDocumentName(savedDoc.getDocumentName());
            result.setFolderPath(folderPath);
            result.setDocumentType(documentType);
            result.setRegistrationNote(registrationNote);
            result.setRecognition(recognition);
            result.setAnalysisProvider(analysisProvider);
            result.setAutomation(automation);
            return result;

        } catch (Exception e) {
            log.error("卷宗录入失败", e);
            CaseFileIntakeResult failed = new CaseFileIntakeResult();
            failed.setStatus("FAILED");
            failed.setMessage(e.getMessage());
            if (recognition != null) {
                failed.setRecognition(recognition);
            }
            return failed;
        }
    }

    /**
     * 用户选定案件后再次归档（仅存档+备注，可跳过 AI）。
     */
    @Transactional(rollbackFor = Exception.class)
    public CaseFileIntakeResult attachToCase(MultipartFile file,
                                             Long userId,
                                             Long caseId,
                                             String userRemark,
                                             AIDocumentRecognitionResult recognition) {
        if (caseId == null) {
            throw new IllegalArgumentException("必须指定案件");
        }
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("案件不存在"));

        String folderPath = resolveFolderPath(caseEntity, recognition);
        String documentType = resolveDocumentType(recognition);

        CaseDocumentDTO savedDoc;
        try {
            savedDoc = caseDocumentService.uploadDocument(
                    caseId, file, documentType, folderPath, userId);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("文件存储失败: " + e.getMessage(), e);
        }

        if (recognition != null) {
            CaseDocumentDTO patch = new CaseDocumentDTO();
            patch.setOcrResult(toJson(recognition));
            patch.setTags(buildTags(userRemark, recognition));
            caseDocumentService.updateDocument(savedDoc.getId(), patch);
        }

        String note = recognition != null
                ? buildRegistrationNote(recognition, userRemark, file.getOriginalFilename())
                : buildSimpleNote(userRemark, file.getOriginalFilename());

        caseTimelineService.createSystemTimeline(caseId, "FILE_INTAKE", note);

        CaseFileIntakeResult result = new CaseFileIntakeResult();
        result.setStatus("SUCCESS");
        result.setMessage("文件已归入案件卷宗");
        result.setCaseId(caseId);
        result.setCaseName(caseEntity.getCaseName());
        result.setCaseNumber(caseEntity.getCaseNumber());
        result.setDocumentId(savedDoc.getId());
        result.setDocumentName(savedDoc.getDocumentName());
        result.setFolderPath(folderPath);
        result.setDocumentType(documentType);
        result.setRegistrationNote(note);
        result.setRecognition(recognition);
        return result;
    }

    public List<CaseFileIntakeResult.CaseBriefDTO> searchCases(String keyword, int limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return caseRepository.findAll().stream()
                    .filter(c -> !Boolean.TRUE.equals(c.getDeleted()))
                    .limit(limit)
                    .map(this::toBrief)
                    .collect(Collectors.toList());
        }
        String q = keyword.trim();
        return caseRepository.findAll().stream()
                .filter(c -> !Boolean.TRUE.equals(c.getDeleted()))
                .filter(c -> matches(c, q))
                .limit(limit)
                .map(this::toBrief)
                .collect(Collectors.toList());
    }

    private boolean matches(Case c, String q) {
        return (c.getCaseNumber() != null && c.getCaseNumber().contains(q))
                || (c.getCaseName() != null && c.getCaseName().contains(q));
    }

    private Long resolveCaseId(Long explicitCaseId, AIDocumentRecognitionResult recognition) {
        if (explicitCaseId != null) {
            return explicitCaseId;
        }
        if (recognition != null && recognition.getCaseNumber() != null
                && !recognition.getCaseNumber().isBlank()) {
            Optional<Case> byNumber = caseRepository.findByCaseNumberAndDeletedFalse(
                    recognition.getCaseNumber().trim());
            if (byNumber.isPresent()) {
                return byNumber.get().getId();
            }
        }
        return null;
    }

    private String buildNoMatchMessage(AIDocumentRecognitionResult recognition) {
        String caseNum = recognition != null && recognition.getCaseNumber() != null
                ? recognition.getCaseNumber() : "（未识别案号）";
        return "未匹配到已有案件（识别案号：" + caseNum + "）。请从下方选择要归档的案件；"
                + "新建案件须走「立案申请 / 审批」流程，本入口不会自动建案。";
    }

    private List<CaseFileIntakeResult.CaseBriefDTO> findCaseCandidates(AIDocumentRecognitionResult recognition) {
        List<CaseFileIntakeResult.CaseBriefDTO> list = new ArrayList<>();
        if (recognition == null) {
            return list;
        }
        if (recognition.getCaseNumber() != null) {
            caseRepository.findByCaseNumberAndDeletedFalse(recognition.getCaseNumber().trim())
                    .ifPresent(c -> list.add(toBrief(c)));
        }
        String nameHint = recognition.getPlaintiffName() != null
                ? recognition.getPlaintiffName() : recognition.getDefendantName();
        if (nameHint != null) {
            caseRepository.findAll().stream()
                    .filter(c -> !Boolean.TRUE.equals(c.getDeleted()))
                    .filter(c -> c.getCaseName() != null && c.getCaseName().contains(nameHint))
                    .limit(5)
                    .forEach(c -> {
                        if (list.stream().noneMatch(b -> b.getId().equals(c.getId()))) {
                            list.add(toBrief(c));
                        }
                    });
        }
        if (list.isEmpty()) {
            list.addAll(searchCases("", 8));
        }
        return list;
    }

    private CaseFileIntakeResult.CaseBriefDTO toBrief(Case c) {
        CaseFileIntakeResult.CaseBriefDTO dto = new CaseFileIntakeResult.CaseBriefDTO();
        dto.setId(c.getId());
        dto.setCaseNumber(c.getCaseNumber());
        dto.setCaseName(c.getCaseName());
        dto.setCurrentStage(c.getCurrentStage());
        return dto;
    }

    private String resolveFolderPath(Case caseEntity, AIDocumentRecognitionResult recognition) {
        String typeFolder = resolveDocumentTypeFolder(recognition);
        return caseDocumentService.resolveStageDocumentFolder(caseEntity, typeFolder);
    }

    private String resolveDocumentTypeFolder(AIDocumentRecognitionResult recognition) {
        String type = recognition != null ? recognition.getDocumentType() : null;
        if (type == null || type.isBlank()) {
            return "其他";
        }
        switch (type) {
            case "起诉状":
                return "起诉状";
            case "答辩状":
                return "答辩状";
            case "判决书":
            case "裁定书":
                return "法院文书";
            case "传票":
            case "通知书":
                return "法院文书";
            case "调解书":
                return "法院文书";
            default:
                return "其他";
        }
    }

    private String resolveDocumentType(AIDocumentRecognitionResult recognition) {
        if (recognition == null || recognition.getDocumentType() == null
                || recognition.getDocumentType().isBlank()) {
            return "其他";
        }
        String t = recognition.getDocumentType();
        if ("裁定书".equals(t)) {
            return "法院文书";
        }
        return t;
    }

    private String buildRegistrationNote(AIDocumentRecognitionResult r, String userRemark, String filename) {
        StringBuilder sb = new StringBuilder();
        sb.append("【卷宗录入】").append(filename != null ? filename : "文件");
        if (r.getDocumentType() != null) {
            sb.append(" | 类型:").append(r.getDocumentType());
        }
        if (r.getCaseNumber() != null) {
            sb.append(" | 案号:").append(r.getCaseNumber());
        }
        if (r.getCourtName() != null) {
            sb.append(" | 法院:").append(r.getCourtName());
        }
        if (r.getHearingDate() != null) {
            sb.append(" | 开庭:").append(r.getHearingDate());
        }
        if (userRemark != null && !userRemark.isBlank()) {
            sb.append("\n备注:").append(userRemark.trim());
        }
        return sb.toString();
    }

    private String buildSimpleNote(String userRemark, String filename) {
        String base = "【卷宗录入】" + (filename != null ? filename : "文件");
        if (userRemark != null && !userRemark.isBlank()) {
            return base + "\n备注:" + userRemark.trim();
        }
        return base;
    }

    private String buildTags(String userRemark, AIDocumentRecognitionResult r) {
        if (userRemark != null && !userRemark.isBlank()) {
            return userRemark.trim();
        }
        return r.getDocumentType() != null ? r.getDocumentType() : "卷宗录入";
    }

    private String toJson(AIDocumentRecognitionResult recognition) {
        try {
            return objectMapper.writeValueAsString(recognition);
        } catch (Exception e) {
            return recognition.getOcrText();
        }
    }

}
