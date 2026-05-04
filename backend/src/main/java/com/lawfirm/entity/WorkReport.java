package com.lawfirm.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 工作汇报实体
 */
@Entity
@Table(name = "work_report", indexes = {
        @Index(name = "idx_reporter_id", columnList = "reporter_id"),
        @Index(name = "idx_report_date", columnList = "report_date"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkReport extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "report_date", nullable = false)
    private LocalDateTime reportDate;

    @Column(name = "report_type", nullable = false, length = 20)
    private String reportType; // DAILY, WEEKLY, MONTHLY, PROJECT

    @Column(name = "content", length = 1000)
    private String content;

    @Column(name = "work_summary", length = 1000)
    private String workSummary;

    @Column(name = "next_plan", length = 1000)
    private String nextPlan;

    @Column(name = "problems", length = 1000)
    private String problems;

    @Column(name = "suggestions", length = 1000)
    private String suggestions;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(name = "reporter_name", length = 50)
    private String reporterName;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "status", length = 20)
    private String status; // DRAFT, SUBMITTED, REVIEWED, APPROVED, REJECTED

    @Column(name = "reviewer_id")
    private Long reviewerId;

    @Column(name = "reviewer_name", length = 50)
    private String reviewerName;

    @Column(name = "review_comment", length = 500)
    private String reviewComment;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
