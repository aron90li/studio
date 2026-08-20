package com.aron.studio.ai.tools.mysql;

import com.aron.studio.ai.config.DbPropertiesConfig;
import com.aron.studio.ai.dto.MysqlQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * MySQL 工具。每次操作都根据 db.connections 创建独立 JDBC 连接。
 */
@Slf4j
@Component
public class MysqlTool {

    private final DbPropertiesConfig dbPropertiesConfig;

    public MysqlTool(DbPropertiesConfig dbPropertiesConfig) {
        this.dbPropertiesConfig = dbPropertiesConfig;
    }

    @Tool(description = "列出所有配置的 MySQL 连接。")
    public List<MysqlConnection> listConnections() {
        return dbPropertiesConfig.getConnections();
    }

    @Tool(description = "执行指定 MySQL 连接上的 SELECT 查询，返回 count 和 data。")
    public MysqlQueryResult mysqlQuery(
            @ToolParam(description = "连接名称") String connectionName,
            @ToolParam(description = "要执行的 SQL SELECT 查询语句") String sql) {
        return execute(connectionName, sql, true);
    }

    public MysqlQueryResult execute(String connectionName, String sql, boolean queryOnly) {
        if (sql == null || sql.trim().isEmpty()) {
            return MysqlQueryResult.error("缺少 sql 参数");
        }

        MysqlConnection connectionConfig = findConnection(connectionName);
        if (connectionConfig == null) {
            return MysqlQueryResult.error("未找到数据库连接: " + connectionName);
        }

        // 先剔除注释（-- 行注释、/* */ 块注释），仅提取可执行语句
        String executableSql = extractExecutableSql(sql);
        if (executableSql.isEmpty()) {
            return MysqlQueryResult.error("SQL 中不包含任何可执行语句");
        }
        // 只把引号（单/双引号字符串）之外的顶层分号视为语句分隔符，
        // 避免把字段值里出现的分号（如 'a;b'）误判为多条语句
        if (isMultipleStatements(executableSql)) {
            return MysqlQueryResult.error("不允许执行多条 SQL 语句");
        }
        // 去掉作为语句末尾的顶层分号后再执行
        String finalSql = removeTrailingSemicolon(executableSql);
        if (finalSql.isEmpty()) {
            return MysqlQueryResult.error("SQL 中不包含任何可执行语句");
        }

        String operation = finalSql.split("\\s+", 2)[0].toUpperCase(Locale.ROOT);
        if (queryOnly && !isQueryOperation(operation)) {
            return MysqlQueryResult.error("只允许执行 SELECT / SHOW / DESC / EXPLAIN 查询");
        }

        try {
            Class.forName(connectionConfig.getDriverClassName());
            try (Connection connection = DriverManager.getConnection(
                    connectionConfig.getUrl(), connectionConfig.getUsername(), connectionConfig.getPassword());
                 Statement statement = connection.createStatement()) {
                if (isQueryOperation(operation)) {
                    log.info("执行查询: {}", finalSql);
                    return MysqlQueryResult.success(readRows(statement.executeQuery(finalSql)));
                }
                log.info("执行更新: {}", finalSql);
                return MysqlQueryResult.updateSuccess(statement.executeUpdate(finalSql));
            }
        } catch (Exception e) {
            log.error("MySQL 操作执行失败, connectionName={}", connectionName, e);
            return MysqlQueryResult.error("SQL 执行失败: " + e.getMessage());
        }
    }

    private MysqlConnection findConnection(String connectionName) {
        return dbPropertiesConfig.getConnections().stream()
                .filter(connection -> connection.getName().equals(connectionName))
                .findFirst()
                .orElse(null);
    }

    /**
     * 剔除注释（-- 行注释和星号斜杠块注释）后，提取出可执行语句片段。
     * 多条语句的判别以及末尾分号的处理，交由后续顶层分号分析方法完成。
     */
    private String extractExecutableSql(String sql) {
        // 1) 先移除块注释 /* ... */
        String withoutBlockComment = sql.replaceAll("(?s)/\\*.*?\\*/", "");

        // 2) 逐行处理，去掉行注释，并按行拼接剩余有效片段
        StringBuilder builder = new StringBuilder();
        for (String line : withoutBlockComment.split("\\R")) {
            int commentStart = findLineCommentStart(line);
            String validPart = commentStart >= 0 ? line.substring(0, commentStart) : line;
            validPart = validPart.trim();
            if (!validPart.isEmpty()) {
                builder.append(validPart).append('\n');
            }
        }

        return builder.toString().trim();
    }

    /**
     * 返回某一行内行注释 "--" 的起始下标，未找到返回 -1。
     * 会识别并跳过单双引号字符串，避免把字符串中出现的 "--" 误判为注释。
     */
    private int findLineCommentStart(String line) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = 0; i < line.length() - 1; i++) {
            char c = line.charAt(i);
            char next = line.charAt(i + 1);

            if (inSingleQuote) {
                if (c == '\\') {
                    i++;
                } else if (c == '\'') {
                    inSingleQuote = false;
                }
                continue;
            }
            if (inDoubleQuote) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    inDoubleQuote = false;
                }
                continue;
            }

            if (c == '\'') {
                inSingleQuote = true;
            } else if (c == '"') {
                inDoubleQuote = true;
            } else if (c == '-' && next == '-') {
                return i;
            }
        }
        return -1;
    }

    /**
     * 统计引号（单 / 双引号字符串）之外的顶层分号位置。
     * SQL 字符串字段值中出现分号时不会被计入，避免误判成多条语句。
     */
    private List<Integer> topLevelSemicolonIndexes(String sql) {
        List<Integer> indexes = new ArrayList<>();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (inSingleQuote) {
                if (c == '\\') {
                    i++;
                } else if (c == '\'') {
                    inSingleQuote = false;
                }
                continue;
            }
            if (inDoubleQuote) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    inDoubleQuote = false;
                }
                continue;
            }
            if (c == '\'') {
                inSingleQuote = true;
            } else if (c == '"') {
                inDoubleQuote = true;
            } else if (c == ';') {
                indexes.add(i);
            }
        }
        return indexes;
    }

    /**
     * 判断是否为多条 SQL 语句（基于引号之外的顶层分号）。
     */
    private boolean isMultipleStatements(String sql) {
        List<Integer> indexes = topLevelSemicolonIndexes(sql);
        if (indexes.isEmpty()) {
            return false;
        }
        int lastTop = indexes.get(indexes.size() - 1);
        if (onlyWhitespaceAfter(sql, lastTop)) {
            // 末尾分号只是语句结束符，去掉后若前面还有其他顶层分号则代表多条
            return indexes.size() > 1;
        }
        // 最后一个顶层分号后面还有内容，说明不止一条语句
        return true;
    }

    /**
     * 去掉末尾作为语句结束符的顶层分号。
     */
    private String removeTrailingSemicolon(String sql) {
        List<Integer> indexes = topLevelSemicolonIndexes(sql);
        if (indexes.isEmpty()) {
            return sql;
        }
        int lastTop = indexes.get(indexes.size() - 1);
        if (onlyWhitespaceAfter(sql, lastTop)) {
            return sql.substring(0, lastTop).trim();
        }
        return sql;
    }

    private boolean onlyWhitespaceAfter(String sql, int fromExclusive) {
        for (int i = fromExclusive + 1; i < sql.length(); i++) {
            if (!Character.isWhitespace(sql.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean isQueryOperation(String operation) {
        return "SELECT".equals(operation) || "SHOW".equals(operation)
                || "DESC".equals(operation) || "EXPLAIN".equals(operation);
    }

    private List<Map<String, Object>> readRows(ResultSet resultSet) throws Exception {
        try (ResultSet rows = resultSet) {
            ResultSetMetaData metadata = rows.getMetaData();
            List<Map<String, Object>> result = new ArrayList<>();
            while (rows.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= metadata.getColumnCount(); i++) {
                    row.put(metadata.getColumnLabel(i), rows.getObject(i));
                }
                result.add(row);
            }
            return result;
        }
    }
}
