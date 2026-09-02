-- Auto-generated H2 init script from init-mysql.sql
-- Generated for H2 with MODE=MySQL

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_account
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_account (
  id varchar(64) NOT NULL COMMENT 'ID',
  user_type varchar(100) NOT NULL DEFAULT 'SysUser' COMMENT '用户类型',
  user_id varchar(64) NOT NULL COMMENT '用户ID',
  auth_type varchar(20) NOT NULL DEFAULT 'PWD' COMMENT '认证方式',
  auth_account varchar(100) NOT NULL COMMENT '用户名',
  auth_secret varchar(64) DEFAULT NULL COMMENT '密码',
  secret_salt varchar(32) DEFAULT NULL COMMENT '加密盐',
  status int NOT NULL DEFAULT '1' COMMENT '用户状态',
  is_deleted tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  PRIMARY KEY (id)
);

-- ----------------------------
-- Records of t_account
-- ----------------------------
BEGIN;
INSERT INTO t_account (id, user_type, user_id, auth_type, auth_account, auth_secret, secret_salt, status, is_deleted, create_at, create_by, update_at, update_by) VALUES ('1', 'SysUser', '1457995481966747649', 'PWD', 'admin', '$2a$10$vZk9P3XtbD2KrdLbQYPvBuPAkkUda0OlkDg7io1Q6VEtfFPig/tqO', NULL, 1, 0, '2021-11-09 16:56:26', NULL, '2022-02-01 23:57:27', '1457995481966747649');
COMMIT;

-- ----------------------------
-- Table structure for t_answer
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_answer (
  id varchar(64) NOT NULL,
  project_id varchar(64) NOT NULL,
  temp_answer text COMMENT '暂存答案',
  survey text COMMENT '问卷',
  answer text COMMENT '问卷答案',
  attachment varchar(1024) DEFAULT NULL COMMENT '问卷元数据',
  meta_info text COMMENT '问卷元数据',
  temp_save int DEFAULT NULL COMMENT '0暂存 1已完成',
  exam_info text COMMENT '考试信息',
  exam_exercise_type varchar(4) DEFAULT NULL COMMENT '考试练习类型',
  exam_score float DEFAULT NULL COMMENT '考试分数',
  is_deleted tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  repo_id varchar(256) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY key_answer_pid (project_id)
);

-- ----------------------------
-- Records of t_answer
-- ----------------------------

-- ----------------------------
-- Table structure for t_answer_detail
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_answer_detail (
  id varchar(64) NOT NULL,
  answer_id varchar(64) DEFAULT NULL COMMENT '答卷 ID',
  project_id varchar(64) DEFAULT NULL COMMENT '问卷 ID',
  question_id varchar(64) DEFAULT NULL COMMENT '题目节点 ID',
  question_type varchar(64) DEFAULT NULL COMMENT '题型',
  subject varchar(256) DEFAULT NULL COMMENT '学科快照',
  chapter varchar(256) DEFAULT NULL COMMENT '章节快照',
  knowledge_point varchar(1024) DEFAULT NULL COMMENT '知识点快照（多值，逗号分隔）',
  user_answer text COMMENT '学生答案',
  is_correct tinyint(1) DEFAULT NULL COMMENT '是否正确：NULL=无标准答案，1=正确，0=错误',
  score decimal(10,2) DEFAULT NULL COMMENT '得分',
  duration_ms bigint DEFAULT NULL COMMENT '用时（毫秒）',
  is_deleted tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL COMMENT '学生 ID',
  update_at timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  PRIMARY KEY (id)
);

-- ----------------------------
-- Records of t_answer_detail
-- ----------------------------

-- ----------------------------
-- Table structure for t_comm_dict
-- ----------------------------
BEGIN;
INSERT INTO t_answer (id, project_id, temp_answer, survey, answer, attachment, meta_info, temp_save, exam_info, exam_exercise_type, exam_score, is_deleted, create_at, create_by, update_at, update_by, repo_id) VALUES ('2721df7ac46eca9f1af4d63cda921683', 'exercise', NULL, '{"id":"1823691948175663106","title":"考试","attribute":{"submitButton":"结束练习","mode":"exam"},"children":[{"id":"1825718352098648066","attribute":{}},{"id":"1825718352119619585","attribute":{}},{"id":"1825718352119619586","attribute":{}},{"id":"1825718352119619587","attribute":{}},{"id":"1825718352119619588","attribute":{}},{"id":"1825718352119619589","attribute":{}},{"id":"1825718352119619590","attribute":{}},{"id":"1825718352119619591","attribute":{}},{"id":"1825718352119619592","attribute":{}},{"id":"1825718352119619593","attribute":{}},{"id":"1825718352119619594","attribute":{}},{"id":"1825718352119619595","attribute":{}},{"id":"1825718352119619596","attribute":{}},{"id":"1825718352119619597","attribute":{}},{"id":"1825718352119619598","attribute":{}},{"id":"1825718352119619599","attribute":{}},{"id":"1825718352119619600","attribute":{}},{"id":"1825718352119619601","attribute":{}},{"id":"1825718352119619602","attribute":{}},{"id":"1825718352119619603","attribute":{}}]}', NULL, NULL, NULL, 0, NULL, 'O', NULL, 0, '2024-08-20 10:16:23', '1457995481966747649', NULL, NULL, '1823691948175663106');
COMMIT;

-- ----------------------------
-- Table structure for t_comm_dict
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_comm_dict (
  id varchar(64) NOT NULL,
  code varchar(256) DEFAULT NULL COMMENT '字典编码',
  name varchar(256) DEFAULT NULL COMMENT '字典中文名称',
  remark varchar(256) DEFAULT NULL COMMENT '备注信息',
  dict_type int DEFAULT '1' COMMENT '字典类型 1:问卷字典 2:系统字典',
  create_at datetime DEFAULT NULL COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at datetime DEFAULT NULL COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Records of t_comm_dict
-- ----------------------------
BEGIN;
INSERT INTO t_comm_dict (id, code, name, remark, dict_type, create_at, create_by, update_at, update_by) VALUES ('1825717194902814722', 'job', '行业字典', NULL, 1, '2024-08-20 10:11:14', '1457995481966747649', NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for t_comm_dict_item
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_comm_dict_item (
  id varchar(64) NOT NULL,
  dict_code varchar(256) DEFAULT NULL COMMENT '字典编码',
  item_name varchar(256) DEFAULT NULL COMMENT '字典项中文名称',
  item_value varchar(256) NOT NULL COMMENT '字典项值',
  item_order int DEFAULT NULL COMMENT '字典顺序',
  item_level int DEFAULT NULL COMMENT '层级',
  parent_item_value varchar(64) DEFAULT NULL COMMENT '父字典项值',
  create_at datetime DEFAULT NULL COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at datetime DEFAULT NULL COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  PRIMARY KEY (id,item_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Records of t_comm_dict_item
-- ----------------------------
BEGIN;
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900174974977', 'job', '计算机硬件', '2400', 1, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900242083842', 'job', '高级硬件工程师', '2401', 2, 2, '2400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900250472449', 'job', '硬件工程师', '2402', 3, 2, '2400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900263055361', 'job', '其他', '2403', 4, 2, '2400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900263055362', 'job', '计算机软件', '100', 5, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900271443969', 'job', '高级软件工程师', '106', 6, 2, '100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900275638274', 'job', '软件工程师', '107', 7, 2, '100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900279832577', 'job', '软件UI设计师/工程师', '144', 8, 2, '100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900284026882', 'job', '算法工程师', '148', 9, 2, '100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900288221185', 'job', '机器学习工程师', '109', 10, 2, '100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900288221186', 'job', '深度学习工程师', '110', 11, 2, '100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900292415490', 'job', '图像算法工程师', '111', 12, 2, '100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900292415491', 'job', '图像处理工程师', '112', 13, 2, '100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900296609794', 'job', '语音识别工程师', '113', 14, 2, '100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900296609795', 'job', '图像识别工程师', '114', 15, 2, '100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900300804098', 'job', '机器视觉工程师', '115', 16, 2, '100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900304998402', 'job', '自然语言处理（NLP）', '116', 17, 2, '100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900309192705', 'job', '仿真应用工程师', '145', 18, 2, '100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900309192706', 'job', 'ERP实施顾问', '146', 19, 2, '100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900313387009', 'job', 'ERP技术开发', '117', 20, 2, '100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900313387010', 'job', '需求工程师', '147', 21, 2, '100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900317581313', 'job', '系统集成工程师', '137', 22, 2, '100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900317581314', 'job', '系统分析员', '123', 23, 2, '100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900321775617', 'job', '系统工程师', '127', 24, 2, '100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900321775618', 'job', '系统架构设计师', '143', 25, 2, '100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900325969922', 'job', '数据库工程师/管理员', '108', 26, 2, '100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900325969923', 'job', '计算机辅助设计工程师', '141', 27, 2, '100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900330164226', 'job', '其他', '142', 28, 2, '100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900330164227', 'job', '互联网/电子商务/网游', '2500', 29, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900334358530', 'job', '互联网软件开发工程师', '2501', 30, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900338552833', 'job', '语音/视频/图形开发工程师', '2514', 31, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900338552834', 'job', '多媒体/游戏开发工程师', '2502', 32, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900342747138', 'job', '手机应用开发工程师', '2537', 33, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900342747139', 'job', '网站运营总监', '2530', 34, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900346941442', 'job', '网站运营经理/主管', '2503', 35, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900346941443', 'job', '网站运营专员', '2516', 36, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900351135746', 'job', '产品总监', '2531', 37, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900351135747', 'job', '产品经理/主管', '2525', 38, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900351135748', 'job', '产品专员', '2526', 39, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900355330049', 'job', 'SEO/SEM', '2524', 40, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900359524353', 'job', '网络推广总监', '2532', 41, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900359524354', 'job', '网络推广经理/主管', '2533', 42, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900363718658', 'job', '网络推广专员', '2534', 43, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900367912962', 'job', '新媒体运营', '2510', 44, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900367912963', 'job', '电子商务总监', '2535', 45, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900372107266', 'job', '电子商务经理/主管', '2527', 46, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900372107267', 'job', '电子商务专员', '2528', 47, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900376301570', 'job', '网络工程师', '2504', 48, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900380495874', 'job', 'UI设计师/顾问', '2515', 49, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900384690178', 'job', '用户体验（UE/UX）设计师', '2536', 50, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900384690179', 'job', '大数据开发/分析', '2529', 51, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900384690180', 'job', 'Web前端开发', '2539', 52, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900388884481', 'job', '网站架构设计师', '2512', 53, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900388884482', 'job', '网站维护工程师', '2513', 54, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900393078786', 'job', '系统管理员/网络管理员', '2505', 55, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900393078787', 'job', '网站策划', '2506', 56, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900397273089', 'job', '网站编辑', '2507', 57, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900397273090', 'job', '网页设计/制作/美工', '2508', 58, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900401467394', 'job', '脚本开发工程师', '2517', 59, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900405661697', 'job', '游戏策划师', '2518', 60, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900405661698', 'job', '游戏界面设计师', '2519', 61, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900409856002', 'job', 'Flash设计/开发', '2520', 62, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900409856003', 'job', '特效设计师', '2521', 63, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900414050305', 'job', '视觉设计师', '2522', 64, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900414050306', 'job', '音效设计师', '2523', 65, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900418244610', 'job', '网络信息安全工程师', '2509', 66, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900418244611', 'job', '其他', '2511', 67, 2, '2500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900422438914', 'job', 'IT-管理', '2600', 68, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900422438915', 'job', '首席技术执行官CTO/首席信息官CIO', '2601', 69, 2, '2600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900435021826', 'job', '技术总监/经理', '2602', 70, 2, '2600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900443410433', 'job', '信息技术经理/主管', '2603', 71, 2, '2600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900447604737', 'job', '信息技术专员', '2604', 72, 2, '2600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900455993346', 'job', '项目总监', '2605', 73, 2, '2600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900460187650', 'job', '项目经理', '2606', 74, 2, '2600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900464381954', 'job', '项目主管', '2607', 75, 2, '2600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900468576258', 'job', '项目执行/协调人员', '2608', 76, 2, '2600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900472770561', 'job', '其他', '2609', 77, 2, '2600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900472770562', 'job', 'IT-品管、技术支持及其它', '2700', 78, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900472770563', 'job', '技术支持/维护经理', '2701', 79, 2, '2700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900476964865', 'job', '技术支持/维护工程师', '2702', 80, 2, '2700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900476964866', 'job', '网络管理(Helpdesk)', '2712', 81, 2, '2700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900493742081', 'job', '网络维修', '2715', 82, 2, '2700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900497936386', 'job', '计量工程师', '2703', 83, 2, '2700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900506324994', 'job', '标准化工程师', '2704', 84, 2, '2700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900510519298', 'job', '品质经理', '2705', 85, 2, '2700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900510519299', 'job', '系统测试', '2706', 86, 2, '2700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900514713602', 'job', '软件测试', '2707', 87, 2, '2700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900514713603', 'job', '硬件测试', '2708', 88, 2, '2700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900518907905', 'job', '测试员', '2709', 89, 2, '2700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900518907906', 'job', '文档工程师', '2713', 90, 2, '2700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900518907907', 'job', '配置管理工程师', '2714', 91, 2, '2700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900523102209', 'job', '技术文员/助理', '2710', 92, 2, '2700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900523102210', 'job', '手机维修', '2716', 93, 2, '2700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900527296513', 'job', '电脑维修', '2717', 94, 2, '2700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900531490817', 'job', '其他', '2711', 95, 2, '2700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900535685122', 'job', '通信技术开发及应用', '2800', 96, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900535685123', 'job', '通信技术工程师', '2801', 97, 2, '2800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900539879425', 'job', '有线传输工程师', '2802', 98, 2, '2800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900539879426', 'job', '无线通信工程师', '2803', 99, 2, '2800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900539879427', 'job', '电信交换工程师', '2804', 100, 2, '2800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900544073729', 'job', '数据通信工程师', '2805', 101, 2, '2800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900544073730', 'job', '移动通信工程师', '2806', 102, 2, '2800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900544073731', 'job', '电信网络工程师', '2807', 103, 2, '2800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900544073732', 'job', '通信电源工程师', '2808', 104, 2, '2800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900548268034', 'job', '增值产品开发工程师', '2810', 105, 2, '2800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900552462338', 'job', '手机软件开发工程师', '2811', 106, 2, '2800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900552462339', 'job', '其他', '2809', 107, 2, '2800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900556656641', 'job', '电子/电器/半导体/仪器仪表', '2900', 108, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900556656642', 'job', '集成电路IC设计/应用工程师', '2901', 109, 2, '2900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900556656643', 'job', 'IC验证工程师', '2902', 110, 2, '2900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900556656644', 'job', '电子工程师/技术员', '2903', 111, 2, '2900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900565045249', 'job', '电子技术研发工程师', '2917', 112, 2, '2900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900569239554', 'job', '射频工程师', '2924', 113, 2, '2900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900569239555', 'job', '电子/电器维修工程师/技师', '2920', 114, 2, '2900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900569239556', 'job', '变压器与磁电工程师', '2921', 115, 2, '2900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900569239557', 'job', '版图设计工程师', '2922', 116, 2, '2900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900573433857', 'job', '电气工程师/技术员', '2904', 117, 2, '2900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900573433858', 'job', '电路工程师/技术员(模拟/数字)', '2905', 118, 2, '2900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900577628161', 'job', '电声/音响工程师/技术员', '2906', 119, 2, '2900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900577628162', 'job', '激光/光电子技术', '2918', 120, 2, '2900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900577628163', 'job', '半导体技术', '2907', 121, 2, '2900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900581822465', 'job', '自动控制工程师/技术员', '2908', 122, 2, '2900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900586016769', 'job', '电子软件开发(ARM/MCU...)', '2909', 123, 2, '2900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900590211074', 'job', '嵌入式软件开发(Linux/单片机/PLC/DSP…)', '2910', 124, 2, '2900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900590211075', 'job', '嵌入式硬件开发(主板机…)', '2919', 125, 2, '2900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900590211076', 'job', '电池/电源开发', '2911', 126, 2, '2900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900594405378', 'job', 'FAE 现场应用工程师', '2912', 127, 2, '2900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900594405379', 'job', '工艺工程师', '2923', 128, 2, '2900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900598599681', 'job', '家用电器/数码产品研发', '2913', 129, 2, '2900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900598599682', 'job', '仪器/仪表/计量分析师', '2914', 130, 2, '2900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900602793985', 'job', '测试工程师', '2915', 131, 2, '2900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900602793986', 'job', '安防系统工程师', '2925', 132, 2, '2900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900606988290', 'job', '其他', '2916', 133, 2, '2900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900606988291', 'job', '销售管理', '200', 134, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900606988292', 'job', '销售总监', '201', 135, 2, '200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900606988293', 'job', '销售经理', '202', 136, 2, '200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900606988294', 'job', '销售主管', '203', 137, 2, '200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900606988295', 'job', '业务拓展主管/经理', '232', 138, 2, '200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900611182593', 'job', '渠道/分销总监', '233', 139, 2, '200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900611182594', 'job', '渠道/分销经理', '207', 140, 2, '200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900611182595', 'job', '渠道/分销主管', '220', 141, 2, '200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900611182596', 'job', '大客户管理', '235', 142, 2, '200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900611182597', 'job', '客户经理/主管', '208', 143, 2, '200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900611182598', 'job', '区域销售总监', '230', 144, 2, '200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900611182599', 'job', '区域销售经理', '226', 145, 2, '200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900615376897', 'job', '团购经理/主管', '234', 146, 2, '200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900615376898', 'job', '其他', '231', 147, 2, '200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900615376899', 'job', '销售人员', '3000', 148, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900615376900', 'job', '大客户销售', '3009', 149, 2, '3000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900615376901', 'job', '销售代表', '3001', 150, 2, '3000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900615376902', 'job', '渠道/分销专员', '3002', 151, 2, '3000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900615376903', 'job', '客户代表', '3003', 152, 2, '3000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900619571201', 'job', '销售工程师', '3004', 153, 2, '3000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900619571202', 'job', '电话销售', '3005', 154, 2, '3000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900619571203', 'job', '网络/在线销售', '3010', 155, 2, '3000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900623765505', 'job', '团购业务员', '3008', 156, 2, '3000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900623765506', 'job', '经销商', '3006', 157, 2, '3000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900623765507', 'job', '会籍顾问', '3011', 158, 2, '3000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900623765508', 'job', '销售助理', '3012', 159, 2, '3000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900623765509', 'job', '其他', '3007', 160, 2, '3000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900627959809', 'job', '销售行政及商务', '3100', 161, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900627959810', 'job', '销售行政经理/主管', '3101', 162, 2, '3100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900627959811', 'job', '销售行政专员', '3102', 163, 2, '3100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900627959812', 'job', '业务分析经理/主管', '3108', 164, 2, '3100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900627959813', 'job', '业务分析专员/助理', '3109', 165, 2, '3100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900627959814', 'job', '商务经理', '3103', 166, 2, '3100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900627959815', 'job', '商务主管/专员', '3104', 167, 2, '3100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900627959816', 'job', '商务助理', '3105', 168, 2, '3100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900632154114', 'job', '销售行政助理', '3106', 169, 2, '3100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900632154115', 'job', '其他', '3107', 170, 2, '3100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900632154116', 'job', '客服及支持', '3200', 171, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900632154117', 'job', '客服总监', '3201', 172, 2, '3200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900632154118', 'job', '客服经理', '3202', 173, 2, '3200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900632154119', 'job', '客服主管', '3203', 174, 2, '3200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900636348418', 'job', '客服专员/助理', '3204', 175, 2, '3200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900640542722', 'job', '客户关系经理/主管', '3210', 176, 2, '3200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900640542723', 'job', '售前/售后技术支持经理', '3205', 177, 2, '3200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900640542724', 'job', '售前/售后技术支持主管', '3206', 178, 2, '3200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900640542725', 'job', '售前/售后技术支持工程师', '3207', 179, 2, '3200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900640542726', 'job', '咨询热线/呼叫中心服务人员', '3208', 180, 2, '3200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900644737026', 'job', '网络/在线客服', '3213', 181, 2, '3200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900644737027', 'job', '投诉专员', '3211', 182, 2, '3200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900644737028', 'job', 'VIP专员', '3212', 183, 2, '3200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900644737029', 'job', '其他', '3209', 184, 2, '3200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900644737030', 'job', '财务/审计/税务', '400', 185, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900653125633', 'job', '首席财务官 CFO', '444', 186, 2, '400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900653125634', 'job', '财务总监', '401', 187, 2, '400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900657319937', 'job', '财务经理', '402', 188, 2, '400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900657319938', 'job', '财务顾问', '445', 189, 2, '400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900657319939', 'job', '财务主管/总账主管', '403', 190, 2, '400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900657319940', 'job', '会计经理/会计主管', '404', 191, 2, '400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900657319941', 'job', '会计', '405', 192, 2, '400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900665708546', 'job', '出纳员', '414', 193, 2, '400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900665708547', 'job', '财务助理/文员', '422', 194, 2, '400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900665708548', 'job', '固定资产会计', '448', 195, 2, '400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900665708549', 'job', '财务分析经理/主管', '406', 196, 2, '400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900669902850', 'job', '财务分析员', '407', 197, 2, '400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900669902851', 'job', '成本经理/成本主管', '408', 198, 2, '400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900669902852', 'job', '成本管理员', '409', 199, 2, '400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900669902853', 'job', '资金经理/主管', '449', 200, 2, '400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900669902854', 'job', '资金专员', '450', 201, 2, '400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900669902855', 'job', '审计经理/主管', '410', 202, 2, '400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900669902856', 'job', '审计专员/助理', '419', 203, 2, '400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900674097154', 'job', '税务经理/税务主管', '411', 204, 2, '400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900674097155', 'job', '税务专员/助理', '412', 205, 2, '400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900674097156', 'job', '统计员', '446', 206, 2, '400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900674097157', 'job', '其他', '443', 207, 2, '400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900674097158', 'job', '金融/证券/期货/投资', '3300', 208, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900674097159', 'job', '证券/期货/外汇经纪人', '3301', 209, 2, '3300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900674097160', 'job', '证券分析师', '3302', 210, 2, '3300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900678291458', 'job', '股票/期货操盘手', '3303', 211, 2, '3300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900678291459', 'job', '金融/经济研究员', '3304', 212, 2, '3300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900678291460', 'job', '金融产品经理', '3312', 213, 2, '3300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900678291461', 'job', '金融产品销售', '3315', 214, 2, '3300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900678291462', 'job', '投资/基金项目经理', '3305', 215, 2, '3300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900682485762', 'job', '投资/理财顾问', '3306', 216, 2, '3300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900686680066', 'job', '投资银行业务', '3307', 217, 2, '3300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900686680067', 'job', '投资银行财务分析', '3313', 218, 2, '3300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900690874369', 'job', '融资经理/融资主管', '3308', 219, 2, '3300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900690874370', 'job', '融资专员', '3309', 220, 2, '3300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900690874371', 'job', '风险管理/控制', '3314', 221, 2, '3300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900690874372', 'job', '拍卖/担保/典当业务', '3310', 222, 2, '3300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900690874373', 'job', '其他', '3311', 223, 2, '3300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900690874374', 'job', '银行', '2200', 224, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900690874375', 'job', '行长/副行长', '2207', 225, 2, '2200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900695068673', 'job', '银行客户总监', '2231', 226, 2, '2200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900695068674', 'job', '个人业务部门经理/主管', '2223', 227, 2, '2200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900695068675', 'job', '个人业务客户经理', '2224', 228, 2, '2200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900695068676', 'job', '公司业务部门经理/主管', '2225', 229, 2, '2200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900695068677', 'job', '公司业务客户经理', '2226', 230, 2, '2200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900695068678', 'job', '综合业务经理/主管', '2227', 231, 2, '2200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900695068679', 'job', '综合业务专员', '2228', 232, 2, '2200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900695068680', 'job', '资产评估/分析', '2208', 233, 2, '2200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900695068681', 'job', '风险控制', '2209', 234, 2, '2200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900695068682', 'job', '信贷管理', '2215', 235, 2, '2200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900695068683', 'job', '信审核查', '2229', 236, 2, '2200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900707651586', 'job', '进出口/信用证结算', '2210', 237, 2, '2200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900707651587', 'job', '外汇交易', '2212', 238, 2, '2200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900711845889', 'job', '清算人员', '2211', 239, 2, '2200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900711845890', 'job', '高级客户经理/客户经理', '2213', 240, 2, '2200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900711845891', 'job', '客户主管/专员', '2214', 241, 2, '2200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900711845892', 'job', '营业部大堂经理', '2230', 242, 2, '2200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900711845893', 'job', '信用卡销售', '2222', 243, 2, '2200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900711845894', 'job', '呼叫中心客服', '2232', 244, 2, '2200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900716040193', 'job', '银行柜员', '2216', 245, 2, '2200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900716040194', 'job', '其他', '2221', 246, 2, '2200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900716040195', 'job', '保险', '3400', 247, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900716040196', 'job', '保险精算师', '3401', 248, 2, '3400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900716040197', 'job', '保险产品开发/项目策划', '3402', 249, 2, '3400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900720234497', 'job', '保险业务经理/主管', '3403', 250, 2, '3400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900720234498', 'job', '保险经纪人/保险代理', '3404', 251, 2, '3400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900720234499', 'job', '理财顾问/财务规划师', '3405', 252, 2, '3400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900720234500', 'job', '储备经理人', '3406', 253, 2, '3400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900720234501', 'job', '保险电销', '3414', 254, 2, '3400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900720234502', 'job', '保险核保', '3407', 255, 2, '3400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900720234503', 'job', '保险理赔', '3408', 256, 2, '3400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900720234504', 'job', '保险客户服务/续期管理', '3409', 257, 2, '3400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900728623105', 'job', '保险培训师', '3410', 258, 2, '3400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900732817409', 'job', '保险内勤', '3411', 259, 2, '3400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900732817410', 'job', '契约管理', '3413', 260, 2, '3400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900732817411', 'job', '其他', '3412', 261, 2, '3400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900732817412', 'job', '生产/营运', '3500', 262, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900732817413', 'job', '工厂经理/厂长', '3501', 263, 2, '3500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900737011713', 'job', '总工程师/副总工程师', '3502', 264, 2, '3500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900737011714', 'job', '项目总监', '3513', 265, 2, '3500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900737011715', 'job', '项目经理/主管', '3503', 266, 2, '3500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900737011716', 'job', '项目工程师', '3504', 267, 2, '3500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900737011717', 'job', '营运经理', '3505', 268, 2, '3500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900737011718', 'job', '营运主管', '3506', 269, 2, '3500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900737011719', 'job', '生产总监', '3514', 270, 2, '3500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900741206017', 'job', '生产经理/车间主任', '3507', 271, 2, '3500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900741206018', 'job', '生产主管', '3509', 272, 2, '3500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900741206019', 'job', '生产领班/组长', '3515', 273, 2, '3500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900741206020', 'job', '生产计划/物料管理(PMC)', '3508', 274, 2, '3500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900741206021', 'job', '生产文员', '3512', 275, 2, '3500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900741206022', 'job', '设备主管', '3516', 276, 2, '3500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900741206023', 'job', '化验员', '3510', 277, 2, '3500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900741206024', 'job', '其他', '3511', 278, 2, '3500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900745400322', 'job', '质量安全', '3600', 279, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900745400323', 'job', '质量管理/测试经理(QA/QC经理)', '3601', 280, 2, '3600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900745400324', 'job', '质量管理/测试主管(QA/QC主管)', '3602', 281, 2, '3600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900745400325', 'job', '质量管理/测试工程师(QA/QC工程师)', '3603', 282, 2, '3600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900745400326', 'job', '质量检验员/测试员', '3604', 283, 2, '3600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900745400327', 'job', '可靠度工程师', '3605', 284, 2, '3600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900749594626', 'job', '故障分析工程师', '3606', 285, 2, '3600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900749594627', 'job', '认证工程师', '3607', 286, 2, '3600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900749594628', 'job', '体系工程师', '3608', 287, 2, '3600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900749594629', 'job', '审核员', '3615', 288, 2, '3600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900749594630', 'job', '环境/健康/安全经理/主管（EHS）', '3609', 289, 2, '3600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900749594631', 'job', '环境/健康/安全工程师（EHS）', '3610', 290, 2, '3600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900749594632', 'job', '安全员', '3614', 291, 2, '3600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900749594633', 'job', '供应商管理', '3611', 292, 2, '3600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900749594634', 'job', '采购材料、设备质量管理', '3612', 293, 2, '3600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900749594635', 'job', '其他', '3613', 294, 2, '3600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900749594636', 'job', '工程/机械/能源', '500', 295, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900749594637', 'job', '技术研发经理/主管', '510', 296, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900753788930', 'job', '技术研发工程师', '511', 297, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900753788931', 'job', '产品工艺/制程工程师', '547', 298, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900753788932', 'job', '产品规划工程师', '559', 299, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900753788933', 'job', '项目管理', '584', 300, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900753788934', 'job', '实验室负责人/工程师', '512', 301, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900753788935', 'job', '工程/设备经理', '513', 302, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900753788936', 'job', '工程/设备主管', '514', 303, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900753788937', 'job', '工程/设备工程师', '515', 304, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900753788938', 'job', '工程/机械绘图员', '523', 305, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900753788939', 'job', '工业工程师', '560', 306, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900757983234', 'job', '材料工程师', '582', 307, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900757983235', 'job', '机械工程师', '539', 308, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900757983236', 'job', '结构工程师', '561', 309, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900757983237', 'job', '模具工程师', '548', 310, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900757983238', 'job', '机电工程师', '544', 311, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900757983239', 'job', '维修经理/主管', '580', 312, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900757983240', 'job', '维修工程师', '537', 313, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900762177537', 'job', '装配工程师/技师', '581', 314, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900762177538', 'job', '铸造/锻造工程师/技师', '562', 315, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900762177539', 'job', '注塑工程师/技师', '563', 316, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900762177540', 'job', '焊接工程师/技师', '564', 317, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900762177541', 'job', '夹具工程师/技师', '565', 318, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900766371842', 'job', 'CNC工程师', '566', 319, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900766371843', 'job', '冲压工程师/技师', '567', 320, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900766371844', 'job', '锅炉工程师/技师', '568', 321, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900770566146', 'job', '电力工程师/技术员', '569', 322, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900774760450', 'job', '光源与照明工程', '570', 323, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900774760451', 'job', '光伏系统工程师', '583', 324, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900774760452', 'job', '汽车/摩托车工程师', '571', 325, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900774760453', 'job', '船舶工程师', '572', 326, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900774760454', 'job', '轨道交通工程师/技术员', '575', 327, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900774760455', 'job', '飞机维修机械师', '576', 328, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900774760456', 'job', '飞行器设计与制造', '573', 329, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900774760457', 'job', '水利/水电工程师', '577', 330, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900774760458', 'job', '空调/热能工程师', '585', 331, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900774760459', 'job', '石油天然气技术人员', '578', 332, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900778954753', 'job', '矿产勘探/地质勘测工程师', '579', 333, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900778954754', 'job', '其他', '574', 334, 2, '500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900778954755', 'job', '汽车制造', '5400', 335, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900778954756', 'job', '研发总监/部长/专家', '5407', 336, 2, '5400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900778954757', 'job', '汽车机构工程师', '5401', 337, 2, '5400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900778954758', 'job', '汽车设计工程师', '5402', 338, 2, '5400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900778954759', 'job', '汽车电子工程师', '5403', 339, 2, '5400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900783149057', 'job', '发动机/总装工程师', '5414', 340, 2, '5400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900783149058', 'job', '动力总成工程师', '5408', 341, 2, '5400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900783149059', 'job', '底盘工程师', '5409', 342, 2, '5400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900783149060', 'job', '汽车项目管理', '5412', 343, 2, '5400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900783149061', 'job', '汽车质量管理', '5404', 344, 2, '5400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900783149062', 'job', '汽车安全性能工程师', '5405', 345, 2, '5400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900783149063', 'job', '汽车装配工艺工程师', '5406', 346, 2, '5400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900783149064', 'job', '电气/电器工程师', '5410', 347, 2, '5400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900783149065', 'job', '附件系统工程师', '5413', 348, 2, '5400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900783149066', 'job', '内外饰工程师', '5415', 349, 2, '5400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900787343362', 'job', '车身/造型设计', '5416', 350, 2, '5400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900812509185', 'job', '车辆质量工程师', '5417', 351, 2, '5400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900820897794', 'job', '新能源电池工程师', '5418', 352, 2, '5400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900833480706', 'job', '新能源电控工程师', '5419', 353, 2, '5400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900837675009', 'job', '新能源电机工程师', '5420', 354, 2, '5400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900837675010', 'job', '其他', '5411', 355, 2, '5400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900837675011', 'job', '汽车销售与服务', '5900', 356, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900837675012', 'job', '4S店经理/维修站经理', '5901', 357, 2, '5900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900837675013', 'job', '汽车销售/经纪人', '5903', 358, 2, '5900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900837675014', 'job', '汽车修理工', '5907', 359, 2, '5900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900837675015', 'job', '汽车电工', '5912', 360, 2, '5900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900837675016', 'job', '汽车钣金', '5913', 361, 2, '5900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900837675017', 'job', '汽车喷漆', '5914', 362, 2, '5900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900837675018', 'job', '汽车检验/检测', '5905', 363, 2, '5900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900837675019', 'job', '汽车装饰美容', '5906', 364, 2, '5900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900837675020', 'job', '洗车工', '5908', 365, 2, '5900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900837675021', 'job', '售后服务/客户服务', '5902', 366, 2, '5900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900841869313', 'job', '二手车评估师', '5904', 367, 2, '5900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900846063617', 'job', '加油站工作员', '5910', 368, 2, '5900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900846063618', 'job', '其他', '5911', 369, 2, '5900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900846063619', 'job', '技工普工', '3700', 370, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900846063620', 'job', '普工/操作工', '3710', 371, 2, '3700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900846063621', 'job', '技工', '3701', 372, 2, '3700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900850257922', 'job', '叉车/铲车工', '3707', 373, 2, '3700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900850257923', 'job', '组装工', '3715', 374, 2, '3700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900850257924', 'job', '包装工', '3716', 375, 2, '3700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900850257925', 'job', '焊工', '3703', 376, 2, '3700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900850257926', 'job', '氩弧焊工', '3717', 377, 2, '3700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900850257927', 'job', '电工', '3706', 378, 2, '3700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900850257928', 'job', '电力线路工', '3718', 379, 2, '3700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900850257929', 'job', '旋压工', '3719', 380, 2, '3700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900850257930', 'job', '仪表工', '3720', 381, 2, '3700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900850257931', 'job', '电镀工', '3721', 382, 2, '3700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900850257932', 'job', '喷塑工', '3722', 383, 2, '3700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900854452225', 'job', '水工', '3709', 384, 2, '3700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900854452226', 'job', '木工', '3723', 385, 2, '3700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900858646530', 'job', '漆工', '3724', 386, 2, '3700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900862840834', 'job', '空调工', '3708', 387, 2, '3700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900867035138', 'job', '电梯工', '3725', 388, 2, '3700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900867035139', 'job', '锅炉工', '3726', 389, 2, '3700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900867035140', 'job', '学徒工', '3727', 390, 2, '3700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900867035141', 'job', '其他', '3713', 391, 2, '3700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900867035142', 'job', '服装/纺织/皮革', '3800', 392, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900867035143', 'job', '服装/纺织设计总监', '3812', 393, 2, '3800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900867035144', 'job', '服装/纺织设计', '3801', 394, 2, '3800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900867035145', 'job', '服装/纺织/皮革工艺师', '3813', 395, 2, '3800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900867035146', 'job', '面料辅料开发', '3802', 396, 2, '3800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900867035147', 'job', '面料辅料采购', '3803', 397, 2, '3800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900871229441', 'job', '服装/纺织/皮革跟单', '3804', 398, 2, '3800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900871229442', 'job', '服装领班', '3814', 399, 2, '3800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900871229443', 'job', '质量管理/验货员(QA/QC)', '3805', 400, 2, '3800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900871229444', 'job', '板房/楦头/底格出格师', '3806', 401, 2, '3800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900871229445', 'job', '电脑放码员', '3811', 402, 2, '3800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900871229446', 'job', '纸样师/车板工', '3808', 403, 2, '3800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900875423746', 'job', '裁床', '3809', 404, 2, '3800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900875423747', 'job', '打样/制版', '3807', 405, 2, '3800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900879618049', 'job', '裁剪工', '3815', 406, 2, '3800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900879618050', 'job', '缝纫工', '3816', 407, 2, '3800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900879618051', 'job', '手缝工', '3817', 408, 2, '3800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900879618052', 'job', '烫工', '3818', 409, 2, '3800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900879618053', 'job', '样衣工', '3819', 410, 2, '3800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900879618054', 'job', '纺织工', '3820', 411, 2, '3800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900879618055', 'job', '针织工', '3821', 412, 2, '3800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900879618056', 'job', '配色工', '3822', 413, 2, '3800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900879618057', 'job', '印染工', '3823', 414, 2, '3800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900879618058', 'job', '漂染工', '3824', 415, 2, '3800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900879618059', 'job', '挡车工', '3825', 416, 2, '3800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900879618060', 'job', '整经工', '3826', 417, 2, '3800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900879618061', 'job', '细纱工', '3827', 418, 2, '3800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900879618062', 'job', '浆纱工', '3828', 419, 2, '3800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900883812353', 'job', '其他', '3810', 420, 2, '3800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900883812354', 'job', '采购', '3900', 421, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900883812355', 'job', '采购总监', '3901', 422, 2, '3900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900883812356', 'job', '采购经理', '3902', 423, 2, '3900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900883812357', 'job', '采购主管', '3903', 424, 2, '3900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900883812358', 'job', '采购员', '3904', 425, 2, '3900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900883812359', 'job', '采购助理', '3905', 426, 2, '3900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900883812360', 'job', '买手', '3908', 427, 2, '3900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900883812361', 'job', '供应商开发', '3909', 428, 2, '3900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900883812362', 'job', '其他', '3907', 429, 2, '3900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900883812363', 'job', '贸易', '4000', 430, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900883812364', 'job', '贸易/外贸经理/主管', '4001', 431, 2, '4000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900883812365', 'job', '贸易/外贸专员/助理', '4002', 432, 2, '4000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900883812366', 'job', '国内贸易人员', '4003', 433, 2, '4000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900888006657', 'job', '业务跟单经理', '4004', 434, 2, '4000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900888006658', 'job', '高级业务跟单', '4005', 435, 2, '4000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900888006659', 'job', '业务跟单', '4006', 436, 2, '4000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900888006660', 'job', '助理业务跟单', '4007', 437, 2, '4000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900888006661', 'job', '其他', '4008', 438, 2, '4000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900888006662', 'job', '物流/仓储', '800', 439, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900888006663', 'job', '物流总监', '827', 440, 2, '800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900888006664', 'job', '物流经理', '801', 441, 2, '800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900888006665', 'job', '物流主管', '802', 442, 2, '800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900888006666', 'job', '物流专员/助理', '814', 443, 2, '800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900888006667', 'job', '供应链总监', '828', 444, 2, '800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900888006668', 'job', '供应链经理', '825', 445, 2, '800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900888006669', 'job', '供应链主管/专员', '826', 446, 2, '800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900888006670', 'job', '物料经理', '803', 447, 2, '800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900888006671', 'job', '物料主管/专员', '804', 448, 2, '800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900892200962', 'job', '仓库经理/主管', '808', 449, 2, '800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900892200963', 'job', '仓库管理员', '809', 450, 2, '800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900892200964', 'job', '订单处理员', '834', 451, 2, '800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900892200965', 'job', '运输经理/主管', '810', 452, 2, '800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900892200966', 'job', '项目经理/主管', '833', 453, 2, '800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900892200967', 'job', '货运代理', '829', 454, 2, '800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900892200968', 'job', '集装箱业务', '830', 455, 2, '800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900892200969', 'job', '海关事务管理', '832', 456, 2, '800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900892200970', 'job', '报关与报检', '811', 457, 2, '800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900892200971', 'job', '单证员', '812', 458, 2, '800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900896395265', 'job', '船务/空运陆运操作', '815', 459, 2, '800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900900589569', 'job', '快递员', '813', 460, 2, '800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900900589570', 'job', '调度员', '831', 461, 2, '800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900900589571', 'job', '安检员', '835', 462, 2, '800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900900589572', 'job', '理货员', '823', 463, 2, '800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900900589573', 'job', '搬运工', '836', 464, 2, '800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900900589574', 'job', '其他', '824', 465, 2, '800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900900589575', 'job', '生物/制药/医疗器械', '4100', 466, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900900589576', 'job', '生物工程/生物制药', '4101', 467, 2, '4100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900900589577', 'job', '化学分析测试员', '4116', 468, 2, '4100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900904783874', 'job', '医药技术研发管理人员', '4103', 469, 2, '4100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900904783875', 'job', '医药技术研发人员', '4104', 470, 2, '4100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900904783876', 'job', '医药学术推广', '4126', 471, 2, '4100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900904783877', 'job', '临床研究员', '4105', 472, 2, '4100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900904783878', 'job', '临床协调员', '4106', 473, 2, '4100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900904783879', 'job', '临床数据分析员', '4123', 474, 2, '4100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900904783880', 'job', '药品注册', '4107', 475, 2, '4100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900904783881', 'job', '药品生产/质量管理', '4108', 476, 2, '4100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900904783882', 'job', '药品市场推广经理', '4109', 477, 2, '4100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900904783883', 'job', '药品市场推广主管/专员', '4110', 478, 2, '4100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900904783884', 'job', '医药招商', '4120', 479, 2, '4100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900904783885', 'job', '政府事务管理', '4121', 480, 2, '4100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900904783886', 'job', '招投标管理', '4122', 481, 2, '4100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900904783887', 'job', '医药销售经理/主管', '4111', 482, 2, '4100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900904783888', 'job', '医药代表', '4112', 483, 2, '4100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900913172482', 'job', '医药销售人员', '4102', 484, 2, '4100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900913172483', 'job', '医疗器械注册', '4117', 485, 2, '4100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900913172484', 'job', '医疗器械研发', '4124', 486, 2, '4100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900913172485', 'job', '医疗器械生产/质量管理', '4118', 487, 2, '4100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900913172486', 'job', '医疗器械市场推广', '4113', 488, 2, '4100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900913172487', 'job', '医疗器械销售经理/主管', '4125', 489, 2, '4100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900913172488', 'job', '医疗器械销售代表', '4114', 490, 2, '4100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900913172489', 'job', '医疗器械维修人员', '4119', 491, 2, '4100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900913172490', 'job', '其他', '4115', 492, 2, '4100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900913172491', 'job', '化工', '5500', 493, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900913172492', 'job', '化工技术应用/化工工程师', '5501', 494, 2, '5500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900913172493', 'job', '化工实验室研究员/技术员', '5502', 495, 2, '5500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900913172494', 'job', '涂料研发工程师', '5503', 496, 2, '5500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900913172495', 'job', '配色技术员', '5504', 497, 2, '5500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900917366786', 'job', '塑料工程师', '5505', 498, 2, '5500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900917366787', 'job', '化妆品研发', '5506', 499, 2, '5500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900921561090', 'job', '食品/饮料研发', '5507', 500, 2, '5500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900921561091', 'job', '造纸研发', '5509', 501, 2, '5500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900921561092', 'job', '其他', '5508', 502, 2, '5500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900921561093', 'job', '医院/医疗/护理', '1300', 503, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900925755394', 'job', '医院管理人员', '1302', 504, 2, '1300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900925755395', 'job', '综合门诊/全科医生', '1328', 505, 2, '1300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900925755396', 'job', '内科医生', '1301', 506, 2, '1300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900925755397', 'job', '外科医生', '1317', 507, 2, '1300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900925755398', 'job', '专科医生', '1318', 508, 2, '1300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900925755399', 'job', '牙科医生', '1319', 509, 2, '1300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900925755400', 'job', '美容整形师', '1320', 510, 2, '1300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900925755401', 'job', '麻醉医生', '1308', 511, 2, '1300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900925755402', 'job', '超声影像/放射科医师', '1327', 512, 2, '1300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900925755403', 'job', '理疗师', '1321', 513, 2, '1300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900929949697', 'job', '中医科医生', '1322', 514, 2, '1300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900929949698', 'job', '针灸/推拿', '1313', 515, 2, '1300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900942532609', 'job', '儿科医生', '1325', 516, 2, '1300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900950921218', 'job', '心理医生', '1309', 517, 2, '1300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900955115521', 'job', '营养师', '1314', 518, 2, '1300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900955115522', 'job', '药库主任/药剂师', '1304', 519, 2, '1300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900955115523', 'job', '医药学检验', '1310', 520, 2, '1300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900955115524', 'job', '公共卫生/疾病控制', '1323', 521, 2, '1300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900955115525', 'job', '护理主任/护士长', '1324', 522, 2, '1300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900955115526', 'job', '护士/护理人员', '1305', 523, 2, '1300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900955115527', 'job', '兽医', '1315', 524, 2, '1300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900955115528', 'job', '验光师', '1326', 525, 2, '1300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900955115529', 'job', '其他', '1311', 526, 2, '1300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900955115530', 'job', '广告', '4200', 527, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900955115531', 'job', '广告客户总监/副总监', '4201', 528, 2, '4200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900955115532', 'job', '广告客户经理', '4202', 529, 2, '4200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900955115533', 'job', '广告客户主管/专员', '4203', 530, 2, '4200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900955115534', 'job', '广告创意/设计经理', '4204', 531, 2, '4200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900959309825', 'job', '广告创意总监', '4205', 532, 2, '4200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900959309826', 'job', '广告创意/设计主管/专员', '4206', 533, 2, '4200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900959309827', 'job', '广告制作执行', '4212', 534, 2, '4200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900959309828', 'job', '美术指导', '4211', 535, 2, '4200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900959309829', 'job', '文案/策划', '4207', 536, 2, '4200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900959309830', 'job', '企业/业务发展经理', '4208', 537, 2, '4200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900959309831', 'job', '企业策划人员', '4209', 538, 2, '4200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900959309832', 'job', '其他', '4210', 539, 2, '4200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900959309833', 'job', '公关/媒介', '4300', 540, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900959309834', 'job', '公关总监', '4315', 541, 2, '4300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900959309835', 'job', '公关经理', '4301', 542, 2, '4300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900959309836', 'job', '公关主管', '4302', 543, 2, '4300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900959309837', 'job', '公关专员', '4303', 544, 2, '4300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900959309838', 'job', '会务/会展经理', '4304', 545, 2, '4300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900959309839', 'job', '会务/会展主管', '4305', 546, 2, '4300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900959309840', 'job', '会务/会展专员', '4306', 547, 2, '4300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900959309841', 'job', '媒介经理', '4307', 548, 2, '4300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900963504129', 'job', '媒介主管', '4308', 549, 2, '4300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900963504130', 'job', '媒介专员', '4309', 550, 2, '4300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900963504131', 'job', '公关/媒介助理', '4310', 551, 2, '4300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900963504132', 'job', '媒介销售', '4312', 552, 2, '4300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900963504133', 'job', '活动策划', '4313', 553, 2, '4300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900963504134', 'job', '活动执行', '4314', 554, 2, '4300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900963504135', 'job', '其他', '4311', 555, 2, '4300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900963504136', 'job', '市场/营销', '300', 556, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900963504137', 'job', '市场/营销/拓展总监', '301', 557, 2, '300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900963504138', 'job', '市场/营销/拓展经理', '302', 558, 2, '300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900963504139', 'job', '市场/营销/拓展主管', '303', 559, 2, '300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900963504140', 'job', '市场/营销/拓展专员', '304', 560, 2, '300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900963504141', 'job', '市场助理', '305', 561, 2, '300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900963504142', 'job', '市场分析/调研人员', '324', 562, 2, '300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900963504143', 'job', '产品/品牌经理', '306', 563, 2, '300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900967698433', 'job', '产品/品牌主管', '307', 564, 2, '300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900967698434', 'job', '产品/品牌专员', '330', 565, 2, '300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900967698435', 'job', '市场通路经理/主管', '308', 566, 2, '300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900967698436', 'job', '市场通路专员', '335', 567, 2, '300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900967698437', 'job', '市场企划经理/主管', '336', 568, 2, '300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900967698438', 'job', '市场企划专员', '337', 569, 2, '300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900967698439', 'job', '促销经理', '310', 570, 2, '300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900967698440', 'job', '促销主管/督导', '311', 571, 2, '300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900967698441', 'job', '促销员/导购', '312', 572, 2, '300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900967698442', 'job', '选址拓展/新店开发', '338', 573, 2, '300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900967698443', 'job', '其他', '329', 574, 2, '300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900967698444', 'job', '影视/媒体', '4400', 575, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900967698445', 'job', '影视策划/制作人员', '4401', 576, 2, '4400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900967698446', 'job', '导演/编导', '4402', 577, 2, '4400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900967698447', 'job', '艺术/设计总监', '4403', 578, 2, '4400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900967698448', 'job', '艺术指导/舞台美术设计', '4414', 579, 2, '4400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900976087042', 'job', '经纪人/星探', '4404', 580, 2, '4400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900976087043', 'job', '主播/主持人', '4405', 581, 2, '4400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900976087044', 'job', '摄影师/摄像师', '4406', 582, 2, '4400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900976087045', 'job', '后期制作', '4411', 583, 2, '4400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900976087046', 'job', '音效师', '4407', 584, 2, '4400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900976087047', 'job', '配音员', '4408', 585, 2, '4400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900976087048', 'job', '灯光师', '4415', 586, 2, '4400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900976087049', 'job', '放映经理/主管', '4412', 587, 2, '4400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900976087050', 'job', '放映员', '4413', 588, 2, '4400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900976087051', 'job', '其他', '4410', 589, 2, '4400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900976087052', 'job', '编辑出版', '4500', 590, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900976087053', 'job', '总编/副总编', '4501', 591, 2, '4500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900976087054', 'job', '编辑', '4502', 592, 2, '4500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900976087055', 'job', '作家/撰稿人', '4517', 593, 2, '4500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900976087056', 'job', '记者', '4503', 594, 2, '4500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900976087057', 'job', '电话采编', '4516', 595, 2, '4500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900976087058', 'job', '美术编辑', '4504', 596, 2, '4500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900976087059', 'job', '排版设计', '4505', 597, 2, '4500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900976087060', 'job', '出版/发行', '4507', 598, 2, '4500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900976087061', 'job', '其他', '4508', 599, 2, '4500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900976087062', 'job', '艺术/设计', '900', 600, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900976087063', 'job', '平面设计总监', '930', 601, 2, '900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900976087064', 'job', '平面设计经理/主管', '931', 602, 2, '900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900976087065', 'job', '平面设计师', '904', 603, 2, '900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900980281346', 'job', '绘画', '932', 604, 2, '900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900980281347', 'job', '动画/3D设计', '924', 605, 2, '900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900980281348', 'job', '原画师', '933', 606, 2, '900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900980281349', 'job', '展览/展示/店面设计', '925', 607, 2, '900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900980281350', 'job', '多媒体设计', '926', 608, 2, '900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900980281351', 'job', '包装设计', '927', 609, 2, '900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900980281352', 'job', '工业/产品设计', '919', 610, 2, '900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900980281353', 'job', '工艺品/珠宝设计鉴定', '920', 611, 2, '900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900980281354', 'job', '家具/家居用品设计', '928', 612, 2, '900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900980281355', 'job', '玩具设计', '929', 613, 2, '900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900980281356', 'job', '其他', '921', 614, 2, '900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900980281357', 'job', '建筑工程与装潢', '2100', 615, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900980281358', 'job', '高级建筑工程师/总工', '2123', 616, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900980281359', 'job', '建筑工程师', '2101', 617, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900980281360', 'job', '建筑设计师', '2131', 618, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900980281361', 'job', '市政工程师', '2132', 619, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900980281362', 'job', '结构/土木/土建工程师', '2102', 620, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900984475650', 'job', '公路/桥梁/港口/隧道工程', '2118', 621, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900984475651', 'job', '岩土工程', '2119', 622, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900984475652', 'job', '楼宇自动化', '2125', 623, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900988669953', 'job', '建筑机电工程师', '2103', 624, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900988669954', 'job', '智能大厦/综合布线/安防/弱电', '2126', 625, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900992864257', 'job', '给排水/暖通工程', '2104', 626, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900992864258', 'job', '幕墙工程师', '2122', 627, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900992864259', 'job', '规划与设计', '2109', 628, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900992864260', 'job', '室内设计', '2108', 629, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900992864261', 'job', '园艺/园林/景观设计', '2117', 630, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900992864262', 'job', '测绘/测量', '2120', 631, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900992864263', 'job', '建筑制图/模型/渲染', '2110', 632, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900992864264', 'job', '开发报建', '2127', 633, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900992864265', 'job', '工程造价师/预结算经理', '2105', 634, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900992864266', 'job', '预结算员', '2124', 635, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900992864267', 'job', '建筑工程管理/项目经理', '2106', 636, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900992864268', 'job', '建筑项目助理', '2133', 637, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900992864269', 'job', '建筑工程验收', '2121', 638, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900992864270', 'job', '工程监理', '2107', 639, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900992864271', 'job', '合同管理', '2128', 640, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900992864272', 'job', '安全员', '2129', 641, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900997058562', 'job', '资料员', '2130', 642, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900997058563', 'job', '建筑安装施工员', '2111', 643, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900997058564', 'job', '砌筑工', '2134', 644, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900997058565', 'job', '瓦工', '2135', 645, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900997058566', 'job', '混凝土工', '2136', 646, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717900997058567', 'job', '浇注工', '2137', 647, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901001252865', 'job', '钢筋工', '2138', 648, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901001252866', 'job', '木工', '2139', 649, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901001252867', 'job', '油漆工', '2140', 650, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901005447169', 'job', '电梯工', '2141', 651, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901009641474', 'job', '抹灰工', '2142', 652, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901009641475', 'job', '施工开料工', '2143', 653, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901009641476', 'job', '管道/暖通', '2144', 654, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901009641477', 'job', '工长', '2145', 655, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901009641478', 'job', '其他', '2116', 656, 2, '2100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901009641479', 'job', '房地产开发', '4600', 657, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901009641480', 'job', '房地产项目/策划经理', '4601', 658, 2, '4600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901009641481', 'job', '房地产项目/策划主管/专员', '4602', 659, 2, '4600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901009641482', 'job', '房地产投资管理', '4604', 660, 2, '4600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901009641483', 'job', '房产项目配套工程师', '4603', 661, 2, '4600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901009641484', 'job', '房地产项目招投标', '4608', 662, 2, '4600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901009641485', 'job', '房地产投资分析', '4610', 663, 2, '4600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901013835777', 'job', '房地产资产管理', '4611', 664, 2, '4600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901013835778', 'job', '监察人员', '4612', 665, 2, '4600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901013835779', 'job', '其他', '4607', 666, 2, '4600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901013835780', 'job', '房地产销售与中介', '6000', 667, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901013835781', 'job', '房地产销售经理/主管', '6009', 668, 2, '6000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901013835782', 'job', '房地产销售', '6010', 669, 2, '6000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901013835783', 'job', '房地产中介/置业顾问', '6001', 670, 2, '6000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901013835784', 'job', '房地产评估', '6002', 671, 2, '6000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901013835785', 'job', '房地产店长/经理', '6004', 672, 2, '6000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901013835786', 'job', '房地产内勤', '6007', 673, 2, '6000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901013835787', 'job', '房地产客服', '6006', 674, 2, '6000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901013835788', 'job', '其他', '6008', 675, 2, '6000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901013835789', 'job', '物业管理', '4700', 676, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901013835790', 'job', '物业管理经理', '4702', 677, 2, '4700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901013835791', 'job', '物业管理主管', '4714', 678, 2, '4700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901013835792', 'job', '物业管理专员/助理', '4703', 679, 2, '4700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901013835793', 'job', '前介工程师', '4716', 680, 2, '4700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901013835794', 'job', '物业设施管理人员', '4705', 681, 2, '4700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901013835795', 'job', '物业机电维修工', '4715', 682, 2, '4700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901013835796', 'job', '物业维修员', '4706', 683, 2, '4700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901013835797', 'job', '高级物业顾问/物业顾问', '4701', 684, 2, '4700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901018030081', 'job', '物业招商/租赁/租售', '4704', 685, 2, '4700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901018030082', 'job', '停车管理员', '4709', 686, 2, '4700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901018030083', 'job', '保安经理', '4710', 687, 2, '4700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901018030084', 'job', '保安人员', '4711', 688, 2, '4700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901018030085', 'job', '物业机电工程师', '4708', 689, 2, '4700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901018030086', 'job', '保洁', '4712', 690, 2, '4700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901018030087', 'job', '绿化工', '4713', 691, 2, '4700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901018030088', 'job', '其他', '4707', 692, 2, '4700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901018030089', 'job', '人力资源', '600', 693, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901018030090', 'job', '人事总监', '601', 694, 2, '600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901018030091', 'job', 'HRBP', '611', 695, 2, '600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901018030092', 'job', '人事经理', '602', 696, 2, '600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901018030093', 'job', '人事主管', '603', 697, 2, '600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901018030094', 'job', '人事专员', '604', 698, 2, '600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901018030095', 'job', '人事助理', '605', 699, 2, '600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901018030096', 'job', '招聘经理/主管', '606', 700, 2, '600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901018030097', 'job', '招聘专员/助理', '626', 701, 2, '600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901018030098', 'job', '薪资福利经理/主管', '607', 702, 2, '600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901018030099', 'job', '薪资福利专员/助理', '608', 703, 2, '600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901018030100', 'job', '绩效考核经理/主管', '627', 704, 2, '600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901022224385', 'job', '绩效考核专员/助理', '628', 705, 2, '600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901022224386', 'job', '培训经理/主管', '609', 706, 2, '600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901022224387', 'job', '培训专员/助理/培训师', '610', 707, 2, '600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901022224388', 'job', '企业文化/员工关系/工会管理', '629', 708, 2, '600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901022224389', 'job', '人力资源信息系统专员', '630', 709, 2, '600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901022224390', 'job', '其他', '625', 710, 2, '600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901022224391', 'job', '高级管理', '700', 711, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901022224392', 'job', '首席执行官CEO/总裁/总经理', '701', 712, 2, '700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901022224393', 'job', '首席运营官COO', '707', 713, 2, '700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901022224394', 'job', '副总经理/副总裁', '702', 714, 2, '700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901022224395', 'job', '合伙人', '704', 715, 2, '700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901022224396', 'job', '总监/部门经理', '705', 716, 2, '700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901022224397', 'job', '策略发展总监', '710', 717, 2, '700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901022224398', 'job', '企业秘书/董事会秘书', '711', 718, 2, '700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901022224399', 'job', '投资者关系', '712', 719, 2, '700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901022224400', 'job', '办事处首席代表', '708', 720, 2, '700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901022224401', 'job', '办事处/分公司/分支机构经理', '709', 721, 2, '700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901022224402', 'job', '总裁助理/总经理助理', '703', 722, 2, '700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901026418690', 'job', '其他', '706', 723, 2, '700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901026418691', 'job', '行政/后勤', '2300', 724, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901026418692', 'job', '行政总监', '2301', 725, 2, '2300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901026418693', 'job', '行政经理/主管/办公室主任', '2302', 726, 2, '2300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901026418694', 'job', '行政专员/助理', '2303', 727, 2, '2300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901026418695', 'job', '经理助理/秘书', '2304', 728, 2, '2300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901026418696', 'job', '党工团干事', '2310', 729, 2, '2300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901026418697', 'job', '前台接待/总机/接待生', '2305', 730, 2, '2300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901026418698', 'job', '图书管理员/资料管理员', '2307', 731, 2, '2300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901026418699', 'job', '电脑操作员/打字员', '2308', 732, 2, '2300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901026418700', 'job', '后勤', '2306', 733, 2, '2300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901026418701', 'job', '其他', '2309', 734, 2, '2300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901026418702', 'job', '咨询/顾问', '1400', 735, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901026418703', 'job', '专业顾问', '1401', 736, 2, '1400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901026418704', 'job', '咨询总监', '1402', 737, 2, '1400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901026418705', 'job', '咨询经理', '1403', 738, 2, '1400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901026418706', 'job', '专业培训师', '1406', 739, 2, '1400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901026418707', 'job', '咨询员', '1404', 740, 2, '1400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901030612993', 'job', '调研员', '1409', 741, 2, '1400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901030612994', 'job', '猎头/人才中介', '1408', 742, 2, '1400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901030612995', 'job', '情报信息分析人员', '1407', 743, 2, '1400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901030612996', 'job', '其他', '1405', 744, 2, '1400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901030612997', 'job', '律师/法务/合规', '1100', 745, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901030612998', 'job', '律师/法律顾问', '1101', 746, 2, '1100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901030612999', 'job', '律师助理', '1103', 747, 2, '1100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901030613000', 'job', '法务经理', '1106', 748, 2, '1100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901030613001', 'job', '法务主管/专员', '1102', 749, 2, '1100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901030613002', 'job', '法务助理', '1107', 750, 2, '1100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901030613003', 'job', '合规经理', '1109', 751, 2, '1100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901030613004', 'job', '合规主管/专员', '1110', 752, 2, '1100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901030613005', 'job', '知识产权/专利/商标', '1108', 753, 2, '1100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901030613006', 'job', '其他', '1105', 754, 2, '1100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901030613007', 'job', '教师', '1200', 755, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901030613008', 'job', '校长', '1213', 756, 2, '1200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901030613009', 'job', '大学教授', '1208', 757, 2, '1200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901030613010', 'job', '讲师/助教', '1204', 758, 2, '1200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901034807298', 'job', '中学教师', '1201', 759, 2, '1200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901034807299', 'job', '小学教师', '1209', 760, 2, '1200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901034807300', 'job', '幼教', '1207', 761, 2, '1200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901034807301', 'job', '外语培训师', '1215', 762, 2, '1200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901034807302', 'job', '院校教务管理人员', '1202', 763, 2, '1200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901034807303', 'job', '兼职教师', '1210', 764, 2, '1200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901034807304', 'job', '家教', '1205', 765, 2, '1200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901034807305', 'job', '音乐/美术教师', '1214', 766, 2, '1200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901034807306', 'job', '体育教师', '1216', 767, 2, '1200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901034807307', 'job', '职业技术教师', '1211', 768, 2, '1200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901034807308', 'job', '其他', '1206', 769, 2, '1200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901034807309', 'job', '培训', '5700', 770, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901034807310', 'job', '培训督导', '5701', 771, 2, '5700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901034807311', 'job', '培训讲师', '5702', 772, 2, '5700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901034807312', 'job', '培训策划', '5703', 773, 2, '5700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901034807313', 'job', '培训产品开发', '5707', 774, 2, '5700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901034807314', 'job', '培训/课程顾问', '5706', 775, 2, '5700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901039001602', 'job', '培训助理', '5704', 776, 2, '5700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901039001603', 'job', '其他', '5705', 777, 2, '5700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901047390209', 'job', '科研', '1000', 778, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901047390210', 'job', '科研管理人员', '1002', 779, 2, '1000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901047390211', 'job', '科研人员', '1001', 780, 2, '1000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901051584514', 'job', '餐饮服务', '4800', 781, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901051584515', 'job', '店长/经理', '4801', 782, 2, '4800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901051584516', 'job', '大堂经理', '4819', 783, 2, '4800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901051584517', 'job', '餐厅领班', '4802', 784, 2, '4800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901051584518', 'job', '餐饮服务员', '4803', 785, 2, '4800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901051584519', 'job', '行政主厨/厨师长', '4806', 786, 2, '4800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901051584520', 'job', '中餐厨师', '4807', 787, 2, '4800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901051584521', 'job', '西餐厨师', '4820', 788, 2, '4800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901051584522', 'job', '日式厨师', '4821', 789, 2, '4800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901051584523', 'job', '面点师', '4822', 790, 2, '4800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901051584524', 'job', '西点师', '4823', 791, 2, '4800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901051584525', 'job', '厨师助理/学徒', '4812', 792, 2, '4800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901051584526', 'job', '茶艺师', '4809', 793, 2, '4800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901051584527', 'job', '咖啡师', '4816', 794, 2, '4800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901051584528', 'job', '调酒师/侍酒师/吧台员', '4808', 795, 2, '4800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901051584529', 'job', '礼仪/迎宾', '4804', 796, 2, '4800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901051584530', 'job', '预订员', '4824', 797, 2, '4800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901055778817', 'job', '收银员', '4818', 798, 2, '4800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901055778818', 'job', '配菜/打荷', '4813', 799, 2, '4800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901055778819', 'job', '传菜主管', '4811', 800, 2, '4800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901055778820', 'job', '传菜员', '4825', 801, 2, '4800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901055778821', 'job', '洗碗工', '4814', 802, 2, '4800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901055778822', 'job', '送餐员', '4815', 803, 2, '4800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901055778823', 'job', '杂工', '4817', 804, 2, '4800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901055778824', 'job', '其他', '4810', 805, 2, '4800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901055778825', 'job', '酒店旅游', '4900', 806, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901055778826', 'job', '酒店/宾馆经理', '4901', 807, 2, '4900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901055778827', 'job', '酒店/宾馆销售', '4902', 808, 2, '4900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901055778828', 'job', '预定部主管', '4916', 809, 2, '4900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901055778829', 'job', '预定员', '4917', 810, 2, '4900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901055778830', 'job', '大堂经理', '4903', 811, 2, '4900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901055778831', 'job', '酒店前台', '4905', 812, 2, '4900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901055778832', 'job', '宴会管理', '4912', 813, 2, '4900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901055778833', 'job', '宾客服务经理', '4915', 814, 2, '4900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901059973122', 'job', '楼面经理', '4904', 815, 2, '4900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901059973123', 'job', '客房服务员/楼面服务员', '4906', 816, 2, '4900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901059973124', 'job', '健身房服务', '4918', 817, 2, '4900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901059973125', 'job', '行李员', '4907', 818, 2, '4900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901059973126', 'job', '管家部经理/主管', '4914', 819, 2, '4900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901059973127', 'job', '清洁服务人员', '4908', 820, 2, '4900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901059973128', 'job', '旅游产品销售', '4919', 821, 2, '4900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901059973129', 'job', '行程管理/计调', '4920', 822, 2, '4900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901059973130', 'job', '签证专员', '4921', 823, 2, '4900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901059973131', 'job', '导游/旅行顾问', '4909', 824, 2, '4900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901059973132', 'job', '票务', '4910', 825, 2, '4900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901059973133', 'job', '机场代表', '4913', 826, 2, '4900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901059973134', 'job', '其他', '4911', 827, 2, '4900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901059973135', 'job', '美容保健', '5000', 828, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901059973136', 'job', '美容店长', '5018', 829, 2, '5000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901059973137', 'job', '美容培训师/导师', '5016', 830, 2, '5000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901059973138', 'job', '美容顾问', '5001', 831, 2, '5000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901059973139', 'job', '美容师', '5019', 832, 2, '5000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901064167425', 'job', '美容助理', '5002', 833, 2, '5000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901064167426', 'job', '彩妆培训师', '5013', 834, 2, '5000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901064167427', 'job', '专柜彩妆顾问(BA)', '5014', 835, 2, '5000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901064167428', 'job', '化妆师', '5020', 836, 2, '5000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901064167429', 'job', '造型师', '5021', 837, 2, '5000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901064167430', 'job', '美发店长', '5022', 838, 2, '5000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901064167431', 'job', '发型师', '5004', 839, 2, '5000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901064167432', 'job', '发型助理/学徒', '5005', 840, 2, '5000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901064167433', 'job', '美甲师', '5006', 841, 2, '5000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901064167434', 'job', '美体师', '5017', 842, 2, '5000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901064167435', 'job', '瘦身顾问', '5003', 843, 2, '5000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901064167436', 'job', 'SPA 技师', '5023', 844, 2, '5000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901064167437', 'job', '按摩', '5007', 845, 2, '5000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901064167438', 'job', '足疗', '5024', 846, 2, '5000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901064167439', 'job', '宠物护理/美容', '5010', 847, 2, '5000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901064167440', 'job', '其他', '5011', 848, 2, '5000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901068361729', 'job', '百货零售', '5100', 849, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901068361730', 'job', '卖场经理/店长', '5101', 850, 2, '5100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901068361731', 'job', '品类经理', '5112', 851, 2, '5100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901068361732', 'job', '品牌/连锁招商管理', '5114', 852, 2, '5100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901068361733', 'job', '奢侈品业务', '5115', 853, 2, '5100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901068361734', 'job', '店员/营业员', '5102', 854, 2, '5100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901068361735', 'job', '珠宝销售顾问', '5116', 855, 2, '5100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901068361736', 'job', '督导/巡店', '5117', 856, 2, '5100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901068361737', 'job', '导购员', '5105', 857, 2, '5100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901068361738', 'job', '促销员', '5118', 858, 2, '5100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901068361739', 'job', '收银主管', '5103', 859, 2, '5100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901068361740', 'job', '收银员', '5119', 860, 2, '5100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901068361741', 'job', '陈列员', '5104', 861, 2, '5100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901068361742', 'job', '收货员', '5120', 862, 2, '5100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901068361743', 'job', '理货员', '5121', 863, 2, '5100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901068361744', 'job', '安防主管', '5113', 864, 2, '5100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901068361745', 'job', '防损员/内保', '5108', 865, 2, '5100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901068361746', 'job', '西点师/面包糕点加工', '5109', 866, 2, '5100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901068361747', 'job', '生鲜食品加工/处理', '5110', 867, 2, '5100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901068361748', 'job', '熟食加工', '5111', 868, 2, '5100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901068361749', 'job', '兼职店员', '5106', 869, 2, '5100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901068361750', 'job', '其他', '5107', 870, 2, '5100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901068361751', 'job', '交通运输服务', '1800', 871, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901072556034', 'job', '飞机机长/副机长', '1822', 872, 2, '1800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901072556035', 'job', '空乘人员', '1823', 873, 2, '1800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901072556036', 'job', '列车/地铁车长', '1825', 874, 2, '1800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901072556037', 'job', '船长/副船长', '1827', 875, 2, '1800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901072556038', 'job', '商务司机', '1810', 876, 2, '1800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901072556039', 'job', '客运司机', '1830', 877, 2, '1800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901072556040', 'job', '货运司机', '1831', 878, 2, '1800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901072556041', 'job', '出租车司机', '1832', 879, 2, '1800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901072556042', 'job', '班车司机', '1833', 880, 2, '1800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901072556043', 'job', '列车/地铁司机', '1826', 881, 2, '1800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901072556044', 'job', '特种车司机', '1835', 882, 2, '1800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901072556045', 'job', '叉车司机', '1836', 883, 2, '1800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901072556046', 'job', '铲车司机', '1837', 884, 2, '1800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901072556047', 'job', '吊车司机', '1838', 885, 2, '1800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901072556048', 'job', '驾校教练', '1839', 886, 2, '1800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901072556049', 'job', '代驾', '1840', 887, 2, '1800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901072556050', 'job', '地勤人员', '1824', 888, 2, '1800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901072556051', 'job', '乘务员', '1801', 889, 2, '1800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901072556052', 'job', '船员', '1828', 890, 2, '1800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901076750337', 'job', '其他', '1829', 891, 2, '1800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901076750338', 'job', '家政保洁', '5200', 892, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901076750339', 'job', '家政服务/保姆', '5206', 893, 2, '5200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901076750340', 'job', '月嫂', '5209', 894, 2, '5200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901076750341', 'job', '育婴师/保育员', '5210', 895, 2, '5200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901076750342', 'job', '护工', '5211', 896, 2, '5200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901076750343', 'job', '清洁工', '5205', 897, 2, '5200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901076750344', 'job', '钟点工', '5212', 898, 2, '5200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901076750345', 'job', '洗衣工', '5213', 899, 2, '5200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901076750346', 'job', '送水工', '5214', 900, 2, '5200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901076750347', 'job', '保镖', '5202', 901, 2, '5200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901076750348', 'job', '空调维修', '5215', 902, 2, '5200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901076750349', 'job', '家电维修', '5216', 903, 2, '5200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901076750350', 'job', '寻呼员/话务员', '5203', 904, 2, '5200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901076750351', 'job', '其他', '5207', 905, 2, '5200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901076750352', 'job', '公务员', '1500', 906, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901076750353', 'job', '公务员', '1501', 907, 2, '1500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901076750354', 'job', '翻译', '2000', 908, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901076750355', 'job', '英语翻译', '2001', 909, 2, '2000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901080944641', 'job', '日语翻译', '2002', 910, 2, '2000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901080944642', 'job', '德语翻译', '2003', 911, 2, '2000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901080944643', 'job', '法语翻译', '2004', 912, 2, '2000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901080944644', 'job', '俄语翻译', '2005', 913, 2, '2000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901080944645', 'job', '意大利语翻译', '2010', 914, 2, '2000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901080944646', 'job', '西班牙语翻译', '2006', 915, 2, '2000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901080944647', 'job', '葡萄牙语翻译', '2011', 916, 2, '2000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901080944648', 'job', '阿拉伯语翻译', '2009', 917, 2, '2000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901080944649', 'job', '韩语/朝鲜语翻译', '2007', 918, 2, '2000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901080944650', 'job', '泰语翻译', '2012', 919, 2, '2000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901080944651', 'job', '中国方言翻译', '2013', 920, 2, '2000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901080944652', 'job', '其他语种翻译', '2008', 921, 2, '2000', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901080944653', 'job', '在校学生', '1600', 922, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901080944654', 'job', '研究生', '1605', 923, 2, '1600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901080944655', 'job', '大学/大专应届毕业生', '1602', 924, 2, '1600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901080944656', 'job', '中专/职校生', '1601', 925, 2, '1600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901080944657', 'job', '其他', '1604', 926, 2, '1600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901080944658', 'job', '储备干部/培训生/实习生', '1700', 927, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901080944659', 'job', '储备干部', '1702', 928, 2, '1700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901080944660', 'job', '培训生', '1701', 929, 2, '1700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901080944661', 'job', '实习生', '1703', 930, 2, '1700', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901080944662', 'job', '兼职', '5300', 931, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901085138945', 'job', '兼职', '5301', 932, 2, '5300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901085138946', 'job', '环保', '5600', 933, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901085138947', 'job', '环保工程师', '5601', 934, 2, '5600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901085138948', 'job', '环境影响评价工程师', '5604', 935, 2, '5600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901085138949', 'job', '生态治理/规划', '5609', 936, 2, '5600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901085138950', 'job', '环保检测', '5605', 937, 2, '5600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901085138951', 'job', '水质检测员', '5606', 938, 2, '5600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901085138952', 'job', '水处理工程师', '5602', 939, 2, '5600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901085138953', 'job', '固废工程师', '5607', 940, 2, '5600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901085138954', 'job', '废气处理工程师', '5608', 941, 2, '5600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901085138955', 'job', '其它', '5603', 942, 2, '5600', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901085138956', 'job', '农/林/牧/渔', '5800', 943, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901085138957', 'job', '养殖部主管', '5801', 944, 2, '5800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901085138958', 'job', '场长(农/林/牧/渔业)', '5802', 945, 2, '5800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901085138959', 'job', '农艺师', '5803', 946, 2, '5800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901085138960', 'job', '畜牧师', '5804', 947, 2, '5800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901085138961', 'job', '饲养员', '5805', 948, 2, '5800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901085138962', 'job', '农业技术员', '5808', 949, 2, '5800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901089333249', 'job', '动物营养/饲料研发', '5806', 950, 2, '5800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901089333250', 'job', '其他', '5807', 951, 2, '5800', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901089333251', 'job', '网店淘宝', '6100', 952, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901089333252', 'job', '网店/淘宝店长', '6101', 953, 2, '6100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901089333253', 'job', '网店/淘宝运营', '6102', 954, 2, '6100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901089333254', 'job', '网店店铺管理员', '6103', 955, 2, '6100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901089333255', 'job', '网店/淘宝客服', '6104', 956, 2, '6100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901089333256', 'job', '店铺推广', '6105', 957, 2, '6100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901089333257', 'job', '网店美工', '6106', 958, 2, '6100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901089333258', 'job', '网店模特', '6107', 959, 2, '6100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901089333259', 'job', '其他', '6108', 960, 2, '6100', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901089333260', 'job', '机械机床', '6200', 961, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901089333261', 'job', '数控操机', '6201', 962, 2, '6200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901089333262', 'job', '数控编程', '6202', 963, 2, '6200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901089333263', 'job', '机修工', '6203', 964, 2, '6200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901089333264', 'job', '折弯工', '6204', 965, 2, '6200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901089333265', 'job', '车工', '6205', 966, 2, '6200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901089333266', 'job', '磨工', '6206', 967, 2, '6200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901089333267', 'job', '铣工', '6207', 968, 2, '6200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901089333268', 'job', '冲压工', '6208', 969, 2, '6200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901089333269', 'job', '刨工', '6209', 970, 2, '6200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901093527554', 'job', '钳工', '6210', 971, 2, '6200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901093527555', 'job', '钻工', '6211', 972, 2, '6200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901093527556', 'job', '镗工', '6212', 973, 2, '6200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901093527557', 'job', '铆工', '6213', 974, 2, '6200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901093527558', 'job', '钣金工', '6214', 975, 2, '6200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901093527559', 'job', '抛光工', '6215', 976, 2, '6200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901093527560', 'job', '切割技工', '6216', 977, 2, '6200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901093527561', 'job', '模具工', '6217', 978, 2, '6200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901093527562', 'job', '炼胶工', '6218', 979, 2, '6200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901093527563', 'job', '硫化工', '6219', 980, 2, '6200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901093527564', 'job', '吹膜工', '6220', 981, 2, '6200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901093527565', 'job', '注塑工', '6221', 982, 2, '6200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901093527566', 'job', '其他', '6222', 983, 2, '6200', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901093527567', 'job', '印刷包装', '6300', 984, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901093527568', 'job', '印刷工', '6301', 985, 2, '6300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901093527569', 'job', '校对/录入', '6302', 986, 2, '6300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901093527570', 'job', '调色员', '6304', 987, 2, '6300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901093527571', 'job', '烫金工', '6305', 988, 2, '6300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901093527572', 'job', '晒版员', '6306', 989, 2, '6300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901093527573', 'job', '印刷排版/制版', '6307', 990, 2, '6300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901093527574', 'job', '装订工', '6308', 991, 2, '6300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901097721858', 'job', '印刷机械机长', '6309', 992, 2, '6300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901097721859', 'job', '数码直印/菲林输出', '6310', 993, 2, '6300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901097721860', 'job', '调墨技师', '6311', 994, 2, '6300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901097721861', 'job', '电分操作员', '6312', 995, 2, '6300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901097721862', 'job', '打稿机操作员', '6313', 996, 2, '6300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901097721863', 'job', '切纸机操作工', '6314', 997, 2, '6300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901097721864', 'job', '裱胶工', '6315', 998, 2, '6300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901097721865', 'job', '压痕工', '6316', 999, 2, '6300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901097721866', 'job', '复卷工', '6317', 1000, 2, '6300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901827530753', 'job', '其他', '6318', 1001, 2, '6300', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901861085185', 'job', '运动健身', '6400', 1002, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901861085186', 'job', '健身顾问/教练', '6401', 1003, 2, '6400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901861085187', 'job', '瑜伽老师', '6402', 1004, 2, '6400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901861085188', 'job', '舞蹈老师', '6403', 1005, 2, '6400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901861085189', 'job', '游泳教练', '6404', 1006, 2, '6400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901861085190', 'job', '救生员', '6405', 1007, 2, '6400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901861085191', 'job', '高尔夫教练', '6406', 1008, 2, '6400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901861085192', 'job', '体育运动教练', '6407', 1009, 2, '6400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901861085193', 'job', '其他', '6408', 1010, 2, '6400', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901861085194', 'job', '休闲娱乐', '6500', 1011, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901865279489', 'job', '司仪', '6501', 1012, 2, '6500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901865279490', 'job', '婚礼/庆典策划服务', '6502', 1013, 2, '6500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901865279491', 'job', 'DJ', '6503', 1014, 2, '6500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901865279492', 'job', '驻唱/歌手', '6504', 1015, 2, '6500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901865279493', 'job', '舞蹈演员', '6505', 1016, 2, '6500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901865279494', 'job', '模特', '6506', 1017, 2, '6500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901865279495', 'job', '演员/群众演员', '6507', 1018, 2, '6500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901865279496', 'job', '娱乐领班', '6509', 1019, 2, '6500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901865279497', 'job', '娱乐服务员', '6510', 1020, 2, '6500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901865279498', 'job', '前台迎宾', '6511', 1021, 2, '6500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901865279499', 'job', '其他', '6508', 1022, 2, '6500', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901865279500', 'job', '其他', '1900', 1023, 1, '', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901865279501', 'job', '驯兽师/助理驯兽师', '1902', 1024, 2, '1900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901865279502', 'job', '志愿者/社会工作者', '1903', 1025, 2, '1900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
INSERT INTO t_comm_dict_item (id, dict_code, item_name, item_value, item_order, item_level, parent_item_value, create_at, create_by, update_at, update_by) VALUES ('1825717901865279503', 'job', '其他', '1901', 1026, 2, '1900', '2024-08-20 10:14:02', '1457995481966747649', NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for t_dashboard
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_dashboard (
  id varchar(64) NOT NULL COMMENT 'ID',
  key varchar(256) NOT NULL COMMENT '仪表盘组件key',
  type int DEFAULT NULL COMMENT '仪表盘分类',
  project_id varchar(64) DEFAULT NULL COMMENT '项目ID',
  setting varchar(1024) DEFAULT NULL COMMENT '仪表盘设置',
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  PRIMARY KEY (id)
);

-- ----------------------------
-- Records of t_dashboard
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for t_dept
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_dept (
  id varchar(64) NOT NULL COMMENT 'ID',
  parent_id varchar(64) NOT NULL,
  name varchar(64) DEFAULT NULL COMMENT '名称',
  short_name varchar(64) NOT NULL COMMENT '简称',
  code varchar(64) DEFAULT NULL COMMENT '数据权限类型',
  manager_id varchar(64) DEFAULT NULL COMMENT '扩展字段',
  sort_code int DEFAULT NULL,
  property_json varchar(256) DEFAULT NULL COMMENT '扩展字段',
  status varchar(20) DEFAULT NULL COMMENT '扩展字段',
  remark varchar(256) DEFAULT NULL COMMENT '扩展字段',
  is_deleted tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  PRIMARY KEY (id)
);

-- ----------------------------
-- Records of t_dept
-- ----------------------------
BEGIN;
INSERT INTO t_dept (id, parent_id, name, short_name, code, manager_id, sort_code, property_json, status, remark, is_deleted, create_at, create_by, update_at, update_by) VALUES ('1', '0', '卷王问卷', 'surveyking', 'surveyking', '1457995481966747649', NULL, NULL, NULL, NULL, 0, '2021-11-21 14:12:08', NULL, '2021-11-21 14:22:58', '1457995481966747649');
COMMIT;

-- ----------------------------
-- Table structure for t_file
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_file (
  id varchar(64) NOT NULL,
  original_name varchar(256) DEFAULT NULL,
  file_name varchar(256) DEFAULT NULL,
  file_path varchar(512) DEFAULT NULL,
  thumb_file_path varchar(512) DEFAULT NULL,
  storage_type int DEFAULT NULL,
  shared int DEFAULT '0',
  is_deleted tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  PRIMARY KEY (id)
);

-- ----------------------------
-- Records of t_file
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for t_position
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_position (
  id varchar(64) NOT NULL COMMENT 'ID',
  name varchar(50) NOT NULL,
  code varchar(20) DEFAULT NULL,
  is_virtual tinyint(1) NOT NULL COMMENT '是否虚拟岗',
  data_permission_type varchar(256) DEFAULT NULL COMMENT '数据权限类型',
  property_json varchar(20) DEFAULT NULL COMMENT '扩展字段',
  is_deleted tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  PRIMARY KEY (id)
);

-- ----------------------------
-- Records of t_position
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for t_project
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_project (
  id varchar(64) NOT NULL,
  parent_id varchar(64) DEFAULT '0' COMMENT '父ID',
  name text COMMENT '项目名称',
  survey text COMMENT '问卷',
  setting text COMMENT '问卷设置',
  status int DEFAULT '0' COMMENT '0未发布 1已发布',
  mode varchar(32) DEFAULT NULL COMMENT '问卷模式',
  priority int DEFAULT '1000' COMMENT '优先级',
  is_deleted tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  PRIMARY KEY (id)
);

-- ----------------------------

-- ----------------------------
-- Table structure for t_project_partner
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_project_partner (
  id varchar(64) NOT NULL,
  uid varchar(64) DEFAULT NULL COMMENT '项目内唯一ID',
  project_id varchar(64) DEFAULT NULL COMMENT '项目id',
  type int DEFAULT NULL COMMENT '参与者类型',
  status int DEFAULT '0' COMMENT '0未访问 1已访问 2已答题',
  user_id varchar(64) DEFAULT NULL COMMENT '参与者id',
  user_name varchar(256) DEFAULT NULL COMMENT '参与者姓名',
  group_id varchar(64) DEFAULT NULL COMMENT '参与组id',
  data_permission text COMMENT '数据权限',
  initial_value text COMMENT '初始值',
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  PRIMARY KEY (id)
);
-- Records of t_project_partner (演示数据已移除)

-- ----------------------------

-- ----------------------------
-- Table structure for t_repo
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_repo (
  id varchar(64) NOT NULL,
  name varchar(64) DEFAULT NULL COMMENT '标题',
  description varchar(512) DEFAULT NULL COMMENT '备注',
  category varchar(64) DEFAULT NULL COMMENT '题库分类',
  mode varchar(32) DEFAULT NULL COMMENT 'survey问卷 exam考试',
  shared tinyint(1) DEFAULT '0' COMMENT '1共享 0私有',
  tag varchar(512) DEFAULT NULL COMMENT '标签',
  priority int DEFAULT NULL COMMENT '排序优先级',
  setting text COMMENT '设置',
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  is_practice tinyint DEFAULT NULL COMMENT '添加到练习题库 1是 0否',
  subject varchar(64) DEFAULT NULL COMMENT '学科标签',
  grade varchar(32) DEFAULT NULL COMMENT '年级标签',
  difficulty varchar(32) DEFAULT NULL COMMENT '难度标签(easy简单/medium中等/hard困难)',
  PRIMARY KEY (id)
);

-- 兼容旧库升级：补齐题库学科/年级/难度标签列（新库建表已包含，已存在则跳过）
ALTER TABLE t_repo ADD COLUMN IF NOT EXISTS subject varchar(64) DEFAULT NULL COMMENT '学科标签';
ALTER TABLE t_repo ADD COLUMN IF NOT EXISTS grade varchar(32) DEFAULT NULL COMMENT '年级标签';
ALTER TABLE t_repo ADD COLUMN IF NOT EXISTS difficulty varchar(32) DEFAULT NULL COMMENT '难度标签(easy简单/medium中等/hard困难)';

-- ----------------------------
-- Records of t_repo
-- ----------------------------
BEGIN;
INSERT INTO t_repo (id, name, description, category, mode, shared, tag, priority, setting, create_at, create_by, update_at, update_by, is_practice) VALUES ('1823691948175663106', '考试', NULL, '驾照考试', 'exam', 0, NULL, NULL, NULL, '2024-08-14 20:03:38', '1457995481966747649', NULL, NULL, 1);
INSERT INTO t_repo (id, name, description, category, mode, shared, tag, priority, setting, create_at, create_by, update_at, update_by, is_practice) VALUES ('1823692048075595777', '人口属性', NULL, NULL, 'survey', 1, NULL, NULL, NULL, '2024-08-14 20:04:01', '1457995481966747649', NULL, NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for t_user_repo（学员-题库分配）
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_user_repo (
  id varchar(64) NOT NULL,
  user_id varchar(64) DEFAULT NULL COMMENT '学员用户ID',
  repo_id varchar(64) DEFAULT NULL COMMENT '题库ID',
  assign_type varchar(16) DEFAULT 'manual' COMMENT '分配方式 manual手动 auto标签自动',
  is_deleted tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='学员题库分配';

-- ----------------------------
-- Records of t_user_repo
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for t_practice_record（练习会话记录）
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_practice_record (
  id varchar(64) NOT NULL,
  user_id varchar(64) DEFAULT NULL COMMENT '练习学员ID',
  mode varchar(16) DEFAULT NULL COMMENT '练习模式 special专项 exam套卷 random随机',
  repo_id varchar(64) DEFAULT NULL COMMENT '来源题库ID',
  total_questions int DEFAULT '0' COMMENT '题目总数',
  correct_count int DEFAULT '0' COMMENT '答对题数',
  score double DEFAULT '0' COMMENT '得分',
  total_score double DEFAULT '0' COMMENT '总分',
  duration_ms bigint DEFAULT NULL COMMENT '练习用时(毫秒)',
  is_deleted tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='练习会话记录';

-- ----------------------------
-- Table structure for t_practice_detail（练习逐题明细/错题标记）
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_practice_detail (
  id varchar(64) NOT NULL,
  practice_id varchar(64) DEFAULT NULL COMMENT '练习会话ID',
  question_id varchar(64) DEFAULT NULL COMMENT '题目ID',
  question_type varchar(16) DEFAULT NULL COMMENT '题型',
  user_answer varchar(1024) DEFAULT NULL COMMENT '学生答案(选项标题/文本)',
  is_correct tinyint(1) DEFAULT NULL COMMENT '判分结果 1正确 0错误 null无标准答案',
  score double DEFAULT '0' COMMENT '本题得分',
  is_deleted tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_practice (practice_id),
  KEY idx_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='练习逐题明细(错题标记)';

-- ----------------------------
-- Table structure for t_repo_template
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_repo_template (
  id varchar(64) NOT NULL,
  template_id varchar(64) DEFAULT NULL COMMENT '模板id',
  repo_id varchar(64) DEFAULT NULL COMMENT '模板库id',
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  PRIMARY KEY (id)
);

-- ----------------------------
-- Records of t_repo_template
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for t_role
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_role (
  id varchar(64) NOT NULL COMMENT 'ID',
  name varchar(50) NOT NULL COMMENT '名称',
  code varchar(50) NOT NULL COMMENT '编码',
  remark varchar(100) DEFAULT NULL COMMENT '备注',
  authority varchar(3000) DEFAULT NULL COMMENT '权限列表',
  status tinyint(1) DEFAULT '1' COMMENT '1激活 0失活',
  is_deleted tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  builtin tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否内置角色 1内置不可删 0普通',
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  PRIMARY KEY (id)
);
-- 兼容已存在的 H2 文件库：为旧表补充 builtin 列
ALTER TABLE t_role ADD COLUMN IF NOT EXISTS builtin tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否内置角色 1内置不可删 0普通';

-- ----------------------------
-- Records of t_role
-- ----------------------------
BEGIN;
-- 管理员：全量权限（含新增知识/学员/订单模块权限点），幂等插入
INSERT INTO t_role (id, name, code, remark, authority, status, is_deleted, builtin, create_at, create_by, update_at, update_by)
SELECT '1457995481928998914', '管理员', 'admin', '系统初始化角色（超管）', 'home,exercise:list,project:list,project:detail,project:create,project:update,project:delete,project:report,answer:list,answer:detail,answer:create,answer:update,answer:delete,answer:export,answer:upload,repo:list,repo:detail,repo:create,repo:update,repo:delete,repo:export,repo:book,template:list,template:create,template:update,template:delete,knowledge:list,knowledge:create,knowledge:update,knowledge:delete,student:list,student:create,student:update,student:delete,order:list,order:create,order:update,order:delete,mall:list,mall:create,mall:update,mall:delete,task:list,task:create,task:update,task:delete,system:user:list,system:user:create,system:user:update,system:user:updatePosition,system:user:delete,system:role:list,system:role:create,system:role:update,system:role:delete,system:dept:list,system:dept:create,system:dept:update,system:dept:delete,system:position:list,system:position:create,system:position:update,system:position:delete,system:dict:list,system:dict:create,system:dict:update,system:dict:delete,system:dictItem:list,system:dictItem:create,system:dictItem:update,system:dictItem:delete,system:dictItem:import,user:update', 1, 0, 1, '2021-11-09 16:56:26', NULL, '2026-08-13 10:00:00', '1457995481966747649'
WHERE NOT EXISTS (SELECT 1 FROM t_role WHERE code = 'admin');
-- 内置角色：校长（决策层）
INSERT INTO t_role (id, name, code, remark, authority, status, is_deleted, builtin, create_at, create_by, update_at, update_by)
SELECT '2608130000000000001', '校长', 'principal', '内置角色（不可删除）', 'home,exercise:list,project:list,project:detail,answer:list,answer:detail,repo:list,repo:detail,template:list,knowledge:list,student:list,student:create,student:update,student:delete,order:list,order:create,order:update,order:delete,mall:list,mall:create,mall:update,mall:delete,task:list,task:create,task:update,task:delete,system:user:list,system:role:list,system:dept:list,system:position:list,system:dict:list,system:dictItem:list', 1, 0, 1, '2026-08-13 10:00:00', '1457995481966747649', NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM t_role WHERE code = 'principal');
UPDATE t_role SET authority = 'home,exercise:list,project:list,project:detail,answer:list,answer:detail,repo:list,repo:detail,template:list,knowledge:list,student:list,student:create,student:update,student:delete,order:list,order:create,order:update,order:delete,mall:list,mall:create,mall:update,mall:delete,task:list,task:create,task:update,task:delete,system:user:list,system:role:list,system:dept:list,system:position:list,system:dict:list,system:dictItem:list' WHERE code = 'principal';
UPDATE t_role SET authority = 'home,exercise:list,project:list,project:detail,answer:list,answer:detail,repo:list,repo:detail,template:list,knowledge:list,student:list,student:create,student:update,student:delete,order:list,order:create,order:update,order:delete,mall:list,mall:create,mall:update,mall:delete,task:list,task:create,task:update,task:delete,system:user:list,system:role:list,system:dept:list,system:position:list,system:dict:list,system:dictItem:list' WHERE code = 'principal';
-- 内置角色：教师（教学执行）
INSERT INTO t_role (id, name, code, remark, authority, status, is_deleted, builtin, create_at, create_by, update_at, update_by)
SELECT '2608130000000000002', '教师', 'teacher', '内置角色（不可删除）', 'home,exercise:list,repo:list,repo:detail,repo:create,repo:update,repo:delete,template:list,template:create,template:update,template:delete,knowledge:list,knowledge:create,knowledge:update,knowledge:delete,student:list,order:list,mall:list,mall:create,mall:update,mall:delete,task:list,task:create,task:update,task:delete,project:list,project:detail,answer:list,answer:detail', 1, 0, 1, '2026-08-13 10:00:00', '1457995481966747649', NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM t_role WHERE code = 'teacher');
UPDATE t_role SET authority = 'home,exercise:list,repo:list,repo:detail,repo:create,repo:update,repo:delete,template:list,template:create,template:update,template:delete,knowledge:list,knowledge:create,knowledge:update,knowledge:delete,student:list,order:list,mall:list,mall:create,mall:update,mall:delete,task:list,task:create,task:update,task:delete,project:list,project:detail,answer:list,answer:detail' WHERE code = 'teacher';
UPDATE t_role SET authority = 'home,exercise:list,repo:list,repo:detail,repo:create,repo:update,repo:delete,template:list,template:create,template:update,template:delete,knowledge:list,knowledge:create,knowledge:update,knowledge:delete,student:list,order:list,mall:list,mall:create,mall:update,mall:delete,task:list,task:create,task:update,task:delete,project:list,project:detail,answer:list,answer:detail' WHERE code = 'teacher';
-- 内置角色：学管师（学员运营）
INSERT INTO t_role (id, name, code, remark, authority, status, is_deleted, builtin, create_at, create_by, update_at, update_by)
SELECT '2608130000000000003', '学管师', 'consultant', '内置角色（不可删除）', 'home,exercise:list,student:list,student:create,student:update,student:delete,order:list,order:create,order:update,order:delete,mall:list,task:list,knowledge:list,repo:list,repo:detail', 1, 0, 1, '2026-08-13 10:00:00', '1457995481966747649', NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM t_role WHERE code = 'consultant');
UPDATE t_role SET authority = 'home,exercise:list,student:list,student:create,student:update,student:delete,order:list,order:create,order:update,order:delete,mall:list,task:list,knowledge:list,repo:list,repo:detail' WHERE code = 'consultant';
UPDATE t_role SET authority = 'home,exercise:list,student:list,student:create,student:update,student:delete,order:list,order:create,order:update,order:delete,mall:list,task:list,knowledge:list,repo:list,repo:detail' WHERE code = 'consultant';
-- 内置角色：教务（教务管理）
INSERT INTO t_role (id, name, code, remark, authority, status, is_deleted, builtin, create_at, create_by, update_at, update_by)
SELECT '2608130000000000004', '教务', 'academic', '内置角色（不可删除）', 'home,exercise:list,repo:list,repo:detail,repo:create,repo:update,repo:delete,knowledge:list,knowledge:create,knowledge:update,knowledge:delete,student:list,order:list,mall:list,mall:create,mall:update,mall:delete,task:list,task:create,task:update,task:delete,project:list,project:detail,answer:list,answer:detail,system:dict:list,system:dictItem:list', 1, 0, 1, '2026-08-13 10:00:00', '1457995481966747649', NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM t_role WHERE code = 'academic');
UPDATE t_role SET authority = 'home,exercise:list,repo:list,repo:detail,repo:create,repo:update,repo:delete,template:list,template:create,template:update,template:delete,knowledge:list,knowledge:create,knowledge:update,knowledge:delete,student:list,order:list,mall:list,mall:create,mall:update,mall:delete,task:list,task:create,task:update,task:delete,project:list,project:detail,answer:list,answer:detail,system:dict:list,system:dictItem:list' WHERE code = 'academic';
UPDATE t_role SET authority = 'home,exercise:list,repo:list,repo:detail,repo:create,repo:update,repo:delete,template:list,template:create,template:update,template:delete,knowledge:list,knowledge:create,knowledge:update,knowledge:delete,student:list,order:list,mall:list,mall:create,mall:update,mall:delete,task:list,task:create,task:update,task:delete,project:list,project:detail,answer:list,answer:detail,system:dict:list,system:dictItem:list' WHERE code = 'academic';
-- 收敛旧库：已有 admin 角色补充内置标记与新权限点
UPDATE t_role SET name = '管理员', remark = '系统初始化角色（超管）', builtin = 1, update_at = '2026-08-13 10:00:00', update_by = '1457995481966747649', authority = 'home,exercise:list,project:list,project:detail,project:create,project:update,project:delete,project:report,answer:list,answer:detail,answer:create,answer:update,answer:delete,answer:export,answer:upload,repo:list,repo:detail,repo:create,repo:update,repo:delete,repo:export,repo:book,template:list,template:create,template:update,template:delete,knowledge:list,knowledge:create,knowledge:update,knowledge:delete,student:list,student:create,student:update,student:delete,order:list,order:create,order:update,order:delete,mall:list,mall:create,mall:update,mall:delete,task:list,task:create,task:update,task:delete,system:user:list,system:user:create,system:user:update,system:user:updatePosition,system:user:delete,system:role:list,system:role:create,system:role:update,system:role:delete,system:dept:list,system:dept:create,system:dept:update,system:dept:delete,system:position:list,system:position:create,system:position:update,system:position:delete,system:dict:list,system:dict:create,system:dict:update,system:dict:delete,system:dictItem:list,system:dictItem:create,system:dictItem:update,system:dictItem:delete,system:dictItem:import,user:update' WHERE code = 'admin';
COMMIT;

-- ----------------------------
-- Table structure for t_sys_info
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_sys_info (
  id varchar(64) NOT NULL COMMENT '主键',
  name varchar(64) DEFAULT NULL COMMENT '系统名称',
  description varchar(128) DEFAULT NULL COMMENT '系统描述信息',
  avatar varchar(64) DEFAULT NULL COMMENT '图标',
  locale varchar(64) DEFAULT NULL COMMENT '默认语言',
  version varchar(64) DEFAULT NULL COMMENT '版本号',
  setting varchar(1024) DEFAULT NULL COMMENT '其他系统设置',
  ai_setting varchar(1024) DEFAULT NULL COMMENT 'AI设置',
  register_info varchar(1024) DEFAULT NULL COMMENT '注册信息',
  is_default tinyint(1) DEFAULT NULL COMMENT '是否默认设置',
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  PRIMARY KEY (id)
);

-- ----------------------------
-- Records of t_sys_info
-- ----------------------------
BEGIN;
INSERT INTO t_sys_info (id, name, description, avatar, locale, version, setting, ai_setting, register_info, is_default, create_at, create_by, update_at, update_by) VALUES ('1', '卷王问卷考试系统', '做更好的调查问卷系统', NULL, 'zh-CN', '{}', NULL, NULL, NULL, 1, '2022-02-11 10:13:19', NULL, '2022-11-07 15:13:02', NULL);
COMMIT;

-- ----------------------------
-- Table structure for t_tag
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_tag (
  id varchar(64) NOT NULL,
  entity_id varchar(64) DEFAULT NULL COMMENT '实体ID',
  name varchar(128) DEFAULT NULL COMMENT '名称',
  category varchar(256) DEFAULT NULL COMMENT '分类',
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  PRIMARY KEY (id)
);

-- ----------------------------
-- Records of t_tag
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for t_template
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_template (
  id varchar(64) NOT NULL,
  repo_id varchar(64) DEFAULT NULL,
  serial_no varchar(256) DEFAULT NULL COMMENT '序号',
  name varchar(1024) DEFAULT NULL COMMENT '模板标题',
  question_type varchar(64) DEFAULT NULL COMMENT '问题类型',
  template text COMMENT '模板',
  mode varchar(32) DEFAULT NULL COMMENT '模板模式 survey/exam',
  category varchar(256) DEFAULT NULL COMMENT '模板分类',
  tag varchar(512) DEFAULT NULL COMMENT '标签',
  priority int DEFAULT NULL COMMENT '排序优先级',
  preview_url varchar(512) DEFAULT NULL COMMENT '预览地址',
  subject varchar(256) DEFAULT NULL COMMENT '学科',
  chapter varchar(256) DEFAULT NULL COMMENT '章节',
  knowledge_point varchar(1024) DEFAULT NULL COMMENT '知识点（多值，逗号分隔）',
  difficulty varchar(32) DEFAULT NULL COMMENT '难度',
  grade varchar(32) DEFAULT NULL COMMENT '年级标签',
  shared tinyint(1) DEFAULT '0',
  is_deleted tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  PRIMARY KEY (id)
);

-- 兼容旧库升级：补齐题目年级标签列（新库建表已包含，已存在则跳过）
ALTER TABLE t_template ADD COLUMN IF NOT EXISTS grade varchar(32) DEFAULT NULL COMMENT '年级标签';

-- ----------------------------
-- Records of t_template
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for t_user
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_user (
  id varchar(64) NOT NULL COMMENT 'ID',
  name varchar(50) NOT NULL COMMENT '真实姓名',
  dept_id varchar(20) DEFAULT NULL,
  gender varchar(10) DEFAULT NULL COMMENT '性别',
  birthday date DEFAULT NULL COMMENT '出生日期',
  phone varchar(20) DEFAULT NULL COMMENT '手机号',
  email varchar(50) DEFAULT NULL COMMENT 'Email',
  avatar varchar(200) DEFAULT NULL COMMENT '头像地址',
  status tinyint(1) NOT NULL DEFAULT '1' COMMENT '用户状态',
  is_deleted tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  profile varchar(255) DEFAULT NULL COMMENT '个人简介',
  correct_times int DEFAULT NULL COMMENT '错题答对清除次数',
  PRIMARY KEY (id)
);

-- ----------------------------
-- Records of t_user
-- ----------------------------
BEGIN;
INSERT INTO t_user (id, name, dept_id, gender, birthday, phone, email, avatar, status, is_deleted, create_at, create_by, update_at, update_by, profile, correct_times) VALUES ('1457995481966747649', 'Admin', '1', 'F', NULL, '13800138000', 'surveyking@qq.com', NULL, 1, 0, '2021-11-09 16:56:26', NULL, '2022-02-11 13:29:17', '1457995481966747649', 'hello surveyking~', NULL);
COMMIT;

-- ----------------------------
-- Table structure for t_user_book
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_user_book (
  id varchar(64) NOT NULL,
  name varchar(2048) DEFAULT NULL COMMENT '问题名称',
  template_id varchar(64) DEFAULT NULL COMMENT '模板ID',
  wrong_times int DEFAULT NULL COMMENT '错误次数',
  correct_times int DEFAULT NULL COMMENT '正确次数',
  note text COMMENT '笔记',
  status int DEFAULT NULL COMMENT '1标记为简单',
  type int DEFAULT NULL COMMENT '1错题 2收藏',
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  repo_id varchar(256) DEFAULT NULL,
  is_marked tinyint DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='错题本';

-- ----------------------------
-- Records of t_user_book
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for t_user_position
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_user_position (
  id varchar(64) NOT NULL COMMENT 'ID',
  user_id varchar(64) NOT NULL,
  dept_id varchar(64) DEFAULT NULL,
  position_id varchar(64) DEFAULT NULL COMMENT '数据权限类型',
  is_primary_position tinyint(1) DEFAULT NULL COMMENT '是否主岗',
  propertyJson varchar(256) DEFAULT NULL COMMENT '扩展字段',
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  PRIMARY KEY (id)
);

-- ----------------------------
-- Records of t_user_position
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for t_user_role
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_user_role (
  id varchar(64) NOT NULL COMMENT 'ID',
  user_type varchar(100) NOT NULL DEFAULT 'SysUser' COMMENT '用户类型',
  user_id varchar(64) NOT NULL COMMENT '用户ID',
  role_id varchar(64) NOT NULL COMMENT '角色ID',
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_t_user_role (user_type,user_id)
);

-- ----------------------------
-- Records of t_user_role
-- ----------------------------
BEGIN;
INSERT INTO t_user_role (id, user_type, user_id, role_id, create_at, create_by, update_at, update_by) VALUES ('1488542015867121666', 'SysUser', '1457995481966747649', '1457995481928998914', '2022-02-01 23:57:27', '1457995481966747649', NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for t_subject（学科字典：知识管理板块一级维度）
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_subject (
  id varchar(64) NOT NULL COMMENT '学科ID',
  name varchar(64) DEFAULT NULL COMMENT '学科名称',
  code varchar(32) DEFAULT NULL COMMENT '学科编码',
  icon varchar(32) DEFAULT NULL COMMENT '图标(emoji)',
  theme_color varchar(32) DEFAULT NULL COMMENT '主题色',
  sort int DEFAULT '1' COMMENT '排序(数字越小越靠前)',
  is_deleted tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  PRIMARY KEY (id)
);

-- ----------------------------
-- Records of t_subject（幂等：固定 id，不存在才插入）
-- ----------------------------
INSERT INTO t_subject (id, name, code, icon, theme_color, sort, is_deleted, create_at, create_by) SELECT '1001', '语文', 'CHINESE', '📚', 'orange', 1, 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_subject WHERE id = '1001');
INSERT INTO t_subject (id, name, code, icon, theme_color, sort, is_deleted, create_at, create_by) SELECT '1002', '数学', 'MATH', '🧮', 'blue', 2, 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_subject WHERE id = '1002');
INSERT INTO t_subject (id, name, code, icon, theme_color, sort, is_deleted, create_at, create_by) SELECT '1003', '英语', 'ENGLISH', '🔤', 'green', 3, 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_subject WHERE id = '1003');

-- ----------------------------
-- Table structure for t_chapter（章节：学科下的大单元）
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_chapter (
  id varchar(64) NOT NULL COMMENT '章节ID',
  subject_id varchar(64) DEFAULT NULL COMMENT '学科ID(t_subject.id)',
  name varchar(64) DEFAULT NULL COMMENT '章节名称',
  icon varchar(32) DEFAULT NULL COMMENT '图标(emoji)',
  sort int DEFAULT '1' COMMENT '排序(数字越小越靠前)',
  is_deleted tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_chapter_subject (subject_id)
);

-- ----------------------------
-- Records of t_chapter
-- ----------------------------
INSERT INTO t_chapter (id, subject_id, name, icon, sort, is_deleted, create_at, create_by) SELECT '2001', '1001', '识字与写字', '🖋️', 1, 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_chapter WHERE id = '2001');
INSERT INTO t_chapter (id, subject_id, name, icon, sort, is_deleted, create_at, create_by) SELECT '2002', '1001', '古诗文诵读', '📜', 2, 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_chapter WHERE id = '2002');
INSERT INTO t_chapter (id, subject_id, name, icon, sort, is_deleted, create_at, create_by) SELECT '2003', '1002', '100以内加减法', '🧮', 1, 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_chapter WHERE id = '2003');
INSERT INTO t_chapter (id, subject_id, name, icon, sort, is_deleted, create_at, create_by) SELECT '2004', '1002', '图形的认识', '📐', 2, 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_chapter WHERE id = '2004');
INSERT INTO t_chapter (id, subject_id, name, icon, sort, is_deleted, create_at, create_by) SELECT '2005', '1003', '字母与拼读', '🔠', 1, 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_chapter WHERE id = '2005');
INSERT INTO t_chapter (id, subject_id, name, icon, sort, is_deleted, create_at, create_by) SELECT '2006', '1003', '基础单词', '🗣️', 2, 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_chapter WHERE id = '2006');

-- ----------------------------
-- Table structure for t_section（小节：章节下的学习小站，含内容/练习设置 JSON）
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_section (
  id varchar(64) NOT NULL COMMENT '小节ID',
  chapter_id varchar(64) DEFAULT NULL COMMENT '章节ID(t_chapter.id)',
  name varchar(64) DEFAULT NULL COMMENT '小节名称',
  sort int DEFAULT '1' COMMENT '排序(数字越小越靠前)',
  content text COMMENT '小节内容设置JSON(objective/overview/points)',
  practice text COMMENT '小节练习设置JSON(questionCount/difficulty/types)',
  is_deleted tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_section_chapter (chapter_id)
);

-- ----------------------------
-- Records of t_section
-- ----------------------------
INSERT INTO t_section (id, chapter_id, name, sort, content, practice, is_deleted, create_at, create_by) SELECT '3001', '2001', '拼音入门', 1, '{"objective":"掌握拼音字母读写与拼读规则","overview":"本小节学习声母、韵母与整体认读音节，打好拼音基础。","points":["声母 23 个 / 韵母 24 个 / 整体认读音节 16 个","四声标调规则","常见拼读组合训练"]}', '{"questionCount":10,"difficulty":"基础","types":["Radio","FillBlank"]}', 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_section WHERE id = '3001');
INSERT INTO t_section (id, chapter_id, name, sort, content, practice, is_deleted, create_at, create_by) SELECT '3002', '2001', '字词辨析', 2, '{"objective":"区分易混字形近字","overview":"通过偏旁与字义对比，掌握形近字辨析方法。","points":["形近字概念","偏旁部首辨义","组词对比记忆"]}', '{"questionCount":8,"difficulty":"基础","types":["Radio","Judge"]}', 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_section WHERE id = '3002');
INSERT INTO t_section (id, chapter_id, name, sort, content, practice, is_deleted, create_at, create_by) SELECT '3003', '2002', '唐诗赏读', 1, '{"objective":"背诵理解三首经典唐诗","overview":"精读《静夜思》《春晓》《咏鹅》。","points":["作者与朝代","诗句大意","名句赏析"]}', '{"questionCount":10,"difficulty":"进阶","types":["Radio","FillBlank"]}', 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_section WHERE id = '3003');
INSERT INTO t_section (id, chapter_id, name, sort, content, practice, is_deleted, create_at, create_by) SELECT '3004', '2002', '名句积累', 2, '{"objective":"积累经典名句","overview":"背诵常用名句并理解含义。","points":["举头望明月，低头思故乡","谁知盘中餐，粒粒皆辛苦"]}', '{"questionCount":8,"difficulty":"基础","types":["FillBlank"]}', 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_section WHERE id = '3004');
INSERT INTO t_section (id, chapter_id, name, sort, content, practice, is_deleted, create_at, create_by) SELECT '3005', '2003', '加法小站', 1, '{"objective":"掌握两位数进位加法","overview":"学习个位相加满十进一的规则与口算技巧。","points":["进位加法竖式书写","凑十法口算","相同数位对齐"]}', '{"questionCount":10,"difficulty":"基础","types":["Radio","FillBlank"]}', 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_section WHERE id = '3005');
INSERT INTO t_section (id, chapter_id, name, sort, content, practice, is_deleted, create_at, create_by) SELECT '3006', '2003', '减法小站', 2, '{"objective":"掌握两位数退位减法与混合运算","overview":"学习个位不够减向十位借一的规则。","points":["退位减法竖式","破十法口算","加减混合运算顺序"]}', '{"questionCount":10,"difficulty":"进阶","types":["Radio","Judge"]}', 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_section WHERE id = '3006');
INSERT INTO t_section (id, chapter_id, name, sort, content, practice, is_deleted, create_at, create_by) SELECT '3007', '2004', '平面图形', 1, '{"objective":"认识常见平面图形","overview":"三角形、长方形、正方形、圆的特征。","points":["边与角的数量","图形分类"]}', '{"questionCount":8,"difficulty":"基础","types":["Radio"]}', 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_section WHERE id = '3007');
INSERT INTO t_section (id, chapter_id, name, sort, content, practice, is_deleted, create_at, create_by) SELECT '3008', '2004', '立体图形', 2, '{"objective":"认识常见立体图形","overview":"长方体、正方体、圆柱、球的特征。","points":["面与棱","图形与实物对应"]}', '{"questionCount":8,"difficulty":"基础","types":["Radio","Judge"]}', 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_section WHERE id = '3008');
INSERT INTO t_section (id, chapter_id, name, sort, content, practice, is_deleted, create_at, create_by) SELECT '3009', '2005', '字母乐园', 1, '{"objective":"掌握 26 个字母","overview":"字母名称音、大小写与书写占格。","points":["26 个字母顺序","大小写对应","5 个元音字母"]}', '{"questionCount":10,"difficulty":"基础","types":["Radio","FillBlank"]}', 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_section WHERE id = '3009');
INSERT INTO t_section (id, chapter_id, name, sort, content, practice, is_deleted, create_at, create_by) SELECT '3010', '2005', '拼读魔法', 2, '{"objective":"掌握自然拼读","overview":"元音字母在单词中的短音规律。","points":["a→/æ/ e→/e/ i→/ɪ/","辅音发音","拼读练习"]}', '{"questionCount":8,"difficulty":"进阶","types":["Radio"]}', 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_section WHERE id = '3010');
INSERT INTO t_section (id, chapter_id, name, sort, content, practice, is_deleted, create_at, create_by) SELECT '3011', '2006', '校园词汇', 1, '{"objective":"掌握校园常用词汇","overview":"教室、文具、颜色等词汇。","points":["book/pen/ruler","red/blue/green"]}', '{"questionCount":10,"difficulty":"基础","types":["Radio","FillBlank"]}', 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_section WHERE id = '3011');

-- ----------------------------
-- Table structure for t_knowledge_point（知识点：最小学习单元，含内容设置 JSON 与图片）
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_knowledge_point (
  id varchar(64) NOT NULL COMMENT '知识点ID',
  section_id varchar(64) DEFAULT NULL COMMENT '小节ID(t_section.id)',
  name varchar(64) DEFAULT NULL COMMENT '知识点名称',
  sort int DEFAULT '1' COMMENT '排序(数字越小越靠前)',
  content text COMMENT '知识点内容设置JSON(points讲解要点数组)',
  image_url varchar(512) DEFAULT NULL COMMENT '知识点图片地址(FileView.previewUrl，可为空)',
  is_deleted tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_kp_section (section_id)
);

-- ----------------------------
-- Records of t_knowledge_point
-- ----------------------------
INSERT INTO t_knowledge_point (id, section_id, name, sort, content, image_url, is_deleted, create_at, create_by) SELECT '4001', '3001', '拼音王国', 1, '{"points":["单韵母 a o e i u ü 的认读","声母与韵母拼读方法","整体认读音节 zh ch sh r 等"]}', NULL, 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_knowledge_point WHERE id = '4001');
INSERT INTO t_knowledge_point (id, section_id, name, sort, content, image_url, is_deleted, create_at, create_by) SELECT '4002', '3001', '汉字笔顺', 2, '{"points":["先横后竖、先撇后捺书写规则","常见偏旁部首","左右/上下/半包围结构"]}', NULL, 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_knowledge_point WHERE id = '4002');
INSERT INTO t_knowledge_point (id, section_id, name, sort, content, image_url, is_deleted, create_at, create_by) SELECT '4003', '3002', '形近字辨析', 1, '{"points":["日与目 / 人入八等易混字","借助偏旁区别字义","组词法巩固"]}', NULL, 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_knowledge_point WHERE id = '4003');
INSERT INTO t_knowledge_point (id, section_id, name, sort, content, image_url, is_deleted, create_at, create_by) SELECT '4004', '3003', '唐诗三首', 1, '{"points":["《静夜思》李白：床前明月光","《春晓》孟浩然：春眠不觉晓","《咏鹅》骆宾王：曲项向天歌"]}', NULL, 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_knowledge_point WHERE id = '4004');
INSERT INTO t_knowledge_point (id, section_id, name, sort, content, image_url, is_deleted, create_at, create_by) SELECT '4005', '3004', '名句积累', 1, '{"points":["名句与出处对应","名句含义理解"]}', NULL, 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_knowledge_point WHERE id = '4005');
INSERT INTO t_knowledge_point (id, section_id, name, sort, content, image_url, is_deleted, create_at, create_by) SELECT '4006', '3005', '进位加法', 1, '{"points":["个位相加满十向十位进 1","竖式书写规范","凑十法快速口算"]}', NULL, 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_knowledge_point WHERE id = '4006');
INSERT INTO t_knowledge_point (id, section_id, name, sort, content, image_url, is_deleted, create_at, create_by) SELECT '4007', '3005', '口算技巧', 2, '{"points":["凑十法","破十法","视算与听算训练"]}', NULL, 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_knowledge_point WHERE id = '4007');
INSERT INTO t_knowledge_point (id, section_id, name, sort, content, image_url, is_deleted, create_at, create_by) SELECT '4008', '3006', '退位减法', 1, '{"points":["个位不够减向十位借 1 当 10","借位标记写法","破十法口算"]}', NULL, 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_knowledge_point WHERE id = '4008');
INSERT INTO t_knowledge_point (id, section_id, name, sort, content, image_url, is_deleted, create_at, create_by) SELECT '4009', '3006', '加减混合运算', 2, '{"points":["从左到右依次计算","有括号先算括号内","两步式混合运算"]}', NULL, 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_knowledge_point WHERE id = '4009');
INSERT INTO t_knowledge_point (id, section_id, name, sort, content, image_url, is_deleted, create_at, create_by) SELECT '4010', '3007', '平面图形', 1, '{"points":["三角形 3 条边 3 个角","长方形对边相等","圆由曲线围成"]}', NULL, 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_knowledge_point WHERE id = '4010');
INSERT INTO t_knowledge_point (id, section_id, name, sort, content, image_url, is_deleted, create_at, create_by) SELECT '4011', '3008', '立体图形', 1, '{"points":["长方体 6 个面","正方体 6 个面都是正方形","球可任意滚动"]}', NULL, 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_knowledge_point WHERE id = '4011');
INSERT INTO t_knowledge_point (id, section_id, name, sort, content, image_url, is_deleted, create_at, create_by) SELECT '4012', '3009', '26个字母', 1, '{"points":["A-Z 字母顺序","元音字母 A E I O U","书写占格规范"]}', NULL, 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_knowledge_point WHERE id = '4012');
INSERT INTO t_knowledge_point (id, section_id, name, sort, content, image_url, is_deleted, create_at, create_by) SELECT '4013', '3010', '自然拼读', 1, '{"points":["短音发音规律","c-a-t 拼读","单词拼读训练"]}', NULL, 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_knowledge_point WHERE id = '4013');
INSERT INTO t_knowledge_point (id, section_id, name, sort, content, image_url, is_deleted, create_at, create_by) SELECT '4014', '3011', '校园词汇', 1, '{"points":["学习用品词汇","颜色词汇","看图说词"]}', NULL, 0, CURRENT_TIMESTAMP, '1457995481966747649' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM t_knowledge_point WHERE id = '4014');

-- ----------------------------
-- Table structure for t_knowledge_point_question（知识点-题目绑定：从题目库选题，多对多）
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_knowledge_point_question (
  id varchar(64) NOT NULL COMMENT '绑定ID',
  knowledge_point_id varchar(64) DEFAULT NULL COMMENT '知识点ID(t_knowledge_point.id)',
  question_id varchar(64) DEFAULT NULL COMMENT '题目ID(t_template.id，仅能从题目库选择)',
  is_deleted tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_kpq_kp (knowledge_point_id),
  KEY idx_kpq_question (question_id)
);

-- ----------------------------
-- Records of t_knowledge_point_question
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for t_section_repo（小节-题库绑定：从题库管理选题库，多对多）
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_section_repo (
  id varchar(64) NOT NULL COMMENT '绑定ID',
  section_id varchar(64) DEFAULT NULL COMMENT '小节ID(t_section.id)',
  repo_id varchar(64) DEFAULT NULL COMMENT '题库ID(t_repo.id，仅能从题库管理选择)',
  is_deleted tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_secr_section (section_id),
  KEY idx_secr_repo (repo_id)
);

-- ----------------------------
-- Records of t_section_repo
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for t_chapter_repo（章节-题库绑定：从题库管理选题库，多对多）
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_chapter_repo (
  id varchar(64) NOT NULL COMMENT '绑定ID',
  chapter_id varchar(64) DEFAULT NULL COMMENT '章节ID(t_chapter.id)',
  repo_id varchar(64) DEFAULT NULL COMMENT '题库ID(t_repo.id，仅能从题库管理选择)',
  is_deleted tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_chapr_chapter (chapter_id),
  KEY idx_chapr_repo (repo_id)
);

-- ----------------------------
-- Records of t_chapter_repo
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- 演示题库种子（带学科/年级/难度标签，供章节/小节绑定题库演示）
-- ----------------------------
INSERT INTO t_repo (id, name, description, category, mode, shared, tag, priority, setting, create_at, create_by, update_at, update_by, is_practice, subject, grade, difficulty) VALUES
('9931000000000000001', '数学三年级-口算与四则运算', '三年级口算与四则运算专项练习', '同步练习', 'exam', 0, '["口算","四则运算"]', NULL, NULL, '2026-08-11 00:00:00', '1457995481966747649', NULL, NULL, 1, '数学', '三年级', 'medium'),
('9931000000000000002', '数学二年级-图形与长度', '二年级图形认识与长度单位练习', '同步练习', 'exam', 0, '["图形","长度"]', NULL, NULL, '2026-08-11 00:00:00', '1457995481966747649', NULL, NULL, 1, '数学', '二年级', 'easy'),
('9931000000000000003', '语文二年级-生字词', '二年级生字词积累与运用', '同步练习', 'exam', 0, '["生字","词语"]', NULL, NULL, '2026-08-11 00:00:00', '1457995481966747649', NULL, NULL, 1, '语文', '二年级', 'easy');

-- ----------------------------
-- 演示题目种子（带学科/章节/年级/难度/知识点标签，挂演示题库下）
-- ----------------------------
INSERT INTO t_template (id, repo_id, serial_no, name, question_type, template, mode, category, tag, priority, preview_url, shared, is_deleted, create_at, create_by, update_at, update_by, subject, chapter, knowledge_point, difficulty, grade) VALUES
('9932000000000000001', '9931000000000000001', '1', '45+27=（ ）', 'FillBlank', '{"id":"demo1","title":"45+27=（ ）","type":"FillBlank","attribute":{"examCorrectAnswer":"72"},"children":[]}', 'exam', '口算', '["口算"]', NULL, NULL, 0, 0, '2026-08-11 00:00:00', '1457995481966747649', NULL, NULL, '数学', '万以内数的加减法', '["两位数加法"]', 'easy', '三年级'),
('9932000000000000002', '9931000000000000001', '2', '计算 120×3=（ ）', 'FillBlank', '{"id":"demo2","title":"计算 120×3=（ ）","type":"FillBlank","attribute":{"examCorrectAnswer":"360"},"children":[]}', 'exam', '口算', '["口算"]', NULL, NULL, 0, 0, '2026-08-11 00:00:00', '1457995481966747649', NULL, NULL, '数学', '多位数乘一位数', '["整百数乘一位数"]', 'medium', '三年级'),
('9932000000000000003', '9931000000000000001', '3', '一个数除以 8 商是 6，这个数是多少？', 'Radio', '{"id":"demo3","title":"一个数除以 8 商是 6，这个数是多少？","type":"Radio","attribute":{"examCorrectAnswer":"c"},"children":[{"id":"a","title":"42"},{"id":"b","title":"46"},{"id":"c","title":"48"},{"id":"d","title":"54"}]}', 'exam', '除法', '["除法"]', NULL, NULL, 0, 0, '2026-08-11 00:00:00', '1457995481966747649', NULL, NULL, '数学', '除数是一位数的除法', '["有余数除法"]', 'medium', '三年级'),
('9932000000000000004', '9931000000000000002', '1', '直角一定比锐角大。', 'Judge', '{"id":"demo4","title":"直角一定比锐角大。","type":"Judge","attribute":{"examCorrectAnswer":"对"},"children":[]}', 'exam', '图形', '["图形"]', NULL, NULL, 0, 0, '2026-08-11 00:00:00', '1457995481966747649', NULL, NULL, '数学', '角的初步认识', '["锐角直角钝角"]', 'easy', '二年级'),
('9932000000000000005', '9931000000000000002', '2', '1 米 =（ ）厘米', 'FillBlank', '{"id":"demo5","title":"1 米 =（ ）厘米","type":"FillBlank","attribute":{"examCorrectAnswer":"100"},"children":[]}', 'exam', '长度', '["长度"]', NULL, NULL, 0, 0, '2026-08-11 00:00:00', '1457995481966747649', NULL, NULL, '数学', '长度单位', '["米和厘米"]', 'easy', '二年级'),
('9932000000000000006', '9931000000000000003', '1', '下列词语书写完全正确的一组是（）', 'Radio', '{"id":"demo6","title":"下列词语书写完全正确的一组是（）","type":"Radio","attribute":{"examCorrectAnswer":"b"},"children":[{"id":"a","title":"波烂壮阔"},{"id":"b","title":"风景秀丽"},{"id":"c","title":"光采夺目"},{"id":"d","title":"山青水秀"}]}', 'exam', '词语', '["词语"]', NULL, NULL, 0, 0, '2026-08-11 00:00:00', '1457995481966747649', NULL, NULL, '语文', '词语积累', '["正确书写"]', 'medium', '二年级'),
('9932000000000000007', '9931000000000000003', '2', '"湖"字的部首是（ ）', 'FillBlank', '{"id":"demo7","title":"\"湖\"字的部首是（ ）","type":"FillBlank","attribute":{"examCorrectAnswer":"氵"},"children":[]}', 'exam', '生字', '["生字"]', NULL, NULL, 0, 0, '2026-08-11 00:00:00', '1457995481966747649', NULL, NULL, '语文', '偏旁部首', '["三点水"]', 'easy', '二年级');

-- ----------------------------
-- Table structure for t_student（学员主数据：学员管理模块，学号唯一）
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_student (
  id varchar(64) NOT NULL COMMENT 'ID',
  student_no varchar(8) NOT NULL COMMENT '学号(8位数字，系统自动生成，全局唯一)',
  name varchar(50) NOT NULL COMMENT '姓名',
  age int DEFAULT NULL COMMENT '年龄',
  phone varchar(20) NOT NULL COMMENT '联系号码',
  school varchar(100) DEFAULT NULL COMMENT '学校',
  campus varchar(50) DEFAULT NULL COMMENT '校区(本迭代仅占位，业务逻辑后续迭代)',
  extra text DEFAULT NULL COMMENT '扩展预留字段(JSON，前端不展示，供后续数据分析)',
  status tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态(1正常 0停用)',
  is_deleted tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_student_no UNIQUE (student_no),
  KEY idx_student_name_phone (name, phone)
);

-- ----------------------------
-- Records of t_student
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for t_student_order（学员订单：开通AI自习室权限）
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_student_order (
  id varchar(64) NOT NULL COMMENT 'ID',
  student_id varchar(64) NOT NULL COMMENT '学员ID(t_student.id)',
  subject_ids varchar(255) NOT NULL COMMENT '学科ID多选(逗号分隔，t_subject.id)',
  grades varchar(255) NOT NULL COMMENT '年级多选(逗号分隔)',
  version varchar(50) DEFAULT NULL COMMENT '教材版本',
  duration int NOT NULL COMMENT '账号时长数值',
  duration_unit varchar(10) NOT NULL COMMENT '时长单位(DAY/MONTH/YEAR)',
  expire_at datetime NOT NULL COMMENT '有效期至(服务端按时长计算)',
  status tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态(1生效 0作废)',
  is_deleted tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_order_student (student_id)
);

-- ----------------------------
-- Records of t_student_order
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for t_student_permission（学员权限：多选学科×年级笛卡尔积展开，供学员端鉴权）
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_student_permission (
  id varchar(64) NOT NULL COMMENT 'ID',
  student_id varchar(64) NOT NULL COMMENT '学员ID(t_student.id)',
  order_id varchar(64) NOT NULL COMMENT '来源订单ID(t_student_order.id)',
  subject_id varchar(64) NOT NULL COMMENT '学科ID(t_subject.id)',
  grade varchar(20) NOT NULL COMMENT '年级',
  version varchar(50) DEFAULT NULL COMMENT '教材版本',
  expire_at datetime NOT NULL COMMENT '有效期至',
  is_deleted tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_perm_student (student_id),
  KEY idx_perm_order (order_id)
);

-- ----------------------------
-- Records of t_student_permission
-- ----------------------------
BEGIN;
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------
-- 学员实时位置（学员端上报 + 后台监控）
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_student_activity (
  id varchar(64) NOT NULL,
  student_id varchar(64) DEFAULT NULL COMMENT '学员ID(t_student.id)',
  page varchar(64) DEFAULT NULL COMMENT '当前页面标识(/student/study 等)',
  question_id varchar(64) DEFAULT NULL COMMENT '当前习题ID(可为空)',
  section_id varchar(64) DEFAULT NULL COMMENT '小节ID(习题上下文,可为空)',
  create_at timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp DEFAULT NULL COMMENT '最后活跃时间',
  update_by varchar(256) DEFAULT NULL,
  is_deleted tinyint DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (id)
);
ALTER TABLE t_student_activity ADD COLUMN IF NOT EXISTS create_at timestamp DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE t_student_activity ADD COLUMN IF NOT EXISTS create_by varchar(256);
ALTER TABLE t_student_activity ADD COLUMN IF NOT EXISTS update_by varchar(256);
ALTER TABLE t_student_activity ADD COLUMN IF NOT EXISTS is_deleted tinyint DEFAULT 0;

-- ----------------------------
-- 积分商城商品（后台商品管理 + 学员端商城展示）
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_mall_goods (
  id varchar(64) NOT NULL,
  name varchar(128) DEFAULT NULL COMMENT '商品名称',
  description varchar(512) DEFAULT NULL COMMENT '商品描述',
  image_url varchar(512) DEFAULT NULL COMMENT '商品图片地址(FileView.previewUrl)',
  points int DEFAULT 0 COMMENT '兑换所需积分',
  sort int DEFAULT 1 COMMENT '排序(数字越小越靠前)',
  status tinyint DEFAULT 1 COMMENT '状态 1上架 0下架',
  create_at timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp DEFAULT NULL COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  is_deleted tinyint DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (id)
);
ALTER TABLE t_mall_goods ADD COLUMN IF NOT EXISTS description varchar(512);
ALTER TABLE t_mall_goods ADD COLUMN IF NOT EXISTS image_url varchar(512);
ALTER TABLE t_mall_goods ADD COLUMN IF NOT EXISTS points int DEFAULT 0;
ALTER TABLE t_mall_goods ADD COLUMN IF NOT EXISTS sort int DEFAULT 1;
ALTER TABLE t_mall_goods ADD COLUMN IF NOT EXISTS status tinyint DEFAULT 1;
ALTER TABLE t_mall_goods ADD COLUMN IF NOT EXISTS create_at timestamp DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE t_mall_goods ADD COLUMN IF NOT EXISTS create_by varchar(256);
ALTER TABLE t_mall_goods ADD COLUMN IF NOT EXISTS update_at timestamp;
ALTER TABLE t_mall_goods ADD COLUMN IF NOT EXISTS update_by varchar(256);
ALTER TABLE t_mall_goods ADD COLUMN IF NOT EXISTS is_deleted tinyint DEFAULT 0;

-- ----------------------------
-- 今日任务（老师后台布置，学员端呈现，交卷且及格判定完成）
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_task (
  id varchar(64) NOT NULL,
  student_id varchar(64) DEFAULT NULL COMMENT '绑定学员ID(t_student.id)',
  name varchar(128) DEFAULT NULL COMMENT '任务名称(选填)',
  description varchar(512) DEFAULT NULL COMMENT '任务描述',
  task_date varchar(16) DEFAULT NULL COMMENT '任务日期(YYYY-MM-DD)',
  content_type varchar(32) DEFAULT NULL COMMENT '内容类型 practice练习 knowledge_point知识点',
  content_id varchar(64) DEFAULT NULL COMMENT '关联内容ID(练习=t_repo.id/知识点=t_knowledge_point.id)',
  status tinyint DEFAULT 1 COMMENT '状态 1发布 0停用',
  sort int DEFAULT 1 COMMENT '排序',
  create_at timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp DEFAULT NULL COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  is_deleted tinyint DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (id)
);
ALTER TABLE t_task ADD COLUMN IF NOT EXISTS student_id varchar(64);
ALTER TABLE t_task ADD COLUMN IF NOT EXISTS description varchar(512);
ALTER TABLE t_task ADD COLUMN IF NOT EXISTS task_date varchar(16);
ALTER TABLE t_task ADD COLUMN IF NOT EXISTS content_type varchar(32);
ALTER TABLE t_task ADD COLUMN IF NOT EXISTS content_id varchar(64);
ALTER TABLE t_task ADD COLUMN IF NOT EXISTS status tinyint DEFAULT 1;
ALTER TABLE t_task ADD COLUMN IF NOT EXISTS sort int DEFAULT 1;
ALTER TABLE t_task ADD COLUMN IF NOT EXISTS create_at timestamp DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE t_task ADD COLUMN IF NOT EXISTS create_by varchar(256);
ALTER TABLE t_task ADD COLUMN IF NOT EXISTS update_at timestamp;
ALTER TABLE t_task ADD COLUMN IF NOT EXISTS update_by varchar(256);
ALTER TABLE t_task ADD COLUMN IF NOT EXISTS is_deleted tinyint DEFAULT 0;
-- 练习记录加知识点维度（知识点型任务完成判定）
ALTER TABLE t_practice_record ADD COLUMN IF NOT EXISTS knowledge_point_id varchar(64);
ALTER TABLE t_practice_detail ADD COLUMN IF NOT EXISTS wrong_reason varchar(32);
ALTER TABLE t_practice_record ADD COLUMN IF NOT EXISTS section_id varchar(64);
-- 学员学币发放记录（老师手动加学币）
CREATE TABLE IF NOT EXISTS t_student_coin (
  id varchar(64) NOT NULL,
  student_id varchar(64) DEFAULT NULL COMMENT '学员ID(t_student.id)',
  coins int DEFAULT 0 COMMENT '学币数量(正加负扣)',
  reason varchar(255) DEFAULT NULL COMMENT '发放原因',
  create_at timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp DEFAULT NULL,
  update_by varchar(256) DEFAULT NULL,
  is_deleted tinyint DEFAULT 0,
  PRIMARY KEY (id)
);
ALTER TABLE t_student_coin ADD COLUMN IF NOT EXISTS coins int DEFAULT 0;
ALTER TABLE t_student_coin ADD COLUMN IF NOT EXISTS reason varchar(255);
ALTER TABLE t_student_coin ADD COLUMN IF NOT EXISTS deleted tinyint DEFAULT 0;
-- 章节表添加年级/学期/版本字段
ALTER TABLE t_chapter ADD COLUMN IF NOT EXISTS grade varchar(32) DEFAULT NULL COMMENT '年级（一年级~六年级）';
ALTER TABLE t_chapter ADD COLUMN IF NOT EXISTS term varchar(16) DEFAULT NULL COMMENT '学期（上/下）';
ALTER TABLE t_chapter ADD COLUMN IF NOT EXISTS version varchar(64) DEFAULT NULL COMMENT '教材版本（人教版/苏教版等）';

-- 英语学习模块
CREATE TABLE IF NOT EXISTS t_english_word (
  id varchar(64) NOT NULL,
  spell varchar(128) NOT NULL COMMENT '单词拼写',
  phonetic varchar(64) COMMENT '音标',
  meaning text COMMENT '释义',
  image_url varchar(512) COMMENT '图片 URL',
  audio_url varchar(512) COMMENT '音频 URL',
  example_sentence text COMMENT '例句',
  version varchar(32) COMMENT '教材版本',
  grade varchar(16) COMMENT '年级',
  unit varchar(32) COMMENT '单元',
  created_at timestamp DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS t_english_word_book (
  id varchar(64) NOT NULL,
  user_id varchar(64) NOT NULL,
  word_id varchar(64) NOT NULL,
  familiarity tinyint DEFAULT 0 COMMENT '熟练度 0-未学习 1-生疏 2-熟悉 3-熟练 4-精通',
  next_review_time timestamp COMMENT '下次复习时间',
  created_at timestamp DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_word (user_id, word_id)
);

CREATE TABLE IF NOT EXISTS t_english_grammar (
  id varchar(64) NOT NULL,
  title varchar(256) NOT NULL COMMENT '标题',
  content text COMMENT '讲解内容',
  examples text COMMENT '例句',
  exercises json COMMENT '练习题',
  grade varchar(16) COMMENT '年级',
  created_at timestamp DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS t_english_learning_log (
  id varchar(64) NOT NULL,
  user_id varchar(64) NOT NULL,
  type varchar(16) NOT NULL COMMENT '类型 word/grammar',
  content_id varchar(64) NOT NULL,
  duration int COMMENT '学习时长 (秒)',
  correct_count int COMMENT '正确数',
  created_at timestamp DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_english_log_user ON t_english_learning_log (user_id, created_at);
