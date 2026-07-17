package com.aron.studio.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 演示数据初始化器 - 启动时自动创建演示表并插入示例数据
 */
@Slf4j
@Component
public class DemoDataInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DemoDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        log.info("========== 开始初始化AI演示数据 ==========");

        // 创建示例 user 表（演示用）
        createUserTable();

        // 创建示例 product 表（演示用）
        createProductTable();

        // 创建示例 order 表（演示用）
        createOrderTable();

        log.info("========== AI演示数据初始化完成 ==========");
    }

    /**
     * 用户表（演示用）
     */
    private void createUserTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS demo_user (
                    id        BIGINT PRIMARY KEY AUTO_INCREMENT,
                    name      VARCHAR(100) NOT NULL COMMENT '姓名',
                    age       INT COMMENT '年龄',
                    email     VARCHAR(200) COMMENT '邮箱',
                    phone     VARCHAR(20) COMMENT '手机号',
                    status    TINYINT DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
                ) COMMENT '演示-用户表'
                """);

        // 插入示例数据（如果表为空）
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM demo_user", Integer.class);
        if (count != null && count == 0) {
            jdbcTemplate.update("INSERT INTO demo_user(name, age, email, phone, status) VALUES (?, ?, ?, ?, ?)",
                    "张三", 28, "zhangsan@example.com", "13800138001", 1);
            jdbcTemplate.update("INSERT INTO demo_user(name, age, email, phone, status) VALUES (?, ?, ?, ?, ?)",
                    "李四", 32, "lisi@example.com", "13800138002", 1);
            jdbcTemplate.update("INSERT INTO demo_user(name, age, email, phone, status) VALUES (?, ?, ?, ?, ?)",
                    "王五", 25, "wangwu@example.com", "13800138003", 0);
            jdbcTemplate.update("INSERT INTO demo_user(name, age, email, phone, status) VALUES (?, ?, ?, ?, ?)",
                    "赵六", 30, "zhaoliu@example.com", "13800138004", 1);
            jdbcTemplate.update("INSERT INTO demo_user(name, age, email, phone, status) VALUES (?, ?, ?, ?, ?)",
                    "张三丰", 80, "zhangsf@example.com", "13800138005", 1);
            log.info("demo_user 表已插入 {} 条示例数据", 5);
        }
    }

    /**
     * 产品表（演示用）
     */
    private void createProductTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS demo_product (
                    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
                    name        VARCHAR(200) NOT NULL COMMENT '产品名称',
                    price       DECIMAL(10,2) NOT NULL COMMENT '价格',
                    stock       INT DEFAULT 0 COMMENT '库存',
                    category    VARCHAR(100) COMMENT '分类',
                    status      TINYINT DEFAULT 1 COMMENT '状态: 1-上架, 0-下架',
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
                ) COMMENT '演示-产品表'
                """);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM demo_product", Integer.class);
        if (count != null && count == 0) {
            jdbcTemplate.update("INSERT INTO demo_product(name, price, stock, category, status) VALUES (?, ?, ?, ?, ?)",
                    "笔记本电脑", 5999.00, 100, "电子产品", 1);
            jdbcTemplate.update("INSERT INTO demo_product(name, price, stock, category, status) VALUES (?, ?, ?, ?, ?)",
                    "机械键盘", 399.00, 200, "外设", 1);
            jdbcTemplate.update("INSERT INTO demo_product(name, price, stock, category, status) VALUES (?, ?, ?, ?, ?)",
                    "无线鼠标", 89.00, 300, "外设", 1);
            jdbcTemplate.update("INSERT INTO demo_product(name, price, stock, category, status) VALUES (?, ?, ?, ?, ?)",
                    "显示器", 1999.00, 50, "电子产品", 1);
            log.info("demo_product 表已插入 {} 条示例数据", 4);
        }
    }

    /**
     * 订单表（演示用）
     */
    private void createOrderTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS demo_order (
                    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
                    order_no     VARCHAR(100) NOT NULL COMMENT '订单号',
                    user_name    VARCHAR(100) COMMENT '用户姓名',
                    product_name VARCHAR(200) COMMENT '产品名称',
                    quantity     INT DEFAULT 1 COMMENT '数量',
                    total_amount DECIMAL(12,2) COMMENT '总金额',
                    status       VARCHAR(32) DEFAULT 'PENDING' COMMENT '状态: PENDING/PAID/SHIPPED/DONE/CANCELLED',
                    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP
                ) COMMENT '演示-订单表'
                """);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM demo_order", Integer.class);
        if (count != null && count == 0) {
            jdbcTemplate.update("INSERT INTO demo_order(order_no, user_name, product_name, quantity, total_amount, status) VALUES (?, ?, ?, ?, ?, ?)",
                    "ORD2024010001", "张三", "笔记本电脑", 1, 5999.00, "PAID");
            jdbcTemplate.update("INSERT INTO demo_order(order_no, user_name, product_name, quantity, total_amount, status) VALUES (?, ?, ?, ?, ?, ?)",
                    "ORD2024010002", "张三", "机械键盘", 2, 798.00, "SHIPPED");
            jdbcTemplate.update("INSERT INTO demo_order(order_no, user_name, product_name, quantity, total_amount, status) VALUES (?, ?, ?, ?, ?, ?)",
                    "ORD2024010003", "李四", "显示器", 1, 1999.00, "DONE");
            log.info("demo_order 表已插入 {} 条示例数据", 3);
        }
    }
}