package com.aron.studio.config;

import com.aron.studio.data.rbac.entity.UserEntity;
import com.aron.studio.service.UserService;
import com.aron.studio.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器 (Spring Boot 4.x / Jakarta)
 * - 从 Authorization Header 中提取 Bearer token
 * - 校验签名、过期
 * - 构建 Authentication 放入 SecurityContext
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String HEADER_STRING = "Authorization";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("JwtAuthenticationFilter doFilter...");
        try {
            String header = request.getHeader(HEADER_STRING);

            if (header != null && header.startsWith(TOKEN_PREFIX)) {
                String token = header.substring(TOKEN_PREFIX.length());

                // 校验 token 是否有效
                if (jwtUtil.validateToken(token)) {
                    Claims claims = jwtUtil.parseToken(token);
                    String username = claims.get("username").toString();
                    String role = claims.get("role").toString();
                    String userId = claims.get("userId").toString();

                    log.info("jwtUtil get username {}, role: {}, userId: {} from token: {}", username, role, userId, token);
                    // 从数据库或缓存加载用户信息，用户不存在会抛出异常，抛出异常就不会设置认证信息
                    // UserDetails userDetails = userService.loadUserByUsername(username);
                    // 构建 Authentication 对象, 三个参数的构造器，得到 authentication.isAuthenticated() = true; authenticated 标志位一定是true
                    // 后续被 AuthorizationFilter 使用

                    // token 带了role，就不查询数据库了
                    UserEntity userDetails = new UserEntity();
                    userDetails.setUsername(username);
                    userDetails.setRole(role);
                    userDetails.setUserId(Long.valueOf(userId));
                    userDetails.getAuthorities();

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    // 放入 SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    log.warn("token 无效： {}", token);
                }
            }
        } catch (Exception e) {
            log.warn("JwtAuthenticationFilter exception: {}", e.getMessage(), e);
            // 出现异常时清空 SecurityContext，确保不会留下错误认证状态
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }

}
