package com.aron.studio.ai.tools;

/**
 * Agent 工具接口，所有 Tool 实现此接口
 */
public interface AgentTool {

    /** 工具名称 */
    String getName();

    /** 工具描述（LLM 理解用） */
    String getDescription();

    /**
     * 执行工具
     * @param args JSON格式参数
     * @return 执行结果文本
     */
    String execute(String args);
}