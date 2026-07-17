package com.aron.studio.ai.dto;

import lombok.Data;

/**
 * AI Agent 聊天请求
 * 用户身份通过 JWT 获取，不需要在请求体中传递
 */
@Data
public class AgentChatRequest {

    /** 用户消息 */
    private String message;

    /** 会话ID，用于记忆关联，不传则自动生成 */
    private String sessionId;

    /** 是否启用流式响应（预留） */
    private boolean stream;
}
