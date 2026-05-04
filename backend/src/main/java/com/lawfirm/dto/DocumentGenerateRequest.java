package com.lawfirm.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * AI文书生成请求DTO
 * 支持起诉状、答辩状、代理词、法律意见书等文书类型生成
 */
@Data
public class DocumentGenerateRequest {

    /**
     * 案件ID（必填）
     */
    @NotNull(message = "案件ID不能为空")
    private Long caseId;

    /**
     * 文书类型（必填）
     * 支持类型：
     * - COMPLAINT: 起诉状
     * - DEFENSE_STATEMENT: 答辩状
     * - BRIEF: 代理词
     * - LEGAL_OPINION: 法律意见书
     */
    @NotBlank(message = "文书类型不能为空")
    private String documentType;

    /**
     * 原告信息（起诉状必填）
     */
    private PartyInfo plaintiff;

    /**
     * 被告信息（起诉状、答辩状必填）
     */
    private PartyInfo defendant;

    /**
     * 第三人信息（可选）
     */
    private PartyInfo thirdParty;

    /**
     * 诉讼请求（起诉状必填）
     */
    private String claims;

    /**
     * 事实与理由（可选）
     */
    private String factsAndReasons;

    /**
     * 证据清单（可选）
     */
    private String evidenceList;

    /**
     * 答辩意见（答辩状必填）
     */
    private String defenseOpinion;

    /**
     * 代理意见要点（代理词必填）
     */
    private String briefPoints;

    /**
     * 咨询问题（法律意见书必填）
     */
    private String consultationQuestions;

    /**
     * 补充信息（可选）
     */
    private String additionalContext;

    /**
     * 自定义Prompt（可选，用于特殊需求）
     */
    private String customPrompt;

    /**
     * 当事人信息内部类
     */
    @Data
    public static class PartyInfo {
        /**
         * 名称（个人姓名或公司名称）
         */
        private String name;

        /**
         * 类型（PERSON/COMPANY）
         */
        private String type;

        /**
         * 性别（个人必填）
         */
        private String gender;

        /**
         * 出生日期（个人）
         */
        private String birthDate;

        /**
         * 民族（个人）
         */
        private String nationality;

        /**
         * 身份证号（个人）
         */
        private String idCard;

        /**
         * 住所地
         */
        private String address;

        /**
         * 联系电话
         */
        private String phone;

        /**
         * 法定代表人（公司）
         */
        private String legalRepresentative;

        /**
         * 统一社会信用代码（公司）
         */
        private String creditCode;
    }
}
