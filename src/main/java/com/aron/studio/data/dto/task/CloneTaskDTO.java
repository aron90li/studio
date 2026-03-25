package com.aron.studio.data.dto.task;

import lombok.Data;

@Data
public class CloneTaskDTO {

    // 必传字段
    private String projectId;
    private String taskId;
    private Integer taskVersion; // mapper中 where 带着 taskVersion，如果返回 0 ，则已被修改，业务中要处理这种现象



}
