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
import reactor.core.publisher.Mono;

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
     * 流式执行 Agent 工作流 - 通过 SSE 逐步推送事件
     * <p>
     * 流程：
     * 1. 立即推送 THINK 事件
     * 2. 调用 LLM 流式接口，每个 token 作为一个 ANSWER 事件即时发出
     * 3. 如果 LLM 返回 TOOL_CALL，推送 TOOL_CALL → TOOL_RESULT → 递归
     * 4. 完成时推送 DONE 事件
     */
    public Flux<AgentChatEvent> executeStream(Long userId, String sessionId, String userMessage) {
        log.info("开始执行Agent工作流(流式), userId={}, sessionId={}", userId, sessionId);

        memoryManager.save(userId, sessionId, "user", userMessage);

        String initialPrompt = promptBuilder.buildPrompt(userId, sessionId, userMessage);

        return Flux.concat(
                // 第1步：立即推送 THINK 事件
                Flux.just(AgentChatEvent.builder()
                        .type("THINK").data("正在理解问题并构建解决方案...")
                        .sessionId(sessionId).build()),

                // 第2步：递归执行 LLM 调用循环（最多 MAX_ITERATIONS 轮）
                executeStreamLoop(userId, sessionId, initialPrompt, 0)
        );
    }

    /**
     * 递归执行流式 Agent 循环
     * <p>
     * 关键设计：将 LLM 流式输出的 token 收集到 StringBuilder 中，
     * 同时每个 token 立即包装为 ANSWER 事件发出，给前端打字机效果。
     * 等 LLM 流结束（onComplete）后再判断是否有 TOOL_CALL。
     * 如果有 TOOL_CALL，递归调用；如果没有，发出 DONE 事件。
     */
    private Flux<AgentChatEvent> executeStreamLoop(Long userId, String sessionId,
                                                   String prompt, int iteration) {
        if (iteration >= MAX_ITERATIONS) {
            log.warn("Agent工作流(流式)超过最大迭代次数 {}, sessionId={}", MAX_ITERATIONS, sessionId);
            return Flux.just(AgentChatEvent.builder()
                    .type("DONE").data(String.format("Agent工作流(流式)超过最大迭代次数 {}", MAX_ITERATIONS))
                    .sessionId(sessionId).build());
        }

        final StringBuilder fullResponse = new StringBuilder();

        // 调用 LLM 流式接口：每个 token 作为 ANSWER 事件发出，同时收集完整文本
        return llmClient.chatStream(prompt)
                .map(token -> {
                    // 去除首尾空白，但保留有效 token
                    fullResponse.append(token);
                    return AgentChatEvent.builder()
                            .type("ANSWER")
                            .data(token)
                            .sessionId(sessionId)
                            .build();
                })
                // 流结束后，判断完整文本是否包含工具调用
                .concatWith(Mono.fromCallable(() -> {
                    String completeText = fullResponse.toString();
                    Matcher matcher = TOOL_CALL_PATTERN.matcher(completeText);

                    if (matcher.find()) {
                        String toolName = matcher.group(1).trim();
                        String toolArgs = matcher.group(2).trim();

                        log.info("检测到TOOL_CALL: toolName={}, toolArgs={}", toolName, toolArgs);
                        memoryManager.save(userId, sessionId, "assistant",
                                "TOOL_CALL: " + toolName + "\nARGS: " + toolArgs, toolName, toolArgs);

                        String toolResult = executeTool(toolName, toolArgs);
                        memoryManager.save(userId, sessionId, "tool", toolResult, toolName, toolArgs);

                        String newPrompt = promptBuilder.buildPromptWithToolResult(
                                sessionId, prompt, toolName, toolResult);

                        // 返回 TOOL_CALL + TOOL_RESULT 事件，然后递归下一轮
                        // 通过 Flux.defer 延迟创建，避免递归栈溢出
                        return Flux.concat(
                                Flux.just(
                                        AgentChatEvent.builder()
                                                .type("TOOL_CALL")
                                                .data("调用工具: " + toolName)
                                                .sessionId(sessionId)
                                                .toolName(toolName)
                                                .build(),
                                        AgentChatEvent.builder()
                                                .type("TOOL_RESULT")
                                                .data(toolResult)
                                                .sessionId(sessionId)
                                                .toolName(toolName)
                                                .build()
                                ),
                                executeStreamLoop(userId, sessionId, newPrompt, iteration + 1)
                        );
                    } else {
                        // 正常回答完成
                        memoryManager.save(userId, sessionId, "assistant", completeText);
                        return Flux.just(AgentChatEvent.builder()
                                .type("DONE").data("正常回答完成").sessionId(sessionId).build());
                    }
                }).flatMapMany(flux -> flux))  // 展开 Flux<Flux<AgentChatEvent>>
                .onErrorResume(e -> {
                    log.error("Agent工作流(流式)异常, sessionId={}", sessionId, e);
                    return Flux.just(
                            AgentChatEvent.builder()
                                    .type("ERROR")
                                    .data("处理异常: " + e.getMessage())
                                    .sessionId(sessionId)
                                    .build(),
                            AgentChatEvent.builder()
                                    .type("DONE").data("异常完成").sessionId(sessionId).build()
                    );
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