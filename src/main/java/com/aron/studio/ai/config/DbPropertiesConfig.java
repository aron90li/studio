package com.aron.studio.ai.config;

import com.aron.studio.ai.tools.mysql.MysqlConnection;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 独立数据库工具连接配置，不复用 spring.datasource。
 */
@ConfigurationProperties(prefix = "tools.db")
public class DbPropertiesConfig {

    private List<MysqlConnection> connections = new ArrayList<>();

    public List<MysqlConnection> getConnections() {
        return connections;
    }

    public void setConnections(List<MysqlConnection> connections) {
        this.connections = connections;
    }
}