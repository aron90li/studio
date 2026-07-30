package com.aron.studio.ai.tools.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka 消息搜索结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaMessage {

    /** 消息的 key */
    private String key;

    /** 消息的 value */
    private String value;

    /** 消息所在的分区 */
    private int partition;

    /** 消息的偏移量 */
    private long offset;

    /** 消息的时间戳 */
    private long timestamp;
}