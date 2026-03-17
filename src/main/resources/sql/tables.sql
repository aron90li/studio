-- 用户表， 管理员用户需要注册后手动改 role
CREATE TABLE if not exists user
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键,技术主键,不参与业务',
    user_id     BIGINT UNIQUE       NOT NULL COMMENT '业务id,java使用Long,前端使用String',
    username    VARCHAR(255) UNIQUE NOT NULL COMMENT '用户名,不能重复,类似微信id',
    nickname    VARCHAR(255) COMMENT '昵称',
    password    VARCHAR(255)        NOT NULL COMMENT '密码',
    enabled     TINYINT             NOT NULL default 1 COMMENT '启用标志',
    role        VARCHAR(255)        NOT NULL DEFAULT 'ROLE_USER' COMMENT '系统级用户角色,管理员-ROLE_ADMIN,普通用户-ROLE_USER',
    description VARCHAR(255) COMMENT '用户描述',
    create_user BIGINT COMMENT '创建用户',
    update_user BIGINT COMMENT '修改用户',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间'
) COMMENT '用户表';

-- 用户详情表 user_detail

-- 项目表
CREATE TABLE if not exists project
(
    id               BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键,技术主键,不参与业务',
    project_id       BIGINT UNIQUE NOT NULL COMMENT '业务id,java使用Long,前端使用String',
    project_name     VARCHAR(255)  NOT NULL COMMENT '项目名称',
    project_identity VARCHAR(255) COMMENT '项目标志',
    enabled          TINYINT       NOT NULL default 1 COMMENT '启用标志',
    description      VARCHAR(255) COMMENT '项目描述',
    create_user      BIGINT COMMENT '创建用户',
    update_user      BIGINT COMMENT '修改用户',
    create_time      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间'
) COMMENT '项目表';

-- 项目详情表
CREATE TABLE if not exists project_detail
(
    id               BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键,技术主键,不参与业务',
    project_id       BIGINT  NOT NULL COMMENT '项目id',
    detail_type      VARCHAR(255)  NOT NULL COMMENT '详情类型，枚举值:sql_params',
    detail_value     LONGTEXT COMMENT '详情值',
    create_user      BIGINT COMMENT '创建用户',
    update_user      BIGINT COMMENT '修改用户',
    create_time      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    UNIQUE (project_id, detail_type)
) COMMENT '项目详情表';

-- 项目授权表，项目成员表
CREATE TABLE if not exists project_user
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键,技术主键,不参与业务',
    project_id   BIGINT NOT NULL COMMENT '业务id,java使用Long,前端使用String',
    user_id      BIGINT NOT NULL COMMENT '用户id,java使用Long,前端使用String',
    project_role VARCHAR(32)  COMMENT '扩展字段,项目级别角色',
    create_user  BIGINT COMMENT '创建用户',
    update_user  BIGINT COMMENT '修改用户',
    create_time  DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    UNIQUE (project_id, user_id)
) COMMENT '项目授权表';

-- 任务树表
CREATE TABLE if not exists tree_node
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键,技术主键,不参与业务',
    node_id      BIGINT NOT NULL COMMENT '业务id,java使用Long,前端使用String',
    project_id   BIGINT NOT NULL COMMENT '项目id',
    node_name    VARCHAR(255) NOT NULL COMMENT '节点名称, 文件夹名字或者任务名字',
    node_type    VARCHAR(32)  COMMENT '节点类型:folder/task',
    parent_node_id    BIGINT  COMMENT '父节点id,根节点是null',
    task_id      BIGINT COMMENT '任务id, node_type是任务才有',
    create_user  BIGINT COMMENT '创建用户',
    update_user  BIGINT COMMENT '修改用户',
    create_time  DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    UNIQUE KEY uq_project_node_id (project_id, node_id)
) COMMENT '任务树表';

-- 任务表，本表不设计 flink 版本字段，意思是它可以提交到任意版本，在 task_instance 运行态设置运行信息
CREATE TABLE if not exists task
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键,技术主键,不参与业务',
    task_id        BIGINT UNIQUE      NOT NULL       COMMENT '任务id,业务id,java使用Long,前端使用String',
    project_id     BIGINT             NOT NULL       COMMENT '项目id',
    task_name      VARCHAR(255)  UNIQUE NOT NULL     COMMENT '任务名称',
    description    VARCHAR(255)                      COMMENT '任务描述',
    task_type      VARCHAR(255)                      COMMENT '任务类型',
    task_sql       LONGTEXT                           COMMENT '任务sql',
    task_param     LONGTEXT                           COMMENT '任务参数,用户传入的参数,优先级最高,覆盖其他地方的同名参数',
    task_source    LONGTEXT                           COMMENT '源表',
    task_side      LONGTEXT                           COMMENT '维表',
    task_sink      LONGTEXT                           COMMENT '结果表',
    task_version   INT           NOT NULL DEFAULT 0      COMMENT '任务版本, 版本表的最新版本, 应用层注意并发更新问题',
    deleted        TINYINT          DEFAULT 0        COMMENT '删除标识',
    publish_status  TINYINT                        COMMENT '预留字段, 发布状态: 0草稿-1已发布-2已下线',
    create_user    BIGINT                            COMMENT '创建用户',
    update_user    BIGINT                            COMMENT '修改用户',
    create_time    DATETIME                          DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME                          DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    KEY idx_project_id (project_id)
) COMMENT '任务表';

-- 任务版本表
CREATE TABLE if not exists task_version
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键,技术主键,不参与业务',
    task_id        BIGINT             NOT NULL       COMMENT '任务id, 来自task表',
    task_version   BIGINT      NOT NULL       COMMENT '任务版本',
    project_id     BIGINT             NOT NULL       COMMENT '项目id',

    task_name      VARCHAR(255)                      COMMENT '任务名称',
    description    VARCHAR(255)                      COMMENT '任务描述',
    task_type      VARCHAR(255)                      COMMENT '任务类型',
    task_sql       LONGTEXT                           COMMENT '任务sql',
    task_param     LONGTEXT                           COMMENT '任务参数,用户传入的参数,优先级最高,覆盖其他地方的同名参数',
    task_source    LONGTEXT                           COMMENT '源表',
    task_side      LONGTEXT                           COMMENT '维表',
    task_sink      LONGTEXT                           COMMENT '结果表',

    create_user    BIGINT                            COMMENT '创建用户',
    update_user    BIGINT                            COMMENT '修改用户',
    create_time    DATETIME                          DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME                          DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    UNIQUE KEY uk_task_version (task_id, task_version)
) COMMENT '任务表版本';

-- 集群定义表
CREATE TABLE if not exists cluster (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键,技术主键,不参与业务',
    cluster_id     BIGINT            UNIQUE NOT NULL       COMMENT '集群id',
    cluster_name   VARCHAR(255)       NOT NULL       COMMENT '集群名称, 一般使用namespace命名',
    description    VARCHAR(255)                      COMMENT '集群描述',
    cluster_type   VARCHAR(255)                      COMMENT '集群类型',
    flink_version  VARCHAR(255)                      COMMENT 'flink任务版本',
    default_conf      LONGTEXT                           COMMENT '集群的默认参数, 优先级低于任务参数,支持yaml和properties两种，后台能够自动识别',
    pod_template     LONGTEXT                           COMMENT 'pod模板',
    kubeconfig      LONGTEXT                           COMMENT 'k8s的kubeconfig配置',
    deleted        TINYINT                          DEFAULT 0 COMMENT '是否删除',
    create_user    BIGINT                            COMMENT '创建用户',
    update_user    BIGINT                            COMMENT '修改用户',
    create_time    DATETIME                          DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME                          DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间'
) COMMENT '集群定义表';

-- 运行态的表
CREATE TABLE if not exists task_instance (

)

