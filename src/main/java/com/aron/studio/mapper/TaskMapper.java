package com.aron.studio.mapper;

import com.aron.studio.data.dao.UpdateTaskDAO;
import com.aron.studio.data.vo.TaskVO;
import com.aron.studio.data.vo.TreeNodeVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TaskMapper {
    // getCountByNodeName
    @Select("""
                select count(*) from tree_node where node_name = #{nodeName} and node_type = #{nodeType}
            """)
    int countByNodeName(@Param("nodeName") String nodeName, @Param("nodeType") String nodeType);

    @Select("""
                select count(*) from tree_node where node_name = #{nodeName} and node_type = #{nodeType} 
                and parent_node_id = #{parentNodeId}
            """)
    int countByNodeNameAndParentNodeId(@Param("nodeName") String nodeName, @Param("nodeType") String nodeType,
                                       @Param("parentNodeId") Long parentNodeId);

    // tree_node操作
    @Insert("""
                INSERT INTO tree_node (node_id, project_id, node_name, node_type, 
                   parent_node_id, task_id, create_user)
                VALUES (#{nodeId}, #{projectId}, #{nodeName}, #{nodeType}, 
                        #{parentNodeId}, #{taskId}, #{createUser})
            """)
    int insertTreeNode(@Param("nodeId") Long nodeId, @Param("projectId") Long projectId,
                       @Param("nodeName") String nodeName, @Param("nodeType") String nodeType,
                       @Param("parentNodeId") Long parentNodeId, @Param("taskId") Long taskId,
                       @Param("createUser") Long createUser);


    @Select("""
                select node_id as nodeId,
                       project_id as projectId,
                       node_name as nodeName,
                       node_type as nodeType,
                       parent_node_id as parentNodeId,
                       task_id as taskId
                from tree_node where project_id = #{projectId}
            """)
    List<TreeNodeVO> getTreeNode(@Param("projectId") Long projectId);

    @Select("""
                select node_id as nodeId,
                       project_id as projectId,
                       node_name as nodeName,
                       node_type as nodeType,
                       parent_node_id as parentNodeId,
                       task_id as taskId
                from tree_node where project_id = #{projectId} and node_id=#{nodeId} limit 1
            """)
    TreeNodeVO getTreeNodeByNodeId(@Param("projectId") Long projectId, @Param("nodeId") Long nodeId);

    @Delete("""
                delete from tree_node where project_id = #{projectId} and node_id = #{nodeId}
            """)
    int deleteTreeNode(@Param("projectId") Long projectId, @Param("nodeId") Long nodeId);

    @Select("""
                select count(*) from tree_node where project_id = #{projectId} and parent_node_id = #{nodeId}
            """)
    int selectChildrenCount(@Param("projectId") Long projectId, @Param("nodeId") Long nodeId);

    /// ////////////////////////////////////////////////////////////////////////////////////////////

    // task, task_version 操作
    @Select("""
                select count(*) from task where task_name = #{taskName}
            """)
    int countTaskByTaskName(@Param("taskName") String taskName);

    // 插入 task 表
    @Insert("""
             insert into task (project_id, task_id, task_name, create_user) values 
             (#{projectId}, #{taskId}, #{taskName}, #{createUser})
            """)
    int insertTask(@Param("projectId") Long projectId, @Param("taskId") Long taskId,
                   @Param("taskName") String taskName, @Param("createUser") Long createUser);

    // 更新 task 表 , 返回 0 说明该版本已经更新过了，业务端要处理这种现象
    // 这个方法 每次修改都增加版本号，后续要优化成特定字段的改变才增加版本号，需要service层协同处理
    @Update("""
            <script>
            UPDATE task
            <set>
                <if test="dao.taskName != null">
                    task_name = #{dao.taskName},
                </if>
                <if test="dao.description != null">
                    description = #{dao.description},
                </if>
                <if test="dao.taskType != null">
                    task_type = #{dao.taskType},
                </if>
                <if test="dao.taskSql != null">
                    task_sql = #{dao.taskSql},
                </if>
                <if test="dao.taskParam != null">
                    task_param = #{dao.taskParam},
                </if>
                <if test="dao.taskSource != null">
                    task_source = #{dao.taskSource},
                </if>
                <if test="dao.taskSide != null">
                    task_side = #{dao.taskSide},
                </if>
                <if test="dao.taskSink != null">
                    task_sink = #{dao.taskSink},
                </if>
                <if test="dao.deleted != null">
                    deleted = #{dao.deleted},
                </if>
                <if test="dao.publishStatus != null">
                    publish_status = #{dao.publishStatus},
                </if>
                task_version = task_version + 1, update_user = #{updateUser}
            </set>
            WHERE task_id = #{dao.taskId} AND project_id = #{dao.projectId} and task_version = #{dao.taskVersion}
            </script>
            """)
    int updateTask(@Param("dao") UpdateTaskDAO dao, @Param("updateUser") Long updateUser);

    @Update("""
            <script>
            UPDATE task
            <set>
                <if test="dao.taskName != null">
                    task_name = #{dao.taskName},
                </if>
                <if test="dao.description != null">
                    description = #{dao.description},
                </if>
                <if test="dao.taskType != null">
                    task_type = #{dao.taskType},
                </if>
                <if test="dao.taskSql != null">
                    task_sql = #{dao.taskSql},
                </if>
                <if test="dao.taskParam != null">
                    task_param = #{dao.taskParam},
                </if>
                <if test="dao.taskSource != null">
                    task_source = #{dao.taskSource},
                </if>
                <if test="dao.taskSide != null">
                    task_side = #{dao.taskSide},
                </if>
                <if test="dao.taskSink != null">
                    task_sink = #{dao.taskSink},
                </if>
                <if test="dao.deleted != null">
                    deleted = #{dao.deleted},
                </if>
                <if test="dao.publishStatus != null">
                    publish_status = #{dao.publishStatus},
                </if>
                update_user = #{updateUser}
            </set>
            WHERE task_id = #{dao.taskId} AND project_id = #{dao.projectId} and task_version = #{dao.taskVersion}
            </script>
            """)
    int updateTaskWithoutVersion(@Param("dao") UpdateTaskDAO dao, @Param("updateUser") Long updateUser);

    // 获取 task
    @Select("""
            select
                project_id   as projectId,
                task_id      as taskId,
                task_name    as taskName,
                description  as description,
                task_type    as taskType,
                task_sql     as taskSql,
                task_param   as taskParam,
                task_source  as taskSource,
                task_side    as taskSide,
                task_sink    as taskSink,
                task_version as taskVersion 
            from task 
            where project_id=#{projectId} and task_id=#{taskId}
            """)
    TaskVO selectTask(@Param("projectId") Long projectId, @Param("taskId") Long taskId);

    // 插入版本表 task_version
    @Insert("""
             INSERT INTO task_version (
                   task_id, task_version, project_id, task_name, description, task_type, task_sql,
                   task_param, task_source, task_side, task_sink, create_user, update_user 
               )
               SELECT
                   task_id, task_version, project_id, task_name, description, task_type, task_sql,
                   task_param, task_source, task_side, task_sink, create_user, update_user 
               FROM task
               WHERE task_id = #{taskId}
               AND project_id = #{projectId}
            """)
    int insertTaskVersion(@Param("projectId") Long projectId, @Param("taskId") Long taskId);


}
