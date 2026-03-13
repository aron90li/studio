package com.aron.studio.data.vo;

import lombok.Data;

@Data
public class TaskVO {
    private String projectId;
    private String taskId;

    private String taskName;
    private String description;
    private String taskType; // 任务类型
    private String taskSql; // 任务sql
    private String taskParam; // 任务参数,用户传入的参数,优先级最高,覆盖其他地方的同名参数
    private String taskSource; // 源表
    private String taskSide; // 维表
    private String taskSink; // 结果表
    private Integer taskVersion;
}
