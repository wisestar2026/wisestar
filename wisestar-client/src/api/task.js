/**
 * task.js - 今日任务 API
 *
 * 接口（后端 TaskApi，前缀 /api/task）:
 *   GET  /task/list?taskDate=&name=     后台任务列表（task:list）
 *   POST /task/create|update|delete     后台任务增删改（task:create/update/delete）
 *   GET  /task/student/tasks?taskDate=  学员端当日任务（含完成状态）
 *
 * 被谁引用: pages/system/TaskManagePage（后台布置）、
 *           pages/student/StudentHomePage（学员端今日任务）
 */

import request from './request';

/** 后台任务列表 */
export async function listTasks(params) {
  return request.get('/task/list', { params });
}

/** 新增任务：{ name, description, taskDate, contentType, contentId, status, sort } */
export async function createTask(data) {
  return request.post('/task/create', data);
}

/** 编辑任务 */
export async function updateTask(data) {
  return request.post('/task/update', data);
}

/** 删除任务 */
export async function deleteTask(data) {
  return request.post('/task/delete', data);
}

/** 学员端当日任务（含完成状态：当日交卷且正确率≥60%） */
export async function studentTasks(taskDate) {
  return request.get('/task/student/tasks', { params: taskDate ? { taskDate } : {} });
}
