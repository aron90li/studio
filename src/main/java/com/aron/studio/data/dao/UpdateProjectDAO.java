package com.aron.studio.data.dao;

import lombok.Data;

@Data
public class UpdateProjectDAO {
    private Long projectId;
    private String projectName;
    private String projectIdentity;
    private String description;

}
