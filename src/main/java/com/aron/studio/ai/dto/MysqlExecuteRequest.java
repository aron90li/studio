package com.aron.studio.ai.dto;

import lombok.Data;

/**
 * MySQL 工具执行请求。
 */
@Data
public class MysqlExecuteRequest {

    private String connectionName;

    private String sql;
}