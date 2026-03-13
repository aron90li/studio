package com.aron.studio.service.impl;

import com.aron.studio.data.enums.RoleEnum;
import com.aron.studio.data.rbac.entity.UserEntity;
import com.aron.studio.data.vo.UserVO;
import com.aron.studio.mapper.UserMapper;
import com.aron.studio.service.UserService;
import com.aron.studio.util.CurrentUserUtil;
import com.aron.studio.util.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserMapper userMapper;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    SnowflakeIdGenerator snowflakeIdGenerator;

    @Autowired
    CurrentUserUtil currentUserUtil;

    @Override
    public Long register(String username, String password) {

        if (userMapper.findByUsername(username) != null) {
            throw new RuntimeException("用户名已存在");
        }
        Long userId = snowflakeIdGenerator.nextId();
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(username);
        userEntity.setPassword(passwordEncoder.encode(password));
        userEntity.setEnabled(true);
        userEntity.setUserId(userId);
        userEntity.setRole(RoleEnum.ROLE_USER.getCode());

        int rows = userMapper.insert(userEntity);
        if (rows != 1) {
            throw new RuntimeException("注册失败");
        }
        return userEntity.getUserId();
    }

    @Override
    public UserVO getCurrentUser() {
        Long currentUserId = currentUserUtil.getCurrentUserId().orElseThrow(() -> new SecurityException("未登录"));
        String userName = currentUserUtil.getCurrentUsername().get();
        UserVO userVO = new UserVO();
        UserEntity userEntity = userMapper.findByUsername(userName);

        userVO.setUserId(userEntity.getUserId().toString());
        userVO.setUsername(userEntity.getUsername());
        userVO.setCreateTime(userEntity.getCreateTime());
        userVO.setUpdateTime(userEntity.getUpdateTime());
        userVO.setRole(userEntity.getRole());
        return userVO;
    }

    @Override
    public List<UserVO> getAllUsers() {
        return userMapper.getAllUsers();
    }

    @Override
    public UserEntity loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity userEntity = userMapper.findByUsername(username);
        if (userEntity == null) {
            throw new UsernameNotFoundException("不存在用户: " + username);
        }
        return userEntity;
    }
}
