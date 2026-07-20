package com.aron.studio.ai.controller;

import com.aron.studio.ai.dto.*;
import com.aron.studio.ai.service.AgentService;
import com.aron.studio.data.Response;
import com.aron.studio.util.CurrentUserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

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

    private Long getCurrentUserId() {
        return currentUserUtil.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("未登录或登录已过期"));
    }

    // ==================== 阻塞聊天 ====================

    /**
     * POST /api/ai/chat
     * 与 AI Agent 对话（阻塞，返回完整结果）
     */
    @PostMapping("/chat")
    public Response<AgentChatResponse> chat(@RequestBody AgentChatRequest request) {
        Long userId = getCurrentUserId();
        log.info("收到AI聊天请求(阻塞): userId={}, message={}", userId, request.getMessage());
        AgentChatResponse response = agentService.chat(userId, request);
        return Response.success(response);
    }

    // ==================== 流式聊天（SSE） ====================

    /**
     * POST /api/ai/chat/stream
     * 与 AI Agent 流式对话，通过 SSE 逐步推送事件
     * <p>
     * 事件类型:
     * - THINK: Agent 正在思考
     * - TOOL_CALL: Agent 调用工具
     * - TOOL_RESULT: 工具返回结果
     * - ANSWER: 最终回答
     * - ERROR: 异常
     * - DONE: 完成
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AgentChatEvent> chatStream(@RequestBody AgentChatRequest request) {
        Long userId = getCurrentUserId();
        log.info("收到AI聊天请求(流式): userId={}, message={}", userId, request.getMessage());
        return agentService.chatStream(userId, request);
    }

    // ==================== 会话管理 ====================

    /**
     * GET /api/ai/sessions
     * 获取当前用户的所有会话列表
     */
    @GetMapping("/sessions")
    public Response<List<SessionInfo>> getSessions() {
        Long userId = getCurrentUserId();
        log.info("查询用户会话列表: userId={}", userId);
        return Response.success(agentService.getSessions(userId));
    }

    /**
     * GET /api/ai/sessions/{sessionId}
     * 获取某个会话的完整历史消息
     */
    @GetMapping("/sessions/{sessionId}")
    public Response<List<ChatMessage>> getSessionMessages(@PathVariable String sessionId) {
        Long userId = getCurrentUserId();
        log.info("查询会话消息: userId={}, sessionId={}", userId, sessionId);
        return Response.success(agentService.getSessionMessages(userId, sessionId));
    }

    /**
     * DELETE /api/ai/sessions/{sessionId}
     * 清空当前用户的某个会话历史
     */
    @DeleteMapping("/sessions/{sessionId}")
    public Response<Void> clearHistory(@PathVariable String sessionId) {
        Long userId = getCurrentUserId();
        log.info("清空会话历史: userId={}, sessionId={}", userId, sessionId);
        agentService.clearHistory(userId, sessionId);
        return Response.success();
    }
}
