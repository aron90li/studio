package com.aron.studio.controller;

import com.aron.studio.ai.tools.kafka.KafkaCluster;
import com.aron.studio.ai.tools.kafka.KafkaMessage;
import com.aron.studio.ai.tools.kafka.KafkaTopicInfo;
import com.aron.studio.data.Response;
import com.aron.studio.service.ToolsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/tools")
public class ToolsController {

    @Autowired
    private ToolsService toolsService;

    @GetMapping("/kafka/clusters")
    public Response<List<KafkaCluster>> listClusters() {
        try {
            return Response.success(toolsService.listCluster());
        } catch (Exception e) {
            log.error("call listClusters error: ", e);
            return Response.fail(e.getMessage());
        }
    }

    @GetMapping("/kafka/topics")
    public Response<List<KafkaTopicInfo>> listTopics(@RequestParam String brokers) {
        try {
            return Response.success(toolsService.listTopic(brokers));
        } catch (Exception e) {
            log.error("call listTopics error: ", e);
            return Response.fail(e.getMessage());
        }
    }

    @GetMapping("/kafka/searchMessage")
    public Response<List<KafkaMessage>> searchMessage(
            @RequestParam String brokers,
            @RequestParam String topic,
            @RequestParam(required = false) String searchValue,
            @RequestParam(required = false) String beginTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) Integer limitCount,
            @RequestParam(required = false) Integer timeout) {
        try {
            return Response.success(toolsService.searchMessage(brokers, topic, searchValue, beginTime, endTime, limitCount, timeout));
        } catch (Exception e) {
            log.error("call searchMessage error: ", e);
            return Response.fail(e.getMessage());
        }
    }


}
