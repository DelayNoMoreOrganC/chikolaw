package com.lawfirm.controller;

import com.lawfirm.dto.ConflictCheckResult;
import com.lawfirm.dto.PartyDTO;
import com.lawfirm.service.ConflictCheckService;
import com.lawfirm.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 利益冲突审查控制器
 */
@Slf4j
@RestController
@RequestMapping("/conflict-check")
@RequiredArgsConstructor
@Tag(name = "利益冲突审查", description = "利益冲突审查相关接口")
public class ConflictCheckController {

    private final ConflictCheckService conflictCheckService;

    /**
     * 检查客户名称冲突
     */
    @PostMapping("/check-client")
    @Operation(summary = "检查客户名称冲突", description = "检查客户名称是否与现有客户冲突")
    public Result<ConflictCheckResult> checkClientNameConflict(@RequestParam String clientName) {
        log.info("检查客户名称冲突: {}", clientName);
        ConflictCheckResult result = conflictCheckService.checkClientNameConflict(clientName);
        return Result.success(result);
    }

    /**
     * 检查当事人冲突
     */
    @PostMapping("/check-party")
    @Operation(summary = "检查当事人冲突", description = "检查当事人是否与现有案件冲突")
    public Result<ConflictCheckResult> checkPartyConflict(@RequestBody List<PartyDTO> parties) {
        log.info("检查当事人冲突，当事人数量: {}", parties != null ? parties.size() : 0);
        ConflictCheckResult result = conflictCheckService.checkPartyConflict(parties);
        return Result.success(result);
    }

    /**
     * 综合利益冲突检查
     */
    @PostMapping("/comprehensive")
    @Operation(summary = "综合利益冲突检查", description = "对案件进行全面利益冲突检查")
    public Result<ConflictCheckResult> comprehensiveCheck(@RequestBody List<PartyDTO> parties) {
        log.info("综合利益冲突检查，当事人数量: {}", parties != null ? parties.size() : 0);
        ConflictCheckResult result = conflictCheckService.comprehensiveCheck(parties);
        return Result.success(result);
    }
}
