package com.aron.studio.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;


    // Security 拦截 -> 匹配 /api/auth/** -> permitAll() -> 放行 -> Controller 正常执行
    // Security 拦截 -> 不匹配 /api/auth/** -> 命中 anyRequest().authenticated() -> 检查是否已登录 -> 没 token 401, token 正确, 放行
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        // SecurityContextHolderFilter -> JwtAuthenticationFilter
        // -> UsernamePasswordAuthenticationFilter 禁用了 -> AuthorizationFilter
        // -> ExceptionTranslationFilter

        http.cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    // 允许所有源（不能和 allowCredentials=true 一起用）
                    config.setAllowedOriginPatterns(List.of("*"));
                    config.setAllowedMethods(List.of("*"));
                    config.setAllowedHeaders(List.of("*"));
                    // 如果是 true，允许浏览器发送 Cookie / Authorization header
                    // 也就允许浏览器添加请求头 Cookie: SESSIONID=abc123; otherCookie=xyz
                    // Authorization: Basic dXNlcjpwYXNz 等
                    config.setAllowCredentials(false); // // 不信任浏览器自动凭证 设置响应头 Access-Control-Allow-Credentials: false，默认就是 false，
                    return config;
                }))
                // 关闭 CSRF（前后端分离必须）
                .csrf(csrf -> csrf.disable())

                // 不使用 Session
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 禁用表单登录（核心）禁用 UsernamePasswordAuthenticationFilter
                .formLogin(form -> form.disable())

                // 禁用 httpBasic（可选但推荐）
                .httpBasic(basic -> basic.disable())

                //权限规则
                // 注意: /api/ai/** 的 SSE 流式接口(chat/stream)使用异步 dispatch,
                // SecurityContext 在异步线程会丢失导致 AccessDenied.
                // AiController 中已在同步线程通过 getCurrentUserId() 手动认证,
                // 因此对 /api/ai/** 放行, 由 Controller 自行控制认证.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/api/ai/**", "/error").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex.authenticationEntryPoint((req, res, e) -> {
                    log.error("未登录或登录已过期 | uri={} | method={} | msg={}", req.getRequestURI(), req.getMethod(), e.getMessage(), e);
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // url级别 401
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("""
                                {"code":401,"msg":"未登录或登录已过期","success":false}
                            """);
                }).accessDeniedHandler((req, res, e) -> {
                    log.error("权限不足 | uri={} | method={} | msg={}", req.getRequestURI(), req.getMethod(), e.getMessage(), e);
                    res.setStatus(HttpServletResponse.SC_FORBIDDEN); // 方法级别 403
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("""
                                {"code":403,"msg":"权限不足","success":false}
                            """);
                }));


        // 实际上会创建两条规则：
        // 1. 规则1: 路径匹配 "/api/auth/**" -> AuthorizationDecision(PERMIT_ALL)
        // 2. 规则2: 路径匹配 anyRequest() -> AuthorizationDecision(IS_AUTHENTICATED)
        // 创建映射表 RequestMatcher → AuthorizationManager
//[
//        (RequestMatcher("/api/auth/**"), AuthorizationManager(PERMIT_ALL)),
//        (RequestMatcher("/error"),      AuthorizationManager(PERMIT_ALL)),
//        (AnyRequestMatcher,             AuthorizationManager(AUTHENTICATED))
//]
        // 给过滤器 AuthorizationFilter 使用
        return http.build();
    }
}