package com.lawfirm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeArticleInitService {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void initKnowledgeArticleTable() {
        try {
            // 检查表是否已存在
            // 检查表是否已存在（H2数据库表名可能是大写或小写）
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name IN ('KNOWLEDGE_ARTICLE', 'knowledge_article')",
                Integer.class
            );

            if (count != null && count > 0) {
                log.info("KnowledgeArticle table already exists, skipping initialization");
                return;
            }

            log.info("Creating KnowledgeArticle table...");

            // 创建知识库文章表 - 使用引用标识符
            String createTableSql =
                "CREATE TABLE \"knowledge_article\" (" +
                "\"id\" BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "\"title\" VARCHAR(200) NOT NULL," +
                "\"article_type\" VARCHAR(20) NOT NULL," +
                "\"category\" VARCHAR(50)," +
                "\"content\" TEXT," +
                "\"tags\" VARCHAR(500)," +
                "\"created_at\" DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "\"updated_at\" DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "\"deleted\" TINYINT DEFAULT 0" +
                ")";

            jdbcTemplate.execute(createTableSql);
            log.info("KnowledgeArticle table created successfully");

            // 插入测试数据
            String insertSql =
                "INSERT INTO \"knowledge_article\" (\"title\", \"article_type\", \"category\", \"content\", \"tags\") VALUES " +
                "('劳动仲裁申请流程', 'guide', '劳动法', '劳动仲裁申请包括：1.提交申请书 2.提交证据 3.开庭审理 4.裁决', '劳动,仲裁,申请')," +
                "('合同违约责任认定', 'guide', '合同法', '合同违约责任包括：实际履行、赔偿损失、支付违约金等', '合同,违约,责任')," +
                "('刑事案件辩护要点', 'guide', '刑法', '刑事案件辩护要点：1.事实认定 2.法律适用 3.程序合法性', '刑事,辩护,案件')";

            jdbcTemplate.update(insertSql);
            log.info("KnowledgeArticle test data inserted successfully");

        } catch (Exception e) {
            log.error("Failed to initialize KnowledgeArticle table", e);
        }
    }
}
