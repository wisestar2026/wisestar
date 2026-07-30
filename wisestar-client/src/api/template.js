/**
 * template.js - 题目模板 API
 *
 * 接口:
 *   GET  /api/template/list        分页查询题目列表
 *   GET  /api/template/get         获取单个题目
 *   POST /api/template/create      创建题目
 *   POST /api/template/update      更新题目
 *   POST /api/template/delete      删除题目（支持批量，传 ids 数组）
 *   POST /api/template/batchCreate 批量创建题目
 */

import request from './request';

/** 分页查询题目列表 */
export async function listTemplate(params) {
  return request.get('/template/list', { params });
}

/** 获取单个题目 */
export async function getTemplate(params) {
  return request.get('/template/get', { params });
}

/** 创建题目 */
export async function createTemplate(data) {
  return request.post('/template/create', data);
}

/** 更新题目 */
export async function updateTemplate(data) {
  return request.post('/template/update', data);
}

/** 删除题目（支持批量，data.ids 为题目 ID 数组） */
export async function deleteTemplate(data) {
  return request.post('/template/delete', data);
}

/** 批量创建题目 */
export async function batchCreateTemplate(data) {
  return request.post('/template/batchCreate', data);
}
