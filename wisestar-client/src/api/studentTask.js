/**
 * studentTask.js - 学员任务发布 API（学管师后台发布 + 学员端展示）
 *
 * 接口（后端 StudentTaskApi，前缀 /api）:
 *   POST /student/task/publish  发布任务（多学员 × 一条纯文本内容）
 *   GET  /student/task/page     任务发布记录分页（?current=&pageSize=&studentId=&content=&status=）
 *   POST /student/task/delete   删除（撤回）已发布任务（?id=）
 *   GET  /student/task/my       当前登录学员收到的全部任务（学员端）
 *
 * 调用方:
 *   - TaskAssignmentPage  : 学员管理 → 任务发布
 *   - StudentHomePage     : 学员端首页右下角任务 Label
 */

import request from './request';

/**
 * 发布任务（可一次选择多个学员，每个学员各收到一条相同文本任务）
 * 后端接口: POST /api/student/task/publish
 * @param {Object} data - { studentIds: string[], content: string }
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
 * 当前登录学员收到的全部任务（学员端首页右下角展示）
 * 后端接口: GET /api/student/task/my
 * @returns {Object} data: [StudentTaskView, ...]
 */
export async function listMyStudentTasks() {
  return request.get('/student/task/my');
}
