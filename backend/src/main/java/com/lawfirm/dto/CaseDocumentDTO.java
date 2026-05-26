package com.lawfirm.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 案件文档DTO
 */
@Data
public class CaseDocumentDTO {

    private Long id;
    private Long caseId;
    private String documentName;
    private String documentType;
    private String filePath;
    private Long fileSize;
    private String folderPath;
    private Long uploadBy;
    private String uploadByName;
    private String tags;
    private String ocrResult;
    private Integer versionNo;
    private String contentType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 文件类型枚举
    public enum DocumentType {
       起诉状("起诉状"),
        答辩状("答辩状"),
        代理词("代理词"),
        原告证据("原告证据"),
        被告证据("被告证据"),
        法院文书("法院文书"),
        判决书("判决书"),
        其他("其他");

        private final String displayName;

        DocumentType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
