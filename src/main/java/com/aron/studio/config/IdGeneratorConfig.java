package com.aron.studio.config;

import com.aron.studio.util.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdGeneratorConfig {
    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator() {
        // 单机 单 K8s 集群可以直接写死
        return new SnowflakeIdGenerator(1, 1);
    }

    public static void main(String[] args) {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
        System.out.println(generator.nextId());
    }
}
