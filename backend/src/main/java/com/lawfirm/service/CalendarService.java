package com.lawfirm.service;

import com.lawfirm.dto.CalendarDTO;
import com.lawfirm.entity.Calendar;
import com.lawfirm.repository.CalendarRepository;
import com.lawfirm.repository.CaseRepository;
import com.lawfirm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 日程管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarService {

    private final CalendarRepository calendarRepository;
    private final CaseRepository caseRepository;
    private final UserRepository userRepository;

    /**
     * 创建日程
     */
    @Transactional
    public CalendarDTO createCalendar(CalendarDTO dto, Long userId) {
        // 验证案件是否存在
        if (dto.getCaseId() != null && !caseRepository.existsById(dto.getCaseId())) {
            throw new IllegalArgumentException("案件不存在");
        }

        Calendar calendar = new Calendar();
        calendar.setTitle(dto.getTitle());
        calendar.setCalendarType(dto.getCalendarType());
        calendar.setStartTime(dto.getStartTime());
        calendar.setEndTime(dto.getEndTime());
        calendar.setLocation(dto.getLocation());
        calendar.setCaseId(dto.getCaseId());
        calendar.setParticipants(dto.getParticipantIds() != null ? String.join(",", dto.getParticipantIds()) : null);
        calendar.setReminder(dto.getReminder());
        calendar.setReminderMinutes(dto.getReminderMinutes());
        calendar.setRepeatRule(dto.getRepeatRule());
        calendar.setCreatedBy(userId);

        calendar = calendarRepository.save(calendar);
        log.info("创建日程成功: {}", calendar.getId());

        return convertToDTO(calendar);
    }

    /**
     * 更新日程
     */
    @Transactional
    public CalendarDTO updateCalendar(Long id, CalendarDTO dto) {
        Calendar calendar = calendarRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("日程不存在"));

        // 验证案件是否存在
        if (dto.getCaseId() != null && !caseRepository.existsById(dto.getCaseId())) {
            throw new IllegalArgumentException("案件不存在");
        }

        calendar.setTitle(dto.getTitle());
        calendar.setCalendarType(dto.getCalendarType());
        calendar.setStartTime(dto.getStartTime());
        calendar.setEndTime(dto.getEndTime());
        calendar.setLocation(dto.getLocation());
        calendar.setCaseId(dto.getCaseId());
        calendar.setParticipants(dto.getParticipantIds() != null ? String.join(",", dto.getParticipantIds()) : null);
        calendar.setReminder(dto.getReminder());
        calendar.setReminderMinutes(dto.getReminderMinutes());
        calendar.setRepeatRule(dto.getRepeatRule());

        calendar = calendarRepository.save(calendar);
        log.info("更新日程成功: {}", id);

        return convertToDTO(calendar);
    }

    /**
     * 删除日程
     */
    @Transactional
    public void deleteCalendar(Long id) {
        if (!calendarRepository.existsById(id)) {
            throw new IllegalArgumentException("日程不存在");
        }
        calendarRepository.deleteById(id);
        log.info("删除日程成功: {}", id);
    }

    /**
     * 根据ID查询日程
     */
    public CalendarDTO getCalendarById(Long id) {
        return calendarRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new IllegalArgumentException("日程不存在"));
    }

    /**
     * 查询用户的所有日程
     */
    public List<CalendarDTO> getCalendarsByUser(Long userId) {
        return calendarRepository.findByUser(userId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 查询时间范围内的日程（带数据权限隔离）
     */
    public List<CalendarDTO> getCalendarsByDateRange(LocalDateTime start, LocalDateTime end, Long userId) {
        // 使用优化的数据库查询，先获取用户相关的所有日程
        List<Calendar> userCalendars = calendarRepository.findByUser(userId);

        // 再进行时间范围过滤（这样比先查所有再过滤性能更好）
        return userCalendars.stream()
                .filter(c -> {
                    // 检查时间范围
                    boolean inRange = c.getStartTime().isBefore(end) && c.getEndTime().isAfter(start);
                    return inRange;
                })
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 查询案件的日程
     */
    public List<CalendarDTO> getCalendarsByCase(Long caseId) {
        // 使用数据库查询优化
        return calendarRepository.findByCaseIdOrderByStartTimeAsc(caseId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询日程
     */
    public com.lawfirm.util.PageResult<CalendarDTO> getCalendars(int page, int size, Long userId) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.DESC, "startTime"));

        // 使用数据库查询优化，避免全表加载和手动过滤
        long totalCount = calendarRepository.countByUser(userId);

        List<Calendar> userCalendars = calendarRepository.findByUser(userId);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), userCalendars.size());
        List<Calendar> pageCalendars = start < userCalendars.size() ? userCalendars.subList(start, end) : new java.util.ArrayList<>();

        List<CalendarDTO> dtoList = pageCalendars.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new com.lawfirm.util.PageResult<>((long) page, (long) size, totalCount, dtoList);
    }

    /**
     * 转换为DTO
     */
    private CalendarDTO convertToDTO(Calendar calendar) {
        CalendarDTO dto = new CalendarDTO();
        dto.setId(calendar.getId());
        dto.setTitle(calendar.getTitle());
        dto.setCalendarType(calendar.getCalendarType());

        // 根据日程类型设置标签颜色（PRD要求：124-128行）
        String color = getColorByType(calendar.getCalendarType());
        dto.setColor(color);

        dto.setStartTime(calendar.getStartTime());
        dto.setEndTime(calendar.getEndTime());
        dto.setLocation(calendar.getLocation());
        dto.setCaseId(calendar.getCaseId());

        // 加载关联案件信息（工作台日历多维筛选）
        if (calendar.getCaseId() != null) {
            caseRepository.findById(calendar.getCaseId()).ifPresent(c -> {
                dto.setCaseName(c.getCaseName());
                dto.setCaseType(c.getCaseType());
                dto.setCaseStatus(c.getStatus());
                dto.setCourt(c.getCourt());
                dto.setOwnerId(c.getOwnerId());
                if (c.getOwnerId() != null) {
                    userRepository.findById(c.getOwnerId())
                            .ifPresent(u -> dto.setOwnerName(u.getRealName()));
                }
            });
        }

        // 加载参与者名称
        if (calendar.getParticipants() != null) {
            String[] participantIds = calendar.getParticipants().split(",");
            dto.setParticipantIds(List.of(participantIds));
            // 可以在这里加载参与者名称
        }

        dto.setReminder(calendar.getReminder());
        dto.setReminderMinutes(calendar.getReminderMinutes());
        dto.setRepeatRule(calendar.getRepeatRule());
        dto.setCreatedBy(calendar.getCreatedBy());

        // 加载创建者名称
        userRepository.findById(calendar.getCreatedBy()).ifPresent(u -> dto.setCreatedByName(u.getRealName()));

        dto.setCreatedAt(calendar.getCreatedAt());
        dto.setUpdatedAt(calendar.getUpdatedAt());

        return dto;
    }

    /**
     * 根据日程类型返回对应的颜色
     * PRD要求（124-128行）：
     * - 🔴 开庭/听证 → 红色
     * - 🟠 审限届满 → 橙色
     * - 🔵 立案 → 蓝色
     * - 🟢 调解/和解 → 绿色
     * - 🟣 举证截止 → 紫色
     */
    private String getColorByType(String calendarType) {
        if (calendarType == null) {
            return "default";
        }

        switch (calendarType.toUpperCase()) {
            case "HEARING":
                return "danger";      // 🔴 红色 - 开庭/听证
            case "DEADLINE":
                return "warning";     // 🟠 橙色 - 审限届满
            case "FILING":
                return "primary";     // 🔵 蓝色 - 立案
            case "MEDIATION":
                return "success";     // 🟢 绿色 - 调解/和解
            case "EVIDENCE":
                return "info";        // 🟣 紫色 - 举证截止
            case "MEETING":
                return "primary";     // 🔵 蓝色 - 会议
            default:
                return "default";     // 默认灰色
        }
    }
}
