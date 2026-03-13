package com.aron.studio.data.dto.project;

import lombok.Data;

import java.util.List;

@Data
public class DeleteProjectsDTO {

    private List<String> projectIds;
}
