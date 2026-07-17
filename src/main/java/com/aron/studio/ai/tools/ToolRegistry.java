package com.aron.studio.ai.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具注册中心 - 管理所有 AgentTool 的注册与查找
 */
@Slf4j
@Component
public class ToolRegistry {

    private final Map<String, AgentTool> tools = new HashMap<>();

    public ToolRegistry(List<AgentTool> toolList) {
        for (AgentTool tool : toolList) {
            register(tool);
        }
        log.info("ToolRegistry 初始化完成，共注册 {} 个工具: {}", tools.size(), tools.keySet());
    }

    /**
     * 注册工具
     */
    public void register(AgentTool tool) {
        tools.put(tool.getName(), tool);
        log.debug("注册工具: {}", tool.getName());
    }

    /**
     * 根据名称获取工具
     */
    public AgentTool getTool(String name) {
        return tools.get(name);
    }

    /**
     * 获取所有已注册的工具
     */
    public Map<String, AgentTool> getAllTools() {
        return Map.copyOf(tools);
    }

    /**
     * 构建给 LLM 的工具描述
     * 从已注册的工具动态生成，新增工具自动生效
     */
    public String buildToolDescriptions() {
        StringBuilder sb = new StringBuilder();
        sb.append("以下是你可以使用的工具列表，根据任务需求选择合适的工具：\n");
        for (AgentTool tool : tools.values()) {
            sb.append("- ").append(tool.getName()).append("\n");
            sb.append("  ").append(tool.getDescription()).append("\n\n");
        }
        return sb.toString();
    }
}