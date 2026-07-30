package com.aron.studio.ai.tools.kafka;

import com.aron.studio.ai.config.KafkaPropertiesConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class KafkaTool {

    @Autowired
    private KafkaPropertiesConfig kafkaPropertiesConfig;


    @Tool(description = "")
    public List<KafkaCluster> listCluster() {
        return kafkaPropertiesConfig.getClusters();
    }


}
