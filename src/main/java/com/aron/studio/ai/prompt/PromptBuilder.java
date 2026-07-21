package com.aron.studio.ai.prompt;

import com.aron.studio.ai.memory.HistoryCompressor;
import com.aron.studio.ai.memory.MemoryManager;
import com.aron.studio.ai.tools.ToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 提示词构建器 - 负责构造发送给 LLM 的完整 Prompt
 * 组装顺序: SystemPrompt + History(压缩后) + ToolDescriptions + UserMessage
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
            - 如果需要同一类信息，可以同时声明多个工具调用，系统会依次执行
            - 需要串行依赖的工具调用请分多轮进行（先调用工具A获取结果，再根据结果决定是否调用工具B）
            - 如果一次查询结果不全，可以调整参数再次查询
            
            ## 工具调用格式（必须严格遵守，否则无法识别）
            当需要调用工具时，在回答的最末尾按以下格式声明（TOOL_CALL 和 ARGS 都必须在行首，不能有缩进）：
            
            TOOL_CALL: 工具名称
            ARGS: {"参数名": "参数值"}
            
            如需调用多个工具，连续声明即可：
            TOOL_CALL: 工具A
            ARGS: {"参数A": "值A"}
            TOOL_CALL: 工具B
            ARGS: {"参数B": "值B"}
            
            ## 回答要求
            - 始终用中文回答用户
            - 如实汇报工具的执行结果，不编造数据
            - 工具返回错误时，分析可能的原因并给出建议
            - 回答简洁清晰，突出关键信息
            - 如果不需要调用工具直接回答，请不要在回答中包含 TOOL_CALL 格式文本
            """;

    private final ToolRegistry toolRegistry;
    private final MemoryManager memoryManager;
    private final HistoryCompressor historyCompressor;

    public PromptBuilder(ToolRegistry toolRegistry, MemoryManager memoryManager,
                         HistoryCompressor historyCompressor) {
        this.toolRegistry = toolRegistry;
        this.memoryManager = memoryManager;
        this.historyCompressor = historyCompressor;
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

        // 3. 历史对话（通过压缩器处理，长对话自动压缩为摘要）
        List<String> history = memoryManager.getHistory(userId, sessionId);
        String compressedHistory = historyCompressor.compress(history);
        if (!compressedHistory.isEmpty()) {
            prompt.append("【历史对话】\n");
            prompt.append(compressedHistory).append("\n");
        }

        // 4. 用户消息
        prompt.append("【用户消息】\n").append(userMessage).append("\n");

        log.debug("构建Prompt完成，长度: {} 字符", prompt.length());
        log.debug("Prompt内容：{}", prompt);
        return prompt.toString();
    }

    /**
     * 构建带工具结果的 Prompt（支持多个工具调用结果，一次性拼回）
     *
     * @param originalPrompt 原始 Prompt
     * @param allResults     所有工具执行结果（已格式化）
     * @return 拼接后的完整 Prompt
     */
    public String buildPromptWithToolResults(String originalPrompt, String allResults) {
        StringBuilder prompt = new StringBuilder(originalPrompt);

        prompt.append("\n【工具执行结果】\n");
        prompt.append(allResults).append("\n");
        prompt.append("请根据以上工具执行结果，继续回答用户的问题。如果结果已足够，给出最终回答。\n");

        return prompt.toString();
    }
}