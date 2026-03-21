package com.aron.studio.service.impl;

import com.aron.studio.data.dao.UpdateProjectDAO;
import com.aron.studio.data.dto.project.*;
import com.aron.studio.data.enums.RoleEnum;
import com.aron.studio.data.rbac.entity.ProjectDetailEntity;
import com.aron.studio.data.rbac.entity.ProjectEntity;
import com.aron.studio.data.vo.ProjectDetailVO;
import com.aron.studio.data.vo.ProjectVO;
import com.aron.studio.data.vo.UserVO;
import com.aron.studio.mapper.ProjectMapper;
import com.aron.studio.mapper.TaskMapper;
import com.aron.studio.mapper.UserMapper;
import com.aron.studio.service.ProjectService;
import com.aron.studio.util.CurrentUserUtil;
import com.aron.studio.util.SnowflakeIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProjectServiceImpl implements ProjectService {

    @Autowired
    SnowflakeIdGenerator snowflakeIdGenerator;

    @Autowired
    ProjectMapper projectMapper;

    @Autowired
    UserMapper userMapper;

    @Autowired
    TaskMapper taskMapper;

    @Autowired
    CurrentUserUtil currentUserUtil;

    @Override
    @Transactional
    public String createProject(CreateProjectDTO createProjectDTO) {
        Long currentUserId = currentUserUtil.getCurrentUserId().get();

        if (!StringUtils.hasText(createProjectDTO.getProjectName()) || !StringUtils.hasText(createProjectDTO.getProjectIdentity())) {
            throw new IllegalArgumentException("项目名字/项目标志不能为空");
        }

        // 0 不能重名
        if (projectMapper.getCountByProjectNameCreate(createProjectDTO.getProjectName()) > 0) {
            throw new RuntimeException("已存在同名的项目");
        }
        // 1 插入项目表
        Long projectId = snowflakeIdGenerator.nextId();
        ProjectEntity projectEntity = new ProjectEntity();
        projectEntity.setProjectId(projectId);
        projectEntity.setProjectName(createProjectDTO.getProjectName());
        projectEntity.setProjectIdentity(createProjectDTO.getProjectIdentity());
        projectEntity.setEnabled(true);
        projectEntity.setCreateUser(currentUserId);
        projectEntity.setUpdateUser(currentUserId);
        projectEntity.setDescription(createProjectDTO.getDescription());
        projectMapper.insertProject(projectEntity);

        // 1 写入权限project_user表, 登陆用户/创建用户写入
        projectMapper.insertProjectUser(projectId, currentUserId, "", currentUserId);
        // 2 含有ROLE_ADMIN系统级别角色的用户写入
        List<Long> adminUserIds = userMapper.selectUserIdsByRole(RoleEnum.ROLE_ADMIN.getCode());
        for (Long adminUserId : adminUserIds) {
            if (adminUserId.equals(currentUserId)) {
                continue;
            }
            projectMapper.insertProjectUser(projectId, adminUserId, "", currentUserId);
        }
        // 3 在 tree_node 表中插入项目的根目录, 任务开发
        Long nodeId = snowflakeIdGenerator.nextId();
        taskMapper.insertTreeNode(nodeId, projectId, "任务开发", "folder", null, null, currentUserId);

        return projectId.toString();
    }

    @Override
    public List<ProjectVO> getProject() {
        Long currentUserId = currentUserUtil.getCurrentUserId().get();
        List<ProjectVO> projectVOs = projectMapper.getProjectByUserId(currentUserId);
        return projectVOs;
    }

    @Override
    public int updateProject(UpdateProjectDTO updateProjectDTO) {
        Long currentUserId = currentUserUtil.getCurrentUserId().get();

        if (projectMapper.getCountByProjectNameUpdate(updateProjectDTO.getProjectName(),
                Long.valueOf(updateProjectDTO.getProjectId())) > 0) {
            throw new RuntimeException("已存在同名的项目");
        }

        UpdateProjectDAO dao = new UpdateProjectDAO();
        dao.setProjectId(Long.valueOf(updateProjectDTO.getProjectId()));
        dao.setProjectName(updateProjectDTO.getProjectName());
        dao.setProjectIdentity(updateProjectDTO.getProjectIdentity());
        dao.setDescription(updateProjectDTO.getDescription());

        return projectMapper.updateProject(dao, currentUserId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public int deleteProjects(List<String> projectIds) {
        List<Long> projectIdList = projectIds.stream().map(Long::valueOf).collect(Collectors.toUnmodifiableList());
        int cnt = projectMapper.deleteProjects(projectIdList);
        projectMapper.deleteProjectUserByProjectIds(projectIdList);
        return cnt;
    }

    @Override
    public int grantProjectToUser(String projectId, String userId) {
        Long currentUserId = currentUserUtil.getCurrentUserId().get();
        List<UserVO> projectUsers = projectMapper.getProjectUsers(Long.valueOf(projectId));
        List<Long> existsUserIds = projectUsers == null ? new ArrayList<>() :
                projectUsers.stream().map(UserVO::getUserId).map(Long::valueOf).collect(Collectors.toUnmodifiableList());

        if (existsUserIds.contains(Long.valueOf(userId))) {
            log.warn("用户 {}, 已经是项目 {} 的成员", userId, projectId);
            return 0;
        }

        return projectMapper.insertProjectUser(Long.valueOf(projectId), Long.valueOf(userId), "", currentUserId);
    }

    @Override
    public int deleteProjectUser(String projectId, String userId) {
        return projectMapper.deleteProjectUser(Long.valueOf(projectId), Long.valueOf(userId));
    }

    @Override
    public List<UserVO> getProjectUsers(String projectId) {
        return projectMapper.getProjectUsers(Long.valueOf(projectId));
    }

    @Override
    public int createProjectDetail(CreateProjectDetailDTO createProjectDetailDTO) {
        Long currentUserId = currentUserUtil.getCurrentUserId().get();
        if (!StringUtils.hasText(createProjectDetailDTO.getProjectId()) ||
                !StringUtils.hasText(createProjectDetailDTO.getDetailType())) {
            throw new IllegalArgumentException("projectId/detailType is required");
        }

        Long projectId = Long.valueOf(createProjectDetailDTO.getProjectId());
        if (projectMapper.getCountByProjectDetail(projectId, createProjectDetailDTO.getDetailType()) > 0) {
            throw new RuntimeException("project detail already exists");
        }

        ProjectDetailEntity projectDetailEntity = new ProjectDetailEntity();
        projectDetailEntity.setProjectId(projectId);
        projectDetailEntity.setDetailType(createProjectDetailDTO.getDetailType());
        projectDetailEntity.setDetailValue(createProjectDetailDTO.getDetailValue());
        projectDetailEntity.setCreateUser(currentUserId);
        projectDetailEntity.setUpdateUser(currentUserId);

        return projectMapper.insertProjectDetail(projectDetailEntity);
    }

    @Override
    public int updateProjectDetail(UpdateProjectDetailDTO updateProjectDetailDTO) {
        Long currentUserId = currentUserUtil.getCurrentUserId().get();
        if (!StringUtils.hasText(updateProjectDetailDTO.getProjectId()) ||
                !StringUtils.hasText(updateProjectDetailDTO.getDetailType())) {
            throw new IllegalArgumentException("projectId/detailType is required");
        }

        return projectMapper.updateProjectDetail(Long.valueOf(updateProjectDetailDTO.getProjectId()),
                updateProjectDetailDTO.getDetailType(),
                updateProjectDetailDTO.getDetailValue(),
                currentUserId, LocalDateTime.now());
    }

    @Override
    public int deleteProjectDetail(DeleteProjectDetailDTO deleteProjectDetailDTO) {
        if (!StringUtils.hasText(deleteProjectDetailDTO.getProjectId()) ||
                !StringUtils.hasText(deleteProjectDetailDTO.getDetailType())) {
            throw new IllegalArgumentException("projectId/detailType is required");
        }

        return projectMapper.deleteProjectDetail(Long.valueOf(deleteProjectDetailDTO.getProjectId()),
                deleteProjectDetailDTO.getDetailType());
    }

    @Override
    public List<ProjectDetailVO> getProjectDetail(String projectId, String detailType) {
        if (!StringUtils.hasText(projectId)) {
            throw new IllegalArgumentException("projectId is required");
        }
        return projectMapper.getProjectDetail(Long.valueOf(projectId), detailType);
    }

    @Override
    public int createOrUpdateProjectDetail(UpdateProjectDetailDTO updateProjectDetailDTO) {
        Long currentUserId = currentUserUtil.getCurrentUserId().get();
        if (!StringUtils.hasText(updateProjectDetailDTO.getProjectId()) ||
                !StringUtils.hasText(updateProjectDetailDTO.getDetailType())) {
            throw new IllegalArgumentException("projectId/detailType is required");
        }

        Long projectId = Long.valueOf(updateProjectDetailDTO.getProjectId());
        if (projectMapper.getCountByProjectDetail(projectId, updateProjectDetailDTO.getDetailType()) > 0) {
            return projectMapper.updateProjectDetail(projectId,
                    updateProjectDetailDTO.getDetailType(),
                    updateProjectDetailDTO.getDetailValue(),
                    currentUserId, LocalDateTime.now());
        }

        ProjectDetailEntity projectDetailEntity = new ProjectDetailEntity();
        projectDetailEntity.setProjectId(projectId);
        projectDetailEntity.setDetailType(updateProjectDetailDTO.getDetailType());
        projectDetailEntity.setDetailValue(updateProjectDetailDTO.getDetailValue());
        projectDetailEntity.setCreateUser(currentUserId);
        projectDetailEntity.setUpdateUser(currentUserId);

        return projectMapper.insertProjectDetail(projectDetailEntity);
    }

}
