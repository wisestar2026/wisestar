/**
 * studentTask.js - 学员任务发布 API（学管师后台发布 + 学员端展示）
 *
 * 接口（后端 StudentTaskApi，前缀 /api）:
 *   POST /student/task/publish  发布任务（多学员 × 最多 3 条纯文本内容，每人每日最多 3 条）
 *   GET  /student/task/page     任务发布记录分页（?current=&pageSize=&studentId=&content=&status=）
 *   POST /student/task/delete   删除（撤回）已发布任务（?id=）
 *   GET  /student/task/my       当前登录学员当日任务（学员端，按发布时间升序）
 *
 * 调用方:
 *   - TaskAssignmentPage  : 学员管理 → 任务发布
 *   - StudentHomePage     : 学员端首页今日任务卡片
 */

import request from './request';

/**
 * 发布任务（可一次选择多个学员，每个学员各收到相同的多条文本任务）
 * 后端接口: POST /api/student/task/publish
 * @param {Object} data - { studentIds: string[], contents: string[] }
 * @returns {Object} data: 实际发布条数
 */
export async function publishStudentTask(data) {
  return request.post('/student/task/publish', data);
}

/**
 * 任务发布记录分页
 * 后端接口: GET /api/student/task/page
 * @param {Object} params - { current, pageSize, studentId?, content?, status? }
 * @returns {Object} data: { total, list: [StudentTaskView, ...] }
 */
export async function pageStudentTasks(params) {
  return request.get('/student/task/page', { params });
}

/**
 * 删除（撤回）已发布任务
 * 后端接口: POST /api/student/task/delete
 * @param {string} id - 任务 ID
 */
export async function deleteStudentTask(id) {
  return request.post('/student/task/delete', null, { params: { id } });
}

/**
 * 当前登录学员当日任务（学员端首页卡片展示）
 * 后端接口: GET /api/student/task/my
 * @returns {Object} data: [StudentTaskView, ...]
 */
export async function listMyStudentTasks() {
  return request.get('/student/task/my');
}
