package com.aron.studio.service;

import com.aron.studio.data.rbac.entity.UserEntity;
import com.aron.studio.data.vo.UserVO;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

public interface UserService extends UserDetailsService {

    Long register(String username, String password);

    UserVO getCurrentUser();

    List<UserVO> getAllUsers();
}
