package com.aron.studio.ai.service;

import com.aron.studio.ai.dto.AgentChatRequest;
import com.aron.studio.ai.dto.AgentChatResponse;

public interface AgentService {

    /**
     * 发送聊天消息给 AI Agent
     *
     * @param userId  用户ID（来自 JWT）
     * @param request 聊天请求
     */
    AgentChatResponse chat(Long userId, AgentChatRequest request);

    /**
     * 清空某个用户的会话历史
     *
     * @param userId    用户ID（来自 JWT）
     * @param sessionId 会话ID
     */
    void clearHistory(Long userId, String sessionId);
}
