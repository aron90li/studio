package com.aron.studio.service;

import com.aron.studio.data.dto.user.AddUserDTO;
import com.aron.studio.data.dto.user.UpdateEnabledDTO;
import com.aron.studio.data.dto.user.ResetPasswordDTO;
import com.aron.studio.data.dto.user.UpdatePasswordDTO;
import com.aron.studio.data.vo.UserVO;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

public interface UserService extends UserDetailsService {

    Long register(String username, String password);

    UserVO getCurrentUser();

    List<UserVO> getAllUsers();

    void addUser(AddUserDTO addUserDTO);

    void updateEnabled(UpdateEnabledDTO updateEnabledDTO);

    void updatePassword(UpdatePasswordDTO updatePasswordDTO);

    void resetPassword(ResetPasswordDTO resetPasswordDTO);
}
