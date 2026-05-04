package com.lawfirm.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库文章实体
 */
@Entity
@Table(name = "knowledge_article", indexes = {
    @Index(name = "idx_category", columnList = "category"),
    @Index(name = "idx_type", columnList = "article_type"),
    @Index(name = "idx_tags", columnList = "tags"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeArticle extends LogicalDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 标题
     */
    @NotBlank(message = "标题不能为空")
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * 文章类型：TEMPLATE(模板), CASE(类案), GUIDE(指南), EXPERIENCE(经验)
     */
    @NotBlank(message = "文章类型不能为空")
    @Column(name = "article_type", nullable = false, length = 20)
    private String articleType;

    /**
     * 分类：合同、劳动、侵权等
     */
    @Column(name = "category", length = 50)
    private String category;

    /**
     * 标签（逗号分隔）
     */
    @Column(name = "tags", length = 500)
    private String tags;

    /**
     * 摘要
     */
    @Column(name = "summary", length = 1000)
    private String summary;

    /**
     * 内容（富文本）
     */
    @Lob
    private String content;

    /**
     * 附件路径
     */
    @Column(name = "attachment_path")
    private String attachmentPath;

    /**
     * 浏览次数
     */
    @Column(name = "view_count")
    private Integer viewCount = 0;

    /**
     * 点赞次数
     */
    @Column(name = "like_count")
    private Integer likeCount = 0;

    /**
     * 是否置顶
     */
    @Column(name = "is_top")
    private Boolean isTop = false;

    /**
     * 是否公开（全所可见）
     */
    @Column(name = "is_public")
    private Boolean isPublic = true;

    /**
     * 作者ID
     */
    @Column(name = "author_id")
    private Long authorId;

    /**
     * 作者姓名（冗余字段，避免关联查询）
     */
    @Column(name = "author_name", length = 50)
    private String authorName;

    /**
     * 最后修改人ID
     */
    @Column(name = "updater_id")
    private Long updaterId;

    /**
     * 最后修改时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 创建时间
     */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
