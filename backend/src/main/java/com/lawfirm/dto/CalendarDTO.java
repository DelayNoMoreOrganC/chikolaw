package com.lawfirm.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 日程DTO
 */
@Data
public class CalendarDTO {

    private Long id;

    @NotBlank(message = "日程标题不能为空")
    private String title;

    @NotBlank(message = "日程类型不能为空")
    private String calendarType;

    /**
     * 标签颜色（根据PRD要求：hearing→红, deadline→橙, filing→蓝, mediation→绿, evidence→紫）
     */
    private String color;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    private String location;

    private Long caseId;

    private String caseName;

    /** 关联案件类型（筛选：工作台日历） */
    private String caseType;

    /** 关联案件状态 */
    private String caseStatus;

    /** 管辖法院 */
    private String court;

    /** 主办律师 ID / 姓名 */
    private Long ownerId;

    private String ownerName;

    private List<String> participantIds;

    private List<String> participantNames;

    private Boolean reminder = false;

    private Integer reminderMinutes = 0;

    private String repeatRule;

    private Long createdBy;

    private String createdByName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
