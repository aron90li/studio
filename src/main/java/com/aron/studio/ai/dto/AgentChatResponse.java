package com.aron.studio.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI Agent 聊天响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentChatResponse {

    /** 最终回答内容 */
    private String answer;

    /** 会话ID */
    private String sessionId;

    /** 思考过程（工具调用记录） */
    private List<ThoughtStep> thoughtProcess;

    /** 是否完成 */
    private boolean finished;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThoughtStep {
        /** 步骤类型: THINK / TOOL_CALL / TOOL_RESULT / ANSWER */
        private String type;
        /** 步骤内容 */
        private String content;
        /** 工具名称（如果是工具调用） */
        private String toolName;
    }
}