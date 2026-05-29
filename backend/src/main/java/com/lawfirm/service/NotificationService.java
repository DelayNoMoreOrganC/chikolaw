package com.lawfirm.service;

import com.lawfirm.dto.NotificationSummaryDTO;
import com.lawfirm.entity.Notification;
import com.lawfirm.repository.NotificationRepository;
import com.lawfirm.util.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 站内通知：审限、待办逾期、审批待办/催办等统一入口
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    public static final String GROUP_TODO = "TODO";
    public static final String GROUP_CASE = "CASE";
    public static final String GROUP_CALENDAR = "CALENDAR";
    public static final String GROUP_APPROVAL = "APPROVAL";
    public static final String GROUP_SYSTEM = "SYSTEM";

    public static final String CATEGORY_TODO = "待办";
    public static final String CATEGORY_CASE = "案件";
    public static final String CATEGORY_CALENDAR = "日程";
    public static final String CATEGORY_APPROVAL = "审批";
    public static final String CATEGORY_SYSTEM = "系统";

    private final NotificationRepository notificationRepository;
    private final CacheManager cacheManager;

    @Transactional
    public void sendTodoOverdueNotification(Long todoId, String title, String dueDate, Long userId) {
        if (userId == null) {
            return;
        }
        save(userId, "待办逾期预警",
                String.format("待办事项「%s」将于%s到期，请及时处理", title, dueDate),
                "TODO_OVERDUE", GROUP_TODO, todoId, "Todo");
    }

    @Transactional
    public void sendCaseDeadlineNotification(Long caseId, String caseName, String deadline, Long lawyerId) {
        if (lawyerId == null) {
            return;
        }
        save(lawyerId, "案件审限预警",
                String.format("案件「%s」的审限截止日期为%s，请关注", caseName, deadline),
                "CASE_DEADLINE", GROUP_CASE, caseId, "Case");
    }

    @Transactional
    public void sendUrgentTodoCountNotification(long count) {
        log.warn("当前有{}个待办将在3天内到期，请及时处理", count);
    }

    @Transactional
    public void sendNotification(Long userId, String title, String content, String category,
                                 Long relatedId, String relatedType) {
        String group = resolveGroupFromCategory(category);
        save(userId, title, content, category, group, relatedId, relatedType);
    }

    @Transactional
    public void sendApprovalPendingNotification(Long approverId, Long approvalId, String title) {
        if (approverId == null) {
            return;
        }
        save(approverId, "待您审批",
                String.format("审批单「%s」待您处理", title),
                CATEGORY_APPROVAL, GROUP_APPROVAL, approvalId, "Approval");
    }

    @Transactional
    public void sendApprovalResultNotification(Long applicantId, Long approvalId, String title, boolean approved) {
        if (applicantId == null) {
            return;
        }
        String status = approved ? "已通过" : "已驳回";
        save(applicantId, "审批" + status,
                String.format("您发起的「%s」%s", title, status),
                CATEGORY_APPROVAL, GROUP_APPROVAL, approvalId, "Approval");
    }

    /** 立案审批通过且已生成草稿案件时通知申请人完善草稿 */
    @Transactional
    public void sendCaseFilingDraftReadyNotification(Long applicantId, Long approvalId, String title,
                                                     Long draftCaseId, boolean intakeAttached) {
        if (applicantId == null || draftCaseId == null) {
            return;
        }
        String attachHint = intakeAttached ? "卷宗已归入草稿案件。" : "卷宗挂接未完成，请在案件卷宗中手动补传。";
        save(applicantId, "立案草稿已生成",
                String.format("「%s」已通过，系统已创建待立案草稿，%s请完善当事人等信息后正式立案。", title, attachHint),
                CATEGORY_APPROVAL, GROUP_APPROVAL, draftCaseId, "CaseDraft");
    }

    private void save(Long userId, String title, String content, String category, String categoryGroup,
                      Long relatedId, String relatedType) {
        Notification notification = new Notification();
        notification.setReceiverId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setCategory(category);
        notification.setCategoryGroup(categoryGroup);
        notification.setRelatedId(relatedId);
        notification.setRelatedType(relatedType);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
        evictUnreadCache(userId);
        log.info("通知已发送：userId={}, group={}, title={}", userId, categoryGroup, title);
    }

    private void evictUnreadCache(Long userId) {
        if (userId == null || cacheManager == null) {
            return;
        }
        var cache = cacheManager.getCache("notificationUnread");
        if (cache != null) {
            cache.evict(userId);
        }
    }

    @Transactional(readOnly = true)
    public PageResult<Map<String, Object>> getNotificationList(Long userId, int page, int size, String categoryGroup) {
        int pageIndex = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageIndex, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Notification> notificationPage;
        if (StringUtils.hasText(categoryGroup)) {
            notificationPage = notificationRepository
                    .findByReceiverIdAndCategoryGroupOrderByCreatedAtDesc(userId, categoryGroup, pageable);
        } else {
            notificationPage = notificationRepository.findByReceiverIdOrderByCreatedAtDesc(userId, pageable);
        }

        List<Map<String, Object>> records = notificationPage.getContent().stream()
                .map(this::toMap)
                .collect(Collectors.toList());

        return new PageResult<>(
                (long) page,
                (long) size,
                notificationPage.getTotalElements(),
                records
        );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUnreadNotifications(Long userId, int limit) {
        return notificationRepository.findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(userId).stream()
                .limit(Math.max(1, limit))
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "notificationUnread", key = "#userId")
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countByReceiverIdAndIsReadFalse(userId);
    }

    @Transactional(readOnly = true)
    public NotificationSummaryDTO getSummary(Long userId) {
        NotificationSummaryDTO summary = new NotificationSummaryDTO();
        summary.setUnreadCount(notificationRepository.countByReceiverIdAndIsReadFalse(userId));

        Map<String, Long> byGroup = new LinkedHashMap<>();
        for (String group : List.of(GROUP_TODO, GROUP_CASE, GROUP_CALENDAR, GROUP_APPROVAL, GROUP_SYSTEM)) {
            long c = notificationRepository.countByReceiverIdAndCategoryGroupAndIsReadFalse(userId, group);
            if (c > 0) {
                byGroup.put(group, c);
            }
        }
        summary.setUnreadByGroup(byGroup);
        return summary;
    }

    @Transactional
    @CacheEvict(value = "notificationUnread", key = "#userId")
    public void markAsRead(Long userId, Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            if (notification.getReceiverId().equals(userId)) {
                notification.setIsRead(true);
                notification.setReadTime(LocalDateTime.now());
                notificationRepository.save(notification);
            }
        });
    }

    @Transactional
    @CacheEvict(value = "notificationUnread", key = "#userId")
    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository.findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        LocalDateTime now = LocalDateTime.now();
        for (Notification n : unread) {
            n.setIsRead(true);
            n.setReadTime(now);
        }
        notificationRepository.saveAll(unread);
    }

    @Transactional
    @CacheEvict(value = "notificationUnread", key = "#userId")
    public void deleteNotification(Long userId, Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            if (notification.getReceiverId().equals(userId)) {
                notificationRepository.delete(notification);
            }
        });
    }

    public List<Map<String, String>> getCategories() {
        return List.of(
                mapCategory(GROUP_TODO, "待办提醒"),
                mapCategory(GROUP_CASE, "案件/审限"),
                mapCategory(GROUP_CALENDAR, "日程"),
                mapCategory(GROUP_APPROVAL, "审批"),
                mapCategory(GROUP_SYSTEM, "系统")
        );
    }

    private Map<String, String> mapCategory(String value, String label) {
        Map<String, String> m = new HashMap<>();
        m.put("value", value);
        m.put("label", label);
        return m;
    }

    private Map<String, Object> toMap(Notification n) {
        String group = n.getCategoryGroup() != null ? n.getCategoryGroup() : resolveGroupFromCategory(n.getCategory());
        Map<String, Object> map = new HashMap<>();
        map.put("id", n.getId());
        map.put("receiverId", n.getReceiverId());
        map.put("title", n.getTitle());
        map.put("content", n.getContent());
        map.put("category", n.getCategory());
        map.put("categoryGroup", group);
        map.put("categoryLabel", labelForGroup(group));
        map.put("relatedId", n.getRelatedId());
        map.put("relatedType", n.getRelatedType());
        map.put("routePath", resolveRoutePath(n));
        map.put("isRead", n.getIsRead());
        map.put("readTime", n.getReadTime());
        map.put("createdAt", n.getCreatedAt());
        return map;
    }

    private String resolveGroupFromCategory(String category) {
        if (category == null) {
            return GROUP_SYSTEM;
        }
        if (category.startsWith("TODO") || CATEGORY_TODO.equals(category)) {
            return GROUP_TODO;
        }
        if (category.startsWith("CASE") || category.contains("DEADLINE") || CATEGORY_CASE.equals(category)) {
            return GROUP_CASE;
        }
        if (category.startsWith("CALENDAR") || CATEGORY_CALENDAR.equals(category)) {
            return GROUP_CALENDAR;
        }
        if (category.contains("APPROVAL") || CATEGORY_APPROVAL.equals(category) || "审批".equals(category)) {
            return GROUP_APPROVAL;
        }
        return GROUP_SYSTEM;
    }

    private String labelForGroup(String group) {
        switch (group) {
            case GROUP_TODO:
                return "待办";
            case GROUP_CASE:
                return "案件";
            case GROUP_CALENDAR:
                return "日程";
            case GROUP_APPROVAL:
                return "审批";
            default:
                return "系统";
        }
    }

    private String resolveRoutePath(Notification n) {
        if (n.getRelatedId() == null) {
            return "/dashboard";
        }
        String type = n.getRelatedType() != null ? n.getRelatedType() : "";
        switch (type) {
            case "Case":
                return "/case/" + n.getRelatedId();
            case "CaseDraft":
                return "/case/" + n.getRelatedId() + "/edit";
            case "Todo":
                return "/calendar";
            case "Approval":
            case "APPROVAL_URGE":
                return "/approval";
            default:
                if (GROUP_CASE.equals(n.getCategoryGroup())) {
                    return "/case/" + n.getRelatedId();
                }
                if (GROUP_APPROVAL.equals(n.getCategoryGroup())) {
                    return "/approval";
                }
                return "/dashboard";
        }
    }
}
