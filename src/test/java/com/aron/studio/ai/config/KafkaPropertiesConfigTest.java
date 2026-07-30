package com.aron.studio.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest(classes = {KafkaPropertiesConfig.class})
@EnableConfigurationProperties(KafkaPropertiesConfig.class)
class KafkaPropertiesConfigTest {

    @Autowired
    KafkaPropertiesConfig kafkaPropertiesConfig;

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void getClusters() {
        log.info(kafkaPropertiesConfig.getClusters().toString());
        assertEquals(kafkaPropertiesConfig.getClusters().size(), 2);
    }
}