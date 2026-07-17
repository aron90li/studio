package com.aron.studio.ai.prompt;

import com.aron.studio.ai.memory.MemoryManager;
import com.aron.studio.ai.tools.ToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 提示词构建器 - 负责构造发送给 LLM 的完整 Prompt
 * 组装顺序: SystemPrompt + History + ToolDescriptions + UserMessage
 */
@Slf4j
@Component
public class PromptBuilder {

    private static final String SYSTEM_PROMPT = """
            你是一个智能 AI Agent，能够使用各种工具完成用户的任务。
            
            ## 工作模式（ReAct）
            你遵循「思考 → 行动 → 观察 → 再思考」的循环模式：
            1. 理解用户的意图
            2. 选择合适的工具来完成任务
            3. 调用工具获取结果
            4. 分析工具返回的结果
            5. 如果还需要更多信息，继续调用其他工具
            6. 如果信息足够，给出最终回答
            
            ## 工具调用规则
            - 一次只能调用一个工具，不要同时调用多个
            - 调用工具后等待结果返回再决定下一步
            - 如果一次查询结果不全，可以调整参数再次查询
            - 如果需要，可以按顺序调用多个不同的工具来完成复杂任务
            
            ## 工具调用格式
            当你需要调用工具时，必须严格按照以下格式返回：
            TOOL_CALL: 工具名称
            ARGS: {"参数名": "参数值"}
            
            ## 回答要求
            - 始终用中文回答用户
            - 如实汇报工具的执行结果，不编造数据
            - 工具返回错误时，分析可能的原因并给出建议
            - 回答简洁清晰，突出关键信息
            """;

    private final ToolRegistry toolRegistry;
    private final MemoryManager memoryManager;

    public PromptBuilder(ToolRegistry toolRegistry, MemoryManager memoryManager) {
        this.toolRegistry = toolRegistry;
        this.memoryManager = memoryManager;
    }

    /**
     * 构建完整 Prompt
     */
    public String buildPrompt(Long userId, String sessionId, String userMessage) {
        StringBuilder prompt = new StringBuilder();

        // 1. System Prompt
        prompt.append("【系统设定】\n").append(SYSTEM_PROMPT).append("\n\n");

        // 2. 工具描述
        prompt.append("【工具信息】\n").append(toolRegistry.buildToolDescriptions()).append("\n");

        // 3. 历史消息
        List<String> history = memoryManager.getHistory(userId, sessionId);
        if (!history.isEmpty()) {
            prompt.append("【历史对话】\n");
            for (String msg : history) {
                prompt.append(msg).append("\n");
            }
            prompt.append("\n");
        }

        // 4. 用户消息
        prompt.append("【用户消息】\n").append(userMessage).append("\n");

        log.debug("构建Prompt完成，长度: {} 字符，内容：{}", prompt.length(), prompt);
        return prompt.toString();
    }

    /**
     * 构建带工具结果的 Prompt（LLM 返回工具调用后，将执行结果拼回）
     */
    public String buildPromptWithToolResult(String sessionId, String originalPrompt,
                                             String toolName, String toolResult) {
        StringBuilder prompt = new StringBuilder(originalPrompt);

        prompt.append("\n【工具执行结果】\n");
        prompt.append("工具: ").append(toolName).append("\n");
        prompt.append("结果: ").append(toolResult).append("\n");
        prompt.append("\n请根据以上工具执行结果，回答用户的问题。\n");

        return prompt.toString();
    }
}