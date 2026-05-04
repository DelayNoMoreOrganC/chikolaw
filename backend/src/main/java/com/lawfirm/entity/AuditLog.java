package com.lawfirm.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作审计日志实体
 */
@Data
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "操作人ID不能为空")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotBlank(message = "模块不能为空")
    @Column(nullable = false, length = 50)
    private String module;

    @NotBlank(message = "操作不能为空")
    @Column(nullable = false, length = 50)
    private String operation;

    @Column(length = 100)
    private String method;

    @Lob
    private String params;

    @Column(length = 50)
    private String ip;

    @Column
    private Integer status;

    @Column(name = "error_msg")
    private String errorMsg;

    @Column(name = "execution_time")
    private Integer executionTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
