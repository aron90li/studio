package com.aron.studio.ai.service.impl;

import com.aron.studio.ai.dto.AgentChatRequest;
import com.aron.studio.ai.dto.AgentChatResponse;
import com.aron.studio.ai.memory.MemoryManager;
import com.aron.studio.ai.service.AgentService;
import com.aron.studio.ai.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Agent 服务实现 - 协调各模块完成 AI 对话
 */
@Slf4j
@Service
public class AgentServiceImpl implements AgentService {

    private final Workflow workflow;
    private final MemoryManager memoryManager;

    public AgentServiceImpl(Workflow workflow, MemoryManager memoryManager) {
        this.workflow = workflow;
        this.memoryManager = memoryManager;
    }

    @Override
    public AgentChatResponse chat(Long userId, AgentChatRequest request) {
        // 如果没有 sessionId，自动生成
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
        }

        log.info("AgentService.chat: userId={}, sessionId={}, message={}", userId, sessionId, request.getMessage());

        // 执行工作流
        return workflow.execute(userId, sessionId, request.getMessage());
    }

    @Override
    public void clearHistory(Long userId, String sessionId) {
        memoryManager.clearHistory(userId, sessionId);
    }
}
