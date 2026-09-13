/**
 * student.js - 学员管理板块 API（学员 + 订单）
 *
 * 接口（后端 StudentApi / OrderApi，前缀 /api）:
 *   学员:   POST /student/create  新增学员（自动生成学号，返回含学号的 StudentView）
 *           GET  /student/list    学员分页（?current=&pageSize=&name=&studentNo=&phone=）
 *           POST /student/update  更新学员（学号不可修改）
 *           POST /student/delete  删除学员
 *           GET  /student/me      当前登录学员信息（学员端档案展示）
 *   订单:   POST /order/create    创建订单（学科×年级多选，服务端展开权限）
 *           GET  /order/list      订单分页（?studentId=&studentName=&status=）
 *           POST /order/cancel    作废订单（status=0 + 权限失效）
 *           POST /order/delete    删除订单
 *
 * 调用方:
 *   - StudentManagePage : 学员列表 CRUD（新增/编辑/删除/按学号姓名电话搜索）
 *   - OrderManagePage   : 订单列表 + 创建订单 + 作废/删除（学科/年级数据来自知识管理
 *                         /api/subject/list 与前端常量）
 */

import request from './request';

// ============================================================
// 学员
// ============================================================

/**
 * 新增学员（系统自动生成 8 位学号作为登录账号）
 * 后端接口: POST /api/student/create
 * @param {Object} data - { name, age?, phone, school?, campus? }
 * @returns {Object} data: StudentView { id, studentNo, name, age, phone, school, campus, createAt }
 */
export async function createStudent(data) {
  return request.post('/student/create', data);
}

/**
 * 学员分页列表
 * 后端接口: GET /api/student/list
 * @param {Object} params - { current, pageSize, name?, studentNo?, phone? }
 * @returns {Object} data: { total, list: [StudentView, ...] }
 */
export async function listStudents(params) {
  return request.get('/student/list', { params });
}

/**
 * 更新学员（学号不可修改）
 * 后端接口: POST /api/student/update
 * @param {Object} data - { id, name, age?, phone, school?, campus? }
 */
export async function updateStudent(data) {
  return request.post('/student/update', data);
}

/**
 * 删除学员（逻辑删除）
 * 后端接口: POST /api/student/delete
 * @param {Object} data - { id }
 */
export async function deleteStudent(data) {
  return request.post('/student/delete', data);
}

/**
 * 当前登录学员信息（学员端档案展示用）
 * 后端接口: GET /api/student/me
 * @returns {Object} data: StudentView { id, studentNo, name, age, phone, school, campus, createAt }
 */
export async function getMyStudentInfo() {
  return request.get('/student/me');
}

// ============================================================
// 订单
// ============================================================

/**
 * 创建订单（学科多选 × 年级多选，服务端按笛卡尔积展开权限）
 * 后端接口: POST /api/order/create
 * @param {Object} data - { studentId, subjectIds: [], grades: [], version, duration, durationUnit }
 * @returns {Object} data: OrderView { id, studentId, studentNo, studentName, subjects, grades, ... }
 */
export async function createOrder(data) {
  return request.post('/order/create', data);
}

/**
 * 订单分页列表
 * 后端接口: GET /api/order/list
 * @param {Object} params - { current, pageSize, studentId?, studentName?, status? }
 * @returns {Object} data: { total, list: [OrderView, ...] }
 */
export async function listOrders(params) {
  return request.get('/order/list', { params });
}

/**
 * 作废订单（status=0 + 权限失效）
 * 后端接口: POST /api/order/cancel
 * @param {Object} data - { id }
 */
export async function cancelOrder(data) {
  return request.post('/order/cancel', data);
}

/**
 * 删除订单（逻辑删除 + 权限清理）
 * 后端接口: POST /api/order/delete
 * @param {Object} data - { id }
 */
export async function deleteOrder(data) {
  return request.post('/order/delete', data);
}

/**
 * 当前学员有效权限（多条有效订单合并）
 * 后端接口: GET /api/student/permissions
 * @returns {Object} data: { subjects: [{id,name}], grades: [], versions: [] }
 */
export async function getMyPermissions() {
  return request.get('/student/permissions');
}

// ------------------------------------------------------------
// 学员端学习内容（按订单权限过滤，后台配置真实呈现）
// ------------------------------------------------------------

/** 学员端学科列表（含该学科有权限的教材版本与年级） */
export async function getStudySubjects() {
  return request.get('/student/study/subjects');
}

/** 学员端章节列表（?subjectId=&grade=，grade 缺省按该学科全部授权年级返回） */
export async function getStudyChapters(subjectId, grade) {
  return request.get('/student/study/chapters', { params: { subjectId, grade } });
}

/** 学员端小节列表（?chapterId=） */
export async function getStudySections(chapterId) {
  return request.get('/student/study/sections', { params: { chapterId } });
}

/** 学员端知识点列表（?sectionId=） */
export async function getStudyPoints(sectionId) {
  return request.get('/student/study/points', { params: { sectionId } });
}

/** 学员端学科学习进度（章节 → 知识点掌握度/评级/薄弱） */
export async function getStudyProgress(subjectId, versionId) {
  return request.get('/student/study/progress', { params: { subjectId, versionId } });
}

/**
 * 学员端练习/试炼题目（剥离标准答案）
 * @param {Object} params - { sectionId, knowledgePointId, knowledgePointIds, count, perKp, types, difficulty, random, exposeAnswer }
 */
export async function getStudyQuestions(params) {
  return request.get('/student/study/questions', { params });
}

/**
 * 学员端小节练习配置（出题模式/题量/难度/题型/通关阈值/解锁开关）
 * @param {string} sectionId - 小节ID
 */
export async function getSectionPracticeConfig(sectionId) {
  return request.get('/student/practice/config', { params: { sectionId } });
}

/**
 * 预习完成（「预习完成」按钮）：标记该小节/知识点预习完成（完成度 100%）并结算奖励
 * 后端接口: POST /api/student/preview/complete
 * @param {Object} data - { sectionId?, knowledgePointId?, repoId? }
 * @returns {Object} data: { ok, firstTime, coins, points }
 */
export async function completePreview(data) {
  return request.post('/student/preview/complete', data);
}

// ------------------------------------------------------------
// 学员端积分·学币·评价（统一账本）
// ------------------------------------------------------------

/** 个人中心档案（姓名/学号/头衔/累计积分/知识点/薄弱数） */
export async function getMyProfile() {
  return request.get('/student/profile');
}

/** 个人中心-积分板块（积分/头衔/下一目标/规则/最近明细） */
export async function getMyPoints() {
  return request.get('/student/points');
}

/** 本学期学习币（分学科 + 手动发币，单科上限 10000） */
export async function getMyCoins() {
  return request.get('/student/coins');
}

/** 每日签到状态（今日是否已签到 + 签到可得学习币） */
export async function getCheckin() {
  return request.get('/student/checkin');
}

/** 领取每日签到奖励（固定学习币，每自然日一次） */
export async function doCheckin() {
  return request.post('/student/checkin');
}

/** 学员端主页今日总览 + 积分获取引导 */
export async function getMyToday() {
  return request.get('/student/today');
}

/** 薄弱知识点列表（含掌握度与攻克奖励） */
export async function getWeakList() {
  return request.get('/student/weak/list');
}

/** 知识点详情（掌握度/评级/薄弱/预习状态） */
export async function getKnowledgeDetail(knowledgePointId) {
  return request.get('/student/knowledge/detail', { params: { knowledgePointId } });
}

/** 学习完成统一结算（预习/练习/试炼/错题订正等） */
export async function completeLearning(data) {
  return request.post('/student/learning/complete', data);
}

/** 错题重做（答对则订正移出错题本并结算奖励） */
export async function wrongRedo(data) {
  return request.post('/student/wrong/redo', data);
}

/** 薄弱知识点攻克（复测达标后消除薄弱并结算奖励） */
export async function weakConquer(data) {
  return request.post('/student/weak/conquer', data);
}

// ------------------------------------------------------------
// 学员实时位置（后台老师监控：学员在哪个页面/哪道习题）
// ------------------------------------------------------------

/** 学员端上报当前位置（路由变化/进入习题时调用，节流） */
export async function uploadActivity(data) {
  return request.post('/student/activity', data);
}

/** 后台学员实时位置列表（student:list 权限） */
export async function listActivities() {
  return request.get('/student/activities');
}

/** 学员督学在线列表（student:supervision 权限：仅最近 5 分钟活跃学员，含章节/小节与题目答案/解析） */
export async function listSupervision() {
  return request.get('/student/supervision/online-students');
}

/** 学员学习统计（首页真实化，基于练习记录聚合） */
export async function getStudentStats() {
  return request.get('/student/stats');
}

/** 老师给学员发放学币（student:update） */
export async function addCoin(data) {
  return request.post('/student/coin', data);
}
