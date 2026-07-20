package com.aron.studio.ai.dto;

import lombok.Data;

/**
 * AI Agent 聊天请求
 * 用户身份通过 JWT 获取，不需要在请求体中传递
 * 阻塞/流式通过不同 URL 路径区分：/chat（阻塞）vs /chat/stream（流式）
 */
@Data
public class AgentChatRequest {

    /** 用户消息 */
    private String message;

    /** 会话ID，用于记忆关联，不传则自动生成 */
    private String sessionId;
}
