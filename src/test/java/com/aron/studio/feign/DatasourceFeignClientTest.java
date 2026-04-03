package com.aron.studio.feign;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class DatasourceFeignClientTest {

    @Autowired
    private DatasourceFeignClient datasourceFeignClient;

    @Test
    void getFeignTest() {
        Object obj = datasourceFeignClient.getFeignTest("haha");
        log.info("ok");
    }

    @Test
    void postFeignTest() {
    }
}