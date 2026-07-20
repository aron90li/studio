package com.aron.studio.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天消息（用于前端展示历史对话）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    /** 角色: user / assistant / tool */
    private String role;

    /** 消息内容 */
    private String content;

    /** 工具名称（如果是工具调用） */
    private String toolName;

    /** 创建时间 */
    private LocalDateTime createTime;
}