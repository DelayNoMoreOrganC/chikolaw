package com.lawfirm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 工作汇报表初始化服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkReportInitService {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void initWorkReportTable() {
        try {
            // 检查表是否已存在（H2数据库表名可能是大写或小写）
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name IN ('WORK_REPORT', 'work_report')",
                Integer.class
            );

            if (count != null && count > 0) {
                log.info("WorkReport table already exists, skipping initialization");
                return;
            }

            log.info("Creating WorkReport table with quoted identifiers...");

            // 创建工作汇报表 - 使用引用标识符以兼容 Hibernate
            String createTableSql =
                "CREATE TABLE \"work_report\" (" +
                "\"id\" BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "\"title\" VARCHAR(200) NOT NULL," +
                "\"report_date\" DATETIME NOT NULL," +
                "\"report_type\" VARCHAR(20) NOT NULL," +
                "\"content\" TEXT," +
                "\"work_summary\" VARCHAR(1000)," +
                "\"next_plan\" VARCHAR(1000)," +
                "\"problems\" VARCHAR(1000)," +
                "\"suggestions\" VARCHAR(1000)," +
                "\"reporter_id\" BIGINT NOT NULL," +
                "\"reporter_name\" VARCHAR(50)," +
                "\"department\" VARCHAR(100)," +
                "\"status\" VARCHAR(20) NOT NULL DEFAULT 'DRAFT'," +
                "\"reviewer_id\" BIGINT," +
                "\"reviewer_name\" VARCHAR(50)," +
                "\"review_comment\" VARCHAR(500)," +
                "\"reviewed_at\" DATETIME," +
                "\"created_at\" DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "\"updated_at\" DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "\"deleted\" TINYINT DEFAULT 0" +
                ")";

            jdbcTemplate.execute(createTableSql);
            log.info("WorkReport table created successfully");

            // 插入测试数据 - 使用引用标识符
            String insertSql =
                "INSERT INTO \"work_report\" (\"title\", \"report_date\", \"report_type\", \"work_summary\", \"next_plan\"," +
                "\"problems\", \"suggestions\", \"reporter_id\", \"reporter_name\", \"department\", \"status\"," +
                "\"created_at\", \"updated_at\", \"deleted\") VALUES " +
                "('周报 - 2026年第16周', '2026-04-19 10:00:00', 'WEEKLY'," +
                "'本周完成案件管理模块开发，解决Hibernate表名映射问题'," +
                "'下周继续开发AI OCR功能', 'H2数据库兼容性问题较多', '建议切换到MySQL生产环境'," +
                "2, '张三律师', '技术部', 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)," +
                "('周报 - 2026年第16周', '2026-04-19 11:00:00', 'WEEKLY'," +
                "'完成3个案件的法律文书起草', '准备下周开庭材料', '客户联系不及时'," +
                "'加强客户沟通管理', 3, '李四律师', '诉讼部', 'SUBMITTED'," +
                "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)";

            jdbcTemplate.update(insertSql);
            log.info("WorkReport test data inserted successfully");

        } catch (Exception e) {
            log.error("Failed to initialize WorkReport table", e);
            // Don't throw - allow application to start even if this fails
        }
    }
}
