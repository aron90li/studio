package com.aron.studio.data.feign.datasource;

import lombok.Data;

@Data
public class TestConnectionReq {

    private String datasourceName;
    private String description;

    private String datasourceType;
    private String datasourceVersion;
    private String linkJson;
}
