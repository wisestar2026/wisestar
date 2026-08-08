-- ============================================================
-- 本地 MySQL migration（wisestar 学生答题分析数据基础）
-- 适用：已有旧库的本地环境，用 DBeaver 对 wisestar 库执行
-- 日期：2026-08-01
-- ============================================================

-- 1. t_template 增加学科/章节/知识点/难度字段
ALTER TABLE `t_template`
  ADD COLUMN `subject` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '学科' AFTER `preview_url`,
  ADD COLUMN `chapter` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '章节' AFTER `subject`,
  ADD COLUMN `knowledge_point` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '知识点（多值，逗号分隔）' AFTER `chapter`,
  ADD COLUMN `difficulty` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '难度' AFTER `knowledge_point`;

-- 2. 新建答题明细表 t_answer_detail
CREATE TABLE `t_answer_detail` (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `answer_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '答卷 ID',
  `project_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '问卷 ID',
  `question_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '题目节点 ID',
  `question_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '题型',
  `subject` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '学科快照',
  `chapter` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '章节快照',
  `knowledge_point` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '知识点快照（多值，逗号分隔）',
  `user_answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '学生答案',
  `is_correct` tinyint(1) DEFAULT NULL COMMENT '是否正确：NULL=无标准答案，1=正确，0=错误',
  `score` decimal(10,2) DEFAULT NULL COMMENT '得分',
  `duration_ms` bigint DEFAULT NULL COMMENT '用时（毫秒）',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  `create_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '学生 ID',
  `update_at` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_by` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_answer_detail_answer` (`answer_id`),
  KEY `idx_answer_detail_create_by` (`create_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='答题明细表';

-- 注意：若本地库已用最新 init-mysql.sql 初始化（含新字段与新表），无需执行本文件
