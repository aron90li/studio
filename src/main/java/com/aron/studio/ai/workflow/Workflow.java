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
import java.util.stream.Collectors;

/**
 * AI Agent 工作流引擎 - 管理多轮 LLM 调用和工具执行的循环
 * <p>
 * 工具调用解析规则（防止误匹配）：
 * 1. TOOL_CALL 必须出现在行首（防止 LLM 回答中的示例文本被误匹配）
 * 2. 工具名必须在 ToolRegistry 中已注册
 * 3. 支持一次响应中包含多个工具调用（顺序执行）
 * 4. 工具调用文本会从最终答案中剥离，不混入对话历史
 */
@Slf4j
@Component
public class Workflow {

    private static final int MAX_ITERATIONS = 5;

    /**
     * 工具调用解析正则
     * <ul>
     *   <li>{@code ^TOOL_CALL:} — 必须在行首，防止匹配 LLM 回答中的示例文本</li>
     *   <li>{@code ([\w.]+)} — 工具名只含字母数字下划线点号</li>
     *   <li>{@code ^ARGS:} — ARGS 必须在下一行行首</li>
     *   <li>{@code (\{[^}]*\})} — 单行 JSON（如有嵌套需求再扩展）</li>
     * </ul>
     */
    private static final Pattern TOOL_CALL_PATTERN = Pattern.compile(
            "^TOOL_CALL:\\s*([\\w.]+)\\s*$\\s*^ARGS:\\s*(\\{[^}]*\\})",
            Pattern.MULTILINE
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

    // ==================== 阻塞执行 ====================

    /**
     * 阻塞执行 Agent 工作流（保留现有接口）
     */
    public AgentChatResponse execute(Long userId, String sessionId, String userMessage) {
        log.info("开始执行Agent工作流(阻塞), userId={}, sessionId={}", userId, sessionId);

        List<AgentChatResponse.ThoughtStep> thoughts = new ArrayList<>();

        String currentPrompt = promptBuilder.buildPrompt(userId, sessionId, userMessage);
        memoryManager.save(userId, sessionId, "user", userMessage);

        thoughts.add(AgentChatResponse.ThoughtStep.builder()
                .type("THINK").content("正在理解问题并构建查询方案...").build());

        String lastLlmResponse = null;

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            String llmResponse = llmClient.chat(currentPrompt);
            lastLlmResponse = llmResponse;

            List<ToolCall> toolCalls = parseAndValidateToolCalls(llmResponse);

            if (!toolCalls.isEmpty()) {
                // 剥离工具调用文本，保留纯净的回答文本存历史
                String cleanText = stripToolCallBlocks(llmResponse);
                if (!cleanText.isBlank()) {
                    memoryManager.save(userId, sessionId, "assistant", cleanText);
                }

                // 顺序执行所有工具调用，记录结果（避免重复执行）
                java.util.LinkedHashMap<String, String> toolResults = new java.util.LinkedHashMap<>();
                for (ToolCall tc : toolCalls) {
                    thoughts.add(AgentChatResponse.ThoughtStep.builder()
                            .type("TOOL_CALL").toolName(tc.toolName)
                            .content("调用工具: " + tc.toolName + ", 参数: " + tc.toolArgs).build());

                    memoryManager.save(userId, sessionId, "assistant",
                            "TOOL_CALL: " + tc.toolName + "\nARGS: " + tc.toolArgs,
                            tc.toolName, tc.toolArgs);

                    String toolResult = executeTool(tc.toolName, tc.toolArgs);
                    thoughts.add(AgentChatResponse.ThoughtStep.builder()
                            .type("TOOL_RESULT").toolName(tc.toolName).content(toolResult).build());

                    memoryManager.save(userId, sessionId, "tool", toolResult, tc.toolName, tc.toolArgs);
                    toolResults.put(tc.toolName, toolResult);
                }

                // 将所有工具结果拼接后反馈给 LLM（复用已执行的结果）
                String allResults = toolResults.entrySet().stream()
                        .map(e -> "工具 " + e.getKey() + " 结果: " + e.getValue())
                        .collect(Collectors.joining("\n"));
                currentPrompt = promptBuilder.buildPromptWithToolResults(
                        currentPrompt, allResults);
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

    // ==================== 流式执行 ====================

    /**
     * 流式执行 Agent 工作流 - 通过 SSE 逐步推送事件
     * <p>
     * 流程：
     * 1. 立即推送 THINK 事件
     * 2. 调用 LLM 流式接口，每个 token 作为 ANSWER 事件即时发出（打字机效果）
     * 3. 流结束后检测完整响应中的工具调用，推送 TOOL_CALL → TOOL_RESULT → 递归
     * 4. 无工具调用时推送 DONE 事件
     */
    public Flux<AgentChatEvent> executeStream(Long userId, String sessionId, String userMessage) {
        log.info("开始执行Agent工作流(流式), userId={}, sessionId={}", userId, sessionId);

        String initialPrompt = promptBuilder.buildPrompt(userId, sessionId, userMessage);
        memoryManager.save(userId, sessionId, "user", userMessage);


        return Flux.concat(
                Flux.just(AgentChatEvent.builder()
                        .type("THINK").data("正在理解问题并构建解决方案...")
                        .sessionId(sessionId).build()),
                executeStreamLoop(userId, sessionId, initialPrompt, 0)
        );
    }

    /**
     * 递归执行流式 Agent 循环
     * <p>
     * 核心流程：
     * <ol>
     *   <li>流式消费 LLM 输出，每个 token 即时推送 ANSWER 事件</li>
     *   <li>收集完整文本后解析工具调用（行首匹配 + 工具名校验）</li>
     *   <li>如有工具调用：推送 TOOL_CALL + TOOL_RESULT 事件，递归下一轮</li>
     *   <li>无工具调用：保存答案，推送 DONE</li>
     * </ol>
     */
    private Flux<AgentChatEvent> executeStreamLoop(Long userId, String sessionId,
                                                   String prompt, int iteration) {
        if (iteration >= MAX_ITERATIONS) {
            log.warn("Agent工作流(流式)超过最大迭代次数 {}", MAX_ITERATIONS);
            return Flux.just(AgentChatEvent.builder()
                    .type("DONE").data(String.format("Agent工作流(流式)超过最大迭代次数 {}", MAX_ITERATIONS))
                    .sessionId(sessionId).build());
        }

        final StringBuilder fullResponse = new StringBuilder();

        return llmClient.chatStream(prompt)
                .map(token -> {
                    fullResponse.append(token);
                    return AgentChatEvent.builder()
                            .type("ANSWER").data(token).sessionId(sessionId).build();
                })
                .concatWith(Flux.defer(() -> processStreamComplete(
                        userId, sessionId, prompt, iteration, fullResponse.toString())))
                .onErrorResume(e -> {
                    log.error("Agent工作流(流式)异常, sessionId={}", sessionId, e);
                    return Flux.just(
                            AgentChatEvent.builder().type("ERROR")
                                    .data("处理异常: " + e.getMessage()).sessionId(sessionId).build(),
                            AgentChatEvent.builder().type("DONE").data("处理异常结束").sessionId(sessionId).build()
                    );
                });
    }

    /**
     * 流式输出完成后处理：检测工具调用并决定后续流程
     *
     * @return Flux<AgentChatEvent> 后续事件流（工具调用事件或 DONE）
     */
    private Flux<AgentChatEvent> processStreamComplete(Long userId, String sessionId,
                                                       String prompt, int iteration,
                                                       String completeText) {
        // log.debug("completeText={}", completeText);
        List<ToolCall> toolCalls = parseAndValidateToolCalls(completeText);

        if (toolCalls.isEmpty()) {
            // 无工具调用：正常回答完成
            memoryManager.save(userId, sessionId, "assistant", completeText);
            return Flux.just(AgentChatEvent.builder()
                    .type("DONE").data("正常结束").sessionId(sessionId).build());
        }

        // 有工具调用
        log.info("检测到 {} 个有效工具调用: {}", toolCalls.size(),
                toolCalls.stream().map(ToolCall::toolName).collect(Collectors.joining(", ")));

        // 剥离工具调用文本，保留纯净的回答文本存历史
        String cleanText = stripToolCallBlocks(completeText);
        if (!cleanText.isBlank()) {
            memoryManager.save(userId, sessionId, "assistant", cleanText);
        }

        // 顺序执行所有工具调用，构建事件列表
        List<AgentChatEvent> toolEvents = new ArrayList<>();
        StringBuilder allResults = new StringBuilder();

        for (int i = 0; i < toolCalls.size(); i++) {
            ToolCall tc = toolCalls.get(i);

            memoryManager.save(userId, sessionId, "assistant",
                    "TOOL_CALL: " + tc.toolName + "\nARGS: " + tc.toolArgs,
                    tc.toolName, tc.toolArgs);

            String toolResult = executeTool(tc.toolName, tc.toolArgs);
            memoryManager.save(userId, sessionId, "tool", toolResult, tc.toolName, tc.toolArgs);

            toolEvents.add(AgentChatEvent.builder()
                    .type("TOOL_CALL").data("调用工具: " + tc.toolName)
                    .sessionId(sessionId).toolName(tc.toolName).build());
            toolEvents.add(AgentChatEvent.builder()
                    .type("TOOL_RESULT").data(toolResult)
                    .sessionId(sessionId).toolName(tc.toolName).build());

            allResults.append("工具 ").append(tc.toolName).append(" 结果: ").append(toolResult).append("\n");
        }

        String newPrompt = promptBuilder.buildPromptWithToolResults(
                prompt, allResults.toString());

        return Flux.concat(
                Flux.fromIterable(toolEvents),
                executeStreamLoop(userId, sessionId, newPrompt, iteration + 1)
        );
    }

    // ==================== 工具调用解析与校验 ====================

    /**
     * 从 LLM 响应文本中解析并校验工具调用
     * <p>
     * 校验规则：
     * <ol>
     *   <li>TOOL_CALL 必须在行首（防止回答中的示例被误匹配）</li>
     *   <li>工具名必须在 ToolRegistry 中已注册（排除虚构工具名）</li>
     *   <li>ARGS 必须是有效 JSON 格式</li>
     * </ol>
     *
     * @param text LLM 完整响应文本
     * @return 通过校验的工具调用列表（可能为空）
     */
    private List<ToolCall> parseAndValidateToolCalls(String text) {
        List<ToolCall> calls = new ArrayList<>();
        Matcher matcher = TOOL_CALL_PATTERN.matcher(text);

        while (matcher.find()) {
            String toolName = matcher.group(1).trim();
            String toolArgs = matcher.group(2).trim();

            // 校验1：工具名非空
            if (toolName.isEmpty()) {
                log.debug("跳过空工具名的TOOL_CALL匹配");
                continue;
            }

            // 校验2：工具必须在注册表中存在
            if (toolRegistry.getTool(toolName) == null) {
                log.info("忽略未注册工具调用: toolName={}, 可用工具={}",
                        toolName, toolRegistry.getAllTools().keySet());
                continue;
            }

            // 校验3：参数必须是合法 JSON
            if (!isValidJson(toolArgs)) {
                log.info("忽略无效JSON参数: toolName={}, args={}", toolName, toolArgs);
                continue;
            }

            calls.add(new ToolCall(toolName, toolArgs));
        }

        return calls;
    }

    /**
     * 简单 JSON 格式校验（不依赖 Jackson，避免引入额外依赖）
     */
    private boolean isValidJson(String str) {
        if (str == null || str.isBlank()) {
            return false;
        }
        String trimmed = str.trim();
        return trimmed.startsWith("{") && trimmed.endsWith("}");
    }

    /**
     * 从 LLM 响应中去除 TOOL_CALL 块，得到纯净的回答文本
     */
    private String stripToolCallBlocks(String text) {
        if (text == null) return "";
        return TOOL_CALL_PATTERN.matcher(text).replaceAll("").trim();
    }

    // ==================== 工具执行 ====================

    private String executeTool(String toolName, String args) {
        var tool = toolRegistry.getTool(toolName);
        // 此方法调用前已通过 parseAndValidateToolCalls 校验，tool 不会为 null
        if (tool == null) {
            return "错误：找不到工具 '" + toolName +
                    "'，可用工具：" + toolRegistry.getAllTools().keySet();
        }
        try {
            return tool.execute(args);
        } catch (Exception e) {
            log.error("工具 {} 执行异常", toolName, e);
            return "工具执行异常：" + e.getMessage();
        }
    }

    // ==================== 内部类型 ====================

    /**
     * 解析后的工具调用
     */
    private record ToolCall(String toolName, String toolArgs) {
    }
}