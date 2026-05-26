package com.lawfirm.service;

import com.lawfirm.dto.TodoDTO;
import com.lawfirm.entity.Case;
import com.lawfirm.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 审限提醒服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeadlineReminderService {

    private final CaseRepository caseRepository;
    private final TodoService todoService;
    private final CaseTimelineService caseTimelineService;
    private final NotificationService notificationService;

    /**
     * 每天早上8点执行审限检查
     * cron表达式: 秒 分 时 日 月 周
     * 0 0 8 * * ? = 每天早上8点执行
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void checkDeadlines() {
        log.info("开始执行审限提醒检查...");
        int reminderCount = 0;

        try {
            // 检查3天内到期的案件
            reminderCount += checkDeadlinesForDays(3, "3天后到期");

            // 检查1天内到期的案件
            reminderCount += checkDeadlinesForDays(1, "明天到期");

            // 检查今天到期的案件
            reminderCount += checkDeadlinesForDays(0, "今天到期");

            log.info("审限提醒检查完成，共生成 {} 个提醒", reminderCount);

        } catch (Exception e) {
            log.error("审限提醒检查失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 检查指定天数内到期的案件
     */
    @Transactional(rollbackFor = Exception.class)
    public int checkDeadlinesForDays(int days, String description) {
        LocalDate targetDate = LocalDate.now().plusDays(days);
        int count = 0;

        try {
            // 查询指定日期到期的进行中案件
            List<Case> cases = caseRepository.findByDeadlineDateAndStatusAndDeletedFalse(
                    targetDate,
                    "ACTIVE"
            );

            log.info("找到 {} 个{}的案件", cases.size(), description);

            for (Case caseEntity : cases) {
                try {
                    // 检查是否已经生成过今天的提醒
                    if (!hasReminderToday(caseEntity.getId(), days)) {
                        // 创建提醒待办
                        TodoDTO todoDTO = new TodoDTO();
                        todoDTO.setTitle("【审限提醒】" + description + " - " + caseEntity.getCaseName());
                        todoDTO.setDescription("案件 " + caseEntity.getCaseName() +
                                "（案号：" + caseEntity.getCaseNumber() + "）" + description +
                                "，请及时处理。审限日期：" + caseEntity.getDeadlineDate());
                        todoDTO.setPriority(days == 0 ? "HIGH" : (days == 1 ? "MEDIUM" : "LOW"));
                        todoDTO.setCaseId(caseEntity.getId());
                        todoDTO.setAssigneeId(caseEntity.getOwnerId());
                        todoDTO.setStatus("PENDING");

                        // 设置截止时间为当天晚上11点
                        LocalDateTime dueDate = LocalDateTime.now()
                                .withHour(23)
                                .withMinute(0)
                                .withSecond(0);
                        todoDTO.setDueDate(dueDate);

                        // 创建待办
                        todoService.createTodo(todoDTO, caseEntity.getOwnerId());
                        count++;

                        // 同步推送通知中心（PRD 审限提醒统一管道）
                        if (caseEntity.getOwnerId() != null && caseEntity.getDeadlineDate() != null) {
                            notificationService.sendCaseDeadlineNotification(
                                    caseEntity.getId(),
                                    caseEntity.getCaseName(),
                                    caseEntity.getDeadlineDate().toString(),
                                    caseEntity.getOwnerId());
                            notificationService.sendNotification(
                                    caseEntity.getOwnerId(),
                                    "【审限提醒】" + description,
                                    todoDTO.getDescription(),
                                    "DEADLINE",
                                    caseEntity.getId(),
                                    "Case");
                        }

                        // 记录到案件动态
                        caseTimelineService.createSystemTimeline(
                                caseEntity.getId(),
                                "DEADLINE_REMINDER",
                                "系统自动生成审限提醒：" + description
                        );

                        log.info("为案件 {} 生成审限提醒", caseEntity.getCaseNumber());

                    } else {
                        log.debug("案件 {} 今天已经生成过提醒，跳过", caseEntity.getCaseNumber());
                    }

                } catch (Exception e) {
                    log.error("为案件 {} 生成审限提醒失败: {}", caseEntity.getCaseNumber(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("检查{}案件失败: {}", description, e.getMessage());
        }

        return count;
    }

    /**
     * 检查今天是否已经生成过指定类型的提醒
     * 简化实现：检查今天是否有相关待办
     */
    private boolean hasReminderToday(Long caseId, int days) {
        try {
            // 查询今天是否有相关提醒待办
            LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            LocalDateTime todayEnd = todayStart.plusDays(1);

            // 这里简化实现，实际应该在TodoRepository中添加查询方法
            // 暂时返回false，允许生成提醒
            return false;

        } catch (Exception e) {
            log.error("检查提醒记录失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 手动触发审限检查（用于测试）
     */
    public void manualCheck() {
        log.info("手动触发审限提醒检查...");
        checkDeadlines();
    }
}