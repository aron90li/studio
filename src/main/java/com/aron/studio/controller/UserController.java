package com.aron.studio.controller;

import com.aron.studio.data.Response;
import com.aron.studio.data.dto.user.AddUserDTO;
import com.aron.studio.data.dto.user.DeleteUserDTO;
import com.aron.studio.data.dto.user.ResetPasswordDTO;
import com.aron.studio.data.dto.user.UpdatePasswordDTO;
import com.aron.studio.data.vo.UserVO;
import com.aron.studio.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 获取当前登陆用户信息
     *
     * @return
     */
    @GetMapping("/getCurrentUser")
    public Response<UserVO> getCurrentUser() {
        try {
            log.info("call getCurrentUser...");
            return Response.success(userService.getCurrentUser());
        } catch (Exception e) {
            log.error("call getCurrentUser error: ", e);
            return Response.fail(e.getMessage());
        }
    }

    /**
     * 获取所有用户，供授权使用 ROLE_ADMIN
     *
     * @return
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/getAllUsers")
    public Response<List<UserVO>> getAllUsers() {
        log.info("call getAllUsers...");
        List<UserVO> userVOs = userService.getAllUsers();
        return Response.success(userVOs);
    }

    /**
     * 修改密码，普通用户和管理员用户均可调用，所有用户包括管理员只允许修改自己的密码
     *
     * @param updatePasswordDTO
     * @return
     */
    @PostMapping("/updatePassword")
    public Response<Void> updatePassword(@RequestBody UpdatePasswordDTO updatePasswordDTO) {
        try {
            log.info("call updatePassword, userId: {}", updatePasswordDTO.getUserId());
            userService.updatePassword(updatePasswordDTO);
            return Response.success();
        } catch (Exception e) {
            log.error("call updatePassword error: ", e);
            return Response.fail(e.getMessage());
        }
    }

    /**
     * 重置密码，只有管理员可以调用，适用于不知道密码的情况。可以重置所有用户的密码
     *
     * @param resetPasswordDTO
     * @return
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/resetPassword")
    public Response<Void> resetPassword(@RequestBody ResetPasswordDTO resetPasswordDTO) {
        try {
            log.info("call resetPassword, userId: {}", resetPasswordDTO.getUserId());
            userService.resetPassword(resetPasswordDTO);
            return Response.success();
        } catch (Exception e) {
            log.error("call resetPassword error: ", e);
            return Response.fail(e.getMessage());
        }
    }

    /**
     * 添加用户
     *
     * @param
     * @return
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/addUser")
    public Response<Void> addUser(@RequestBody AddUserDTO addUserDTO) {
        try {
            log.info("call addUser, username: {}", addUserDTO.getUsername());
            userService.addUser(addUserDTO);
            return Response.success();
        } catch (Exception e) {
            log.error("call addUser error: ", e);
            return Response.fail(e.getMessage());
        }
    }

    /**
     * 删除用户
     *
     * @param
     * @return
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/deleteUser")
    public Response<Void> deleteUser(@RequestBody DeleteUserDTO deleteUserDTO) {
        try {
            log.info("call deleteUser, userId: {}", deleteUserDTO.getUserId());
            userService.deleteUser(deleteUserDTO);
            return Response.success();
        } catch (Exception e) {
            log.error("call deleteUser error: ", e);
            return Response.fail(e.getMessage());
        }
    }


}
