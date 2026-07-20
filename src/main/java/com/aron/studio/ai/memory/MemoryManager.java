package com.aron.studio.ai.memory;

import com.aron.studio.ai.memory.entity.ChatHistory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 记忆管理器 - 负责管理对话历史
 * 采用数据库存储会话历史，支持持久化和历史回溯
 * 按用户隔离，每个用户只能看到自己的聊天记录
 */
@Slf4j
@Component
public class MemoryManager {

    private static final int MAX_HISTORY_SIZE = 20;

    private final JdbcTemplate jdbcTemplate;

    public MemoryManager(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        initTable();
    }

    /**
     * 初始化对话历史表
     */
    private void initTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS ai_chat_history (
                    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
                    user_id    BIGINT       NOT NULL COMMENT '用户ID，用于隔离不同用户的对话',
                    session_id VARCHAR(128) NOT NULL,
                    role       VARCHAR(32)  NOT NULL COMMENT 'user/assistant/tool',
                    content    LONGTEXT     NOT NULL,
                    tool_name  VARCHAR(128) COMMENT '工具名称（如果是工具调用）',
                    tool_args  LONGTEXT     COMMENT '工具参数',
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_user_session (user_id, session_id)
                ) COMMENT 'AI对话历史表'
                """;
        jdbcTemplate.execute(sql);
        log.info("AI对话历史表初始化完成");

        // 兼容旧表：如果已有表但没有 user_id 字段则添加
        try {
            jdbcTemplate.queryForList("SELECT user_id FROM ai_chat_history LIMIT 1");
        } catch (Exception e) {
            jdbcTemplate.execute("ALTER TABLE ai_chat_history ADD COLUMN user_id BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID'");
            jdbcTemplate.execute("ALTER TABLE ai_chat_history ADD INDEX idx_user_session (user_id, session_id)");
            log.info("ai_chat_history 表已添加 user_id 字段");
        }
    }

    /**
     * 保存一条对话记录
     */
    public void save(Long userId, String sessionId, String role, String content) {
        save(userId, sessionId, role, content, null, null);
    }

    /**
     * 保存一条对话记录（带工具信息）
     */
    public void save(Long userId, String sessionId, String role, String content, String toolName, String toolArgs) {
        String sql = "INSERT INTO ai_chat_history (user_id, session_id, role, content, tool_name, tool_args, create_time) VALUES (?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, userId, sessionId, role, content, toolName, toolArgs, LocalDateTime.now());

        // 清理超出上限的历史记录
        trimHistory(userId, sessionId);
    }

    /**
     * 获取历史消息列表（按用户+会话隔离）
     */
    public List<String> getHistory(Long userId, String sessionId) {
        String sql = """
                SELECT role, content, tool_name
                FROM ai_chat_history
                WHERE user_id = ? AND session_id = ?
                ORDER BY create_time ASC
                LIMIT ?
                """;
        List<ChatHistory> histories = jdbcTemplate.query(sql,
                (rs, rowNum) -> {
                    ChatHistory h = new ChatHistory();
                    h.setRole(rs.getString("role"));
                    h.setContent(rs.getString("content"));
                    h.setToolName(rs.getString("tool_name"));
                    return h;
                },
                userId, sessionId, MAX_HISTORY_SIZE);

        return histories.stream()
                .map(h -> {
                    if ("tool".equals(h.getRole())) {
                        return "tool(" + h.getToolName() + "): " + h.getContent();
                    }
                    return h.getRole() + ": " + h.getContent();
                })
                .collect(Collectors.toList());
    }

    /**
     * 清理超出上限的历史
     */
    private void trimHistory(Long userId, String sessionId) {
        String countSql = "SELECT COUNT(*) FROM ai_chat_history WHERE user_id = ? AND session_id = ?";
        Integer count = jdbcTemplate.queryForObject(countSql, Integer.class, userId, sessionId);
        if (count != null && count > MAX_HISTORY_SIZE) {
            String deleteSql = """
                    DELETE FROM ai_chat_history
                    WHERE user_id = ? AND session_id = ? AND id NOT IN (
                        SELECT id FROM (
                            SELECT id FROM ai_chat_history WHERE user_id = ? AND session_id = ? ORDER BY create_time DESC LIMIT ?
                        ) tmp
                    )
                    """;
            jdbcTemplate.update(deleteSql, userId, sessionId, userId, sessionId, MAX_HISTORY_SIZE);
        }
    }

    /**
     * 获取用户的所有会话列表（按最后活跃时间倒序）
     */
    public List<com.aron.studio.ai.dto.SessionInfo> getSessions(Long userId) {
        String sql = """
                SELECT session_id,
                       COUNT(*) AS message_count,
                       MAX(create_time) AS last_active_time,
                       MIN(CASE WHEN role = 'user' THEN create_time END) AS first_user_msg_time
                FROM ai_chat_history
                WHERE user_id = ?
                GROUP BY session_id
                ORDER BY last_active_time DESC
                """;
        List<com.aron.studio.ai.dto.SessionInfo> sessions = jdbcTemplate.query(sql,
                (rs, rowNum) -> com.aron.studio.ai.dto.SessionInfo.builder()
                        .sessionId(rs.getString("session_id"))
                        .messageCount(rs.getInt("message_count"))
                        .lastActiveTime(rs.getTimestamp("last_active_time").toLocalDateTime())
                        .build(),
                userId);

        // 单独查询每条会话的第一条用户消息作为标题，避免 SQL 逗号截断问题
        for (com.aron.studio.ai.dto.SessionInfo session : sessions) {
            String titleSql = """
                    SELECT content FROM ai_chat_history
                    WHERE user_id = ? AND session_id = ? AND role = 'user'
                    ORDER BY create_time ASC LIMIT 1
                    """;
            String firstMessage = jdbcTemplate.query(titleSql,
                    (rs) -> rs.next() ? rs.getString("content") : null,
                    userId, session.getSessionId());
            session.setTitle(truncateTitle(firstMessage));
        }
        return sessions;
    }

    /**
     * 获取某个会话的完整历史消息（结构化格式，用于前端展示）
     */
    public List<com.aron.studio.ai.dto.ChatMessage> getSessionMessages(Long userId, String sessionId) {
        String sql = """
                SELECT role, content, tool_name, create_time
                FROM ai_chat_history
                WHERE user_id = ? AND session_id = ?
                ORDER BY create_time ASC
                """;
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> com.aron.studio.ai.dto.ChatMessage.builder()
                        .role(rs.getString("role"))
                        .content(rs.getString("content"))
                        .toolName(rs.getString("tool_name"))
                        .createTime(rs.getTimestamp("create_time").toLocalDateTime())
                        .build(),
                userId, sessionId);
    }

    /**
     * 截取标题（取第一条用户消息的前20字）
     */
    private String truncateTitle(String message) {
        if (message == null || message.isBlank()) {
            return "新对话";
        }
        String trimmed = message.trim();
        return trimmed.length() > 20 ? trimmed.substring(0, 20) + "..." : trimmed;
    }

    /**
     * 清空某个用户的会话历史
     */
    public void clearHistory(Long userId, String sessionId) {
        jdbcTemplate.update("DELETE FROM ai_chat_history WHERE user_id = ? AND session_id = ?", userId, sessionId);
        log.info("清空用户 {} 的会话 {} 的历史记录", userId, sessionId);
    }
}
