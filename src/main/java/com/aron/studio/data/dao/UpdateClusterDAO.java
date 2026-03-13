package com.aron.studio.data.dao;

import lombok.Data;

@Data
public class UpdateClusterDAO {
    private Long clusterId;
    private String clusterName;
    private String description;
    private String clusterType;
    private String flinkVersion;
    private String defaultConf;
    private String podTemplate;
    private String kubeconfig;
}