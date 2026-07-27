package com.aron.studio.ai.service;

import com.aron.studio.ai.dto.AgentChatEvent;
import com.aron.studio.ai.dto.AgentChatRequest;
import reactor.core.publisher.Flux;

/**
 * Chat 服务 V2 — 使用 Spring AI 2.0 ToolCallback + @Tool 注解的新模式
 * <p>
 * 与旧 AgentService（走 Workflow 正则解析 TOOL_CALL 文本）完全独立，
 * 利用 Spring AI 2.0 原生的 Tool Calling 机制自动编排工具调用循环。
 */
public interface ChatServiceV2 {

    /**
     * 流式聊天 — 使用 Spring AI 2.0 ToolCallback 自动处理工具调用
     *
     * @param userId  当前用户ID
     * @param request 聊天请求
     * @return SSE 流式事件
     */
    Flux<AgentChatEvent> chatStream(Long userId, AgentChatRequest request);
}