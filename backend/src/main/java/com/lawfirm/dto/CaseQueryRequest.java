package com.lawfirm.dto;

import lombok.Data;

/**
 * 案件查询请求DTO
 */
@Data
public class CaseQueryRequest {

    /**
     * 页码（从0开始）
     */
    private Integer page = 0;

    /**
     * 每页大小
     */
    private Integer size = 20;

    /**
     * 案件类型
     */
    private String caseType;

    /**
     * 案件状态
     */
    private String status;

    /**
     * 案件等级
     */
    private String level;

    /**
     * 主办律师ID
     */
    private Long ownerId;

    /**
     * 关联客户ID
     */
    private Long clientId;

    /**
     * 管辖法院（模糊搜索）
     */
    private String court;

    /**
     * 关键字搜索（案件名称、案号、当事人姓名）
     */
    private String keyword;

    /**
     * 标签搜索
     */
    private String tag;

    /**
     * 开始日期（立案时间）
     */
    private String startDate;

    /**
     * 结束日期（立案时间）
     */
    private String endDate;

    /**
     * 是否归档
     */
    private Boolean archived;

    /**
     * 是否已删除
     */
    private Boolean deleted;

    /**
     * 排序字段
     */
    private String sortField = "createdAt";

    /**
     * 排序方向（ASC/DESC）
     */
    private String sortDirection = "DESC";

    /**
     * 当前用户ID（用于数据权限过滤）
     */
    private Long currentUserId;

    /**
     * 快捷筛选：pending_approval（待利冲审批）、pending_intake（待挂接卷宗）
     */
    private String quickFilter;
}
