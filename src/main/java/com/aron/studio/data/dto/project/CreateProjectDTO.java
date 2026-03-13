package com.aron.studio.data.dto.project;

import lombok.Data;

@Data
public class CreateProjectDTO {
    private String projectName; // 前端限制必传
    private String projectIdentity; // 前端限制必传
    private String description;
    // private String userId; // 从token解析, 这里不需要
}
