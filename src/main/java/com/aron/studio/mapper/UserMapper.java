package com.aron.studio.mapper;

import com.aron.studio.data.rbac.entity.UserEntity;
import com.aron.studio.data.vo.UserVO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserMapper {

    @Select("""
                SELECT user_id, username, password, enabled, role, create_time, update_time
                FROM sys_user
                WHERE username = #{username} and enabled = 1
            """)
    UserEntity findByUsername(@Param("username") String username);

    @Select("""
                SELECT COUNT(*) FROM sys_user WHERE username = #{username}
            """)
    int countByUsername(@Param("username") String username);

    @Select("""
                SELECT user_id, username, password, enabled, role, create_time, update_time
                FROM sys_user
                WHERE user_id = #{userId} and enabled = 1
            """)
    UserEntity findByUserId(@Param("userId") Long userId);

    @Insert("""
                INSERT INTO sys_user (user_id, username, password, enabled)
                VALUES (#{userId}, #{username}, #{password}, #{enabled})
            """)
    // @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserEntity userEntity);

    @Select("""
                SELECT user_id FROM sys_user WHERE role =  #{role} and enabled = 1
            """)
    List<Long> selectUserIdsByRole(@Param("role") String role);

    @Update("""
                UPDATE sys_user
                SET password = #{password},
                    update_user = #{updateUser},
                    update_time = #{updateTime}
                WHERE user_id = #{userId}
                  AND enabled = 1
            """)
    int updatePasswordByUserId(@Param("userId") Long userId, @Param("password") String password,
                               @Param("updateUser") Long updateUser, @Param("updateTime") LocalDateTime updateTime);

    @Update("""
                UPDATE sys_user
                SET enabled = 0,
                    update_user = #{updateUser},
                    update_time = #{updateTime}
                WHERE user_id = #{userId}
                  AND enabled = 1
            """)
    int disableUserByUserId(@Param("userId") Long userId, @Param("updateUser") Long updateUser,
                            @Param("updateTime") LocalDateTime updateTime);


    @Select("""
                SELECT user_id as userId, 
                       username as username,
                       role as role,
                       create_time as createTime,
                       update_time as updateTime
                FROM sys_user
                WHERE enabled = 1
            """)
    List<UserVO> getAllUsers();

}
