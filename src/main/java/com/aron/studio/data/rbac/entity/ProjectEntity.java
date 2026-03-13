package com.aron.studio.data.rbac.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectEntity {
    private Long id;
    private Long projectId;
    private String projectName;
    private String projectIdentity;
    private Boolean enabled;
    private String description;
    private Long createUser;
    private Long updateUser;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
