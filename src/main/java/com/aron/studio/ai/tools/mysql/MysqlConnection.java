package com.aron.studio.ai.tools.mysql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 已配置的 MySQL 连接信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MysqlConnection {

    private String name;
    private String driverClassName;
    private String url;
    private String username;

    @JsonIgnore
    private String password;
}