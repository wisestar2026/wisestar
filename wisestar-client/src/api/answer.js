/**
 * answer.js - 答卷管理 API
 *
 * 接口：
 *   GET  /api/answer/list?projectId=xxx   分页查询答卷列表
 *   GET  /api/answer?id=xxx               获取单条答卷详情
 *   POST /api/answer/delete               软删除答卷（移入回收站）
 *   POST /api/answer/destroy              物理删除答卷
 *   POST /api/answer/restore              从回收站恢复
 */

import request from './request';

/**
 * 分页查询答卷列表
 * @param {Object} params - { projectId, current, pageSize }
 */
export async function listAnswers(params) {
  return request.get('/answer/list', { params });
}

/**
 * 获取单条答卷详情
 * @param {string} id - 答卷 ID
 */
export async function getAnswer(id) {
  return request.get('/answer', { params: { id } });
}

/**
 * 软删除答卷
 * @param {Object} data - { id: "答卷ID" }
 */
export async function deleteAnswer(data) {
  return request.post('/answer/delete', data);
}

/**
 * 物理删除答卷
 * @param {Object} data - { ids: ["id1","id2"] }
 */
export async function destroyAnswer(data) {
  return request.post('/answer/destroy', data);
}

/**
 * 从回收站恢复答卷
 * @param {Object} data - { id: "答卷ID" }
 */
export async function restoreAnswer(data) {
  return request.post('/answer/restore', data);
}
