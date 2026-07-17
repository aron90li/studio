package com.aron.studio.ai.controller;

import com.aron.studio.ai.dto.AgentChatRequest;
import com.aron.studio.ai.dto.AgentChatResponse;
import com.aron.studio.ai.service.AgentService;
import com.aron.studio.data.Response;
import com.aron.studio.util.CurrentUserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/ai/")
public class AiController {

    private final AgentService agentService;
    private final CurrentUserUtil currentUserUtil;

    public AiController(AgentService agentService, CurrentUserUtil currentUserUtil) {
        this.agentService = agentService;
        this.currentUserUtil = currentUserUtil;
    }

    /**
     * POST /api/ai/chat
     * 与 AI Agent 对话（需 JWT 认证）
     */
    @PostMapping("/chat")
    public Response<AgentChatResponse> chat(@RequestBody AgentChatRequest request) {
        Long userId = currentUserUtil.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("未登录或登录已过期"));
        log.info("收到AI聊天请求: userId={}, message={}, sessionId={}", userId, request.getMessage(), request.getSessionId());
        AgentChatResponse response = agentService.chat(userId, request);
        return Response.success(response);
    }

    /**
     * DELETE /api/ai/chat/{sessionId}
     * 清空当前用户的某个会话历史（需 JWT 认证）
     */
    @DeleteMapping("/chat/{sessionId}")
    public Response<Void> clearHistory(@PathVariable String sessionId) {
        Long userId = currentUserUtil.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("未登录或登录已过期"));
        log.info("清空会话历史: userId={}, sessionId={}", userId, sessionId);
        agentService.clearHistory(userId, sessionId);
        return Response.success();
    }
}
