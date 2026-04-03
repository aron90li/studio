package com.aron.studio.data.feign.datasource;

import lombok.Data;

@Data
public class UpdateDatasourceReq {

    private String datasourceId;
    private String datasourceName;
    private String description;

    private String datasourceType;
    private String datasourceVersion;
    private String linkJson;
}
