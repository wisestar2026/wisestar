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

/** 学员端学科列表（含该学科有权限的教材版本） */
export async function getStudySubjects() {
  return request.get('/student/study/subjects');
}

/** 学员端章节列表（?subjectId=） */
export async function getStudyChapters(subjectId) {
  return request.get('/student/study/chapters', { params: { subjectId } });
}

/** 学员端小节列表（?chapterId=） */
export async function getStudySections(chapterId) {
  return request.get('/student/study/sections', { params: { chapterId } });
}

/** 学员端知识点列表（?sectionId=） */
export async function getStudyPoints(sectionId) {
  return request.get('/student/study/points', { params: { sectionId } });
}

/**
 * 学员端练习/试炼题目（剥离标准答案）
 * @param {Object} params - { sectionId, knowledgePointId, count, types, difficulty }
 */
export async function getStudyQuestions(params) {
  return request.get('/student/study/questions', { params });
}
