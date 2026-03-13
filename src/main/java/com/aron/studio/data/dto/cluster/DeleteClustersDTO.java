package com.aron.studio.data.dto.cluster;

import lombok.Data;

import java.util.List;

@Data
public class DeleteClustersDTO {
    private List<String> clusterIds;
}
