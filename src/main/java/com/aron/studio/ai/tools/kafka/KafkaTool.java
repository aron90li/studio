package com.aron.studio.ai.tools.kafka;

import com.aron.studio.ai.config.KafkaPropertiesConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Kafka 管理工具 — 使用 Spring AI 2.0 @Tool 注解定义
 * <p>
 * 提供 Kafka 集群信息查看、Topic 列表查询、消息搜索等功能。
 * 所有 Kafka 操作通过 AdminClient / KafkaConsumer 原生 API 实现，
 * 无需依赖任何自定义 SDK，高效可靠。
 */
@Slf4j
@Component
public class KafkaTool {

    @Autowired
    private KafkaPropertiesConfig kafkaPropertiesConfig;

    /**
     * 列出所有已配置的 Kafka 集群
     * <p>
     * 从配置文件中读取 kafka.clusters 列表，包含集群名称、地址等信息。
     *
     * @return 集群列表
     */
    @Tool(description = "列出所有已配置的 Kafka 集群信息，返回每个集群的 name（集群名称）和 brokers（集群地址）。"
            + "调用此方法可获取可用的 Kafka 集群列表，以便后续操作（如 listTopic / searchMessage）使用。")
    public List<KafkaCluster> listCluster() {
        return kafkaPropertiesConfig.getClusters();
    }

    /**
     * 测试 Kafka 集群连接
     * <p>
     * 通过 AdminClient 连接指定 brokers，验证网络可达性和 Kafka 服务可用性。10 秒内必须返回结果。
     *
     * @param brokers Kafka 集群地址，例如 "localhost:9092" 或 "kafka1:9092,kafka2:9092"
     * @return KafkaConnectionResult 结构化连接测试结果
     */
    @Tool(description = "测试 Kafka 集群连接是否可用。"
            + "在调用 listTopic、searchMessage 等任何需要 brokers 参数的 Kafka 操作之前，必须先调用此工具验证连接。"
            + "返回 KafkaConnectionResult，包含 connected（是否连接成功）、nodeCount（节点数）、topicCount（topic数量）、"
            + "elapsedMs（耗时毫秒）、brokers（地址）、message（详细信息）。"
            + "如果 connected=false，则不应继续调用其他 Kafka 工具。"
            + "连接超时时间为 10 秒。")
    public KafkaConnectionResult testConnection(
            @ToolParam(description = "Kafka 集群的 brokers 地址，格式如 \"localhost:9092\" 或 \"kafka1:9092,kafka2:9092\"", required = true) String brokers) {

        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
        // 关键：default.api.timeout.ms 是 AdminClient 所有内部操作的硬上限，默认 60000ms
        props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 8000);
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        // 最大重试次数设为 0，连接错误立即失败不重试
        props.put(AdminClientConfig.RETRIES_CONFIG, 0);

        long startTime = System.currentTimeMillis();
        log.info("testConnection 开始：{}", brokers);
        try (AdminClient adminClient = AdminClient.create(props)) {
            ListTopicsResult topicsResult = adminClient.listTopics();
            Set<String> topics = topicsResult.names().get(8, TimeUnit.SECONDS);
            int brokerCount = adminClient.describeCluster().nodes().get(8, TimeUnit.SECONDS).size();

            long elapsed = System.currentTimeMillis() - startTime;
            String message = String.format("集群连接正常，broker节点数=%d，topic数量=%d", brokerCount, topics.size());
            log.info("testConnection 成功: brokers={}, nodeCount={}, topicCount={}, elapsed={}ms",
                    brokers, brokerCount, topics.size(), elapsed);
            return KafkaConnectionResult.builder()
                    .connected(true)
                    .nodeCount(brokerCount)
                    .topicCount(topics.size())
                    .elapsedMs(elapsed)
                    .brokers(brokers)
                    .message(message)
                    .build();
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            String message = "连接失败: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            log.warn("testConnection 失败: brokers={}, elapsed={}ms, error={}", brokers, elapsed, e.getMessage());
            return KafkaConnectionResult.builder()
                    .connected(false)
                    .nodeCount(0)
                    .topicCount(0)
                    .elapsedMs(elapsed)
                    .brokers(brokers)
                    .message(message)
                    .build();
        }
    }

    /**
     * 列出指定 Kafka 集群的所有 Topic
     *
     * @param brokers Kafka 集群地址，例如 "localhost:9092" 或 "kafka1:9092,kafka2:9092"
     * @return Topic 信息列表，包含 name（Topic名称）、partitions（分区数）、replicationInfo（副本信息）
     */
    @Tool(description = "列出指定 Kafka 集群的所有 Topic 及其分区信息。"
            + "返回结果包含每个 Topic 的 name（Topic名称）、partitions（分区数）、replicationInfo（副本信息简要描述）。"
            + "调用此方法前必须先调用 testConnection 验证 brokers 连接可用。"
            + "需要先通过 listCluster 获取可用的 brokers 地址。")
    public List<KafkaTopicInfo> listTopic(
            @ToolParam(description = "Kafka 集群的 brokers 地址，格式如 \"localhost:9092\" 或 \"kafka1:9092,kafka2:9092\"", required = true) String brokers) {
        log.info("call listTopic begin, brokers: {}", brokers);
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 10000);

        List<KafkaTopicInfo> topicInfoList = new ArrayList<>();

        try (AdminClient adminClient = AdminClient.create(props)) {
            // 排除 Kafka 内部 topic
            ListTopicsResult topicsResult = adminClient.listTopics();
            Set<String> topicNames = topicsResult.names().get();
            topicNames = topicNames.stream()
                    .filter(name -> !name.startsWith("__"))
                    .collect(Collectors.toSet());

            if (topicNames.isEmpty()) {
                return topicInfoList;
            }

            // 获取每个 topic 的描述信息
            Map<String, TopicDescription> descriptions = adminClient.describeTopics(topicNames)
                    .allTopicNames()
                    .get();

            for (Map.Entry<String, TopicDescription> entry : descriptions.entrySet()) {
                TopicDescription desc = entry.getValue();
                int partitionCount = desc.partitions().size();
                // 构建副本信息：取第一个分区的副本信息作为代表
                String replicationInfo = desc.partitions().stream()
                        .findFirst()
                        .map(p -> "replicas=" + p.replicas().size() + ", isr=" + p.isr().size())
                        .orElse("unknown");

                topicInfoList.add(KafkaTopicInfo.builder()
                        .name(entry.getKey())
                        .partitions(partitionCount)
                        .replicationInfo(replicationInfo)
                        .build());
            }
        } catch (ExecutionException e) {
            log.error("listTopic 执行失败: brokers={}", brokers, e);
            throw new RuntimeException("获取 Kafka Topic 列表失败: " + e.getCause().getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取 Kafka Topic 列表被中断");
        }
        log.info("call listTopic end, brokers: {}", brokers);

        return topicInfoList;
    }

    /**
     * 搜索指定 Topic 中的 Kafka 消息
     * <p>
     * 遍历指定 topic 的所有分区，按条件过滤消息：
     * <ul>
     *   <li>topic：必须指定要搜索的 Topic 名称</li>
     *   <li>searchValue：可指定多个关键词（逗号分隔），消息 value 需包含全部关键词（AND 逻辑），为空则匹配所有</li>
     *   <li>beginTime：起始时间（含），格式 yyyy-MM-dd HH:mm:ss，为空则不限制起始时间</li>
     *   <li>endTime：结束时间（含），格式 yyyy-MM-dd HH:mm:ss，为空则不限制结束时间</li>
     *   <li>limitCount：最多返回条数，默认 1000</li>
     *   <li>timeout：最长搜索时间（分钟），默认 10 分钟</li>
     * </ul>
     *
     * @param brokers     Kafka 集群地址
     * @param topic       要搜索的 Topic 名称（必填）
     * @param searchValue 搜索关键词，多个关键词用逗号分隔（AND 逻辑），按消息 value 匹配，可选
     * @param beginTime   起始时间（格式 yyyy-MM-dd HH:mm:ss），可选
     * @param endTime     结束时间（格式 yyyy-MM-dd HH:mm:ss），可选
     * @param limitCount  最多返回条数，默认 1000
     * @param timeout     最长搜索超时时间（分钟），默认 10
     * @return 匹配的消息列表，每条包含 key、value、partition、offset、timestamp
     */
    @Tool(description = "在指定 Kafka Topic 中搜索消息。调用此方法前必须先调用 testConnection 验证 brokers 连接可用。"
            + "根据 searchValue（多个关键词逗号分隔，AND 匹配）、时间范围（beginTime/endTime，格式 yyyy-MM-dd HH:mm:ss）搜索消息。"
            + "参数说明："
            + "brokers（必填）: Kafka 集群地址；"
            + "topic（必填）: 要搜索的 Topic 名称，需先通过 listTopic 获取可用的 Topic 列表；"
            + "searchValue（可选）: 搜索的消息 value 关键词，多个用逗号分隔（AND 逻辑：消息 value 需同时包含所有关键词），不传则返回该 Topic 所有 value 的消息；"
            + "beginTime（可选）: 起始时间，格式 yyyy-MM-dd HH:mm:ss，例如 2025-01-01 00:00:00，不传则不限制起始时间；"
            + "endTime（可选）: 结束时间，格式同上，不传则不限制结束时间；"
            + "limitCount（可选）: 最多返回的消息条数，默认 1000；"
            + "timeout（可选）: 搜索超时时间（分钟），默认 10 分钟。"
            + "结果返回匹配消息列表，每条包含 key、value、partition、offset、timestamp。")
    public List<KafkaMessage> searchMessage(
            @ToolParam(description = "Kafka 集群的 brokers 地址，格式如 \"localhost:9092\" 或多个用逗号分隔", required = true) String brokers,
            @ToolParam(description = "要搜索的 Topic 名称，例如 \"order-topic\"。需先通过 listTopic 获取可用的 Topic 列表",
                    required = true) String topic,
            @ToolParam(description = "搜索的消息 value 关键词，多个关键词用逗号分隔（AND 逻辑：消息 value 需同时包含所有关键词）。"
                    + "例如 \"success,paid\" 表示搜索 value 中同时包含 success 和 paid 的消息。不传则匹配所有 value。", required = false) String searchValue,
            @ToolParam(description = "起始时间，格式 yyyy-MM-dd HH:mm:ss，例如 \"2025-01-01 00:00:00\"。"
                    + "只返回时间戳 >= beginTime 的消息。不传则不限制起始时间。", required = false) String beginTime,
            @ToolParam(description = "结束时间，格式 yyyy-MM-dd HH:mm:ss，例如 \"2025-12-31 23:59:59\"。"
                    + "只返回时间戳 <= endTime 的消息。不传则不限制结束时间。", required = false) String endTime,
            @ToolParam(description = "最多返回的消息条数，默认 1000", required = false) Integer limitCount,
            @ToolParam(description = "搜索超时时间（分钟），默认 10 分钟", required = false) Integer timeout) {

        // 参数默认值处理
        int maxResults = (limitCount != null && limitCount > 0) ? limitCount : 1000;
        int timeoutMinutes = (timeout != null && timeout > 0) ? timeout : 10;

        // 解析时间范围
        Long beginTimestamp = parseTime(beginTime);
        Long endTimestamp = parseTime(endTime);

        // 校验时间范围合法性
        if (beginTimestamp != null && endTimestamp != null && beginTimestamp > endTimestamp) {
            throw new IllegalArgumentException("beginTime 不能晚于 endTime: beginTime=" + beginTime + ", endTime=" + endTime);
        }

        // 解析搜索关键词（AND 逻辑，逗号分隔多个 value 关键词）
        List<String> searchValues = parseSearchValues(searchValue);

        // 构建 Consumer 配置
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-tool-search-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1000);
        props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 50 * 1024 * 1024);
        props.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, 100 * 1024 * 1024);

        Duration pollTimeout = Duration.ofSeconds(1);
        long deadline = System.currentTimeMillis() + timeoutMinutes * 60 * 1000L;

        List<KafkaMessage> results = new ArrayList<>();

        log.info("开始搜索 Kafka 消息: brokers={}, topic={}, searchValue={}, beginTime={}, endTime={}, limitCount={}, timeout={}min",
                brokers, topic, searchValue, beginTime, endTime, maxResults, timeoutMinutes);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            List<TopicPartition> allPartitions = new ArrayList<>();
            try {
                List<org.apache.kafka.common.PartitionInfo> partitionInfos = consumer.partitionsFor(topic);
                if (partitionInfos == null || partitionInfos.isEmpty()) {
                    throw new IllegalArgumentException("Topic 不存在或无分区: " + topic);
                }
                for (org.apache.kafka.common.PartitionInfo pi : partitionInfos) {
                    allPartitions.add(new TopicPartition(topic, pi.partition()));
                }
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                log.error("获取 topic {} 分区信息失败", topic, e);
                throw new RuntimeException("获取 Topic 分区信息失败: " + topic + ", " + e.getMessage());
            }

            if (allPartitions.isEmpty()) {
                log.warn("未找到任何可用分区");
                return results;
            }

            consumer.assign(allPartitions);

            // === 根据 beginTime 通过时间戳定位起始 offset ===
            Map<TopicPartition, Long> partitionEndOffsets;
            if (beginTimestamp != null) {
                Map<TopicPartition, Long> timestampsToSearch = new HashMap<>();
                for (TopicPartition tp : allPartitions) {
                    timestampsToSearch.put(tp, beginTimestamp);
                }
                Map<TopicPartition, org.apache.kafka.clients.consumer.OffsetAndTimestamp> offsetTimestamps =
                        consumer.offsetsForTimes(timestampsToSearch);

                int seekedCount = 0;
                int noOffsetCount = 0;
                for (Map.Entry<TopicPartition, org.apache.kafka.clients.consumer.OffsetAndTimestamp> entry : offsetTimestamps.entrySet()) {
                    if (entry.getValue() != null) {
                        consumer.seek(entry.getKey(), entry.getValue().offset());
                        seekedCount++;
                    } else {
                        consumer.seekToEnd(Collections.singletonList(entry.getKey()));
                        noOffsetCount++;
                    }
                }
                log.info("根据 beginTime 定位 offset: {} 个分区已 seek，{} 个分区无匹配数据已跳过", seekedCount, noOffsetCount);
            } else {
                consumer.seekToBeginning(allPartitions);
                log.info("无 beginTime，共分配 {} 个分区，从 earliest offset 开始消费", allPartitions.size());
            }

            // === 根据 endTime 确定截止 offset ===
            if (endTimestamp != null) {
                Map<TopicPartition, Long> endTimestamps = new HashMap<>();
                for (TopicPartition tp : allPartitions) {
                    endTimestamps.put(tp, endTimestamp);
                }
                Map<TopicPartition, org.apache.kafka.clients.consumer.OffsetAndTimestamp> endOffsetTimestamps =
                        consumer.offsetsForTimes(endTimestamps);

                partitionEndOffsets = new HashMap<>();
                for (Map.Entry<TopicPartition, org.apache.kafka.clients.consumer.OffsetAndTimestamp> entry : endOffsetTimestamps.entrySet()) {
                    if (entry.getValue() != null) {
                        partitionEndOffsets.put(entry.getKey(), entry.getValue().offset());
                    } else {
                        partitionEndOffsets.put(entry.getKey(),
                                consumer.endOffsets(Collections.singletonList(entry.getKey())).get(entry.getKey()));
                    }
                }
                log.info("根据 endTime 确定各分区截止 offset");
            } else {
                partitionEndOffsets = consumer.endOffsets(allPartitions);
            }

            boolean allPartitionsReachedEnd = false;
            while (!allPartitionsReachedEnd && System.currentTimeMillis() < deadline && results.size() < maxResults) {
                ConsumerRecords<String, String> records = consumer.poll(pollTimeout);

                if (records.isEmpty()) {
                    allPartitionsReachedEnd = checkAllPartitionsEnd(consumer, allPartitions, partitionEndOffsets);
                    continue;
                }

                for (var record : records) {
                    if (results.size() >= maxResults || System.currentTimeMillis() >= deadline) {
                        break;
                    }

                    TopicPartition tp = new TopicPartition(record.topic(), record.partition());
                    Long endOffset = partitionEndOffsets.get(tp);

                    if (endOffset != null && record.offset() >= endOffset) {
                        continue;
                    }

                    // 时间过滤（兜底）
                    if (beginTimestamp != null && record.timestamp() < beginTimestamp) {
                        continue;
                    }
                    if (endTimestamp != null && record.timestamp() > endTimestamp) {
                        continue;
                    }

                    // value 关键词过滤（AND 逻辑）
                    if (!searchValues.isEmpty()) {
                        String msgValue = record.value();
                        if (msgValue == null || !matchesAllKeywords(msgValue, searchValues)) {
                            continue;
                        }
                    }

                    results.add(KafkaMessage.builder()
                            .key(record.key())
                            .value(record.value())
                            .partition(record.partition())
                            .offset(record.offset())
                            .timestamp(record.timestamp())
                            .build());
                }
            }

            if (System.currentTimeMillis() >= deadline) {
                log.warn("搜索超时（{}分钟），已返回 {} 条结果，可能不完整", timeoutMinutes, results.size());
            }
        }

        log.info("Kafka 消息搜索完成: 返回 {} 条结果", results.size());
        return results;
    }

    /**
     * 解析时间字符串为毫秒时间戳
     */
    private Long parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime localDateTime = LocalDateTime.parse(timeStr, formatter);
            ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.systemDefault());
            return zonedDateTime.toInstant().toEpochMilli();
        } catch (Exception e) {
            throw new IllegalArgumentException("时间格式错误，请使用 yyyy-MM-dd HH:mm:ss 格式，例如 2025-01-01 00:00:00。输入: " + timeStr);
        }
    }

    /**
     * 解析搜索关键词列表（逗号分隔，AND 逻辑）
     */
    private List<String> parseSearchValues(String searchValue) {
        if (searchValue == null || searchValue.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        for (String part : searchValue.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return values;
    }

    /**
     * 检查消息内容是否包含所有关键词（AND 逻辑）
     */
    private boolean matchesAllKeywords(String msgContent, List<String> keywords) {
        for (String keyword : keywords) {
            if (!msgContent.contains(keyword)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查所有分区是否都已消费到指定的截止 offset
     */
    private boolean checkAllPartitionsEnd(KafkaConsumer<String, String> consumer,
                                          List<TopicPartition> allPartitions,
                                          Map<TopicPartition, Long> partitionEndOffsets) {
        for (TopicPartition tp : allPartitions) {
            long position = consumer.position(tp);
            Long endOffset = partitionEndOffsets.get(tp);
            if (endOffset != null && position < endOffset) {
                return false;
            }
        }
        return true;
    }
}