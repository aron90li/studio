package com.aron.studio.mapper;

import com.aron.studio.data.rbac.entity.UserEntity;
import com.aron.studio.data.vo.UserVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("""
                SELECT user_id, username, password, enabled, role, create_time, update_time
                FROM sys_user
                WHERE username = #{username} and enabled = 1                
            """)
    UserEntity findByUsername(@Param("username") String username);

    @Insert("""
                INSERT INTO sys_user (user_id, username, password, enabled)
                VALUES (#{userId}, #{username}, #{password}, #{enabled})
            """)
    // @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserEntity userEntity);

    @Select("""
                SELECT user_id FROM sys_user WHERE role =  #{role}               
            """)
    List<Long> selectUserIdsByRole(@Param("role") String role);


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
