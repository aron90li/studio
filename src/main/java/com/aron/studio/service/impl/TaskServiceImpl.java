package com.aron.studio.service.impl;

import com.aron.studio.data.dao.UpdateTaskDAO;
import com.aron.studio.data.dto.task.CloneTaskDTO;
import com.aron.studio.data.dto.task.UpdateTaskDTO;
import com.aron.studio.data.dto.tree.CreateTreeNodeDTO;
import com.aron.studio.data.dto.tree.DeleteTreeNodeDTO;
import com.aron.studio.data.dto.tree.UpdateTreeNodeDTO;
import com.aron.studio.data.vo.ProjectDetailVO;
import com.aron.studio.data.vo.TaskVO;
import com.aron.studio.data.vo.TreeNodeVO;
import com.aron.studio.mapper.ProjectMapper;
import com.aron.studio.mapper.TaskMapper;
import com.aron.studio.service.TaskService;
import com.aron.studio.util.CurrentUserUtil;
import com.aron.studio.util.SnowflakeIdGenerator;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class TaskServiceImpl implements TaskService {

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Autowired
    private CurrentUserUtil currentUserUtil;

    @Transactional
    @Override
    public Map<String, String> createTreeNode(CreateTreeNodeDTO createTreeNodeDTO) {
        Long currentUserId = currentUserUtil.getCurrentUserId().orElseThrow(() -> new SecurityException("未登录"));

        Long nodeId = snowflakeIdGenerator.nextId();
        Long projectId = Long.valueOf(createTreeNodeDTO.getProjectId());
        Long parentNodeId = Long.valueOf(createTreeNodeDTO.getParentNodeId());
        String nodeName = createTreeNodeDTO.getNodeName();
        String nodeType = createTreeNodeDTO.getNodeType();

        // 唯一键是 project_id, parent_node_id, node_name, node_type
        // 目录和任务都满足同一个项目下，同级不能同名，由数据库唯一键约束
        if (nodeType.equalsIgnoreCase("folder")) {
            // 会抛异常，同目录下目录名相同异常
            try {
                taskMapper.insertTreeNode(nodeId, projectId, nodeName, "folder", parentNodeId, null, currentUserId);
            } catch (DuplicateKeyException e) {
                throw new DuplicateKeyException("该目录下已存在同名子目录", e);
            }
            return Map.of("nodeId", nodeId.toString());
        } else if (nodeType.equalsIgnoreCase("task")) {
            // a. 插入树节点，会抛异常，同目录下任务名相同异常
            Long taskId = snowflakeIdGenerator.nextId();
            try {
                int cnt = taskMapper.insertTreeNode(nodeId, projectId, nodeName, "task", parentNodeId, taskId, currentUserId);
            } catch (DuplicateKeyException e) {
                throw new DuplicateKeyException("该目录下已存在同名任务", e);
            }

            // 任务名字的额外限制，在 task 表中实现
            // b. 插入任务表，会抛异常，任务名重复异常
            String taskParam = null;
            List<ProjectDetailVO> pdList = projectMapper.getProjectDetail(projectId, "env_template");
            if (pdList != null && pdList.size() > 0) {
                taskParam = pdList.get(0).getDetailValue();
            }
            String taskSql = String.format("-- createUser: %s\n-- createTime: %s", currentUserUtil.getCurrentUsername().get(),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            try {
                int taskCnt = taskMapper.insertTask(projectId, taskId, nodeName, currentUserId, taskParam, taskSql);
            } catch (DuplicateKeyException e) {
                throw new DuplicateKeyException("任务表中已存在同名任务", e);
            }
            return Map.of("nodeId", nodeId.toString(), "taskId", taskId.toString());
        } else {
            throw new RuntimeException("不支持的节点类型");
        }
    }

    @Override
    public List<TreeNodeVO> getTreeNode(String projectId) {
        return taskMapper.getTreeNode(Long.valueOf(projectId));
    }

    @Transactional
    @Override
    public Integer deleteTreeNode(DeleteTreeNodeDTO deleteTreeNodeDTO) {
        Long currentUserId = currentUserUtil.getCurrentUserId().orElseThrow(() -> new SecurityException("未登录"));
        Long projectId = Long.valueOf(deleteTreeNodeDTO.getProjectId());
        Long nodeId = Long.valueOf(deleteTreeNodeDTO.getNodeId());

        TreeNodeVO treeNodeVO = taskMapper.getTreeNodeByNodeId(projectId, nodeId);
        if (treeNodeVO == null) {
            return 0;
        }

        if (treeNodeVO.getNodeType().equalsIgnoreCase("folder")) {
            if (Long.valueOf(treeNodeVO.getParentNodeId()) == 0L) {
                throw new RuntimeException("根节点不能删除");
            }

            int childrenCount = taskMapper.selectChildrenCount(projectId, nodeId);
            if (childrenCount > 0) {
                throw new RuntimeException("不能删除含有子节点的目录");
            }

        } else {
            //  todo 任务有 task_instance 不能删除，未下线

            // 删除任务
            Long taskId = Long.valueOf(treeNodeVO.getTaskId());
            taskMapper.softDeleteTask(projectId, taskId, currentUserId, LocalDateTime.now());
        }

        return taskMapper.deleteTreeNode(projectId, nodeId);
    }

    @Transactional
    @Override
    public Integer updateTreeNode(UpdateTreeNodeDTO updateTreeNodeDTO) {
        Long currentUserId = currentUserUtil.getCurrentUserId().orElseThrow(() -> new SecurityException("未登录"));

        Long nodeId = Long.valueOf(updateTreeNodeDTO.getNodeId());
        Long projectId = Long.valueOf(updateTreeNodeDTO.getProjectId());
        String nodeType = updateTreeNodeDTO.getNodeType();

        if (StringUtils.hasText(updateTreeNodeDTO.getNodeName()) && StringUtils.hasText(updateTreeNodeDTO.getParentNodeId())) {
            throw new IllegalArgumentException("不能同时更新名字和移动目录，nodeName和parentNodeId只能传一个");
        }
        if (!StringUtils.hasText(updateTreeNodeDTO.getNodeName()) && !StringUtils.hasText(updateTreeNodeDTO.getParentNodeId())) {
            throw new IllegalArgumentException("nodeName和parentNodeId必须传一个");
        }

        // 1 这是移动节点到其他目录
        if (StringUtils.hasText(updateTreeNodeDTO.getParentNodeId())) {
            if (nodeType.equalsIgnoreCase("folder")) {
                throw new RuntimeException("不能移动目录");
            }

            Long targetParentNodeId = Long.valueOf(updateTreeNodeDTO.getParentNodeId());
            if (nodeId.longValue() == targetParentNodeId.longValue()) {
                throw new RuntimeException("不能移动自己到自己");
            }

            try {
                taskMapper.updateTreeNode(nodeId, projectId, nodeType, null, targetParentNodeId,
                        currentUserId, LocalDateTime.now());
            } catch (DuplicateKeyException e) {
                throw new DuplicateKeyException("同目录下有相同名字节点", e);
            }
        }

        // 2 这是更新节点名字
        if (StringUtils.hasText(updateTreeNodeDTO.getNodeName())) {
            String targetNodeName = updateTreeNodeDTO.getNodeName();

            // 更新node_tree表中的 nodeName
            try {
                taskMapper.updateTreeNode(nodeId, projectId, nodeType, targetNodeName, null,
                        currentUserId, LocalDateTime.now());
            } catch (DuplicateKeyException e) {
                throw new DuplicateKeyException("同目录下有相同名字节点", e);
            }

            // 是任务节点的话，更新task表中的task_name
            if (nodeType.equalsIgnoreCase("task")) {
                Long taskId;
                try {
                    taskId = Long.valueOf(updateTreeNodeDTO.getTaskId());
                } catch (NumberFormatException e) {
                    throw new NumberFormatException("更新任务节点taskId转换失败");
                }

                try {
                    taskMapper.updateTaskName(projectId, taskId, targetNodeName, currentUserId, LocalDateTime.now());
                } catch (DuplicateKeyException e) {
                    throw new DuplicateKeyException("任务表中已存在同名任务", e);
                }
            }
        }

        return 1;
    }

    // 带有版本的更新，后续如果有回滚，记得回滚生成新的版本
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskVO updateTask(UpdateTaskDTO updateTaskDTO) {
        // 1 获取当前用户
        Long currentUserId = currentUserUtil.getCurrentUserId().orElseThrow(() -> new SecurityException("未登录"));

        String projectIdStr = updateTaskDTO.getProjectId();
        String taskIdStr = updateTaskDTO.getTaskId();
        Integer taskVersion = updateTaskDTO.getTaskVersion();

        if (!StringUtils.hasText(projectIdStr) || !StringUtils.hasText(taskIdStr) || taskVersion == null) {
            throw new RuntimeException("缺少必要参数：项目id或任务id或任务版本号");
        }

        // 2 安全类型转换
        Long projectId;
        Long taskId;
        try {
            projectId = Long.valueOf(projectIdStr);
            taskId = Long.valueOf(taskIdStr);
        } catch (NumberFormatException e) {
            throw new RuntimeException("项目id或任务id格式错误");
        }

        // 3 查询当前数据库中的任务
        TaskVO oldTask = taskMapper.selectTask(projectId, taskId);
        if (oldTask == null) {
            throw new RuntimeException("任务不存在");
        }

        // 4 乐观锁前置校验（可选但推荐）
        if (!Objects.equals(oldTask.getTaskVersion(), taskVersion)) {
            throw new RuntimeException("任务版本已变更，你打开后别人已经修改保存，请复制你的修改以免丢失，然后关闭任务或者刷新后重试");
        }

        // 5 判断是否需要新增版本
        boolean needNewVersion = isNeedNewVersion(updateTaskDTO, oldTask);

        // 6 DTO 转 DAO（字段名相同可以直接拷贝）
        UpdateTaskDAO dao = new UpdateTaskDAO();
        BeanUtils.copyProperties(updateTaskDTO, dao);

        dao.setProjectId(projectId);
        dao.setTaskId(taskId);
        dao.setTaskVersion(taskVersion);

        int rows;
        // 7 执行更新
        if (needNewVersion) {
            // 版本 +1 更新
            rows = taskMapper.updateTask(dao, currentUserId, LocalDateTime.now());
            if (rows == 0) {
                throw new RuntimeException("任务更新失败，可能存在并发冲突");
            }
            // 插入版本表（插入的是更新后的最新版本）
            taskMapper.insertTaskVersion(projectId, taskId);
            // 删除过早的版本，最多保留50个版本
            taskMapper.deleteTaskVersion(projectId, taskId, taskVersion - 50);

        } else {
            // 普通更新（不增加版本号）
            rows = taskMapper.updateTaskWithoutVersion(dao, currentUserId, LocalDateTime.now());
            if (rows == 0) {
                throw new RuntimeException("任务更新失败，可能存在并发冲突");
            }
        }
        return taskMapper.selectTask(projectId, taskId);
    }

    @Override
    public TaskVO getTask(String projectId, String taskId) {
        return taskMapper.selectTask(Long.valueOf(projectId), Long.valueOf(taskId));
    }

    @Transactional
    @Override
    public void cloneTask(CloneTaskDTO cloneTaskDTO) {
        Long currentUserId = currentUserUtil.getCurrentUserId().orElseThrow(() -> new SecurityException("未登录"));

        if (!StringUtils.hasText(cloneTaskDTO.getProjectId()) || !StringUtils.hasText(cloneTaskDTO.getTaskId())
                || !StringUtils.hasText(cloneTaskDTO.getTaskName()) || !StringUtils.hasText(cloneTaskDTO.getParentNodeId())) {
            throw new RuntimeException("缺少必要参数：projectId/taskId/taskName/parentNodeId");
        }

        Long projectId;
        Long sourceTaskId;
        Long parentNodeId;
        try {
            projectId = Long.valueOf(cloneTaskDTO.getProjectId());
            sourceTaskId = Long.valueOf(cloneTaskDTO.getTaskId());
            parentNodeId = Long.valueOf(cloneTaskDTO.getParentNodeId());
        } catch (NumberFormatException e) {
            throw new RuntimeException("projectId/taskId/parentNodeId格式错误");
        }

        Long targetNodeId = snowflakeIdGenerator.nextId();
        Long targetTaskId = snowflakeIdGenerator.nextId();
        String targetTaskName = cloneTaskDTO.getTaskName();

        try {
            taskMapper.insertTreeNode(targetNodeId, projectId, targetTaskName, "task", parentNodeId, targetTaskId, currentUserId);
        } catch (DuplicateKeyException e) {
            throw new DuplicateKeyException("该目录下已存在同名任务", e);
        }

        int cnt;
        try {
            cnt = taskMapper.insertCloneTask(projectId, sourceTaskId, targetTaskId, targetTaskName, currentUserId);
        } catch (DuplicateKeyException e) {
            throw new DuplicateKeyException("任务表中已存在同名任务", e);
        }

        if (cnt == 0) {
            throw new RuntimeException("被克隆的任务不存在");
        }
    }

    private static boolean isNeedNewVersion(UpdateTaskDTO updateTaskDTO, TaskVO oldTask) {
        boolean needNewVersion = false;

        if (updateTaskDTO.getTaskSql() != null && !Objects.equals(updateTaskDTO.getTaskSql(), oldTask.getTaskSql())) {
            needNewVersion = true;
        }

        if (updateTaskDTO.getTaskParam() != null && !Objects.equals(updateTaskDTO.getTaskParam(), oldTask.getTaskParam())) {
            needNewVersion = true;
        }

        if (updateTaskDTO.getTaskSource() != null && !Objects.equals(updateTaskDTO.getTaskSource(), oldTask.getTaskSource())) {
            needNewVersion = true;
        }

        if (updateTaskDTO.getTaskSide() != null && !Objects.equals(updateTaskDTO.getTaskSide(), oldTask.getTaskSide())) {
            needNewVersion = true;
        }

        if (updateTaskDTO.getTaskSink() != null && !Objects.equals(updateTaskDTO.getTaskSink(), oldTask.getTaskSink())) {
            needNewVersion = true;
        }
        return needNewVersion;
    }


}
