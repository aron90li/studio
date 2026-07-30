package com.aron.studio.ai.service.impl;

import com.aron.studio.ai.dto.AgentChatEvent;
import com.aron.studio.ai.dto.AgentChatRequest;
import com.aron.studio.ai.dto.ChatMessage;
import com.aron.studio.ai.dto.SessionInfo;
import com.aron.studio.ai.service.ChatService;
import com.aron.studio.ai.tools.impl.MysqlQueryToolV2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Chat 服务 V2 实现 — Spring AI 2.0 原生 Tool Calling 模式
 * <p>
 * 架构链路：
 * <pre>
 * Controller
 *         │
 *         ▼
 * ChatServiceImpl
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
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;

    @Autowired
    private MysqlQueryToolV2 mysqlQueryToolV2;

    public ChatServiceImpl(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory) {

        this.chatClient = chatClientBuilder
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .defaultSystem("""
                        你是FlinkStudio智能助手。
                        
                        你擅长:
                        - Flink SQL
                        - Kafka
                        - Paimon
                        - Kubernetes
                        - MySQL
                        - Redis
                        - Spark
                        
                        如果需要查询外部数据，使用提供的工具。
                        """)
                .build();
    }

    @Override
    public String chat(Long userId, AgentChatRequest request) {
        String sessionId = resolveSessionId(request);
        String userMessage = request.getMessage();
        log.info("ChatService.chat: userId={}, sessionId={}, message={}", userId, sessionId, userMessage);

        try {
            String result = chatClient.prompt()
                    .advisors(advisorSpec -> advisorSpec.param(
                            ChatMemory.CONVERSATION_ID, sessionId))
                    .user(userMessage)
                    .tools(mysqlQueryToolV2)
                    .call()
                    .content();
            log.info("ChatService.chat 完成, sessionId={}", sessionId);
            return result;
        } catch (Exception e) {
            log.error("ChatService.chat 异常, sessionId={}", sessionId, e);
            return "处理异常: " + e.getMessage();
        }
    }

    @Override
    public Flux<AgentChatEvent> chatStream(Long userId, AgentChatRequest request) {
        String sessionId = resolveSessionId(request);
        String userMessage = request.getMessage();
        log.info("ChatService.chatStream: userId={}, sessionId={}, message={}", userId, sessionId, userMessage);

        return Flux.concat(
                // 1. 先发送 THINK 事件
                Flux.just(AgentChatEvent.builder()
                        .type("THINK")
                        .data("正在使用 Spring AI 2.0 原生 Tool Calling 处理请求...")
                        .sessionId(sessionId)
                        .build()),
                chatClient.prompt()
                        .advisors(advisorSpec -> advisorSpec.param(
                                ChatMemory.CONVERSATION_ID, sessionId))
                        .user(userMessage)
                        .tools(mysqlQueryToolV2)
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
                            log.error("ChatService 流式输出异常, sessionId={}", sessionId, e);
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

    @Override
    public List<SessionInfo> getSessions(Long userId) {
        // todo
        return List.of();
    }

    @Override
    public List<ChatMessage> getSessionMessages(Long userId, String sessionId) {
        // todo
        return List.of();
    }

    @Override
    public void clearHistory(Long userId, String sessionId) {
        // todo
    }

    private String resolveSessionId(AgentChatRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
        }
        return sessionId;
    }
}