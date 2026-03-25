package com.aron.studio.controller;

import com.aron.studio.data.Response;
import com.aron.studio.data.dto.project.*;
import com.aron.studio.data.vo.ProjectDetailVO;
import com.aron.studio.data.vo.ProjectVO;
import com.aron.studio.data.vo.UserVO;
import com.aron.studio.service.ProjectService;
import com.aron.studio.util.CurrentUserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 项目的管理，增删改查，管理员用户可以增删改，普通用户只能查看
 */
@Slf4j
@RestController
@RequestMapping("/api/project")
public class ProjectController {
    @Autowired
    ProjectService projectService;
    @Autowired
    CurrentUserUtil currentUserUtil;

    // 这里异常不会在执行下面方法，向上抛出 AuthorizationDeniedException, 会被GlobalExceptionHandler catch如果有的话，
    // 如果没有会被GlobalExceptionHandler, 回到 SecurityConfig 中的 accessDeniedHandler
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/adminTest")
    public Response<String> adminTest() {
        Long userId = currentUserUtil.getCurrentUserId().get();
        String username = currentUserUtil.getCurrentUsername().get();
        log.info("current username: {}, userId: {}", username, userId);
        return Response.success("admin ok");
    }

    /**
     * 新建项目, 需要限制角色, 普通用户不能调用
     *
     * @param createProjectDTO
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @PostMapping("/createProject")
    public Response<Map<String, String>> createProject(@RequestBody CreateProjectDTO createProjectDTO) {
        try {
            log.info("call createProject, params: {}", createProjectDTO);
            String projectId = projectService.createProject(createProjectDTO);
            return Response.success(Map.of("projectId", projectId));
        } catch (Exception e) {
            log.error("call createProject error: ", e);
            return Response.fail(e.getMessage());
        }
    }

    /**
     * 更新项目, 需要限制角色, 普通用户不能调用
     *
     * @param updateProjectDTO
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @PostMapping("/updateProject")
    public Response<Integer> updateProject(@RequestBody UpdateProjectDTO updateProjectDTO) {
        try {
            log.info("call updateProject, params: {}", updateProjectDTO);
            int cnt = projectService.updateProject(updateProjectDTO);
            return Response.success(cnt);
        } catch (DuplicateKeyException e) {
            log.error("call updateProject error: ", e);
            return Response.fail("已存在相同项目标识");
        } catch (Exception e) {
            log.error("call updateProject error: ", e);
            return Response.fail(e.getMessage());
        }
    }

    /**
     * 删除项目, 需要限制角色, 普通用户不能调用
     *
     * @param deleteProjectsDTO
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @PostMapping("/deleteProjects")
    public Response<Integer> deleteProjects(@RequestBody DeleteProjectsDTO deleteProjectsDTO) {
        try {
            log.info("call deleteProject, params: {}", deleteProjectsDTO.getProjectIds());
            int cnt = projectService.deleteProjects(deleteProjectsDTO.getProjectIds());
            return Response.success(cnt);
        } catch (Exception e) {
            log.error("call deleteProject error: ", e);
            return Response.fail(e.getMessage());
        }
    }

    /**
     * 添加项目成员, 需要限制角色, 普通用户不能调用
     *
     * @return
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/grantProjectToUser")
    public Response<Integer> grantProjectToUser(@RequestBody ProjectUserDTO dto) {
        return Response.success(projectService.grantProjectToUser(dto.getProjectId(), dto.getUserId()));
    }

    /**
     * 删除项目成员, 需要限制角色, 普通用户不能调用
     *
     * @return
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/deleteProjectUser")
    public Response<Integer> deleteProjectUser(@RequestBody ProjectUserDTO dto) {
        return Response.success(projectService.deleteProjectUser(dto.getProjectId(), dto.getUserId()));
    }

    /**
     * 获取项目成员, 需要限制角色, 普通用户不能调用
     *
     * @return
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/getProjectUsers")
    public Response<List<UserVO>> getProjectUsers(@RequestParam("projectId") String projectId) {
        return Response.success(projectService.getProjectUsers(projectId));
    }

    /**
     * 获取当前用户项目信息, 所有用户可以调用
     *
     * @return
     */
    @GetMapping("/getProject")
    public Response<List<ProjectVO>> getProject() {
        try {
            List<ProjectVO> projectVOList = projectService.getProject();
            return Response.success(projectVOList);
        } catch (Exception e) {
            log.error("call getProject error: ", e);
            return Response.fail(e.getMessage());
        }
    }

    // project_detail
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @PostMapping("/createOrUpdateProjectDetail")
    public Response<Integer> createOrUpdateProjectDetail(@RequestBody UpdateProjectDetailDTO updateProjectDetailDTO) {
        try {
            log.info("call createOrUpdateProjectDetail, params: {}", updateProjectDetailDTO);
            return Response.success(projectService.createOrUpdateProjectDetail(updateProjectDetailDTO));
        } catch (Exception e) {
            log.error("call createOrUpdateProjectDetail error: ", e);
            return Response.fail(e.getMessage());
        }
    }

    @GetMapping("/getProjectDetail")
    public Response<List<ProjectDetailVO>> getProjectDetail(@RequestParam("projectId") String projectId,
                                                            @RequestParam(value = "detailType", required = false) String detailType) {
        try {
            return Response.success(projectService.getProjectDetail(projectId, detailType));
        } catch (Exception e) {
            log.error("call getProjectDetail error: ", e);
            return Response.fail(e.getMessage());
        }
    }

}
