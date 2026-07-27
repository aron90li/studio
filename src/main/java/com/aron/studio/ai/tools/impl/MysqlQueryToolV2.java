package com.aron.studio.ai.tools.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
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
public class MysqlQueryToolV2 {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public MysqlQueryToolV2(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 执行 MySQL 查询（仅允许 SELECT/SHOW/DESC）
     *
     * @param sql 要执行的 SQL 查询语句
     * @return JSON 结构化查询结果，格式为 {"count": N, "data": [...]}
     */
    @Tool(description = "执行MySQL查询，参数格式：{\"sql\": \"SELECT * FROM user WHERE name = '张三'\"}。"
            + "可以查询任何数据库表。查询结果以JSON结构化形式返回，格式为{\"count\":记录数, \"data\":[数据数组]}。"
            + "注意：只允许执行 SELECT 查询，不允许修改数据。")
    public String mysqlQuery(
            @ToolParam(description = "要执行的 SQL SELECT 查询语句") String sql) {

        if (sql == null || sql.trim().isEmpty()) {
            return buildErrorResult("缺少 sql 参数");
        }

        // 安全检查：只允许 SELECT
        String trimmedSql = sql.trim().toUpperCase();
        if (!trimmedSql.startsWith("SELECT")
                && !trimmedSql.startsWith("SHOW")
                && !trimmedSql.startsWith("DESC")
                && !trimmedSql.startsWith("EXPLAIN")) {
            return buildErrorResult("只允许执行 SELECT / SHOW / DESC / EXPLAIN 查询");
        }

        log.info("MysqlQueryToolV2 执行SQL: {}", sql);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        return buildSuccessResult(rows);
    }

    /**
     * 构建成功结果的 JSON 字符串
     *
     * @param rows 查询结果行列表
     * @return JSON 格式字符串 {"count": N, "data": [...]}
     */
    private String buildSuccessResult(List<Map<String, Object>> rows) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", rows.size());
        result.put("data", rows);
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            log.error("序列化查询结果失败", e);
            return buildErrorResult("序列化查询结果失败: " + e.getMessage());
        }
    }

    /**
     * 构建错误结果的 JSON 字符串
     *
     * @param message 错误信息
     * @return JSON 格式字符串 {"count": 0, "data": [], "error": "错误信息"}
     */
    private String buildErrorResult(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", 0);
        result.put("data", List.of());
        result.put("error", message);
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            log.error("序列化错误结果失败", e);
            return "{\"count\":0,\"data\":[],\"error\":\"" + message + "\"}";
        }
    }
}