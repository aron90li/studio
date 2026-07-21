package com.aron.studio.config;

import com.aron.studio.data.Response;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
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
    @ExceptionHandler(RuntimeException.class)
    public Response<?> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("Runtime异常 | uri={} | method={} | msg={}", request.getRequestURI(), request.getMethod(), e.getMessage(), e);
        return Response.fail("Runtime异常：" + e.getMessage());
    }

    // 客户端主动断开连接异常（如用户刷新页面、关闭标签页等）----------------------------------------------
    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbortException(ClientAbortException e, HttpServletRequest request) {
        log.info("客户端主动断开连接 | uri={} | method={} | msg={}", request.getRequestURI(), request.getMethod(), e.getMessage());
        // 不返回任何内容，因为连接已经断开
    }

    // 兜底异常处理 ------------------------------------------------------------------------------------------------------
    @ExceptionHandler(Exception.class)
    public Response<?> handleException(Exception e, HttpServletRequest request) {
        // 如果是 ClientAbortException 的包装异常，也按 INFO 处理
        if (isClientAbortException(e)) {
            log.info("客户端主动断开连接(包装异常) | uri={} | method={} | msg={}", request.getRequestURI(), request.getMethod(), e.getMessage());
            return null;
        }
        log.error("系统异常 | uri={} | method={} | msg={}", request.getRequestURI(), request.getMethod(), e.getMessage(), e);
        return Response.fail("系统异常：" + e.getMessage());
    }

    /**
     * 判断是否为客户端断开连接的包装异常
     */
    private boolean isClientAbortException(Exception e) {
        String message = e.getMessage();
        Throwable cause = e.getCause();
        String causeMessage = cause != null ? cause.getMessage() : "";

        // 检查异常消息中是否包含客户端断开连接的关键字
        return (message != null && message.contains("你的主机中的软件中止了一个已建立的连接"))
                || (causeMessage != null && causeMessage.contains("你的主机中的软件中止了一个已建立的连接"))
                || (cause instanceof ClientAbortException)
                || (cause != null && cause.getCause() instanceof ClientAbortException);
    }


}
