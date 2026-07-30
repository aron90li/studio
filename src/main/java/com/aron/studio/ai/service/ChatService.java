package com.aron.studio.ai.service;

import com.aron.studio.ai.dto.AgentChatEvent;
import com.aron.studio.ai.dto.AgentChatRequest;
import com.aron.studio.ai.dto.ChatMessage;
import com.aron.studio.ai.dto.SessionInfo;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Chat 服务 V2 — 使用 Spring AI 2.0 ToolCallback + @Tool 注解的新模式
 * <p>
 * 与旧 AgentService（走 Workflow 正则解析 TOOL_CALL 文本）完全独立，
 * 利用 Spring AI 2.0 原生的 Tool Calling 机制自动编排工具调用循环。
 */
public interface ChatService {

    /**
     * 阻塞聊天 — 同步等待完整回答后返回
     *
     * @param userId  当前用户ID
     * @param request 聊天请求
     * @return 完整的回答文本
     */
    String chat(Long userId, AgentChatRequest request);

    /**
     * 流式聊天 — 使用 Spring AI 2.0 ToolCallback 自动处理工具调用
     *
     * @param userId  当前用户ID
     * @param request 聊天请求
     * @return SSE 流式事件
     */
    Flux<AgentChatEvent> chatStream(Long userId, AgentChatRequest request);

    /**
     * 获取用户的所有会话列表
     */
    List<SessionInfo> getSessions(Long userId);

    /**
     * 获取某个会话的完整历史消息
     */
    List<ChatMessage> getSessionMessages(Long userId, String sessionId);

    /**
     * 清空某个用户的会话历史
     */
    void clearHistory(Long userId, String sessionId);
}
