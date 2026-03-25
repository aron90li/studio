package com.aron.studio.data.dto.tree;

import lombok.Data;

@Data
public class UpdateTreeNodeDTO {
    // 这是限制条件, 主要看nodeId
    private String nodeId;
    private String projectId;
    private String nodeType;

    // 可选
    private String taskId;

    // 以下是可以改变的
    private String nodeName;
    private String parentNodeId;
}
