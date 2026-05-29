package com.lawfirm.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AI文档识别结果DTO
 * 用于表示从法律文书中提取的关键信息
 */
@Data
public class AIDocumentRecognitionResult {

    /**
     * 案号
     */
    @JsonProperty("caseNumber")
    private String caseNumber;

    /**
     * 法院名称
     */
    @JsonProperty("courtName")
    private String courtName;

    /**
     * 开庭时间 (YYYY-MM-DD HH:mm)
     */
    @JsonProperty("hearingDate")
    private String hearingDate;

    /**
     * 开庭地点/法庭号
     */
    @JsonProperty("hearingPlace")
    private String hearingPlace;

    /**
     * 承办法官姓名
     */
    @JsonProperty("judgeName")
    private String judgeName;

    /**
     * 书记员姓名
     */
    @JsonProperty("clerkName")
    private String clerkName;

    /**
     * 原告姓名/名称
     */
    @JsonProperty("plaintiffName")
    private String plaintiffName;

    /**
     * 被告姓名/名称
     */
    @JsonProperty("defendantName")
    private String defendantName;

    /**
     * 案由
     */
    @JsonProperty("caseReason")
    private String caseReason;

    /**
     * 联系电话
     */
    @JsonProperty("contactPhone")
    private String contactPhone;

    /**
     * 文书类型
     * 类型：传票/判决书/裁定书/通知书/起诉状/答辩状/调解书/其他
     */
    @JsonProperty("documentType")
    private String documentType;

    /**
     * 判决日期 (YYYY-MM-DD)
     * 仅对判决书/裁定书等文书有效
     */
    @JsonProperty("judgmentDate")
    private String judgmentDate;

    /**
     * 上诉期到期日 (YYYY-MM-DD)
     * 系统自动计算，通常为判决日期+15日
     */
    @JsonProperty("appealDeadline")
    private String appealDeadline;

    /**
     * 识别置信度 (0-1)
     */
    private Double confidence;

    /**
     * OCR原始文本
     */
    private String ocrText;

    /**
     * 处理时间（毫秒）
     */
    private Long processingTime;

    /** 是否已尝试执行业务自动化（待办/建案等） */
    private Boolean businessLogicExecuted;

    /** 业务自动化结果摘要（如 todoId、caseId） */
    private java.util.Map<String, Object> businessLogic;
}