package com.aron.studio.ai.service.impl;

import com.aron.studio.ai.dto.AgentChatEvent;
import com.aron.studio.ai.dto.AgentChatRequest;
import com.aron.studio.ai.service.ChatServiceV2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Chat 服务 V2 实现 — Spring AI 2.0 原生 Tool Calling 模式
 * <p>
 * 架构链路：
 * <pre>
 * Controller
 *         │
 *         ▼
 * ChatServiceV2Impl
 *         │
 *         ▼
 * ChatClient
 *         │
 *         ├──── Memory Advisor (JdbcChatMemoryRepository — 对话记忆持久化到 MySQL)
 *         ├──── Logger Advisor (SimpleLoggerAdvisor — 日志记录)
 *         │
 *         ▼
 *     @Tool 注解方法 (自动发现，无需 defaultTools 手动注册)
 *         │
 *         ▼
 *  DeepSeek (OpenAI Compatible)
 * </pre>
 */
@Slf4j
@Service
public class ChatServiceV2Impl implements ChatServiceV2 {

    private final ChatClient chatClient;

    public ChatServiceV2Impl(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory) {

        this.chatClient = chatClientBuilder
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .defaultSystem("")

                .build();
    }

    @Override
    public Flux<AgentChatEvent> chatStream(Long userId, AgentChatRequest request) {
        String sessionId = resolveSessionId(request);
        String userMessage = request.getMessage();
        log.info("ChatServiceV2.chatStream: userId={}, sessionId={}, message={}", userId, sessionId, userMessage);

        return Flux.concat(
                // 1. 先发送 THINK 事件
                Flux.just(AgentChatEvent.builder()
                        .type("THINK")
                        .data("正在使用 Spring AI 2.0 原生 Tool Calling 处理请求...")
                        .sessionId(sessionId)
                        .build()),

                chatClient.prompt() // chatMemory 会拦截这个方法
                        .advisors(advisorSpec -> advisorSpec.param(
                                ChatMemory.CONVERSATION_ID, sessionId))
                        .user(userMessage)
                        .stream()
                        .chatResponse()
                        .flatMap(response -> {
                            String token = response.getResult().getOutput().getText();
                            if (token == null || token.isEmpty()) {
                                return Mono.empty();
                            }
                            return Mono.just(token);
                        })
                        .map(token -> AgentChatEvent.builder()
                                .type("ANSWER")
                                .data(token)
                                .sessionId(sessionId)
                                .build())
                        .onErrorResume(e -> {
                            log.error("ChatServiceV2 流式输出异常, sessionId={}", sessionId, e);
                            return Mono.just(AgentChatEvent.builder()
                                    .type("ERROR")
                                    .data("处理异常: " + e.getMessage())
                                    .sessionId(sessionId)
                                    .build());
                        }),

                // 3. 发送 DONE 事件
                Flux.just(AgentChatEvent.builder()
                        .type("DONE")
                        .data("处理完成（Spring AI 2.0 模式）")
                        .sessionId(sessionId)
                        .build())
        );
    }

    private String resolveSessionId(AgentChatRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
        }
        return sessionId;
    }
}