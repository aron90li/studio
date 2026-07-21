package com.aron.studio.ai.memory;

import java.util.List;

/**
 * 历史对话压缩器策略接口
 * <p>
 * 当对话历史过长时，通过压缩/摘要减少 Prompt 的 token 消耗。
 * 采用策略模式，方便切换不同压缩算法：
 * <ul>
 *     <li>SummaryHistoryCompressor - LLM 摘要压缩（推荐）</li>
 *     <li>TruncateHistoryCompressor - 简单截断</li>
 *     <li>SlidingWindowCompressor - 滑动窗口</li>
 * </ul>
 */
public interface HistoryCompressor {

    /**
     * 压缩历史对话列表
     *
     * @param history 原始历史消息列表，格式为 "role: content" 或 "tool(toolName): content"
     * @return 压缩后的文本，可直接拼接进 Prompt 的【历史对话】区域
     *         如果历史为空，返回空字符串
     */
    String compress(List<String> history);
}