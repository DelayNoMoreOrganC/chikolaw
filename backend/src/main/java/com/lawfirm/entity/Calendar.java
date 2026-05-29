package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 日程实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "calendar", indexes = {
    @Index(name = "idx_calendar_created_by", columnList = "created_by"),
    @Index(name = "idx_calendar_type", columnList = "calendar_type"),
    @Index(name = "idx_calendar_start_time", columnList = "start_time"),
    @Index(name = "idx_calendar_case", columnList = "case_id"),
    @Index(name = "idx_calendar_deleted", columnList = "deleted")
})
public class Calendar extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "日程标题不能为空")
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank(message = "日程类型不能为空")
    @Column(name = "calendar_type", nullable = false, length = 20)
    private String calendarType;

    @NotNull(message = "开始时间不能为空")
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column
    private String location;

    @Column(name = "case_id")
    private Long caseId;

    private String participants;

    @Column
    private Boolean reminder = false;

    @Column(name = "reminder_minutes")
    private Integer reminderMinutes = 0;

    @Column(name = "repeat_rule", length = 50)
    private String repeatRule;

    @NotNull(message = "创建人不能为空")
    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    /**
     * 案件同步来源：CASE_HEARING / CASE_DEADLINE（与案件 hearingDate/deadlineDate 双向同步）
     */
    @Column(name = "sync_source", length = 32)
    private String syncSource;
}
