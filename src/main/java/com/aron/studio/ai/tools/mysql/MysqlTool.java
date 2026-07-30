package com.aron.studio.ai.tools.mysql;

import com.aron.studio.ai.dto.MysqlQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MySQL 查询工具 V2 — 使用 Spring AI 2.0 @Tool 注解定义
 * <p>
 * 与旧版 MysqlQueryTool 的区别：
 * <ul>
 *   <li>旧版：实现 AgentTool 接口，在 Workflow 中通过正则解析文本调用</li>
 *   <li>新版：使用 @Tool + @ToolParam 注解，由 Spring AI 框架自动发现和调用</li>
 * </ul>
 * 旧版 MysqlQueryTool 不受影响，两个工具可以共存。
 */
@Slf4j
@Component
public class MysqlTool {

    private final JdbcTemplate jdbcTemplate;

    public MysqlTool(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 执行 MySQL 查询（仅允许 SELECT/SHOW/DESC）
     *
     * @param sql 要执行的 SQL 查询语句
     * @return JSON 结构化查询结果，格式为 {"count": N, "data": [...]}
     */
    @Tool(description = "执行MySQL查询，参数格式：{\"sql\": \"SELECT * FROM user WHERE name = '张三'\"}。"
            + "可以查询任何数据库表。查询结果以结构化形式返回，包含 count(记录数)、data(数据数组)、error(错误信息)。"
            + "注意：只允许执行 SELECT 查询，不允许修改数据。")
    public MysqlQueryResult mysqlQuery(
            @ToolParam(description = "要执行的 SQL SELECT 查询语句") String sql) {

        if (sql == null || sql.trim().isEmpty()) {
            return MysqlQueryResult.error("缺少 sql 参数");
        }

        // 安全检查：只允许 SELECT / SHOW / DESC / EXPLAIN
        String trimmedSql = sql.trim().toUpperCase();
        if (!trimmedSql.startsWith("SELECT")
                && !trimmedSql.startsWith("SHOW")
                && !trimmedSql.startsWith("DESC")
                && !trimmedSql.startsWith("EXPLAIN")) {
            return MysqlQueryResult.error("只允许执行 SELECT / SHOW / DESC / EXPLAIN 查询");
        }

        log.info("MysqlQueryToolV2 执行SQL: {}", sql);
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            return MysqlQueryResult.success(rows);
        } catch (Exception e) {
            log.error("查询执行失败", e);
            return MysqlQueryResult.error("查询执行失败: " + e.getMessage());
        }
    }
}