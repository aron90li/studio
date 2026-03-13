package com.aron.studio.service;

import com.aron.studio.data.dto.cluster.CreateClusterDTO;
import com.aron.studio.data.dto.cluster.UpdateClusterDTO;
import com.aron.studio.data.vo.ClusterVO;

import java.util.List;

public interface ClusterService {
    String createCluster(CreateClusterDTO createClusterDTO);

    List<ClusterVO> getCluster();

    int updateCluster(UpdateClusterDTO updateClusterDTO);

    int deleteClusters(List<String> clusterIds);
}
