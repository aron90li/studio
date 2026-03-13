package com.aron.studio.data.rbac.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectUserEntity {
    private Long id;
    private Long projectId;
    private Long userId;
    private String projectRole; // 预留字段
    private Long createUser;
    private Long updateUser;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
