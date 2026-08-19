package com.aron.studio.service.impl;

import com.aron.studio.ai.tools.kafka.KafkaCluster;
import com.aron.studio.ai.tools.kafka.KafkaMessage;
import com.aron.studio.ai.tools.kafka.KafkaTopicInfo;
import com.aron.studio.ai.tools.kafka.KafkaTool;
import com.aron.studio.service.ToolsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ToolsServiceImpl implements ToolsService {

    @Autowired
    private KafkaTool kafkaTool;

    @Override
    public List<KafkaCluster> listCluster() {
        return kafkaTool.listCluster();
    }

    @Override
    public List<KafkaTopicInfo> listTopic(String brokers) {
        return kafkaTool.listTopic(brokers);
    }

    @Override
    public List<KafkaMessage> searchMessage(String brokers, String topic, String searchValue, String beginTime, String endTime, Integer limitCount, Integer timeout) {
        return kafkaTool.searchMessage(brokers, topic, searchValue, beginTime, endTime, limitCount, timeout);
    }
}
