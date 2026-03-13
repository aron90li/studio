package com.aron.studio.mapper;

import com.aron.studio.data.dao.UpdateProjectDAO;
import com.aron.studio.data.rbac.entity.ProjectEntity;
import com.aron.studio.data.vo.ProjectVO;
import com.aron.studio.data.vo.UserVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProjectMapper {

    @Select("""
                select count(*) from project where project_name = #{projectName}
            """)
    int getCountByProjectName(@Param("projectName") String projectName);

    @Insert("""
                INSERT INTO project (project_id, project_name, project_identity, 
                                  enabled, description, create_user, update_user)
                VALUES (#{projectId}, #{projectName}, #{projectIdentity}, 
                        #{enabled}, #{description}, #{createUser}, #{updateUser})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertProject(ProjectEntity userEntity);


    @Select("""
                SELECT
                        p.project_id       AS projectId,
                        p.project_name     AS projectName,
                        p.project_identity AS projectIdentity,
                        p.description      AS description,
                        p.create_time      AS createTime,
                        p.update_time      AS updateTime,            
                        cu.username        AS createUsername,
                        cu.user_id         AS createUserId,            
                        uu.username        AS updateUsername,
                        uu.user_id         AS updateUserId
                    FROM project_user pu
                    JOIN project p
                        ON pu.project_id = p.project_id
                    LEFT JOIN user cu
                        ON p.create_user = cu.user_id
                    LEFT JOIN user uu
                        ON p.update_user = uu.user_id
                    WHERE pu.user_id = #{userId}
                      AND p.enabled = 1
                    ORDER BY p.create_time DESC
            """)
    List<ProjectVO> getProjectByUserId(Long userId);


    @Update("""
            update project set project_name=#{dao.projectName}, description=#{dao.description}, 
                               update_user=#{currentUserId}
                           where project_id = #{dao.projectId}
            """)
    int updateProject(@Param("dao") UpdateProjectDAO updateProjectDAO, @Param("currentUserId") Long currentUserId);

    @Delete(""" 
            <script> DELETE FROM project WHERE project_id IN
            <foreach collection="projectIds" item="projectId" open="(" separator="," close=")">
                #{projectId}
            </foreach>
            </script>
            """)
    int deleteProjects(@Param("projectIds") List<Long> projectIds);

    @Delete(""" 
            <script> DELETE FROM project_user WHERE project_id IN
            <foreach collection="projectIds" item="projectId" open="(" separator="," close=")">
                #{projectId}
            </foreach>
            </script>
            """)
    int deleteProjectUser(@Param("projectIds") List<Long> projectIds);

    @Insert("""
                INSERT INTO project_user (project_id, user_id, project_role, create_user)
                VALUES (#{projectId}, #{userId}, #{projectRole}, #{createUser})
            """)
        // @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertProjectUser(@Param("projectId") Long projectId, @Param("userId") Long userId, @Param("projectRole") String projectRole, @Param("createUser") Long createUser);

    @Select("""
                SELECT
                        u.user_id   AS userId,
                        u.username  AS username,
                        u.role      AS role,            
                        u.create_time        AS createTime,
                        u.update_time        AS updateTime
                FROM project_user pu
                JOIN user u 
                ON pu.user_id = u.user_id
                where pu.project_id =  #{projectId}            
            """)
    List<UserVO> getProjectUsers(@Param("projectId") Long projectId);

    @Update("""
            delete from project_user where project_id= #{projectId} and user_id=#{userId}                
            """)
    int deleteProjectUser(@Param("projectId") Long projectId, @Param("userId") Long userId);


}
