package com.aron.studio.data.vo;

import lombok.Data;

@Data
public class TreeNodeVO {
    private String nodeId;
    private String projectId;
    private String nodeName;
    private String nodeType;
    private String parentNodeId;
    private String taskId;
}
