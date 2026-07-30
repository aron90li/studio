package com.aron.studio.ai.config;

import com.aron.studio.ai.tools.kafka.KafkaCluster;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "kafka")
public class KafkaPropertiesConfig {
    private List<KafkaCluster> clusters = new ArrayList<>();

    public List<KafkaCluster> getClusters() {
        return clusters;
    }

    public void setClusters(List<KafkaCluster> clusters) {
        this.clusters = clusters;
    }

}
