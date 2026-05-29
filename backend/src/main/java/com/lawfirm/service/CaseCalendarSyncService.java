package com.lawfirm.service;

import com.lawfirm.entity.Calendar;
import com.lawfirm.entity.Case;
import com.lawfirm.repository.CalendarRepository;
import com.lawfirm.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 案件开庭/审限日期与日程双向同步。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseCalendarSyncService {

    public static final String SYNC_HEARING = "CASE_HEARING";
    public static final String SYNC_DEADLINE = "CASE_DEADLINE";

    private static final ThreadLocal<Boolean> SUPPRESS = ThreadLocal.withInitial(() -> false);

    private final CalendarRepository calendarRepository;
    private final CaseRepository caseRepository;

    public static boolean isSuppressed() {
        return Boolean.TRUE.equals(SUPPRESS.get());
    }

    public static void runSuppressed(Runnable action) {
        SUPPRESS.set(true);
        try {
            action.run();
        } finally {
            SUPPRESS.remove();
        }
    }

    /**
     * 案件保存后：将 hearingDate / deadlineDate 同步到日程。
     */
    @Transactional(rollbackFor = Exception.class)
    public void syncFromCase(Case caseEntity, Long operatorUserId) {
        if (caseEntity == null || caseEntity.getId() == null || isSuppressed()) {
            return;
        }
        Long userId = operatorUserId != null ? operatorUserId : caseEntity.getOwnerId();
        if (userId == null) {
            return;
        }
        upsertFromCaseDate(caseEntity, caseEntity.getHearingDate(), SYNC_HEARING, "HEARING",
                buildHearingTitle(caseEntity), userId);
        upsertFromCaseDate(caseEntity, caseEntity.getDeadlineDate(), SYNC_DEADLINE, "DEADLINE",
                buildDeadlineTitle(caseEntity), userId);
    }

    /**
     * 日程变更后：将 HEARING/DEADLINE 写回案件日期。
     */
    @Transactional(rollbackFor = Exception.class)
    public void syncFromCalendar(Calendar calendar) {
        if (calendar == null || calendar.getCaseId() == null || isSuppressed()) {
            return;
        }
        String type = normalizeType(calendar.getCalendarType());
        if (!"HEARING".equals(type) && !"DEADLINE".equals(type)) {
            return;
        }
        Case caseEntity = caseRepository.findById(calendar.getCaseId()).orElse(null);
        if (caseEntity == null) {
            return;
        }
        LocalDate eventDate = calendar.getStartTime() != null
                ? calendar.getStartTime().toLocalDate()
                : null;

        runSuppressed(() -> {
            if ("HEARING".equals(type)) {
                caseEntity.setHearingDate(eventDate);
                if (calendar.getSyncSource() == null) {
                    calendar.setSyncSource(SYNC_HEARING);
                    calendarRepository.save(calendar);
                }
            } else {
                caseEntity.setDeadlineDate(eventDate);
                if (calendar.getSyncSource() == null) {
                    calendar.setSyncSource(SYNC_DEADLINE);
                    calendarRepository.save(calendar);
                }
            }
            caseRepository.save(caseEntity);
            log.debug("日程→案件同步: caseId={}, type={}, date={}", caseEntity.getId(), type, eventDate);
        });
    }

    /**
     * 删除日程后：若为案件同步日程，清空对应案件日期。
     */
    @Transactional(rollbackFor = Exception.class)
    public void onCalendarDeleted(Calendar calendar) {
        if (calendar == null || calendar.getCaseId() == null || isSuppressed()) {
            return;
        }
        String resolvedSync = calendar.getSyncSource();
        if (resolvedSync == null) {
            String type = normalizeType(calendar.getCalendarType());
            if ("HEARING".equals(type)) {
                resolvedSync = SYNC_HEARING;
            } else if ("DEADLINE".equals(type)) {
                resolvedSync = SYNC_DEADLINE;
            } else {
                return;
            }
        }
        final String syncSource = resolvedSync;
        Case caseEntity = caseRepository.findById(calendar.getCaseId()).orElse(null);
        if (caseEntity == null) {
            return;
        }
        LocalDate removed = calendar.getStartTime() != null
                ? calendar.getStartTime().toLocalDate()
                : null;

        runSuppressed(() -> {
            if (SYNC_HEARING.equals(syncSource) && datesEqual(caseEntity.getHearingDate(), removed)) {
                caseEntity.setHearingDate(null);
            } else if (SYNC_DEADLINE.equals(syncSource) && datesEqual(caseEntity.getDeadlineDate(), removed)) {
                caseEntity.setDeadlineDate(null);
            }
            caseRepository.save(caseEntity);
        });
    }

    private void upsertFromCaseDate(Case caseEntity, LocalDate date, String syncSource,
                                    String calendarType, String title, Long userId) {
        Long caseId = caseEntity.getId();
        if (date == null) {
            calendarRepository.findFirstByCaseIdAndSyncSourceAndDeletedFalse(caseId, syncSource)
                    .ifPresent(cal -> {
                        cal.setDeleted(true);
                        calendarRepository.save(cal);
                    });
            return;
        }

        LocalDateTime start = date.atTime(9, 0);
        LocalDateTime end = date.atTime(18, 0);

        Calendar calendar = calendarRepository
                .findFirstByCaseIdAndSyncSourceAndDeletedFalse(caseId, syncSource)
                .orElseGet(Calendar::new);

        calendar.setTitle(title);
        calendar.setCalendarType(calendarType);
        calendar.setStartTime(start);
        calendar.setEndTime(end);
        calendar.setCaseId(caseId);
        calendar.setSyncSource(syncSource);
        calendar.setReminder(true);
        calendar.setReminderMinutes(1440);
        if (calendar.getCreatedBy() == null) {
            calendar.setCreatedBy(userId);
        }
        calendar.setDeleted(false);
        calendarRepository.save(calendar);
    }

    private static String buildHearingTitle(Case c) {
        String name = c.getCaseName() != null ? c.getCaseName() : c.getCaseNumber();
        return "开庭 - " + (name != null ? name : "案件");
    }

    private static String buildDeadlineTitle(Case c) {
        String name = c.getCaseName() != null ? c.getCaseName() : c.getCaseNumber();
        return "审限届满 - " + (name != null ? name : "案件");
    }

    private static String normalizeType(String calendarType) {
        return calendarType != null ? calendarType.trim().toUpperCase() : "";
    }

    private static boolean datesEqual(LocalDate a, LocalDate b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }
}
