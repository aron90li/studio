package com.aron.studio.mapper;

import com.aron.studio.data.dao.UpdateClusterDAO;
import com.aron.studio.data.entity.ClusterEntity;
import com.aron.studio.data.vo.ClusterVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ClusterMapper {

    @Select("""
                select count(*) from cluster where cluster_name = #{clusterName} and deleted = 0 and cluster_id != #{clusterId}
            """)
    int getCountByClusterNameUpdate(@Param("clusterName") String clusterName, @Param("clusterId") Long clusterId);

    @Select("""
                select count(*) from cluster where cluster_name = #{clusterName} and deleted = 0
            """)
    int getCountByClusterNameCreate(@Param("clusterName") String clusterName);

    @Insert("""
                INSERT INTO cluster (cluster_id, cluster_name, description, cluster_type,
                                     flink_version, default_conf, pod_template, kubeconfig,
                                     deleted, create_user, update_user)
                VALUES (#{clusterId}, #{clusterName}, #{description}, #{clusterType},
                        #{flinkVersion}, #{defaultConf}, #{podTemplate}, #{kubeconfig},
                        #{deleted}, #{createUser}, #{updateUser})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertCluster(ClusterEntity clusterEntity);

    @Select("""
                SELECT
                    c.cluster_id    AS clusterId,
                    c.cluster_name  AS clusterName,
                    c.description   AS description,
                    c.cluster_type  AS clusterType,
                    c.flink_version AS flinkVersion,
                    c.default_conf  AS defaultConf,
                    c.pod_template  AS podTemplate,
                    c.kubeconfig    AS kubeconfig,
                    c.create_time   AS createTime,
                    c.update_time   AS updateTime,
                    cu.username     AS createUsername,
                    cu.user_id      AS createUserId,
                    uu.username     AS updateUsername,
                    uu.user_id      AS updateUserId
                FROM cluster c
                LEFT JOIN user cu
                    ON c.create_user = cu.user_id
                LEFT JOIN user uu
                    ON c.update_user = uu.user_id
                WHERE c.deleted = 0
                ORDER BY c.create_time DESC
            """)
    List<ClusterVO> getClusters();

    @Update("""
            <script>
            UPDATE cluster
            <set>
                <if test="dao.clusterName != null">
                    cluster_name = #{dao.clusterName},
                </if>
                <if test="dao.description != null">
                    description = #{dao.description},
                </if>
                <if test="dao.clusterType != null">
                    cluster_type = #{dao.clusterType},
                </if>
                <if test="dao.flinkVersion != null">
                    flink_version = #{dao.flinkVersion},
                </if>
                <if test="dao.defaultConf != null">
                    default_conf = #{dao.defaultConf},
                </if>
                <if test="dao.podTemplate != null">
                    pod_template = #{dao.podTemplate},
                </if>
                <if test="dao.kubeconfig != null">
                    kubeconfig = #{dao.kubeconfig},
                </if>
                update_user = #{currentUserId}
            </set>
            WHERE cluster_id = #{dao.clusterId} AND deleted = 0
            </script>
            """)
    int updateCluster(@Param("dao") UpdateClusterDAO dao, @Param("currentUserId") Long currentUserId);

    @Update("""
            <script>
            UPDATE cluster
            SET deleted = 1, update_user = #{currentUserId}
            WHERE deleted = 0 AND cluster_id IN
            <foreach collection="clusterIds" item="clusterId" open="(" separator="," close=")">
                #{clusterId}
            </foreach>
            </script>
            """)
    int deleteClusters(@Param("clusterIds") List<Long> clusterIds, @Param("currentUserId") Long currentUserId);
}