package com.aron.studio.ai.memory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

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



}
