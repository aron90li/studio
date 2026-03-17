package com.aron.studio.data.dto.project;

import lombok.Data;

@Data
public class UpdateProjectDetailDTO {
    private String projectId;
    private String detailType;
    private String detailValue;
}
