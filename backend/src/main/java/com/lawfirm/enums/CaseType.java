package com.lawfirm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 案件类型枚举
 */
public enum CaseType {
    CIVIL("民事"),
    COMMERCIAL("商事"),
    ARBITRATION("仲裁"),
    CRIMINAL("刑事"),
    ADMINISTRATIVE("行政"),
    NON_LITIGATION("非诉"),
    ADVISORY("顾问"),
    FINANCIAL_NPA("金融不良资产");

    private final String description;

    CaseType(String description) {
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
