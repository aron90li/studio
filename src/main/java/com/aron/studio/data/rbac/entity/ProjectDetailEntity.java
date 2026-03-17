package com.aron.studio.data.rbac.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectDetailEntity {
    private Long id;
    private Long projectId;
    private String detailType;
    private String detailValue;
    private Long createUser;
    private Long updateUser;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
