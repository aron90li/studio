package com.aron.studio.ai.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

/**
 * LLM 客户端 - 封装与 DeepSeek 模型的交互
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
     * 发送消息给 LLM 并获取回复
     *
     * @param prompt 完整的提示词
     * @return LLM 返回的文本
     */
    public String chat(String prompt) {
        log.debug("发送给LLM的Prompt长度: {} 字符", prompt.length());

        ChatResponse response = chatClient.prompt()
                .messages(new UserMessage(prompt))
                .call()
                .chatResponse();

        String result = response.getResult().getOutput().getText();
        log.debug("LLM返回结果长度: {} 字符, 返回：{}", result.length(), result);
        return result;
    }
}