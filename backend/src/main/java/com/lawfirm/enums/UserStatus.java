package com.lawfirm.enums;

/**
 * 用户状态枚举
 */
public enum UserStatus {
    /**
     * 启用
     */
    ACTIVE(1, "启用"),

    /**
     * 禁用
     */
    DISABLED(0, "禁用");

    private final Integer code;
    private final String description;

    UserStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据code获取枚举
     */
    public static UserStatus fromCode(Integer code) {
        if (code == null) {
            return ACTIVE;
        }
        for (UserStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return ACTIVE;
    }

    /**
     * 检查是否启用
     */
    public boolean isActive() {
        return this == ACTIVE;
    }
}
