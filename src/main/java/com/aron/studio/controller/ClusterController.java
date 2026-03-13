package com.aron.studio.controller;

import com.aron.studio.data.Response;
import com.aron.studio.data.vo.TreeNodeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 *
 */
@Slf4j
@RestController
@RequestMapping("/api/cluster")
public class ClusterController {

    @Autowired
    // private ClusterService clusterService;

    @PostMapping("/createCluster")
    public Response<Map<String, String>> createCluster() {
        log.info("call createCluster, param: {}");
        return Response.success();
    }

    @GetMapping("/getCluster")
    public Response<List<TreeNodeVO>> getCluster() {
        return Response.success();
    }

    @PostMapping("/deleteCluster")
    public Response<Integer> deleteCluster() {
        log.info("call deleteCluster, param: {}");
        return Response.success();
    }

    @PostMapping("/updateCluster")
    public Response<Integer> updateCluster() {
        log.info("call updateCluster, param: {}");
        return Response.success();
    }


}
