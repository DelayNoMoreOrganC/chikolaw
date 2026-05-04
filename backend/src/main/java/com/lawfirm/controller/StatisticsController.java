package com.lawfirm.controller;

import com.lawfirm.service.StatisticsService;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 统计报表控制器
 */
@Slf4j
@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    /**
     * 获取统计卡片数据
     * GET /api/statistics/overview
     */
    @GetMapping({"/overview", "/cards", "/cases"})
    public Result<Map<String, Object>> getOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            Map<String, Object> result = statisticsService.getStatsCards(startDate, endDate);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取统计概览失败", e);
            return Result.error("获取统计概览失败: " + e.getMessage());
        }
    }

    /**
     * 获取案件数量趋势
     * GET /api/statistics/case-trends
     */
    @GetMapping({"/case-trends", "/case-trend"})
    public Result<Map<String, Object>> getCaseTrends(
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            Map<String, Object> result = statisticsService.getCaseTrend(period, startDate, endDate);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取案件趋势失败", e);
            return Result.error("获取案件趋势失败: " + e.getMessage());
        }
    }

    /**
     * 获取案件类型分布
     * GET /api/statistics/case-types
     */
    @GetMapping({"/case-types", "/case-type-distribution"})
    public Result<Map<String, Object>> getCaseTypes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            LocalDate start = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
            LocalDate end = endDate != null ? endDate : LocalDate.now();
            Map<String, Object> result = statisticsService.getCaseTypeDistribution(start, end);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取案件类型分布失败", e);
            return Result.error("获取案件类型分布失败: " + e.getMessage());
        }
    }

    /**
     * 获取收费统计
     * GET /api/statistics/fees
     */
    @GetMapping({"/fees", "/fee-statistics"})
    public Result<Map<String, Object>> getFeeStatistics(
            @RequestParam(defaultValue = "month") String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            Map<String, Object> result = statisticsService.getFeeStatistics(type, startDate, endDate);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取收费统计失败", e);
            return Result.error("获取收费统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取律师业绩排名
     * GET /api/statistics/lawyer-performance
     */
    @GetMapping("/lawyer-performance")
    public Result<Map<String, Object>> getLawyerPerformance(
            @RequestParam(defaultValue = "caseCount") String metric,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            Map<String, Object> result = statisticsService.getLawyerPerformance(metric, startDate, endDate);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取律师业绩排名失败", e);
            return Result.error("获取律师业绩排名失败: " + e.getMessage());
        }
    }

    /**
     * 获取胜诉率统计
     * GET /api/statistics/win-rate
     */
    @GetMapping("/win-rate")
    public Result<Map<String, Object>> getWinRate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            Map<String, Object> result = statisticsService.getWinRate(startDate, endDate);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取胜诉率统计失败", e);
            return Result.error("获取胜诉率统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取收款率统计
     * GET /api/statistics/collection-rate
     */
    @GetMapping("/collection-rate")
    public Result<Map<String, Object>> getCollectionRate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            Map<String, Object> result = statisticsService.getCollectionRate(startDate, endDate);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取收款率统计失败", e);
            return Result.error("获取收款率统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取金融不良资产专项统计
     * GET /api/statistics/npa
     */
    @GetMapping("/npa")
    public Result<Map<String, Object>> getNpaStatistics() {
        try {
            Map<String, Object> result = statisticsService.getNpaStatistics();
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取不良资产统计失败", e);
            return Result.error("获取不良资产统计失败: " + e.getMessage());
        }
    }

    /**
     * 导出Excel
     * POST /api/statistics/export/excel
     */
    @PostMapping("/export/excel")
    public Result<String> exportExcel(@RequestBody Map<String, Object> params) {
        try {
            String filePath = statisticsService.exportExcel(params);
            return Result.success("Excel导出成功", filePath);
        } catch (Exception e) {
            log.error("导出Excel失败", e);
            return Result.error("导出Excel失败: " + e.getMessage());
        }
    }

    /**
     * 导出PDF
     * POST /api/statistics/export/pdf
     */
    @PostMapping("/export/pdf")
    public Result<String> exportPdf(@RequestBody Map<String, Object> params) {
        try {
            String filePath = statisticsService.exportPdf(params);
            return Result.success("PDF导出成功", filePath);
        } catch (Exception e) {
            log.error("导出PDF失败", e);
            return Result.error("导出PDF失败: " + e.getMessage());
        }
    }
}
