package com.aron.studio.service;

import com.aron.studio.ai.tools.kafka.KafkaCluster;
import com.aron.studio.ai.tools.kafka.KafkaMessage;
import com.aron.studio.ai.tools.kafka.KafkaTopicInfo;
import com.aron.studio.ai.dto.MysqlQueryResult;
import com.aron.studio.ai.tools.mysql.MysqlConnection;

import java.util.List;

public interface ToolsService {
    List<MysqlConnection> listMysqlConnections();

    MysqlQueryResult executeMysql(String connectionName, String sql);

    List<KafkaCluster> listCluster();

    List<KafkaTopicInfo> listTopic(String brokers);

    List<KafkaMessage> searchMessage(String brokers, String topic,
                                     String searchValue, String beginTime, String endTime,
                                     Integer limitCount, Integer timeout);
}
