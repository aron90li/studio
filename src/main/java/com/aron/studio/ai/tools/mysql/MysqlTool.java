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

        String normalizedSql = normalizeSql(sql);
        if (normalizedSql.indexOf(';') >= 0) {
            return MysqlQueryResult.error("不允许执行多条 SQL 语句");
        }

        String operation = normalizedSql.split("\\s+", 2)[0].toUpperCase(Locale.ROOT);
        if (queryOnly && !isQueryOperation(operation)) {
            return MysqlQueryResult.error("只允许执行 SELECT / SHOW / DESC / EXPLAIN 查询");
        }

        try {
            Class.forName(connectionConfig.getDriverClassName());
            try (Connection connection = DriverManager.getConnection(
                    connectionConfig.getUrl(), connectionConfig.getUsername(), connectionConfig.getPassword());
                 Statement statement = connection.createStatement()) {
                if (isQueryOperation(operation)) {
                    return MysqlQueryResult.success(readRows(statement.executeQuery(normalizedSql)));
                }
                return MysqlQueryResult.updateSuccess(statement.executeUpdate(normalizedSql));
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

    private String normalizeSql(String sql) {
        String normalizedSql = sql.trim();
        if (normalizedSql.endsWith(";")) {
            normalizedSql = normalizedSql.substring(0, normalizedSql.length() - 1).trim();
        }
        return normalizedSql;
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
