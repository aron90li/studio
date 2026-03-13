package com.aron.studio.data.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClusterEntity {

    private Long id; // 自增主键,技术主键,不参与业务
    private Long clusterId; // 集群id,业务id,java使用Long,前端使用String
    private String clusterName; // 集群名称
    private String description; // 集群描述
    private String clusterType; // 集群类型
    private String flinkVersion; // flink版本
    private String defaultConf; // 默认配置
    private String podTemplate; // 源表
    private String kubeconfig; // 维表
    private Integer deleted; // 删除标识
    private Long createUser; // 创建用户
    private Long updateUser; // 修改用户
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 修改时间
}
