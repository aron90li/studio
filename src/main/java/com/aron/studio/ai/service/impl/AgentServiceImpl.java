package com.aron.studio.ai.service.impl;

import com.aron.studio.ai.dto.*;
import com.aron.studio.ai.memory.MemoryManager;
import com.aron.studio.ai.service.AgentService;
import com.aron.studio.ai.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
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
        String sessionId = resolveSessionId(request);
        log.info("AgentService.chat: userId={}, sessionId={}, message={}", userId, sessionId, request.getMessage());
        return workflow.execute(userId, sessionId, request.getMessage());
    }

    @Override
    public Flux<AgentChatEvent> chatStream(Long userId, AgentChatRequest request) {
        String sessionId = resolveSessionId(request);
        log.info("AgentService.chatStream: userId={}, sessionId={}, message={}", userId, sessionId, request.getMessage());
        return workflow.executeStream(userId, sessionId, request.getMessage());
    }

    @Override
    public List<SessionInfo> getSessions(Long userId) {
        return memoryManager.getSessions(userId);
    }

    @Override
    public List<ChatMessage> getSessionMessages(Long userId, String sessionId) {
        return memoryManager.getSessionMessages(userId, sessionId);
    }

    @Override
    public void clearHistory(Long userId, String sessionId) {
        memoryManager.clearHistory(userId, sessionId);
    }

    private String resolveSessionId(AgentChatRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
        }
        return sessionId;
    }
}
