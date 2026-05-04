package com.lawfirm.controller;

import com.lawfirm.dto.CalendarDTO;
import com.lawfirm.service.CalendarService;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 日程管理控制器
 */
@Slf4j
@RestController
@RequestMapping("calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;
    private final SecurityUtils securityUtils;

    /**
     * 创建日程
     * POST /api/calendar
     */
    @PostMapping
    public Result<CalendarDTO> createCalendar(@Valid @RequestBody CalendarDTO dto) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            CalendarDTO result = calendarService.createCalendar(dto, userId);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.error("创建日程失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("创建日程异常", e);
            return Result.error("创建日程失败");
        }
    }

    /**
     * 更新日程
     * PUT /api/calendar/{id}
     */
    @PutMapping("/{id}")
    public Result<CalendarDTO> updateCalendar(@PathVariable Long id, @Valid @RequestBody CalendarDTO dto) {
        try {
            CalendarDTO result = calendarService.updateCalendar(id, dto);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.error("更新日程失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("更新日程异常", e);
            return Result.error("更新日程失败");
        }
    }

    /**
     * 删除日程
     * DELETE /api/calendar/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteCalendar(@PathVariable Long id) {
        try {
            calendarService.deleteCalendar(id);
            return Result.success();
        } catch (IllegalArgumentException e) {
            log.error("删除日程失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("删除日程异常", e);
            return Result.error("删除日程失败");
        }
    }

    /**
     * 获取日程事件列表（兼容前端调用）
     * GET /api/calendar/events?start={start}&end={end}
     * 必须在 /{id} 路由之前，否则Spring会把"events"当作id参数
     */
    @GetMapping("/events")
    public Result<List<CalendarDTO>> getCalendarEvents(
            @RequestParam String start,
            @RequestParam String end) {
        try {
            // 从JWT token中获取当前用户ID，而不是从X-User-Id header
            Long userId = securityUtils.getCurrentUserId();
            LocalDateTime startTime = LocalDateTime.parse(start + "T00:00:00");
            LocalDateTime endTime = LocalDateTime.parse(end + "T23:59:59");
            List<CalendarDTO> result = calendarService.getCalendarsByDateRange(startTime, endTime, userId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询日程事件异常", e);
            return Result.error("查询日程失败");
        }
    }

    /**
     * 查询日程详情
     * GET /api/calendar/{id}
     */
    @GetMapping("/{id}")
    public Result<CalendarDTO> getCalendar(@PathVariable Long id) {
        try {
            CalendarDTO result = calendarService.getCalendarById(id);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.error("查询日程失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("查询日程异常", e);
            return Result.error("查询日程失败");
        }
    }

    /**
     * 查询用户的所有日程
     * GET /api/calendar/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public Result<List<CalendarDTO>> getCalendarsByUser(@PathVariable Long userId) {
        try {
            List<CalendarDTO> result = calendarService.getCalendarsByUser(userId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询用户日程异常", e);
            return Result.error("查询用户日程失败");
        }
    }

    /**
     * 查询时间范围内的日程
     * GET /api/calendar/range?start={start}&end={end}
     */
    @GetMapping("/range")
    public Result<List<CalendarDTO>> getCalendarsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        try {
            // 从JWT token中获取当前用户ID，而不是从请求参数中获取
            Long userId = securityUtils.getCurrentUserId();
            List<CalendarDTO> result = calendarService.getCalendarsByDateRange(start, end, userId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询时间范围内日程异常", e);
            return Result.error("查询日程失败");
        }
    }

    /**
     * 查询案件的日程
     * GET /api/calendar/case/{caseId}
     */
    @GetMapping("/case/{caseId}")
    public Result<List<CalendarDTO>> getCalendarsByCase(@PathVariable Long caseId) {
        try {
            List<CalendarDTO> result = calendarService.getCalendarsByCase(caseId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询案件日程异常", e);
            return Result.error("查询案件日程失败");
        }
    }

    /**
     * 分页查询日程
     * GET /api/calendar?page={page}&size={size}&userId={userId}
     */
    @GetMapping
    public Result<com.lawfirm.util.PageResult<CalendarDTO>> getCalendars(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long userId) {
        try {
            Long queryUserId = userId != null ? userId : securityUtils.getCurrentUserId();
            com.lawfirm.util.PageResult<CalendarDTO> result = calendarService.getCalendars(page, size, queryUserId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("分页查询日程异常", e);
            return Result.error("分页查询日程失败");
        }
    }
}
