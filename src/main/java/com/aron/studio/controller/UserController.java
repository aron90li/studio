package com.aron.studio.controller;

import com.aron.studio.data.Response;
import com.aron.studio.data.vo.UserVO;
import com.aron.studio.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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


}
