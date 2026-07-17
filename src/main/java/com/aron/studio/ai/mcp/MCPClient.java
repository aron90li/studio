package com.aron.studio.ai.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * MCP 客户端 - 预留扩展，用于与 MCP 服务器交互
 * 后续可扩展为调用外部 MCP 服务（如文件系统、API网关等）
 */
@Slf4j
@Component
public class MCPClient {

    /**
     * 调用 MCP 工具（预留）
     */
    public String callTool(String serverName, String toolName, String args) {
        log.info("MCPClient 调用工具: server={}, tool={}, args={}", serverName, toolName, args);
        // TODO: 实现 MCP 工具调用
        return "MCP工具调用功能开发中...";
    }
}