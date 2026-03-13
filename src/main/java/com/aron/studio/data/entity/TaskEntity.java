package com.aron.studio.data.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务表实体类
 */
@Data
public class TaskEntity {

    private Long id; // 自增主键,技术主键,不参与业务
    private Long taskId; // 任务id,业务id,java使用Long,前端使用String
    private Long projectId; // 项目id
    private String taskName; // 任务名称
    private String description; // 任务描述
    private String taskType; // 任务类型
    private String taskSql; // 任务sql
    private String taskParam; // 任务参数,用户传入的参数,优先级最高,覆盖其他地方的同名参数
    private String taskSource; // 源表
    private String taskSide; // 维表
    private String taskSink; // 结果表
    private Integer taskVersion; // 任务版本, 版本表的最新版本, 应用层注意并发更新问题
    private Integer deleted; // 删除标识
    private Integer publishStatus; // 预留字段, 发布状态: 0草稿-1已发布-2已下线
    private Long createUser; // 创建用户
    private Long updateUser; // 修改用户
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 修改时间
}