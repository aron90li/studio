package com.aron.studio.config;

import com.aron.studio.data.Response;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    // todo 其他异常在此定义，不要在controller中 try...catch...
    // 目前还没用到，目前在controller中写了 try...catch...
    // !!! 这里的异常顺序非常重要 !!!

    // AuthorizationDeniedException, 无权限抛出到 SecurityConfig 处理
    @ExceptionHandler(AuthorizationDeniedException.class)
    public void handleAuthorizationDeniedException(AuthorizationDeniedException e) {
        throw e;
    }

    // 这里写一般业务异常处理 ----------------------------------------------------------------------------------------------



    // 兜底异常处理 ------------------------------------------------------------------------------------------------------
    @ExceptionHandler(Exception.class)
    public Response<?> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 | uri={} | method={} | msg={}", request.getRequestURI(), request.getMethod(), e.getMessage(), e);
        return Response.fail("系统异常：" + e.getMessage());
    }


}
