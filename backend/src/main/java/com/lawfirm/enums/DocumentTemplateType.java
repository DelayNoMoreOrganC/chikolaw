package com.lawfirm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 文书模板类型枚举
 */
public enum DocumentTemplateType {
    COMPLAINT("起诉状"),
    DEFENSE_STATEMENT("答辩状"),
    BRIEF("代理词"),
    LEGAL_OPINION("法律意见书"),
    /** 兼容旧版 DocGenerateService（LEGACY_DOCUMENT 路由） */
    LAWYER_LETTER("律师函");

    private final String description;

    DocumentTemplateType(String description) {
        this.description = description;
    }

    @JsonValue
    public String getDescription() {
        return description;
    }

    public String getCode() {
        return this.name();
    }
}
