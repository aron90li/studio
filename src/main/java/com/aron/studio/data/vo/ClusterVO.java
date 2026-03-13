package com.aron.studio.data.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClusterVO {
    private String clusterId;
    private String clusterName;
    private String description;
    private String clusterType;
    private String flinkVersion;
    private String defaultConf;
    private String podTemplate;
    private String kubeconfig;
    private String createUsername;
    private String createUserId;
    private String updateUsername;
    private String updateUserId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
