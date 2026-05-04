package com.lawfirm.dto;

import lombok.Data;
import java.util.List;

/**
 * 利益冲突审查结果
 */
@Data
public class ConflictCheckResult {

    /**
     * 是否有冲突
     */
    private Boolean hasConflict;

    /**
     * 冲突类型：CLIENT_NAME（客户名称冲突）、PARTY_CONFLICT（当事人冲突）、HIGH_SIMILARITY（高度相似）
     */
    private List<String> conflictTypes;

    /**
     * 冲突详情列表
     */
    private List<ConflictDetail> conflicts;

    /**
     * 建议：APPLY_FOR_WAIVER（申请豁免）、MODIFY_INFO（修改信息）、NO_ACTION（无冲突）
     */
    private String recommendation;

    /**
     * 冲突详情
     */
    @Data
    public static class ConflictDetail {
        /**
         * 冲突类型
         */
        private String type;

        /**
         * 冲突描述
         */
        private String description;

        /**
         * 冲突的案件/客户ID
         */
        private Long relatedId;

        /**
         * 冲突的案件/客户名称
         */
        private String relatedName;

        /**
         * 严重程度：HIGH（高）、MEDIUM（中）、LOW（低）
         */
        private String severity;
    }
}
