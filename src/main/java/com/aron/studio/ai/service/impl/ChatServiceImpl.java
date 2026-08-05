package com.aron.studio.ai.service.impl;

import com.aron.studio.ai.dto.AgentChatEvent;
import com.aron.studio.ai.dto.AgentChatRequest;
import com.aron.studio.ai.dto.ChatMessage;
import com.aron.studio.ai.dto.SessionInfo;
import com.aron.studio.ai.memory.MemoryManagerV2;
import com.aron.studio.ai.service.ChatService;
import com.aron.studio.ai.tools.mysql.MysqlTool;
import com.aron.studio.ai.tools.kafka.KafkaTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.Generation;
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
    private MemoryManagerV2 memoryManagerV2;

    public ChatServiceImpl(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory,
                           MysqlTool mysqlTool, KafkaTool kafkaTool) {

        this.chatClient = chatClientBuilder
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .defaultTools(mysqlTool, kafkaTool)
                .defaultSystem("""
                        你是FlinkStudio智能助手，你擅长:
                        
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

        // 维护 ai_chat_session 表
        memoryManagerV2.upsertSession(userId, sessionId, userMessage);

        try {
            String result = chatClient.prompt()
                    .advisors(advisorSpec -> advisorSpec.param(
                            ChatMemory.CONVERSATION_ID, sessionId))
                    .user(userMessage)
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

        // 维护 ai_chat_session 表
        memoryManagerV2.upsertSession(userId, sessionId, userMessage);

        // 建议使用 stream().content() 返回 Flux<String>，Spring AI 已屏蔽了各 Provider（DeepSeek、OpenAI、Qwen、Claude 等）的 ChatResponse 差异。
        return Flux.concat(
                // 1. 先发送 THINK 事件
                Flux.just(AgentChatEvent.builder()
                        .type("THINK")
                        .data("正在使用 Spring AI 2.0 处理请求...")
                        .sessionId(sessionId)
                        .build()),
                chatClient.prompt()
                        .advisors(advisorSpec -> advisorSpec.param(
                                ChatMemory.CONVERSATION_ID, sessionId))
                        .user(userMessage)
                        .stream()
                        // 以下这种处理方式前端渲染markdown会有问题，改成content直接获取
//                        .chatResponse()
//                        .flatMap(response -> {
//                            if (response == null) {
//                                return Mono.empty();
//                            }
//
//                            Generation generation = response.getResult();
//                            if (generation == null) {
//                                return Mono.empty();
//                            }
//
//                            AssistantMessage assistant = generation.getOutput();
//                            if (assistant == null) {
//                                return Mono.empty();
//                            }
//
//                            String text = assistant.getText();
//                            if (text == null || text.isBlank()) {
//                                return Mono.empty();
//                            }
//
//                            return Mono.just(text);
//                        })
                        .content()
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
        return memoryManagerV2.getSessions(userId);
    }

    @Override
    public List<ChatMessage> getSessionMessages(Long userId, String sessionId) {
        return memoryManagerV2.getSessionMessages(userId, sessionId);
    }

    @Override
    public void clearHistory(Long userId, String sessionId) {
        memoryManagerV2.clearHistory(userId, sessionId);
    }

    private String resolveSessionId(AgentChatRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
        }
        return sessionId;
    }
}