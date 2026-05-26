package com.lawfirm.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

/**
 * 卷宗录入未匹配案件时的暂存文件（待关联案件或立案审批通过后挂接）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "case_intake_pending")
public class CaseIntakePending extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "stored_path", nullable = false)
    private String storedPath;

    @Column(name = "content_type", length = 128)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    /** 识别结果 JSON */
    @Column(name = "recognition_json", columnDefinition = "CLOB")
    private String recognitionJson;

    @Column(length = 500)
    private String remark;

    /** PENDING | ATTACHED | CANCELLED */
    @Column(length = 20, nullable = false)
    private String status = "PENDING";

    @Column(name = "case_id")
    private Long caseId;

    @Column(name = "approval_id")
    private Long approvalId;
}
