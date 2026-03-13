package com.aron.studio.data.dto.project;

import lombok.Data;

@Data
public class UpdateProjectDTO {
    private String projectId;
    private String projectName;
    private String projectIdentity;
    private String description;
}
