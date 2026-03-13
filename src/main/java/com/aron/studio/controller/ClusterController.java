package com.aron.studio.controller;

import com.aron.studio.data.Response;
import com.aron.studio.data.dto.cluster.CreateClusterDTO;
import com.aron.studio.data.dto.cluster.DeleteClustersDTO;
import com.aron.studio.data.dto.cluster.UpdateClusterDTO;
import com.aron.studio.data.vo.ClusterVO;
import com.aron.studio.service.ClusterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 *
 * 集群定义管理
 */
@Slf4j
@RestController
@RequestMapping("/api/cluster")
public class ClusterController {

    @Autowired
    private ClusterService clusterService;

    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @PostMapping("/createCluster")
    public Response<Map<String, String>> createCluster(@RequestBody CreateClusterDTO createClusterDTO) {
        try {
            log.info("call createCluster, param: {}", createClusterDTO);
            String clusterId = clusterService.createCluster(createClusterDTO);
            return Response.success(Map.of("clusterId", clusterId));
        } catch (Exception e) {
            log.error("call createCluster error: ", e);
            return Response.fail(e.getMessage());
        }
    }

    @GetMapping("/getCluster")
    public Response<List<ClusterVO>> getCluster() {
        try {
            return Response.success(clusterService.getCluster());
        } catch (Exception e) {
            log.error("call getCluster error: ", e);
            return Response.fail(e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @PostMapping("/deleteClusters")
    public Response<Integer> deleteClusters(@RequestBody DeleteClustersDTO deleteClustersDTO) {
        try {
            log.info("call deleteClusters, param: {}", deleteClustersDTO.getClusterIds());
            return Response.success(clusterService.deleteClusters(deleteClustersDTO.getClusterIds()));
        } catch (Exception e) {
            log.error("call deleteClusters error: ", e);
            return Response.fail(e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @PostMapping("/updateCluster")
    public Response<Integer> updateCluster(@RequestBody UpdateClusterDTO updateClusterDTO) {
        try {
            log.info("call updateCluster, param: {}", updateClusterDTO);
            return Response.success(clusterService.updateCluster(updateClusterDTO));
        } catch (Exception e) {
            log.error("call updateCluster error: ", e);
            return Response.fail(e.getMessage());
        }
    }
}