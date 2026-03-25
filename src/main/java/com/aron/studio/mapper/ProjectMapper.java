package com.aron.studio.mapper;

import com.aron.studio.data.dao.UpdateProjectDAO;
import com.aron.studio.data.rbac.entity.ProjectDetailEntity;
import com.aron.studio.data.rbac.entity.ProjectEntity;
import com.aron.studio.data.vo.ProjectDetailVO;
import com.aron.studio.data.vo.ProjectVO;
import com.aron.studio.data.vo.UserVO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ProjectMapper {

    @Insert("""
                INSERT INTO project (project_id, project_name, project_identity, 
                                  delete_id, description, create_user, update_user)
                VALUES (#{projectId}, #{projectName}, #{projectIdentity}, 
                        #{deleteId}, #{description}, #{createUser}, #{updateUser})
            """)
        // @Options(useGeneratedKeys = true, keyProperty = "id")
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
                    LEFT JOIN sys_user cu
                        ON p.create_user = cu.user_id
                    LEFT JOIN sys_user uu
                        ON p.update_user = uu.user_id
                    WHERE pu.user_id = #{userId}
                      AND p.delete_id = 0
                    ORDER BY p.create_time DESC
            """)
    List<ProjectVO> getProjectByUserId(Long userId);


    @Update("""
            update project set project_name=#{dao.projectName}, description=#{dao.description}, 
                               update_user=#{currentUserId}, update_time = #{updateTime} 
                           where project_id = #{dao.projectId} and delete_id=0
            """)
    int updateProject(@Param("dao") UpdateProjectDAO updateProjectDAO, @Param("currentUserId") Long currentUserId,
                      @Param("updateTime") LocalDateTime updateTime);

    @Update("""
                UPDATE project SET delete_id = project_id
                WHERE project_id = #{projectId} AND delete_id = 0
            """)
    int softDeleteProject(@Param("projectId") Long projectId);


    @Delete(""" 
            <script> DELETE FROM project_user WHERE project_id IN
            <foreach collection="projectIds" item="projectId" open="(" separator="," close=")">
                #{projectId}
            </foreach>
            </script>
            """)
    int deleteProjectUserByProjectIds(@Param("projectIds") List<Long> projectIds);

    // project_user
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
                JOIN sys_user u 
                ON pu.user_id = u.user_id
                where pu.project_id =  #{projectId}            
            """)
    List<UserVO> getProjectUsers(@Param("projectId") Long projectId);

    @Delete("""
            delete from project_user where project_id= #{projectId} and user_id=#{userId} 
            """)
    int deleteProjectUser(@Param("projectId") Long projectId, @Param("userId") Long userId);

    // project_detail
    @Select("""
                select count(*) from project_detail
                where project_id = #{projectId} and detail_type = #{detailType}
            """)
    int getCountByProjectDetail(@Param("projectId") Long projectId, @Param("detailType") String detailType);

    @Insert("""
                INSERT INTO project_detail (project_id, detail_type, detail_value,
                                            create_user, update_user)
                VALUES (#{projectId}, #{detailType}, #{detailValue},
                        #{createUser}, #{updateUser})
            """)
        // @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertProjectDetail(ProjectDetailEntity projectDetailEntity);

    @Update("""
                update project_detail set detail_value = #{detailValue}, update_user = #{updateUser}, 
                update_time = #{updateTime} 
                where project_id = #{projectId} and detail_type = #{detailType}
            """)
    int updateProjectDetail(@Param("projectId") Long projectId, @Param("detailType") String detailType,
                            @Param("detailValue") String detailValue, @Param("updateUser") Long updateUser,
                            @Param("updateTime") LocalDateTime updateTime);

    @Select("""
            <script>
            SELECT
                    pd.project_id   AS projectId,
                    pd.detail_type  AS detailType,
                    pd.detail_value AS detailValue,
                    pd.create_time  AS createTime,
                    pd.update_time  AS updateTime,
                    cu.username     AS createUsername,
                    cu.user_id      AS createUserId,
                    uu.username     AS updateUsername,
                    uu.user_id      AS updateUserId
            FROM project_detail pd
            LEFT JOIN sys_user cu
                ON pd.create_user = cu.user_id
            LEFT JOIN sys_user uu
                ON pd.update_user = uu.user_id
            WHERE pd.project_id = #{projectId}
            <if test="detailType != null and detailType != ''">
                AND pd.detail_type = #{detailType}
            </if>
            ORDER BY pd.create_time DESC
            </script>
            """)
    List<ProjectDetailVO> getProjectDetail(@Param("projectId") Long projectId, @Param("detailType") String detailType);

}
