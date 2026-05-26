package com.lawfirm.controller;

import com.lawfirm.dto.NotificationSummaryDTO;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.service.NotificationService;
import com.lawfirm.util.PageResult;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    /**
     * GET /api/notification?page=1&size=20&categoryGroup=APPROVAL
     */
    @GetMapping
    public Result<PageResult<Map<String, Object>>> getNotificationList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String categoryGroup) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            return Result.success(notificationService.getNotificationList(userId, page, size, categoryGroup));
        } catch (Exception e) {
            log.error("获取通知列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/unread")
    public Result<List<Map<String, Object>>> getUnreadNotifications(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            return Result.success(notificationService.getUnreadNotifications(userId, limit));
        } catch (Exception e) {
            log.error("获取未读通知失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount() {
        try {
            Long userId = securityUtils.getCurrentUserId();
            return Result.success(notificationService.getUnreadCount(userId));
        } catch (Exception e) {
            log.error("获取未读数量失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/summary")
    public Result<NotificationSummaryDTO> getSummary() {
        try {
            Long userId = securityUtils.getCurrentUserId();
            return Result.success(notificationService.getSummary(userId));
        } catch (Exception e) {
            log.error("获取通知摘要失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            notificationService.markAsRead(userId, id);
            return Result.success();
        } catch (Exception e) {
            log.error("标记已读失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/read-all")
    public Result<Void> markAllAsRead() {
        try {
            Long userId = securityUtils.getCurrentUserId();
            notificationService.markAllAsRead(userId);
            return Result.success();
        } catch (Exception e) {
            log.error("全部标记已读失败", e);
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteNotification(@PathVariable Long id) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            notificationService.deleteNotification(userId, id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除通知失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/categories")
    public Result<List<Map<String, String>>> getCategories() {
        try {
            return Result.success(notificationService.getCategories());
        } catch (Exception e) {
            log.error("获取通知分类失败", e);
            return Result.error(e.getMessage());
        }
    }
}
