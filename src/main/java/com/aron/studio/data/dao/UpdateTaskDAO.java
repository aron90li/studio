package com.aron.studio.data.dao;

import lombok.Data;

@Data
public class UpdateTaskDAO {

    // 必传字段
    private Long projectId;
    private Long taskId;
    private Integer taskVersion; // mapper中 where 带着 taskVersion，如果返回 0 ，则已被修改，业务中要处理这种现象

    // 下面字段传入哪个更新哪个，如果传入null则不更新，不传入默认是null，则不会更新该字段
    private String taskName;
    private String description;
    private String taskType; // 任务类型
    private String taskSql; // 任务sql
    private String taskParam; // 任务参数,用户传入的参数,优先级最高,覆盖其他地方的同名参数
    private String taskSource; // 源表
    private String taskSide; // 维表
    private String taskSink; // 结果表
    private Integer deleted; // 删除标识
    private Integer publishStatus; // 预留字段, 发布状态: 0草稿-1已发布-2已下线
}
