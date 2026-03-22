package com.aron.studio.service.impl;

import com.aron.studio.data.dto.user.AddUserDTO;
import com.aron.studio.data.dto.user.DeleteUserDTO;
import com.aron.studio.data.dto.user.ResetPasswordDTO;
import com.aron.studio.data.dto.user.UpdatePasswordDTO;
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
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
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

        if (userMapper.countByUsername(username) > 0) {
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
    public void addUser(AddUserDTO addUserDTO) {
        if (addUserDTO == null || !StringUtils.hasText(addUserDTO.getUsername()) ||
                !StringUtils.hasText(addUserDTO.getPassword())) {
            throw new IllegalArgumentException("username/password is required");
        }

        register(addUserDTO.getUsername(), addUserDTO.getPassword());
    }

    @Override
    public void deleteUser(DeleteUserDTO deleteUserDTO) {
        if (deleteUserDTO == null || !StringUtils.hasText(deleteUserDTO.getUserId())) {
            throw new IllegalArgumentException("userId is required");
        }

        Long currentUserId = currentUserUtil.getCurrentUserId().orElseThrow(() -> new SecurityException("未登录"));
        Long targetUserId = parseUserId(deleteUserDTO.getUserId());
        UserEntity targetUser = userMapper.findByUserId(targetUserId);
        if (targetUser == null) {
            throw new RuntimeException("用户不存在或已禁用");
        }

        int rows = userMapper.disableUserByUserId(targetUserId, currentUserId, LocalDateTime.now());
        if (rows != 1) {
            throw new RuntimeException("删除用户失败");
        }
    }

    @Override
    public void updatePassword(UpdatePasswordDTO updatePasswordDTO) {
        if (updatePasswordDTO == null || !StringUtils.hasText(updatePasswordDTO.getUserId()) ||
                !StringUtils.hasText(updatePasswordDTO.getOldPassword()) ||
                !StringUtils.hasText(updatePasswordDTO.getNewPassword())) {
            throw new IllegalArgumentException("userId/oldPassword/newPassword is required");
        }

        Long currentUserId = currentUserUtil.getCurrentUserId().orElseThrow(() -> new SecurityException("未登录"));
        Long targetUserId = parseUserId(updatePasswordDTO.getUserId());

        // 只允许修改自己的密码
        if (!currentUserId.equals(targetUserId)) {
            throw new SecurityException("只能修改自己的密码");
        }

        UserEntity targetUser = userMapper.findByUserId(targetUserId);
        if (targetUser == null) {
            throw new RuntimeException("用户不存在或已禁用");
        }

        if (!passwordEncoder.matches(updatePasswordDTO.getOldPassword(), targetUser.getPassword())) {
            throw new RuntimeException("旧密码错误");
        }

        int rows = userMapper.updatePasswordByUserId(targetUserId,
                passwordEncoder.encode(updatePasswordDTO.getNewPassword()), currentUserId, LocalDateTime.now());
        if (rows != 1) {
            throw new RuntimeException("修改密码失败");
        }
    }

    @Override
    public void resetPassword(ResetPasswordDTO resetPasswordDTO) {
        if (resetPasswordDTO == null || !StringUtils.hasText(resetPasswordDTO.getUserId()) ||
                !StringUtils.hasText(resetPasswordDTO.getNewPassword())) {
            throw new IllegalArgumentException("userId/newPassword is required");
        }

        Long currentUserId = currentUserUtil.getCurrentUserId().orElseThrow(() -> new SecurityException("未登录"));
        String currentRole = currentUserUtil.getCurrentRole().orElseThrow(() -> new SecurityException("未登录"));
        if (!RoleEnum.ROLE_ADMIN.getCode().equals(currentRole)) {
            throw new SecurityException("只有管理员可以重置密码");
        }

        Long targetUserId = parseUserId(resetPasswordDTO.getUserId());
        UserEntity targetUser = userMapper.findByUserId(targetUserId);
        if (targetUser == null) {
            throw new RuntimeException("用户不存在或已禁用");
        }

        int rows = userMapper.updatePasswordByUserId(targetUserId,
                passwordEncoder.encode(resetPasswordDTO.getNewPassword()), currentUserId, LocalDateTime.now());
        if (rows != 1) {
            throw new RuntimeException("重置密码失败");
        }
    }

    private Long parseUserId(String userId) {
        try {
            return Long.valueOf(userId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("userId格式错误");
        }
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
