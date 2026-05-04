package com.lawfirm.dto;

import javax.validation.constraints.NotBlank;

/**
 * 法律咨询请求DTO
 */
public class LegalChatRequest {

    @NotBlank(message = "问题不能为空")
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
