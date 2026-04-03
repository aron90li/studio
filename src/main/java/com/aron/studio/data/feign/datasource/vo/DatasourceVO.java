package com.aron.studio.data.feign.datasource.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DatasourceVO {

    private String datasourceId;
    private String datasourceName;
    private String description;

    private String datasourceType;
    private String datasourceVersion;
    private String linkJson;

    private String createUserId;
    private String createUsername;
    private String updateUserId;
    private String updateUsername;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
