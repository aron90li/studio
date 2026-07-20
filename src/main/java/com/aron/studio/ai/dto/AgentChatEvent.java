package com.aron.studio.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSE 流式事件 - 逐步推送 Agent 的思考过程和中间结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentChatEvent {

    /** 事件类型: THINK / TOOL_CALL / TOOL_RESULT / ANSWER / ERROR / DONE */
    private String type;

    /** 事件数据 */
    private String data;

    /** 会话ID */
    private String sessionId;

    /** 工具名称（如果是工具调用） */
    private String toolName;
}