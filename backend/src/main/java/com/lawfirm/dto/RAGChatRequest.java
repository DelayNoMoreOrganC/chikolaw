package com.lawfirm.dto;

import javax.validation.constraints.NotBlank;

/**
 * RAG聊天请求DTO
 */
public class RAGChatRequest {

    @NotBlank(message = "问题不能为空")
    private String question;

    private Integer topK = 5;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }
}
