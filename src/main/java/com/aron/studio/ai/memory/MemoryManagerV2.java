package com.aron.studio.ai.memory;

import com.aron.studio.ai.dto.ChatMessage;
import com.aron.studio.ai.dto.SessionInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class MemoryManagerV2 {

    private final JdbcTemplate jdbcTemplate;

    public MemoryManagerV2(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        initTable();
    }

    private void initTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ai_chat_session
                (
                    id                  BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',                
                    session_id          VARCHAR(36) NOT NULL COMMENT '会话ID，对应SPRING_AI_CHAT_MEMORY.conversation_id',                
                    user_id             BIGINT NOT NULL COMMENT '用户ID',                
                    title               VARCHAR(200) NOT NULL DEFAULT '新对话' COMMENT '会话标题',                
                    model               VARCHAR(100) DEFAULT NULL COMMENT '模型，例如 deepseek-v4-pro',                
                    assistant_code      VARCHAR(100) DEFAULT NULL COMMENT '助手编码，支持多个AI助手',                
                    system_prompt       TEXT COMMENT 'System Prompt，可选保存',                
                    last_message        VARCHAR(500) DEFAULT NULL COMMENT '最后一句话，方便列表展示',                
                    message_count       INT NOT NULL DEFAULT 0 COMMENT '消息数量',                
                    total_tokens        BIGINT NOT NULL DEFAULT 0 COMMENT '累计Token',                
                    pinned              TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶',                
                    archived            TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否归档',                
                    deleted             TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',                
                    create_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',                
                    update_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',                
                    last_chat_time      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最后聊天时间',                
                    UNIQUE KEY uk_session_id(session_id),                
                    INDEX idx_user(user_id),                
                    INDEX idx_user_update(user_id, update_time),                
                    INDEX idx_user_last_chat(user_id, last_chat_time)
                ) COMMENT='AI聊天会话';                
                """);
    }

    /**
     * 创建或更新会话记录（每次聊天时调用）
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param userMessage 用户消息（用于title/更新last_message）
     */
    public void upsertSession(Long userId, String sessionId, String userMessage) {
        String title = truncateTitle(userMessage);
        jdbcTemplate.update("""
                INSERT INTO ai_chat_session (session_id, user_id, title, last_message, message_count, last_chat_time)
                VALUES (?, ?, ?, ?, 2, NOW())
                ON DUPLICATE KEY UPDATE
                    last_message = VALUES(last_message),
                    message_count = message_count + 2,
                    last_chat_time = NOW(),
                    deleted = 0
                """, sessionId, userId, title, truncateLastMessage(userMessage));
    }

    /**
     * 获取用户的所有会话列表
     */
    public List<SessionInfo> getSessions(Long userId) {
        String sql = """
                SELECT session_id, title, message_count, last_chat_time
                FROM ai_chat_session
                WHERE user_id = ? AND deleted = 0
                ORDER BY last_chat_time DESC
                """;
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> SessionInfo.builder()
                        .sessionId(rs.getString("session_id"))
                        .title(rs.getString("title"))
                        .messageCount(rs.getInt("message_count"))
                        .lastActiveTime(rs.getTimestamp("last_chat_time").toLocalDateTime())
                        .build(),
                userId);
    }

    /**
     * 获取某个会话的完整历史消息（从 SPRING_AI_CHAT_MEMORY 读取）
     */
    public List<ChatMessage> getSessionMessages(Long userId, String sessionId) {
        // 先校验该会话是否属于该用户
        String checkSql = "SELECT COUNT(*) FROM ai_chat_session WHERE user_id = ? AND session_id = ? AND deleted = 0";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, userId, sessionId);
        if (count == null || count == 0) {
            return List.of();
        }

        String sql = """
                SELECT content, type, timestamp
                FROM SPRING_AI_CHAT_MEMORY
                WHERE conversation_id = ?
                ORDER BY sequence_id ASC
                """;
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> ChatMessage.builder()
                        .role(rs.getString("type").toLowerCase())
                        .content(rs.getString("content"))
                        .toolName(null)
                        .createTime(rs.getTimestamp("timestamp").toLocalDateTime())
                        .build(),
                sessionId);
    }

    /**
     * 清空某个会话历史（删除 SPRING_AI_CHAT_MEMORY 和 ai_chat_session 中的数据）
     */
    public void clearHistory(Long userId, String sessionId) {
        // 先校验该会话是否属于该用户
        String checkSql = "SELECT id FROM ai_chat_session WHERE user_id = ? AND session_id = ? AND deleted = 0";
        List<Long> ids = jdbcTemplate.queryForList(checkSql, Long.class, userId, sessionId);
        if (ids.isEmpty()) {
            log.warn("会话不存在或不属于该用户: userId={}, sessionId={}", userId, sessionId);
            return;
        }
        // 物理删除 ai_chat_session, 可以逻辑删除
        jdbcTemplate.update("DELETE FROM ai_chat_session WHERE session_id = ?", sessionId);
        // 物理删除 SPRING_AI_CHAT_MEMORY 中的消息
        jdbcTemplate.update("DELETE FROM SPRING_AI_CHAT_MEMORY WHERE conversation_id = ?", sessionId);
        log.info("已清空会话: userId={}, sessionId={}", userId, sessionId);
    }

    private String truncateTitle(String message) {
        if (message == null || message.isBlank()) {
            return "新对话";
        }
        String trimmed = message.trim();
        return trimmed.length() > 20 ? trimmed.substring(0, 20) + "..." : trimmed;
    }

    private String truncateLastMessage(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }
        String trimmed = message.trim();
        return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
    }
}
