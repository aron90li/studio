package com.aron.studio.service;

import com.aron.studio.data.dto.tree.CreateTreeNodeDTO;
import com.aron.studio.data.dto.tree.DeleteTreeNodeDTO;
import com.aron.studio.data.dto.task.UpdateTaskDTO;
import com.aron.studio.data.vo.TaskVO;
import com.aron.studio.data.vo.TreeNodeVO;

import java.util.List;
import java.util.Map;

public interface TaskService {

    // todo 添加更新 treeNode 后要注意 nodeName 和 task表中 taskName同步更新
    Map<String, String> createTreeNode(CreateTreeNodeDTO createTreeNodeDTO);

    List<TreeNodeVO> getTreeNode(String projectId);

    Integer deleteTreeNode(DeleteTreeNodeDTO deleteTreeNodeDTO);

    // 创建节点后自动创建了task，后续只是更新task中的内容
    TaskVO updateTask(UpdateTaskDTO updateTaskDTO);

    TaskVO getTask(String projectId, String taskId);
}
