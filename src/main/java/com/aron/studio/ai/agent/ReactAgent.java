package com.aron.studio.ai.agent;

import com.aron.studio.ai.dto.AgentChatResponse;
import com.aron.studio.ai.llm.LLMClient;
import com.aron.studio.ai.memory.MemoryManager;
import com.aron.studio.ai.prompt.PromptBuilder;
import com.aron.studio.ai.tools.ToolRegistry;
import com.aron.studio.ai.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * React (Reasoning + Acting) Agent
 * <p>
 * 遵循 React 模式：思考 -> 行动(工具调用) -> 观察(工具结果) -> 再思考
 * 封装了完整的 ReAct 循环，支持多轮 Tool Calling
 * <p>
 * 该类作为 Workflow 的高级封装，提供更丰富的 ReAct 策略控制
 */
@Slf4j
@Component
public class ReactAgent {

    private final Workflow workflow;

    public ReactAgent(Workflow workflow) {
        this.workflow = workflow;
    }

    /**
     * 执行 ReAct 推理循环
     *
     * @param userId      用户ID（来自 JWT）
     * @param sessionId   会话ID
     * @param userMessage 用户消息
     */
    public AgentChatResponse execute(Long userId, String sessionId, String userMessage) {
        return workflow.execute(userId, sessionId, userMessage);
    }
}