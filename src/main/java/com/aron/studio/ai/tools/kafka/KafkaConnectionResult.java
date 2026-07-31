package com.aron.studio.ai.tools.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka 连接测试结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaConnectionResult {

    /** 是否连接成功 */
    private boolean connected;

    /** 集群 broker 节点数，连接失败时为 0 */
    private int nodeCount;

    /** 集群 topic 数量，连接失败时为 0 */
    private int topicCount;

    /** 连接耗时（毫秒） */
    private long elapsedMs;

    /** 请求的 brokers 地址 */
    private String brokers;

    /** 详细信息：成功时描述集群概要，失败时包含错误原因 */
    private String message;
}