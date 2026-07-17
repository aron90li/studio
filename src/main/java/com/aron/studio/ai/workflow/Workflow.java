package com.aron.studio.ai.workflow;

import com.aron.studio.ai.dto.AgentChatResponse;
import com.aron.studio.ai.llm.LLMClient;
import com.aron.studio.ai.memory.MemoryManager;
import com.aron.studio.ai.prompt.PromptBuilder;
import com.aron.studio.ai.tools.ToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI Agent 工作流引擎 - 管理多轮 LLM 调用和工具执行的循环
 * <p>
 * 一次用户请求的工作流：
 * 1. 构建 Prompt (System + History + Tools + User)
 * 2. 调用 LLM
 * 3. 解析 LLM 返回：如果是 TOOL_CALL -> 执行工具 -> 回填结果 -> 再次调用 LLM
 * 4. 重复直到 LLM 返回最终回答
 */
@Slf4j
@Component
public class Workflow {

    private static final int MAX_ITERATIONS = 5;

    private static final Pattern TOOL_CALL_PATTERN = Pattern.compile(
            "TOOL_CALL:\\s*(\\S+)\\s*ARGS:\\s*(\\{.+\\})",
            Pattern.DOTALL
    );

    private final LLMClient llmClient;
    private final PromptBuilder promptBuilder;
    private final ToolRegistry toolRegistry;
    private final MemoryManager memoryManager;

    public Workflow(LLMClient llmClient, PromptBuilder promptBuilder,
                    ToolRegistry toolRegistry, MemoryManager memoryManager) {
        this.llmClient = llmClient;
        this.promptBuilder = promptBuilder;
        this.toolRegistry = toolRegistry;
        this.memoryManager = memoryManager;
    }

    /**
     * 执行 Agent 工作流
     *
     * @param userId      用户ID（来自 JWT）
     * @param sessionId   会话ID
     * @param userMessage 用户消息
     * @return Agent 最终响应
     */
    public AgentChatResponse execute(Long userId, String sessionId, String userMessage) {
        log.info("开始执行Agent工作流, userId={}, sessionId={}, userMessage={}", userId, sessionId, userMessage);

        List<AgentChatResponse.ThoughtStep> thoughts = new ArrayList<>();

        // 保存用户消息到记忆
        memoryManager.save(userId, sessionId, "user", userMessage);

        // 初次构建 Prompt
        String currentPrompt = promptBuilder.buildPrompt(userId, sessionId, userMessage);
        thoughts.add(AgentChatResponse.ThoughtStep.builder()
                .type("THINK")
                .content("正在理解问题并构建查询方案...")
                .build());

        String lastLlmResponse = null;

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            log.info("第 {} 次 LLM 调用, userId={}", i + 1, userId);

            // 调用 LLM
            String llmResponse = llmClient.chat(currentPrompt);
            lastLlmResponse = llmResponse;

            // 解析是否包含工具调用
            Matcher matcher = TOOL_CALL_PATTERN.matcher(llmResponse);
            if (matcher.find()) {
                String toolName = matcher.group(1).trim();
                String toolArgs = matcher.group(2).trim();

                log.info("解析到工具调用: toolName={}, args={}", toolName, toolArgs);
                thoughts.add(AgentChatResponse.ThoughtStep.builder()
                        .type("TOOL_CALL")
                        .toolName(toolName)
                        .content("调用工具: " + toolName + ", 参数: " + toolArgs)
                        .build());

                // 保存 LLM 回复到记忆
                memoryManager.save(userId, sessionId, "assistant",
                        "TOOL_CALL: " + toolName + "\nARGS: " + toolArgs,
                        toolName, toolArgs);

                // 执行工具
                String toolResult = executeTool(toolName, toolArgs);
                log.info("工具执行结果: {}", toolResult);

                thoughts.add(AgentChatResponse.ThoughtStep.builder()
                        .type("TOOL_RESULT")
                        .toolName(toolName)
                        .content(toolResult)
                        .build());

                // 保存工具结果到记忆
                memoryManager.save(userId, sessionId, "tool", toolResult, toolName, toolArgs);

                // 构建带工具结果的 Prompt，继续下一轮 LLM 调用
                currentPrompt = promptBuilder.buildPromptWithToolResult(
                        sessionId, currentPrompt, toolName, toolResult);
                log.debug("currentPrompt: {}", currentPrompt);

            } else {
                // 没有工具调用，这是最终回答
                log.info("LLM 返回最终回答");
                thoughts.add(AgentChatResponse.ThoughtStep.builder()
                        .type("ANSWER")
                        .content(llmResponse)
                        .build());

                // 保存最终回答到记忆
                memoryManager.save(userId, sessionId, "assistant", llmResponse);

                return AgentChatResponse.builder()
                        .answer(llmResponse)
                        .sessionId(sessionId)
                        .thoughtProcess(thoughts)
                        .finished(true)
                        .build();
            }
        }

        // 超过最大迭代次数，返回最后一次 LLM 响应
        log.warn("Agent工作流超过最大迭代次数({})", MAX_ITERATIONS);
        thoughts.add(AgentChatResponse.ThoughtStep.builder()
                .type("ANSWER")
                .content(lastLlmResponse)
                .build());

        if (lastLlmResponse != null) {
            memoryManager.save(userId, sessionId, "assistant", lastLlmResponse);
        }

        return AgentChatResponse.builder()
                .answer(lastLlmResponse != null ? lastLlmResponse : "抱歉，处理您的问题时出现了异常")
                .sessionId(sessionId)
                .thoughtProcess(thoughts)
                .finished(true)
                .build();
    }

    /**
     * 执行工具
     */
    private String executeTool(String toolName, String args) {
        var tool = toolRegistry.getTool(toolName);
        if (tool == null) {
            return "错误：找不到工具 '" + toolName + "'，可用工具：" + toolRegistry.getAllTools().keySet();
        }
        try {
            return tool.execute(args);
        } catch (Exception e) {
            log.error("工具 {} 执行异常", toolName, e);
            return "工具执行异常：" + e.getMessage();
        }
    }
}