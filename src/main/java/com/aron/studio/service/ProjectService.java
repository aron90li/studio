package com.aron.studio.service;

import com.aron.studio.data.dto.project.*;
import com.aron.studio.data.vo.ProjectDetailVO;
import com.aron.studio.data.vo.ProjectVO;
import com.aron.studio.data.vo.UserVO;

import java.util.List;

public interface ProjectService {
    String createProject(CreateProjectDTO createProjectDTO);

    List<ProjectVO> getProject();

    int updateProject(UpdateProjectDTO updateProjectDTO);

    int deleteProjects(List<String> projectIds);

    int grantProjectToUser(String projectId, String userId);

    int deleteProjectUser(String projectId, String userId);

    List<UserVO> getProjectUsers(String projectId);

    int createProjectDetail(CreateProjectDetailDTO createProjectDetailDTO);

    int updateProjectDetail(UpdateProjectDetailDTO updateProjectDetailDTO);

    int deleteProjectDetail(DeleteProjectDetailDTO deleteProjectDetailDTO);

    List<ProjectDetailVO> getProjectDetail(String projectId, String detailType);

    int createOrUpdateProjectDetail(UpdateProjectDetailDTO updateProjectDetailDTO);
}
