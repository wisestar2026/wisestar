-- 教研平台 MySQL 幂等建表脚本（生产环境手工执行一次）
CREATE DATABASE IF NOT EXISTS wisestar_research DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE wisestar_research;

CREATE TABLE IF NOT EXISTS t_research_admin (
    id VARCHAR(32) NOT NULL,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(100) NOT NULL,
    name VARCHAR(64),
    role VARCHAR(32) NOT NULL DEFAULT 'ADMIN',
    status TINYINT NOT NULL DEFAULT 1,
    create_at DATETIME,
    create_by VARCHAR(32),
    update_at DATETIME,
    update_by VARCHAR(32),
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_research_admin_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_research_plan (
    id VARCHAR(32) NOT NULL,
    semester VARCHAR(32) NOT NULL,
    subject VARCHAR(32) NOT NULL,
    grade VARCHAR(32) NOT NULL,
    class_type VARCHAR(32) NOT NULL,
    title VARCHAR(200) NOT NULL,
    owner_name VARCHAR(64),
    goal VARCHAR(2000),
    outline VARCHAR(4000),
    class_hours INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    published_key VARCHAR(8),
    review_remark VARCHAR(500),
    submit_by VARCHAR(32),
    submit_at DATETIME,
    approve_by VARCHAR(32),
    approve_at DATETIME,
    create_at DATETIME,
    create_by VARCHAR(32),
    update_at DATETIME,
    update_by VARCHAR(32),
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_research_plan_published (semester, subject, grade, class_type, published_key),
    KEY idx_research_plan_owner (create_by),
    KEY idx_research_plan_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_research_flow_log (
    id VARCHAR(32) NOT NULL,
    biz_type VARCHAR(32) NOT NULL,
    biz_id VARCHAR(32) NOT NULL,
    action VARCHAR(32) NOT NULL,
    from_status VARCHAR(16),
    to_status VARCHAR(16),
    remark VARCHAR(500),
    operator_id VARCHAR(32),
    operator_name VARCHAR(64),
    create_at DATETIME,
    create_by VARCHAR(32),
    update_at DATETIME,
    update_by VARCHAR(32),
    is_deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_research_flow_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO t_research_admin (id, username, password, name, role, status, create_at, create_by, is_deleted)
SELECT '1', 'admin', '$2a$10$KUohXnxBeIYysDpXDJ2OXeyLasTmoz19wo8fE5qhan82dBkFLk2A2', '教研管理员', 'ADMIN', 1, NOW(), 'system', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_research_admin WHERE username = 'admin');
