package com.aron.studio.data.dto.tree;

import lombok.Data;

@Data
public class CreateTreeNodeDTO {
    private String projectId;
    private String nodeName;
    private String nodeType;
    private String parentNodeId;
}
