package com.lawfirm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 角色编码枚举
 */
public enum RoleCode {
    ADMIN("系统管理员"),
    DIRECTOR("主任"),
    MANAGER("主任"),
    LAWYER_MAIN("主办律师"),
    LAWYER_ASSIST("协办律师"),
    ASSISTANT("律师助理"),
    FINANCE("财务"),
    GUEST("访客");

    private final String description;

    RoleCode(String description) {
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
