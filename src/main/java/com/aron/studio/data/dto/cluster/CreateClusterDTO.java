package com.aron.studio.data.dto.cluster;

import lombok.Data;

@Data
public class CreateClusterDTO {
    private String clusterName;
    private String description;
    private String clusterType;
    private String flinkVersion;
    private String defaultConf;
    private String podTemplate;
    private String kubeconfig;
}
