package com.aron.studio.service.impl;

import com.aron.studio.data.dao.UpdateTaskDAO;
import com.aron.studio.data.dto.task.UpdateTaskDTO;
import com.aron.studio.data.dto.tree.CreateTreeNodeDTO;
import com.aron.studio.data.dto.tree.DeleteTreeNodeDTO;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
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
        Long parentNodeId = Long.valueOf(createTreeNodeDTO.getParentNodeId()); // 除了根目录, 其他不许为空
        String nodeName = createTreeNodeDTO.getNodeName();
        String nodeType = createTreeNodeDTO.getNodeType();

        // 目录同级不能同名， 任务目前全局唯一
        if (nodeType.equalsIgnoreCase("folder")) {
            if (taskMapper.countByNodeNameAndParentNodeId(nodeName, "folder", parentNodeId) > 0) {
                throw new RuntimeException("同级已存在同名的目录");
            }

            int cnt = taskMapper.insertTreeNode(nodeId, projectId, nodeName, "folder", parentNodeId, null, currentUserId);
            return Map.of("nodeId", nodeId.toString());
        } else if (nodeType.equalsIgnoreCase("task")) {
            if (taskMapper.countByNodeName(nodeName, "task") > 0) {
                throw new RuntimeException("已存在同名的任务");
            }

            if (taskMapper.countTaskByTaskName(nodeName) > 0) {
                throw new RuntimeException("已存在同名的任务");
            }

            Long taskId = snowflakeIdGenerator.nextId();
            int cnt = taskMapper.insertTreeNode(nodeId, projectId, nodeName, "task", parentNodeId, taskId, currentUserId);

            // 项目下的环境参数模板
            String taskParam = null;
            List<ProjectDetailVO> pdList = projectMapper.getProjectDetail(projectId, "env_template");
            if (pdList != null && pdList.size() > 0) {
                taskParam = pdList.get(1).getDetailValue();
            }

            // sql前缀
            String taskSql = String.format("-- createUser: %s\n-- createTime: %s", currentUserUtil.getCurrentUsername().get(),
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            int taskCnt = taskMapper.insertTask(projectId, taskId, nodeName, currentUserId, taskParam, taskSql);

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
        Long projectId = Long.valueOf(deleteTreeNodeDTO.getProjectId());
        Long nodeId = Long.valueOf(deleteTreeNodeDTO.getNodeId());

        TreeNodeVO treeNodeVO = taskMapper.getTreeNodeByNodeId(projectId, nodeId);
        if (treeNodeVO == null) {
            return 0;
        }

        if (treeNodeVO.getNodeType().equalsIgnoreCase("folder")) {
            if (treeNodeVO.getParentNodeId() == null) {
                throw new RuntimeException("根节点不能删除");
            }

            int childrenCount = taskMapper.selectChildrenCount(projectId, nodeId);
            if (childrenCount > 0) {
                throw new RuntimeException("不能删除含有子节点的目录");
            }
        }

        return taskMapper.deleteTreeNode(projectId, nodeId);
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
            rows = taskMapper.updateTask(dao, currentUserId);
            if (rows == 0) {
                throw new RuntimeException("任务更新失败，可能存在并发冲突");
            }
            // 插入版本表（插入的是更新后的最新版本）
            taskMapper.insertTaskVersion(projectId, taskId);
            // 删除过早的版本，最多保留50个版本
            taskMapper.deleteTaskVersion(projectId, taskId, taskVersion - 50);

        } else {
            // 普通更新（不增加版本号）
            rows = taskMapper.updateTaskWithoutVersion(dao, currentUserId);
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
