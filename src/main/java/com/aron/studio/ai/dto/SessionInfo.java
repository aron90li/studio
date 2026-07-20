package com.aron.studio.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会话概要信息（用于前端会话列表展示）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionInfo {

    /** 会话ID */
    private String sessionId;

    /** 会话标题（第一条用户消息的前20字） */
    private String title;

    /** 消息总数 */
    private int messageCount;

    /** 最后活跃时间 */
    private LocalDateTime lastActiveTime;
}