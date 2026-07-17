package com.aron.studio.ai.tools.impl;

import com.aron.studio.ai.tools.AgentTool;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MySQL 查询工具 - 允许 Agent 查询数据库中任意表的数据
 */
@Slf4j
@Component
public class MysqlQueryTool implements AgentTool {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public MysqlQueryTool(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "mysql_query";
    }

    @Override
    public String getDescription() {
        return "执行MySQL查询，参数格式：{\"sql\": \"SELECT * FROM user WHERE name = '张三'\"}。"
                + " 可以查询任何数据库表。查询结果以表格形式返回。"
                + " 注意：只允许执行 SELECT 查询，不允许修改数据。";
    }

    @Override
    public String execute(String args) {
        try {
            Map<String, String> params = objectMapper.readValue(args, new TypeReference<Map<String, String>>() {});
            String sql = params.get("sql");

            if (sql == null || sql.trim().isEmpty()) {
                return "错误：缺少 sql 参数";
            }

            // 安全检查：只允许 SELECT
            String trimmedSql = sql.trim().toUpperCase();
            if (!trimmedSql.startsWith("SELECT") && !trimmedSql.startsWith("SHOW") && !trimmedSql.startsWith("DESC")) {
                return "错误：只允许执行 SELECT / SHOW / DESC 查询";
            }

            log.info("MysqlQueryTool 执行SQL: {}", sql);
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

            if (rows.isEmpty()) {
                return "查询结果：未找到数据（0条记录）";
            }

            // 格式化成表格文本返回
            String result = rows.stream()
                    .map(row -> row.entrySet().stream()
                            .map(e -> e.getKey() + "=" + e.getValue())
                            .collect(Collectors.joining(", ", "{", "}")))
                    .collect(Collectors.joining("\n"));

            return String.format("查询结果：共 %d 条记录\n%s", rows.size(), result);

        } catch (Exception e) {
            log.error("MysqlQueryTool 执行异常", e);
            return "查询执行错误：" + e.getMessage();
        }
    }
}