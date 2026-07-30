package com.aron.studio.ai.tools.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka Topic 信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaTopicInfo {

    /** Topic 名称 */
    private String name;

    /** 分区数 */
    private int partitions;

    /** 每个分区的副本信息 */
    private String replicationInfo;
}