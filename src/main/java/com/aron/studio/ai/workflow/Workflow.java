package com.aron.studio.ai.workflow;

import com.aron.studio.ai.dto.AgentChatEvent;
import com.aron.studio.ai.dto.AgentChatResponse;
import com.aron.studio.ai.llm.LLMClient;
import com.aron.studio.ai.memory.MemoryManager;
import com.aron.studio.ai.prompt.PromptBuilder;
import com.aron.studio.ai.tools.ToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI Agent 工作流引擎 - 管理多轮 LLM 调用和工具执行的循环
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
     * 阻塞执行 Agent 工作流（保留现有接口）
     */
    public AgentChatResponse execute(Long userId, String sessionId, String userMessage) {
        log.info("开始执行Agent工作流(阻塞), userId={}, sessionId={}", userId, sessionId);

        List<AgentChatResponse.ThoughtStep> thoughts = new ArrayList<>();
        memoryManager.save(userId, sessionId, "user", userMessage);

        String currentPrompt = promptBuilder.buildPrompt(userId, sessionId, userMessage);
        thoughts.add(AgentChatResponse.ThoughtStep.builder()
                .type("THINK").content("正在理解问题并构建查询方案...").build());

        String lastLlmResponse = null;

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            String llmResponse = llmClient.chat(currentPrompt);
            lastLlmResponse = llmResponse;

            Matcher matcher = TOOL_CALL_PATTERN.matcher(llmResponse);
            if (matcher.find()) {
                String toolName = matcher.group(1).trim();
                String toolArgs = matcher.group(2).trim();

                thoughts.add(AgentChatResponse.ThoughtStep.builder()
                        .type("TOOL_CALL").toolName(toolName)
                        .content("调用工具: " + toolName + ", 参数: " + toolArgs).build());

                memoryManager.save(userId, sessionId, "assistant",
                        "TOOL_CALL: " + toolName + "\nARGS: " + toolArgs, toolName, toolArgs);

                String toolResult = executeTool(toolName, toolArgs);
                thoughts.add(AgentChatResponse.ThoughtStep.builder()
                        .type("TOOL_RESULT").toolName(toolName).content(toolResult).build());

                memoryManager.save(userId, sessionId, "tool", toolResult, toolName, toolArgs);
                currentPrompt = promptBuilder.buildPromptWithToolResult(
                        sessionId, currentPrompt, toolName, toolResult);
            } else {
                thoughts.add(AgentChatResponse.ThoughtStep.builder()
                        .type("ANSWER").content(llmResponse).build());
                memoryManager.save(userId, sessionId, "assistant", llmResponse);
                return AgentChatResponse.builder()
                        .answer(llmResponse).sessionId(sessionId)
                        .thoughtProcess(thoughts).finished(true).build();
            }
        }

        if (lastLlmResponse != null) {
            memoryManager.save(userId, sessionId, "assistant", lastLlmResponse);
        }
        return AgentChatResponse.builder()
                .answer(lastLlmResponse != null ? lastLlmResponse : "抱歉，处理您的问题时出现了异常")
                .sessionId(sessionId).thoughtProcess(thoughts).finished(true).build();
    }

    /**
     * 流式执行 Agent 工作流 - 逐步推送事件
     */
    public Flux<AgentChatEvent> executeStream(Long userId, String sessionId, String userMessage) {
        log.info("开始执行Agent工作流(流式), userId={}, sessionId={}", userId, sessionId);

        return Flux.create(sink -> {
            try {
                memoryManager.save(userId, sessionId, "user", userMessage);

                // 推送 THINK 事件
                sink.next(AgentChatEvent.builder()
                        .type("THINK").data("正在理解问题并构建查询方案...")
                        .sessionId(sessionId).build());

                String currentPrompt = promptBuilder.buildPrompt(userId, sessionId, userMessage);
                String lastLlmResponse = null;

                for (int i = 0; i < MAX_ITERATIONS; i++) {
                    String llmResponse = llmClient.chat(currentPrompt);
                    lastLlmResponse = llmResponse;

                    Matcher matcher = TOOL_CALL_PATTERN.matcher(llmResponse);
                    if (matcher.find()) {
                        String toolName = matcher.group(1).trim();
                        String toolArgs = matcher.group(2).trim();

                        // 推送 TOOL_CALL 事件
                        sink.next(AgentChatEvent.builder()
                                .type("TOOL_CALL").data("调用工具: " + toolName)
                                .sessionId(sessionId).toolName(toolName).build());

                        memoryManager.save(userId, sessionId, "assistant",
                                "TOOL_CALL: " + toolName + "\nARGS: " + toolArgs, toolName, toolArgs);

                        String toolResult = executeTool(toolName, toolArgs);

                        // 推送 TOOL_RESULT 事件
                        sink.next(AgentChatEvent.builder()
                                .type("TOOL_RESULT").data(toolResult)
                                .sessionId(sessionId).toolName(toolName).build());

                        memoryManager.save(userId, sessionId, "tool", toolResult, toolName, toolArgs);
                        currentPrompt = promptBuilder.buildPromptWithToolResult(
                                sessionId, currentPrompt, toolName, toolResult);
                    } else {
                        // 推送 ANSWER 事件
                        sink.next(AgentChatEvent.builder()
                                .type("ANSWER").data(llmResponse)
                                .sessionId(sessionId).build());

                        memoryManager.save(userId, sessionId, "assistant", llmResponse);
                        sink.next(AgentChatEvent.builder().type("DONE").data("")
                                .sessionId(sessionId).build());
                        sink.complete();
                        return;
                    }
                }

                // 超过最大迭代次数
                if (lastLlmResponse != null) {
                    memoryManager.save(userId, sessionId, "assistant", lastLlmResponse);
                    sink.next(AgentChatEvent.builder().type("ANSWER").data(lastLlmResponse)
                            .sessionId(sessionId).build());
                }
                sink.next(AgentChatEvent.builder().type("DONE").data("").sessionId(sessionId).build());
                sink.complete();

            } catch (Exception e) {
                log.error("Agent工作流(流式)异常", e);
                sink.next(AgentChatEvent.builder().type("ERROR")
                        .data("处理异常: " + e.getMessage()).sessionId(sessionId).build());
                sink.error(e);
            }
        });
    }

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