package com.aron.studio.controller;

import com.aron.studio.data.Response;
import com.aron.studio.data.dto.login.LoginDTO;
import com.aron.studio.data.dto.login.RegisterDTO;
import com.aron.studio.data.rbac.entity.UserEntity;
import com.aron.studio.data.vo.LoginVO;
import com.aron.studio.service.UserService;
import com.aron.studio.util.CaptchaUtil;
import com.aron.studio.util.JwtUtil;
import com.google.code.kaptcha.Producer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private CaptchaUtil captchaUtil;

    @Autowired
    public Producer producer;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/captcha")
    public Response<Map<String, String>> captcha() {
        try {
            log.info("call captcha...");
            Map<String, String> captcha = captchaUtil.generateCaptcha(producer);
            log.info("return captcha: {}", captcha.get("uuid"));
            return Response.success(captcha);
        } catch (IOException e) {
            log.error("get captcha error: ", e);
            return Response.fail(e.getMessage());
        }
    }

    @PostMapping("/register")
    public Response<Map<String, String>> register(@RequestBody RegisterDTO registerDTO) {
        try {
            log.info("call register, params: {}", registerDTO);
            captchaUtil.validateCaptcha(registerDTO.getUuid(), registerDTO.getCode());
            Long userId = userService.register(registerDTO.getUsername(), registerDTO.getPassword());
            return Response.success(Map.of("userId", userId.toString()));
        } catch (RuntimeException e0) {
            log.error("error: ", e0);
            return Response.fail(e0.getMessage());
        } catch (Exception e) {
            log.error("error: ", e);
            return Response.fail(e.getMessage());
        }
    }

    @PostMapping("/login")
    public Response<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        try {
            log.info("call login, params: {}", loginDTO);
            captchaUtil.validateCaptcha(loginDTO.getUuid(), loginDTO.getCode());
            UserEntity userEntity = (UserEntity) userService.loadUserByUsername(loginDTO.getUsername());

            if (userEntity == null || !userEntity.isEnabled()) {
                throw new Exception("用户不存在或者被禁用");
            }

            if (!passwordEncoder.matches(loginDTO.getPassword(), userEntity.getPassword())) {
                throw new RuntimeException("用户名或密码错误");
            }
            String userId = String.valueOf(userEntity.getUserId());
            String token = jwtUtil.generateToken(userEntity.getUsername(), userEntity.getRole(), userId);
            log.info("username: {}, token: {}", userEntity.getUsername(), token);
            LoginVO loginVO = new LoginVO();
            loginVO.setToken(token);
            return Response.success(loginVO);
        } catch (Exception e) {
            log.error("error: ", e);
            return Response.fail(e.getMessage());
        }
    }

    @PostMapping("/refresh-token")
    public Response<Void> refreshToken() {
        return null;
    }

    @PostMapping("/logout")
    public Response<Void> logout() {
        // 前端删除本地存储的token
        return Response.success();
    }


}
