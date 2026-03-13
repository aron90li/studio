package com.aron.studio.data.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TreeNodeEntity {
    private Long id;
    private Long nodeId;
    private Long projectId;
    private String nodeName;
    private String nodeType;
    private Long parentNodeId;
    private Long taskId;
    private Long createUser;
    private Long updateUser;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}
