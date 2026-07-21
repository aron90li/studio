package com.aron.studio.ai.memory;

import com.aron.studio.ai.llm.LLMClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于 LLM 摘要的历史对话压缩器
 * <p>
 * 策略：保留最近 3 轮对话作为上下文，将更早的历史通过 LLM 压缩为一段摘要。
 * 这样既保留了近期对话的精确信息，又通过摘要保留了历史关键信息，
 * 大幅减少 Prompt token 消耗。
 * <p>
 * 参考：LangChain ConversationSummaryBufferMemory
 */
@Slf4j
@Component
public class SummaryHistoryCompressor implements HistoryCompressor {

    /**
     * 保留最近对话轮数（每轮 = 用户消息 + Assistant 回复 + 可能的 tool 消息）
     * 经验值 3 轮，保证 LLM 有足够的上下文理解当前问题
     */
    private static final int KEEP_RECENT_ROUNDS = 3;

    /**
     * 触发压缩的最小消息条数（消息数少于此值时不压缩，直接原样返回）
     */
    private static final int MIN_MSG_COUNT_TO_COMPRESS = 10;

    private static final String SUMMARIZE_PROMPT = """
            请将以下对话历史总结为一段简洁的摘要，保留关键信息：
            - 用户的意图和需求
            - 已采取的行动（如调用了哪些工具）
            - 已获得的重要结果和结论
            - 对话中达成的共识或决定
            
            要求：
            - 用中文总结
            - 长度控制在200字以内
            - 只输出摘要内容，不要加任何前缀或解释
            
            === 对话历史 ===
            %s
            === 结束 ===
            """;

    private final LLMClient llmClient;

    public SummaryHistoryCompressor(LLMClient llmClient) {
        this.llmClient = llmClient;
    }

    @Override
    public String compress(List<String> history) {
        if (history == null || history.isEmpty()) {
            return "";
        }

        // 消息较少时不压缩
        if (history.size() < MIN_MSG_COUNT_TO_COMPRESS) {
            log.debug("历史消息{}条，小于{}条，不压缩", history.size(), MIN_MSG_COUNT_TO_COMPRESS);
            return formatHistoryBlock(history);
        }

        // 估算保留最近的 N 轮消息
        int keepCount = estimateRecentMessages(history, KEEP_RECENT_ROUNDS);

        if (keepCount >= history.size()) {
            // 不够一轮完整对话，全保留
            return formatHistoryBlock(history);
        }

        List<String> oldHistory = history.subList(0, history.size() - keepCount);
        List<String> recentHistory = history.subList(history.size() - keepCount, history.size());

        // 调用 LLM 生成早期对话摘要
        String oldText = oldHistory.stream()
                .map(msg -> msg.replaceAll("\n", " "))
                .collect(Collectors.joining("\n"));

        String summaryPrompt = String.format(SUMMARIZE_PROMPT, oldText);
        log.info("开始压缩历史对话: 总消息数={}, 早期消息数={}, 保留近期消息数={}", 
                history.size(), oldHistory.size(), keepCount);

        try {
            String summary = llmClient.chat(summaryPrompt);
            log.info("历史对话压缩完成，摘要长度: {} 字符", summary.length());

            // 拼接：摘要 + 近期对话
            StringBuilder result = new StringBuilder();
            result.append("【对话历史摘要】\n");
            result.append(summary.trim()).append("\n\n");
            result.append("【近期对话（最近").append(keepCount).append("条消息）】\n");
            for (String msg : recentHistory) {
                result.append(msg).append("\n");
            }
            return result.toString();
        } catch (Exception e) {
            // 压缩失败时降级：保留近期消息，丢弃早期消息
            log.warn("历史对话压缩失败，降级为保留近期消息。错误: {}", e.getMessage());
            return formatHistoryBlock(recentHistory);
        }
    }

    /**
     * 估算最近 N 轮对话对应的消息条数
     * <p>
     * 一轮对话通常包含：user + assistant + 可能的 tool 消息
     * 从后向前统计，遇到 user 消息计数一轮
     */
    private int estimateRecentMessages(List<String> history, int rounds) {
        int userCount = 0;
        for (int i = history.size() - 1; i >= 0; i--) {
            String msg = history.get(i);
            if (msg != null && msg.startsWith("user:")) {
                userCount++;
                if (userCount >= rounds) {
                    return history.size() - i;
                }
            }
        }
        // 如果历史中没有足够的 user 消息，全保留
        return history.size();
    }

    /**
     * 将历史消息列表格式化为 Prompt 中的【历史对话】块
     */
    private String formatHistoryBlock(List<String> history) {
        StringBuilder sb = new StringBuilder();
        for (String msg : history) {
            sb.append(msg).append("\n");
        }
        return sb.toString();
    }
}