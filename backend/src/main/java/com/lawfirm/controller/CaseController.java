package com.lawfirm.controller;

import com.lawfirm.dto.*;
import com.lawfirm.annotation.AuditLog;
import com.lawfirm.entity.CaseRecord;
import com.lawfirm.entity.CaseTimeline;
import com.lawfirm.entity.User;
import com.lawfirm.exception.InvalidParameterException;
import com.lawfirm.repository.UserRepository;
import com.lawfirm.service.*;
import com.lawfirm.util.PageResult;
import com.lawfirm.util.Result;
import com.lawfirm.vo.CaseDetailVO;
import com.lawfirm.vo.CaseListVO;
import com.lawfirm.vo.CaseProcedureVO;
import com.lawfirm.vo.PartyVO;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 案件管理控制器
 */
@Slf4j
@RestController
@RequestMapping("cases")
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;
    private final PartyService partyService;
    private final CaseProcedureService caseProcedureService;
    private final CaseRecordService caseRecordService;
    private final CaseTimelineService caseTimelineService;
    private final CaseStageService caseStageService;
    private final UserRepository userRepository;
    private final ArchivePdfService archivePdfService;
    private final ExcelExportService excelExportService;
    private final com.lawfirm.security.SecurityUtils securityUtils;
    private final CaseAnalysisService caseAnalysisService;

    /**
     * 创建案件
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @AuditLog(value = "创建案件", operationType = "CREATE")
    public Result<CaseDetailVO> createCase(
            @Valid @RequestBody CaseCreateRequest request) {
        try {
            Long currentUserId = securityUtils.getCurrentUserId();
            CaseDetailVO caseDetail = caseService.createCase(request, currentUserId);
            return Result.success("案件创建成功", caseDetail);
        } catch (Exception e) {
            log.error("案件创建失败", e);
            return Result.error("案件创建失败: " + e.getMessage());
        }
    }

    /**
     * 确认正式建案（草稿/待立案 → 审理中）
     * PUT /api/cases/{id}/confirm-establishment
     */
    @PutMapping("/{id}/confirm-establishment")
    @PreAuthorize("isAuthenticated()")
    @AuditLog(value = "确认建案", operationType = "UPDATE")
    public Result<CaseDetailVO> confirmEstablishment(@PathVariable Long id) {
        try {
            Long currentUserId = securityUtils.getCurrentUserId();
            CaseDetailVO detail = caseService.confirmEstablishment(id, currentUserId);
            return Result.success("案件已正式建立", detail);
        } catch (Exception e) {
            log.error("确认建案失败: id={}", id, e);
            return Result.error("确认建案失败: " + e.getMessage());
        }
    }

    /**
     * 获取案件列表
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<CaseListVO>> getCaseList(CaseQueryRequest request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        PageResult<CaseListVO> result = caseService.getCaseList(request, currentUserId);
        return Result.success(result);
    }

    /**
     * AI 案件分析（LLM 语义化）
     * GET /api/cases/{id}/ai-analysis
     */
    @GetMapping("/{id}/ai-analysis")
    @PreAuthorize("isAuthenticated()")
    public Result<java.util.Map<String, Object>> analyzeCase(@PathVariable Long id) {
        try {
            return Result.success(caseAnalysisService.analyzeCase(id));
        } catch (Exception e) {
            log.error("案件 AI 分析失败: caseId={}", id, e);
            return Result.error("案件分析失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<CaseDetailVO> getCaseDetail(@PathVariable Long id) {
        CaseDetailVO caseDetail = caseService.getCaseDetail(id);
        return Result.success(caseDetail);
    }

    /**
     * 更新案件
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    @AuditLog(value = "更新案件", operationType = "UPDATE")
    public Result<CaseDetailVO> updateCase(
            @PathVariable Long id,
            @Valid @RequestBody CaseUpdateRequest request) {
        CaseDetailVO caseDetail = caseService.updateCase(id, request);
        return Result.success("案件更新成功", caseDetail);
    }

    /**
     * 删除案件
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CASE_DELETE')")
    @AuditLog(value = "删除案件", operationType = "DELETE")
    public Result<Void> deleteCase(@PathVariable Long id) {
        caseService.deleteCase(id);
        return Result.success();
    }

    /**
     * 恢复已删除的案件
     */
    @PutMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('CASE_DELETE')")
    @AuditLog(value = "恢复案件", operationType = "RESTORE")
    public Result<String> restoreCase(@PathVariable Long id) {
        caseService.restoreCase(id);
        return Result.success("案件恢复成功");
    }

    /**
     * 变更案件状态
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    @AuditLog(value = "变更案件状态", operationType = "UPDATE")
    public Result<Void> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusChangeRequest request) {
        try {
            Long currentUserId = securityUtils.getCurrentUserId();
            caseStageService.changeStatus(id, request.getTargetStage(), request.getReason(), currentUserId);
            return Result.success();
        } catch (RuntimeException e) {
            log.error("变更案件状态失败: {}", e.getMessage());
            return Result.error("变更失败: " + e.getMessage());
        }
    }

    /**
     * 获取状态历史
     */
    @GetMapping("/{id}/status-history")
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public Result<List<com.lawfirm.service.CaseStageService.StageHistoryVO>> getStatusHistory(@PathVariable Long id) {
        List<com.lawfirm.service.CaseStageService.StageHistoryVO> history = caseStageService.getStatusHistory(id);
        return Result.success(history);
    }

    /**
     * 回退状态
     */
    @PutMapping("/{id}/status/rollback")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    public Result<Void> rollbackStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusChangeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long currentUserId = securityUtils.getCurrentUserId();
        caseStageService.rollbackStatus(id, request.getTargetStage(), request.getReason(), currentUserId);
        return Result.success();
    }

    /**
     * 归档案件
     */
    @PutMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('CASE_ARCHIVE')")
    @AuditLog(value = "归档案件", operationType = "ARCHIVE")
    public Result<Void> archiveCase(
            @PathVariable Long id,
            @Valid @RequestBody ArchiveRequest request) {
        caseService.archiveCase(id, request.getArchiveLocation());
        return Result.success();
    }

    /**
     * 查重检查
     */
    /**
     * 查重检查
     * PRD要求（228行）：GET /api/cases/check-duplicate?name=xxx
     */
    @GetMapping("/check-duplicate")
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public Result<List<CaseListVO>> checkDuplicate(
            @RequestParam String name,
            @RequestParam(required = false) String caseNumber) {
        List<CaseListVO> duplicates = caseService.checkDuplicate(name, caseNumber);
        return Result.success(duplicates);
    }

    /**
     * 搜索法院（用于案件筛选的法院选择器）
     */
    @GetMapping("/courts/search")
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public Result<List<String>> searchCourts(@RequestParam(required = false) String keyword) {
        List<String> courts = caseService.searchCourts(keyword);
        return Result.success(courts);
    }

    // ========== 当事人管理 ==========

    /**
     * 获取案件当事人列表
     */
    @GetMapping("/{id}/parties")
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public Result<List<PartyVO>> getParties(@PathVariable Long id) {
        List<PartyVO> parties = partyService.getByCaseId(id);
        return Result.success(parties);
    }

    /**
     * 添加当事人
     */
    @PostMapping("/{id}/parties")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    public Result<PartyVO> addParty(
            @PathVariable Long id,
            @Valid @RequestBody PartyDTO dto) {
        var party = partyService.create(dto, id);
        return Result.success("当事人添加成功", partyService.getById(party.getId()));
    }

    /**
     * 更新当事人
     */
    @PutMapping("/{caseId}/parties/{partyId}")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    public Result<PartyVO> updateParty(
            @PathVariable Long caseId,
            @PathVariable Long partyId,
            @Valid @RequestBody PartyDTO dto) {
        partyService.update(partyId, dto);
        return Result.success("当事人更新成功", partyService.getById(partyId));
    }

    /**
     * 删除当事人
     */
    @DeleteMapping("/{caseId}/parties/{partyId}")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    public Result<Void> deleteParty(@PathVariable Long caseId, @PathVariable Long partyId) {
        partyService.delete(partyId);
        return Result.success();
    }

    // ========== 案件程序管理 ==========

    /**
     * 获取案件程序列表
     */
    @GetMapping("/{id}/procedures")
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public Result<List<CaseProcedureVO>> getProcedures(@PathVariable Long id) {
        List<CaseProcedureVO> procedures = caseProcedureService.getByCaseId(id);
        return Result.success(procedures);
    }

    /**
     * 添加案件程序
     */
    @PostMapping("/{id}/procedures")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    public Result<CaseProcedureVO> addProcedure(
            @PathVariable Long id,
            @Valid @RequestBody CaseProcedureDTO dto) {
        var procedure = caseProcedureService.create(dto, id);
        return Result.success("案件程序添加成功", caseProcedureService.getById(procedure.getId()));
    }

    /**
     * 更新案件程序
     */
    @PutMapping("/{caseId}/procedures/{procedureId}")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    public Result<CaseProcedureVO> updateProcedure(
            @PathVariable Long caseId,
            @PathVariable Long procedureId,
            @Valid @RequestBody CaseProcedureDTO dto) {
        caseProcedureService.update(procedureId, dto);
        return Result.success("案件程序更新成功", caseProcedureService.getById(procedureId));
    }

    /**
     * 删除案件程序
     */
    @DeleteMapping("/{caseId}/procedures/{procedureId}")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    public Result<Void> deleteProcedure(@PathVariable Long caseId, @PathVariable Long procedureId) {
        caseProcedureService.delete(procedureId);
        return Result.success();
    }

    // ========== 办案记录管理 ==========

    /**
     * 获取办案记录列表
     */
    @GetMapping("/{id}/records")
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public Result<List<com.lawfirm.vo.CaseRecordVO>> getRecords(@PathVariable Long id) {
        List<CaseRecord> records = caseRecordService.getByCaseId(id);
        List<com.lawfirm.vo.CaseRecordVO> voList = caseRecordService.toVOList(records);
        return Result.success(voList);
    }

    /**
     * 添加办案记录
     */
    @PostMapping("/{id}/records")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    public Result<com.lawfirm.vo.CaseRecordVO> addRecord(
            @PathVariable Long id,
            @Valid @RequestBody CaseRecordDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long currentUserId = securityUtils.getCurrentUserId();
        CaseRecord record = caseRecordService.create(dto, id, currentUserId);
        com.lawfirm.vo.CaseRecordVO vo = caseRecordService.toVO(record);
        return Result.success("办案记录添加成功", vo);
    }

    /**
     * 更新办案记录
     */
    @PutMapping("/{caseId}/records/{recordId}")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    public Result<com.lawfirm.vo.CaseRecordVO> updateRecord(
            @PathVariable Long caseId,
            @PathVariable Long recordId,
            @Valid @RequestBody CaseRecordDTO dto) {
        CaseRecord record = caseRecordService.update(recordId, dto);
        com.lawfirm.vo.CaseRecordVO vo = caseRecordService.toVO(record);
        return Result.success("办案记录更新成功", vo);
    }

    /**
     * 删除办案记录
     */
    @DeleteMapping("/{caseId}/records/{recordId}")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    public Result<Void> deleteRecord(@PathVariable Long caseId, @PathVariable Long recordId) {
        caseRecordService.delete(recordId);
        return Result.success();
    }

    /**
     * 导出办案记录
     */
    /**
     * 导出办案记录（Excel）
     */
    @PostMapping("/{id}/records/export")
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public void exportRecords(
            @PathVariable Long id,
            String stage,
            String startDate,
            String endDate,
            String keyword,
            javax.servlet.http.HttpServletResponse response) {
        try {
            byte[] excelData = excelExportService.exportCaseRecords(id, stage, startDate, endDate, keyword);

            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" +
                new String(("办案记录_" + id + "_" + System.currentTimeMillis() + ".xlsx").getBytes("UTF-8"), "ISO-8859-1"));

            // 输出Excel文件
            response.getOutputStream().write(excelData);
            response.getOutputStream().flush();

        } catch (Exception e) {
            log.error("导出办案记录Excel失败", e);
            throw new RuntimeException("导出办案记录Excel失败: " + e.getMessage());
        }
    }

    /**
     * 导出办案记录（Word）
     * PRD要求（320行）：支持导出Word格式
     */
    @PostMapping("/{id}/records/export/word")
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public void exportRecordsWord(
            @PathVariable Long id,
            String stage,
            String startDate,
            String endDate,
            String keyword,
            javax.servlet.http.HttpServletResponse response) {
        try {
            byte[] wordData = excelExportService.exportCaseRecordsToWord(id, stage, startDate, endDate, keyword);

            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            response.setHeader("Content-Disposition", "attachment; filename=" +
                new String(("办案记录_" + id + "_" + System.currentTimeMillis() + ".docx").getBytes("UTF-8"), "ISO-8859-1"));

            // 输出Word文件
            response.getOutputStream().write(wordData);
            response.getOutputStream().flush();

        } catch (Exception e) {
            log.error("导出办案记录Word失败", e);
            throw new RuntimeException("导出办案记录Word失败: " + e.getMessage());
        }
    }

    // ========== 案件动态管理 ==========

    /**
     * 获取案件动态列表
     */
    @GetMapping("/{id}/timeline")
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public Result<List<CaseTimeline>> getTimeline(@PathVariable Long id) {
        List<CaseTimeline> timeline = caseTimelineService.getByCaseId(id);
        return Result.success(timeline);
    }

    /**
     * 添加评论
     */
    @PostMapping("/{id}/timeline")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    public Result<CaseTimeline> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long currentUserId = securityUtils.getCurrentUserId();
        CaseTimeline timeline = caseTimelineService.addComment(id, request.getContent(), currentUserId, request.getParentId());
        return Result.success("评论添加成功", timeline);
    }

    /**
     * 删除动态
     */
    @DeleteMapping("/{caseId}/timeline/{timelineId}")
    @PreAuthorize("hasAuthority('CASE_EDIT')")
    public Result<Void> deleteTimeline(@PathVariable Long caseId, @PathVariable Long timelineId) {
        caseTimelineService.delete(timelineId);
        return Result.success();
    }

    /**
     * 一键归档PDF
     */
    @PostMapping("/{id}/archive-pdf")
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public Result<String> generateArchivePdf(@PathVariable Long id) {
        String pdfUrl = archivePdfService.generateArchivePdf(id);
        return Result.success("PDF生成成功", pdfUrl);
    }

    /**
     * 下载归档PDF
     */
    @GetMapping("/{id}/archive-pdf/download")
    @PreAuthorize("hasAuthority('CASE_VIEW')")
    public void downloadArchivePdf(@PathVariable Long id, javax.servlet.http.HttpServletResponse response) {
        archivePdfService.downloadArchivePdf(id, response);
    }

    // ========== 请求DTO ==========

    @lombok.Data
    public static class StatusChangeRequest {
        @javax.validation.constraints.NotBlank(message = "目标阶段不能为空")
        private String targetStage;

        private String reason;
    }

    @lombok.Data
    public static class ArchiveRequest {
        @javax.validation.constraints.NotBlank(message = "档案保管地不能为空")
        private String archiveLocation;
    }

    @lombok.Data
    public static class CommentRequest {
        @javax.validation.constraints.NotBlank(message = "评论内容不能为空")
        private String content;

        private Long parentId;
    }

    // 辅助方法
    private Long getCurrentUserId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new InvalidParameterException("userDetails", "用户信息不能为空");
        }
        // 从 UserDetails 的 username 中提取用户ID
        // 假设 username 格式为 "userId:actualUsername" 或者直接从实现中获取
        // 这里通过查询数据库获取
        String username = userDetails.getUsername();
        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new InvalidParameterException("userDetails", "无效的用户信息"));
    }
}
