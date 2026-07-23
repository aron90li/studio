package com.aron.studio.ai.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * LLM 客户端 - 封装与大语言模型的交互（通过 OpenAI 兼容协议）
 */
@Slf4j
@Component
public class LLMClient {

    private final ChatClient chatClient;

    public LLMClient(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .build();
    }

    /**
     * 发送消息给 LLM 并获取回复（阻塞）
     *
     * @param prompt 完整的提示词
     * @return LLM 返回的文本
     */
    public String chat(String prompt) {
        log.info("发送给LLM的Prompt长度: {} 字符", prompt.length());

        ChatResponse response = chatClient.prompt()
                .messages(new UserMessage(prompt))
                .call()
                .chatResponse();

        String result = response.getResult().getOutput().getText();
        log.info("LLM返回结果长度: {} 字符, 返回：{}", result.length(), result);
        return result;
    }

    /**
     * 发送消息给 LLM 并获取流式回复（逐 token 推送）
     *
     * @param prompt 完整的提示词
     * @return 逐 token 推送的 Flux<String>
     */
    public Flux<String> chatStream(String prompt) {
        log.info("发送给LLM的Prompt长度(流式): {} 字符", prompt.length());

        return chatClient.prompt()
                .messages(new UserMessage(prompt))
                .stream()
                .chatResponse()
                .flatMap(response -> {
                    String token = response.getResult().getOutput().getText();
                    // Spring AI 流式输出中某些 chunk 可能为空，需要过滤
                    if (token == null || token.isEmpty()) {
                        return Mono.empty();
                    }
                    return Mono.just(token);
                })
                .doOnComplete(() -> log.info("LLM流式输出完成"))
                .doOnError(e -> log.error("LLM流式输出异常", e));
    }
}