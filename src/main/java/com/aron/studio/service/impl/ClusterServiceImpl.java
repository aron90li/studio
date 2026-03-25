package com.aron.studio.service.impl;

import com.aron.studio.data.dao.UpdateClusterDAO;
import com.aron.studio.data.dto.cluster.CreateClusterDTO;
import com.aron.studio.data.dto.cluster.UpdateClusterDTO;
import com.aron.studio.data.entity.ClusterEntity;
import com.aron.studio.data.vo.ClusterVO;
import com.aron.studio.mapper.ClusterMapper;
import com.aron.studio.service.ClusterService;
import com.aron.studio.util.CurrentUserUtil;
import com.aron.studio.util.SnowflakeIdGenerator;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClusterServiceImpl implements ClusterService {

    @Autowired
    private ClusterMapper clusterMapper;

    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Autowired
    private CurrentUserUtil currentUserUtil;

    @Override
    public String createCluster(CreateClusterDTO createClusterDTO) {
        Long currentUserId = currentUserUtil.getCurrentUserId().orElseThrow(() -> new SecurityException("未登录"));

        Long clusterId = snowflakeIdGenerator.nextId();
        ClusterEntity clusterEntity = new ClusterEntity();
        BeanUtils.copyProperties(createClusterDTO, clusterEntity);
        clusterEntity.setClusterId(clusterId);
        clusterEntity.setDeleteId(0L);
        clusterEntity.setCreateUser(currentUserId);
        clusterEntity.setUpdateUser(currentUserId);

        try {
            clusterMapper.insertCluster(clusterEntity);
        } catch (DuplicateKeyException e) {
            throw new DuplicateKeyException("已存在相同的集群名字", e);
        }
        return String.valueOf(clusterEntity.getClusterId());
    }

    @Override
    public List<ClusterVO> getCluster() {
        return clusterMapper.getClusters();
    }

    @Override
    public int updateCluster(UpdateClusterDTO updateClusterDTO) {
        Long currentUserId = currentUserUtil.getCurrentUserId().orElseThrow(() -> new SecurityException("未登录"));

        UpdateClusterDAO dao = new UpdateClusterDAO();
        BeanUtils.copyProperties(updateClusterDTO, dao);
        dao.setClusterId(Long.valueOf(updateClusterDTO.getClusterId()));

        int count = 0;
        try {
            count = clusterMapper.updateCluster(dao, currentUserId, LocalDateTime.now());
        } catch (DuplicateKeyException e) {
            throw new DuplicateKeyException("已存在相同的集群名字", e);
        }
        return count;
    }

    @Override
    public int deleteClusters(List<String> clusterIds) {
        List<Long> clusterIdList = clusterIds.stream().map(Long::valueOf).collect(Collectors.toUnmodifiableList());
        Long currentUserId = currentUserUtil.getCurrentUserId().orElseThrow(() -> new SecurityException("未登录"));

        clusterIdList.forEach((clusterId) -> clusterMapper.softDeleteCluster(clusterId, currentUserId, LocalDateTime.now()));
        return clusterIdList.size();
    }
}
