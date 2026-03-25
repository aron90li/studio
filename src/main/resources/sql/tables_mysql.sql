-- 用户表, username全局唯一，软删除，使用enabled字段标识用户是否禁用
CREATE TABLE if not exists sys_user
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
-- 插入 admin用户，密码是admin
insert into sys_user(user_id, username, nickname, password, enabled, role, description)
values (1, 'admin', '管理员', '$2a$10$n7GshUuBhFhfoRk9u.GC/uXzSqu7M6DsoQtOkx0iBKFO7eZc6GiKq',
        '1', 'ROLE_ADMIN', '管理员');

-- 项目表，delete_id支持project_identity的无限删除重用
CREATE TABLE if not exists project
(
    id               BIGINT           PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键,技术主键,不参与业务',
    project_id       BIGINT           UNIQUE NOT NULL    COMMENT '业务id,java使用Long,前端使用String',
    project_identity VARCHAR(255)     NOT NULL    COMMENT '项目标识, 未删除的保持唯一',
    project_name     VARCHAR(255)     NOT NULL           COMMENT '项目名称',
    description      VARCHAR(255)     COMMENT '项目描述',
    delete_id        BIGINT           NOT NULL default 0 COMMENT '0-未删除, >0-已删除,存储project_id',
    create_user      BIGINT           COMMENT '创建用户',
    update_user      BIGINT COMMENT '修改用户',
    create_time      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    UNIQUE (project_identity, delete_id)
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

-- 项目授权表，项目成员表，删除是真删除
CREATE TABLE if not exists project_user
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键,技术主键,不参与业务',
    project_id   BIGINT NOT NULL COMMENT '业务id,java使用Long,前端使用String',
    user_id      BIGINT NOT NULL COMMENT '用户id,java使用Long,前端使用String',
    project_role VARCHAR(32)  COMMENT '扩展字段,项目级别角色, 未使用',
    create_user  BIGINT COMMENT '创建用户',
    update_user  BIGINT COMMENT '修改用户',
    create_time  DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    UNIQUE (project_id, user_id)
) COMMENT '项目授权表';

-- =====================================
-- 任务树表，硬删除
-- 同一个父目录下，同一node_type不能重名 unique (parent_node_id, node_type, node_name)，主要限制文件夹名字
-- 因为null不参与唯一判断，所以上述索引根目录下可以有多个重名的文件夹或文件。可以设根目录为0限制。
-- 全局，当 node_type = task时候，node_name唯一，函数索引条件索引实现，这里不管，通过应用实现
-- =====================================
CREATE TABLE if not exists tree_node
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键,技术主键,不参与业务',
    node_id      BIGINT UNIQUE NOT NULL COMMENT '业务id,java使用Long,前端使用String',
    project_id   BIGINT NOT NULL COMMENT '项目id',
    node_name    VARCHAR(255) NOT NULL COMMENT '节点名称, 文件夹名字或者任务名字',
    node_type    VARCHAR(32)  COMMENT '节点类型:folder/task',
    parent_node_id    BIGINT NOT NULL DEFAULT 0 COMMENT '父节点id,根节点是0',
    task_id      BIGINT COMMENT '任务id, node_type是任务才有',
    create_user  BIGINT COMMENT '创建用户',
    update_user  BIGINT COMMENT '修改用户',
    create_time  DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    INDEX (project_id),
    UNIQUE (project_id, parent_node_id, node_type, node_name)
) COMMENT '任务树表';

-- ==============================================
-- 任务表，本表不设计 flink 版本字段，意思是它可以提交到任意版本，在 task_instance 运行态设置运行信息
-- 本表要求 在未删除情况下，task_name唯一，添加 delete_id, 0 未删除， > 0 已删除，存task_id, 无限删除重用
-- ==============================================
CREATE TABLE if not exists task
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键,技术主键,不参与业务',
    task_id        BIGINT UNIQUE      NOT NULL       COMMENT '任务id,业务id,java使用Long,前端使用String',
    project_id     BIGINT             NOT NULL       COMMENT '项目id',
    task_name      VARCHAR(255)       NOT NULL       COMMENT '任务名称, 未删除时唯一',
    description    VARCHAR(255)                      COMMENT '任务描述',
    task_type      VARCHAR(255)                      COMMENT '任务类型',
    task_sql       LONGTEXT                           COMMENT '任务sql',
    task_param     LONGTEXT                           COMMENT '任务参数,用户传入的参数,优先级最高,覆盖其他地方的同名参数',
    task_source    LONGTEXT                           COMMENT '源表',
    task_side      LONGTEXT                           COMMENT '维表',
    task_sink      LONGTEXT                           COMMENT '结果表',
    task_version   INT         NOT NULL DEFAULT 0     COMMENT '任务版本, 版本表的最新版本, 应用层注意并发更新问题',
    delete_id      BIGINT        NOT NULL DEFAULT 0     COMMENT '0-未删除, >0-已删除,存储task_id',
    publish_status  TINYINT                        COMMENT '预留字段, 发布状态: 0草稿-1已发布-2已下线',
    create_user    BIGINT                            COMMENT '创建用户',
    update_user    BIGINT                            COMMENT '修改用户',
    create_time    DATETIME                          DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME                          DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    INDEX  (project_id),
    UNIQUE (task_name, delete_id)
) COMMENT '任务表';

-- 任务版本表
CREATE TABLE if not exists task_version
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键,技术主键,不参与业务',
    task_id        BIGINT             NOT NULL       COMMENT '任务id, 来自task表',
    task_version   BIGINT             NOT NULL       COMMENT '任务版本',
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
    UNIQUE (task_id, task_version)
) COMMENT '任务表版本';

-- 集群定义表
CREATE TABLE if not exists sys_cluster (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键,技术主键,不参与业务',
    cluster_id     BIGINT            UNIQUE NOT NULL       COMMENT '集群id',
    cluster_name   VARCHAR(255)       NOT NULL       COMMENT '集群名称, 任务名称, 未删除时唯一',
    description    VARCHAR(255)                      COMMENT '集群描述',
    cluster_type   VARCHAR(255)                      COMMENT '集群类型',
    flink_version  VARCHAR(255)                      COMMENT 'flink任务版本',
    default_conf      LONGTEXT                           COMMENT '集群的默认参数, 优先级低于任务参数,支持yaml和properties两种，后台能够自动识别',
    pod_template     LONGTEXT                           COMMENT 'pod模板',
    kubeconfig      LONGTEXT                           COMMENT 'k8s的kubeconfig配置',
    delete_id      BIGINT                  NOT NULL  DEFAULT 0 COMMENT '0-未删除, >0-已删除,存储 cluster_id',
    create_user    BIGINT                            COMMENT '创建用户',
    update_user    BIGINT                            COMMENT '修改用户',
    create_time    DATETIME                          DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME                          DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    UNIQUE (cluster_name, delete_id)
) COMMENT '集群定义表';

-- 因为要兼容oracle，后续建表的 update_time 字段一定要在程序中更新，不使用数据库的特性