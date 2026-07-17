package com.aron.studio.ai.memory.entity;

import lombok.Data;

/**
 * 对话历史实体
 */
@Data
public class ChatHistory {
    private Long id;
    private String sessionId;
    private String role;
    private String content;
    private String toolName;
    private String toolArgs;
}