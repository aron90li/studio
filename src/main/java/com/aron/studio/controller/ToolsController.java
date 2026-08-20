package com.aron.studio.controller;

import com.aron.studio.ai.tools.kafka.KafkaCluster;
import com.aron.studio.ai.tools.kafka.KafkaMessage;
import com.aron.studio.ai.tools.kafka.KafkaTopicInfo;
import com.aron.studio.ai.dto.MysqlExecuteRequest;
import com.aron.studio.ai.dto.MysqlQueryResult;
import com.aron.studio.ai.tools.mysql.MysqlConnection;
import com.aron.studio.data.Response;
import com.aron.studio.service.ToolsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tools")
public class ToolsController {

    @Autowired
    private ToolsService toolsService;

    @GetMapping("/db/connections")
    public Response<List<MysqlConnection>> listMysqlConnections() {
        return Response.success(toolsService.listMysqlConnections());
    }

    @PostMapping("/db/execute")
    public Response<MysqlQueryResult> executeMysql(@RequestBody MysqlExecuteRequest request) {
        return Response.success(toolsService.executeMysql(
                request == null ? null : request.getConnectionName(),
                request == null ? null : request.getSql()));
    }

    @GetMapping("/kafka/clusters")
    public Response<List<KafkaCluster>> listClusters() {
        return Response.success(toolsService.listCluster());
    }

    @GetMapping("/kafka/topics")
    public Response<List<KafkaTopicInfo>> listTopics(@RequestParam String brokers) {
        return Response.success(toolsService.listTopic(brokers));
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
        return Response.success(toolsService.searchMessage(brokers, topic, searchValue, beginTime, endTime, limitCount, timeout));
    }


}
