-- Auto-generated H2 init script from init-mysql.sql
-- Generated for H2 with MODE=MySQL

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
  PRIMARY KEY (id) USING BTREE,
  KEY key_answer_pid (project_id) USING BTREE
);

-- ----------------------------
-- Records of t_answer
-- ----------------------------

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
  PRIMARY KEY (id) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Records of t_comm_dict
-- ----------------------------

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
  PRIMARY KEY (id,item_value) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Records of t_comm_dict_item
-- ----------------------------

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
-- Records of t_project
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

-- ----------------------------
-- Records of t_project_partner
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
  PRIMARY KEY (id)
);

-- ----------------------------
-- Records of t_repo
-- ----------------------------

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
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  PRIMARY KEY (id)
);

-- ----------------------------
-- Records of t_role
-- ----------------------------

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
  shared tinyint(1) DEFAULT '0',
  is_deleted tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  create_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_by varchar(256) DEFAULT NULL,
  update_at timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  update_by varchar(256) DEFAULT NULL,
  PRIMARY KEY (id)
);

-- ----------------------------
-- Records of t_template
-- ----------------------------

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
  PRIMARY KEY (id) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='错题本';

-- ----------------------------
-- Records of t_user_book
-- ----------------------------

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

SET FOREIGN_KEY_CHECKS = 1;
