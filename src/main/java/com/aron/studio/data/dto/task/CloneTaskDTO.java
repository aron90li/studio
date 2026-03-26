package com.aron.studio.data.dto.task;

import lombok.Data;

@Data
public class CloneTaskDTO {

    // 项目id
    private String projectId;
    // 被克隆的 任务id
    private String taskId;
    // 克隆后的任务名字
    private String taskName;
    // 克隆后的节点任务所在的父目录id
    private String parentNodeId;
}
