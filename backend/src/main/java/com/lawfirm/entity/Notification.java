package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 站内通知实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "notification")
public class Notification extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "接收人ID不能为空")
    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @NotBlank(message = "通知标题不能为空")
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank(message = "通知内容不能为空")
    @Column(nullable = false)
    private String content;

    @Column(length = 50)
    private String category;

    /** 聚合分类：TODO / CASE / CALENDAR / APPROVAL / SYSTEM */
    @Column(name = "category_group", length = 20)
    private String categoryGroup;

    @Column(name = "related_id")
    private Long relatedId;

    @Column(length = 50)
    private String relatedType;

    @Column(nullable = false)
    private Boolean isRead = false;

    @Column(name = "read_time")
    private java.time.LocalDateTime readTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();
}
