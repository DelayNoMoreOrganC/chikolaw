package com.lawfirm.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class TextToDocxRequest {

    @NotBlank(message = "文书内容不能为空")
    private String content;

    private String title;

    private String fileName;
}
