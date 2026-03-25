-- =============================================
-- 1. 用户表 (SYS_USER)
-- =============================================
CREATE TABLE sys_user
(
    user_id     NUMBER(19,0) PRIMARY KEY,
    username    VARCHAR2(255) UNIQUE NOT NULL,
    nickname    VARCHAR2(255),
    password    VARCHAR2(255)        NOT NULL,
    enabled     NUMBER(1,0)          NOT NULL DEFAULT 1,
    role        VARCHAR2(255)        NOT NULL DEFAULT 'ROLE_USER',
    description VARCHAR2(255),
    create_user NUMBER(19,0),
    update_user NUMBER(19,0),
    create_time DATE                 DEFAULT SYSDATE,
    update_time DATE                 DEFAULT SYSDATE
);

COMMENT ON TABLE sys_user IS '用户表';
COMMENT ON COLUMN sys_user.user_id IS '业务 id,java 使用 Long,前端使用 String (现作为主键)';
COMMENT ON COLUMN sys_user.username IS '用户名，不能重复，类似微信 id';
COMMENT ON COLUMN sys_user.nickname IS '昵称';
COMMENT ON COLUMN sys_user.password IS '密码';
COMMENT ON COLUMN sys_user.enabled IS '启用标志';
COMMENT ON COLUMN sys_user.role IS '系统级用户角色，管理员-ROLE_ADMIN,普通用户-ROLE_USER';
COMMENT ON COLUMN sys_user.description IS '用户描述';
COMMENT ON COLUMN sys_user.create_user IS '创建用户';
COMMENT ON COLUMN sys_user.update_user IS '修改用户';
COMMENT ON COLUMN sys_user.create_time IS '创建时间';
COMMENT ON COLUMN sys_user.update_time IS '修改时间';

-- 插入 admin 用户 (id 列已移除，直接插入 user_id)
INSERT INTO sys_user(user_id, username, nickname, password, enabled, role, description)
VALUES (1, 'admin', '管理员', '$2a$10$n7GshUuBhFhfoRk9u.GC/uXzSqu7M6DsoQtOkx0iBKFO7eZc6GiKq', 1, 'ROLE_ADMIN', '管理员');

-- =============================================
-- 2. 项目表 (PROJECT)
-- =============================================
CREATE TABLE project
(
    project_id       NUMBER(19,0)   PRIMARY KEY,
    project_identity VARCHAR2(255)  NOT NULL,
    project_name     VARCHAR2(255)  NOT NULL,
    description      VARCHAR2(255),
    delete_id        NUMBER(19,0)   NOT NULL DEFAULT 0,
    create_user      NUMBER(19,0),
    update_user      NUMBER(19,0),
    create_time      DATE           DEFAULT SYSDATE,
    update_time      DATE           DEFAULT SYSDATE,
    CONSTRAINT uk_identity_delete_id UNIQUE (project_identity, delete_id)
);

COMMENT ON TABLE project IS '项目表';
COMMENT ON COLUMN project.project_id IS '业务id,java 使用 Long,前端使用 String (现作为主键)';
COMMENT ON COLUMN project.project_identity IS '项目标识, 未删除的保持唯一';
COMMENT ON COLUMN project.project_name   IS '项目名称';
COMMENT ON COLUMN project.delete_id      IS '0-未删除, >0-已删除,存储project_id';
COMMENT ON COLUMN project.description IS '项目描述';
COMMENT ON COLUMN project.create_user IS '创建用户';
COMMENT ON COLUMN project.update_user IS '修改用户';
COMMENT ON COLUMN project.create_time IS '创建时间';
COMMENT ON COLUMN project.update_time IS '修改时间';

-- =============================================
-- 3. 项目详情表 (PROJECT_DETAIL)
-- =============================================
CREATE TABLE project_detail
(
    project_id   NUMBER(19,0) NOT NULL,
    detail_type  VARCHAR2(255) NOT NULL,
    detail_value CLOB,
    create_user  NUMBER(19,0),
    update_user  NUMBER(19,0),
    create_time  DATE          DEFAULT SYSDATE,
    update_time  DATE          DEFAULT SYSDATE,
    CONSTRAINT pk_project_detail PRIMARY KEY (project_id, detail_type)
);

COMMENT ON TABLE project_detail IS '项目详情表';
COMMENT ON COLUMN project_detail.project_id IS '项目 id';
COMMENT ON COLUMN project_detail.detail_type IS '详情类型，枚举值:sql_params';
COMMENT ON COLUMN project_detail.detail_value IS '详情值';
COMMENT ON COLUMN project_detail.create_user IS '创建用户';
COMMENT ON COLUMN project_detail.update_user IS '修改用户';
COMMENT ON COLUMN project_detail.create_time IS '创建时间';
COMMENT ON COLUMN project_detail.update_time IS '修改时间';


-- =============================================
-- 4. 项目授权表 (PROJECT_USER)
-- =============================================
CREATE TABLE project_user
(
    project_id   NUMBER(19,0) NOT NULL,
    user_id      NUMBER(19,0) NOT NULL,
    project_role VARCHAR2(32),
    create_user  NUMBER(19,0),
    update_user  NUMBER(19,0),
    create_time  DATE          DEFAULT SYSDATE,
    update_time  DATE          DEFAULT SYSDATE,
    CONSTRAINT pk_project_user PRIMARY KEY (project_id, user_id)
);

COMMENT ON TABLE project_user IS '项目授权表';
COMMENT ON COLUMN project_user.project_id IS '业务 id,java 使用 Long,前端使用 String';
COMMENT ON COLUMN project_user.user_id IS '用户 id,java 使用 Long,前端使用 String';
COMMENT ON COLUMN project_user.project_role IS '扩展字段，项目级别角色，未使用';
COMMENT ON COLUMN project_user.create_user IS '创建用户';
COMMENT ON COLUMN project_user.update_user IS '修改用户';
COMMENT ON COLUMN project_user.create_time IS '创建时间';
COMMENT ON COLUMN project_user.update_time IS '修改时间';

-- =============================================
-- 5. 任务树表 (TREE_NODE)
-- =============================================
CREATE TABLE tree_node
(
    node_id        NUMBER(19,0) PRIMARY KEY,
    project_id     NUMBER(19,0) NOT NULL,
    node_name      VARCHAR2(255) NOT NULL,
    node_type      VARCHAR2(32),
    parent_node_id NUMBER(19,0) NOT NULL DEFAULT 0,
    task_id        NUMBER(19,0),
    create_user    NUMBER(19,0),
    update_user    NUMBER(19,0),
    create_time    DATE           DEFAULT SYSDATE,
    update_time    DATE           DEFAULT SYSDATE,
    CONSTRAINT uk_node_id_type_name UNIQUE (project_id, parent_node_id, node_type, node_name)
);
CREATE INDEX idx_tree_node_project_id ON tree_node(project_id);
COMMENT ON TABLE tree_node IS '任务树表';
COMMENT ON COLUMN tree_node.node_id IS '业务 id,java 使用 Long,前端使用 String (现作为主键一部分)';
COMMENT ON COLUMN tree_node.project_id IS '项目 id';
COMMENT ON COLUMN tree_node.node_name IS '节点名称，文件夹名字或者任务名字';
COMMENT ON COLUMN tree_node.node_type IS '节点类型:folder/task';
COMMENT ON COLUMN tree_node.parent_node_id IS '父节点 id,根节点是0';
COMMENT ON COLUMN tree_node.task_id IS '任务 id, node_type 是任务才有';
COMMENT ON COLUMN tree_node.create_user IS '创建用户';
COMMENT ON COLUMN tree_node.update_user IS '修改用户';
COMMENT ON COLUMN tree_node.create_time IS '创建时间';
COMMENT ON COLUMN tree_node.update_time IS '修改时间';

-- =============================================
-- 6. 任务表 (TASK)
-- 主键变更为: task_id
-- 移除了 id 列
-- =============================================
CREATE TABLE task
(
    task_id        NUMBER(19,0) PRIMARY KEY,
    project_id     NUMBER(19,0) NOT NULL,
    task_name      VARCHAR2(255) NOT NULL,
    description    VARCHAR2(255),
    task_type      VARCHAR2(255),
    task_sql       CLOB,
    task_param     CLOB,
    task_source    CLOB,
    task_side      CLOB,
    task_sink      CLOB,
    task_version   NUMBER(10,0) NOT NULL DEFAULT 0,
    delete_id      NUMBER(19,0) NOT NULL DEFAULT 0,
    publish_status NUMBER(1,0),
    create_user    NUMBER(19,0),
    update_user    NUMBER(19,0),
    create_time    DATE         DEFAULT SYSDATE,
    update_time    DATE         DEFAULT SYSDATE,
    CONSTRAINT uk_task_name_delete_id UNIQUE (task_name, delete_id)
);

COMMENT ON TABLE task IS '任务表';
COMMENT ON COLUMN task.task_id IS '任务 id,业务 id,java 使用 Long,前端使用 String (现作为主键)';
COMMENT ON COLUMN task.project_id IS '项目 id';
COMMENT ON COLUMN task.task_name IS '任务名称';
COMMENT ON COLUMN task.description IS '任务描述';
COMMENT ON COLUMN task.task_type IS '任务类型';
COMMENT ON COLUMN task.task_sql IS '任务 sql';
COMMENT ON COLUMN task.task_param IS '任务参数，用户传入的参数，优先级最高，覆盖其他地方的同名参数';
COMMENT ON COLUMN task.task_source IS '源表';
COMMENT ON COLUMN task.task_side IS '维表';
COMMENT ON COLUMN task.task_sink IS '结果表';
COMMENT ON COLUMN task.task_version IS '任务版本，版本表的最新版本，应用层注意并发更新问题';
COMMENT ON COLUMN task.delete_id IS '0-未删除, >0-已删除,存储task_id';
COMMENT ON COLUMN task.publish_status IS '预留字段，发布状态：0 草稿 -1 已发布 -2 已下线';
COMMENT ON COLUMN task.create_user IS '创建用户';
COMMENT ON COLUMN task.update_user IS '修改用户';
COMMENT ON COLUMN task.create_time IS '创建时间';
COMMENT ON COLUMN task.update_time IS '修改时间';

-- 重建普通索引
CREATE INDEX idx_task_project_id ON task(project_id);


-- =============================================
-- 7. 任务版本表 (TASK_VERSION)
-- 主键变更为复合主键: (task_id, task_version)
-- 移除了 id 列
-- =============================================
CREATE TABLE task_version
(
    task_id        NUMBER(19,0) NOT NULL,
    task_version   NUMBER(19,0) NOT NULL,
    project_id     NUMBER(19,0) NOT NULL,
    task_name      VARCHAR2(255),
    description    VARCHAR2(255),
    task_type      VARCHAR2(255),
    task_sql       CLOB,
    task_param     CLOB,
    task_source    CLOB,
    task_side      CLOB,
    task_sink      CLOB,
    create_user    NUMBER(19,0),
    update_user    NUMBER(19,0),
    create_time    DATE         DEFAULT SYSDATE,
    update_time    DATE         DEFAULT SYSDATE,
    CONSTRAINT pk_task_version PRIMARY KEY (task_id, task_version)
);

COMMENT ON TABLE task_version IS '任务表版本';
COMMENT ON COLUMN task_version.task_id IS '任务 id, 来自 task 表';
COMMENT ON COLUMN task_version.task_version IS '任务版本';
COMMENT ON COLUMN task_version.project_id IS '项目 id';
COMMENT ON COLUMN task_version.task_name IS '任务名称';
COMMENT ON COLUMN task_version.description IS '任务描述';
COMMENT ON COLUMN task_version.task_type IS '任务类型';
COMMENT ON COLUMN task_version.task_sql IS '任务 sql';
COMMENT ON COLUMN task_version.task_param IS '任务参数，用户传入的参数，优先级最高，覆盖其他地方的同名参数';
COMMENT ON COLUMN task_version.task_source IS '源表';
COMMENT ON COLUMN task_version.task_side IS '维表';
COMMENT ON COLUMN task_version.task_sink IS '结果表';
COMMENT ON COLUMN task_version.create_user IS '创建用户';
COMMENT ON COLUMN task_version.update_user IS '修改用户';
COMMENT ON COLUMN task_version.create_time IS '创建时间';
COMMENT ON COLUMN task_version.update_time IS '修改时间';


-- =============================================
-- 8. 集群定义表 (CLUSTER)
-- 主键变更为: cluster_id
-- 移除了 id 列
-- =============================================
CREATE TABLE sys_cluster
(
    cluster_id     NUMBER(19,0) PRIMARY KEY,
    cluster_name   VARCHAR2(255) NOT NULL,
    description    VARCHAR2(255),
    cluster_type   VARCHAR2(255),
    flink_version  VARCHAR2(255),
    default_conf   CLOB,
    pod_template   CLOB,
    kubeconfig     CLOB,
    delete_id      NUMBER(19,0) NOT NULL DEFAULT 0,
    create_user    NUMBER(19,0),
    update_user    NUMBER(19,0),
    create_time    DATE         DEFAULT SYSDATE,
    update_time    DATE         DEFAULT SYSDATE,
    CONSTRAINT uk_cluster_name_delete_id UNIQUE (cluster_name, delete_id)
);

COMMENT ON TABLE sys_cluster IS '集群定义表';
COMMENT ON COLUMN sys_cluster.cluster_id IS '集群 id (现作为主键)';
COMMENT ON COLUMN sys_cluster.cluster_name IS '集群名称，一般使用 namespace 命名';
COMMENT ON COLUMN sys_cluster.description IS '集群描述';
COMMENT ON COLUMN sys_cluster.cluster_type IS '集群类型';
COMMENT ON COLUMN sys_cluster.flink_version IS 'flink 任务版本';
COMMENT ON COLUMN sys_cluster.default_conf IS '集群的默认参数，优先级低于任务参数，支持 yaml 和 properties 两种，后台能够自动识别';
COMMENT ON COLUMN sys_cluster.pod_template IS 'pod 模板';
COMMENT ON COLUMN sys_cluster.kubeconfig IS 'k8s 的 kubeconfig 配置';
COMMENT ON COLUMN sys_cluster.delete_id IS '0-未删除, >0-已删除,存储 cluster_id';
COMMENT ON COLUMN sys_cluster.create_user IS '创建用户';
COMMENT ON COLUMN sys_cluster.update_user IS '修改用户';
COMMENT ON COLUMN sys_cluster.create_time IS '创建时间';
COMMENT ON COLUMN sys_cluster.update_time IS '修改时间';

-- 因为要兼容oracle，后续建表的 update_time 字段一定要在程序中更新，不使用数据库的特性