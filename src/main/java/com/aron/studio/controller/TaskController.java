package com.aron.studio.controller;

import com.aron.studio.data.Response;
import com.aron.studio.data.dto.task.CloneTaskDTO;
import com.aron.studio.data.dto.task.UpdateTaskDTO;
import com.aron.studio.data.dto.tree.CreateTreeNodeDTO;
import com.aron.studio.data.dto.tree.DeleteTreeNodeDTO;
import com.aron.studio.data.dto.tree.UpdateTreeNodeDTO;
import com.aron.studio.data.vo.TaskVO;
import com.aron.studio.data.vo.TreeNodeVO;
import com.aron.studio.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 *
 */
@Slf4j
@RestController
@RequestMapping("/api/task")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @PostMapping("/createTreeNode")
    public Response<Map<String, String>> createTreeNode(@RequestBody CreateTreeNodeDTO createTreeNodeDTO) {
        log.info("call createTreeNode, param: {}", createTreeNodeDTO);
        return Response.success(taskService.createTreeNode(createTreeNodeDTO));
    }

    @GetMapping("/getTreeNode")
    public Response<List<TreeNodeVO>> getTreeNode(@RequestParam("projectId") String projectId) {
        return Response.success(taskService.getTreeNode(projectId));
    }

    @PostMapping("/deleteTreeNode")
    public Response<Integer> deleteTreeNode(@RequestBody DeleteTreeNodeDTO deleteTreeNodeDTO) {
        log.info("call deleteTreeNode, param: {}", deleteTreeNodeDTO);
        return Response.success(taskService.deleteTreeNode(deleteTreeNodeDTO));
    }

    /**
     * 更新节点，包括节点重命名和节点移动目录
     *
     * @param updateTreeNodeDTO
     * @return
     */
    @PostMapping("/updateTreeNode")
    public Response<Integer> updateTreeNode(@RequestBody UpdateTreeNodeDTO updateTreeNodeDTO) {
        log.info("call updateTreeNode, param: {}", updateTreeNodeDTO);
        return Response.success(taskService.updateTreeNode(updateTreeNodeDTO));
    }

    /**
     * 只承担从 编辑器区域更改任务的逻辑，在树节点修改参考 updateTreeNode
     * 所以此方法不修改 taskName, 不修改它的父目录 parentNodeId
     *
     * @param updateTaskDTO
     * @return
     */
    @PostMapping("/updateTask")
    public Response<TaskVO> updateTask(@RequestBody UpdateTaskDTO updateTaskDTO) {
        log.info("call updateTask, taskId: {}, taskName: {}", updateTaskDTO.getTaskId(), updateTaskDTO.getTaskName());
        return Response.success(taskService.updateTask(updateTaskDTO));
    }

    @GetMapping("/getTask")
    public Response<TaskVO> getTask(@RequestParam("projectId") String projectId, @RequestParam("taskId") String taskId) {
        TaskVO taskVO = taskService.getTask(projectId, taskId);
        return Response.success(taskVO);
    }

    @PostMapping("/cloneTask")
    public Response<Void> cloneTask(@RequestBody CloneTaskDTO cloneTaskDTO) {
        log.info("call cloneTask, param: {}", cloneTaskDTO);
        taskService.cloneTask(cloneTaskDTO);
        return Response.success();
    }


}
