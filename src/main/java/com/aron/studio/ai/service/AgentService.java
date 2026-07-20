package com.aron.studio.ai.service;

import com.aron.studio.ai.dto.*;
import reactor.core.publisher.Flux;

import java.util.List;

public interface AgentService {

    /**
     * 发送聊天消息给 AI Agent（阻塞）
     */
    AgentChatResponse chat(Long userId, AgentChatRequest request);

    /**
     * 发送聊天消息给 AI Agent（流式）
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
