/**
 * repo.js - 题库管理 API
 *
 * 接口:
 *   GET  /api/repo/list           分页查询题库列表
 *   POST /api/repo/create         创建题库
 *   POST /api/repo/update         更新题库
 *   POST /api/repo/delete         删除题库（级联删除题目）
 *   POST /api/repo/batchCreate    批量添加题目到题库
 *   POST /api/repo/unbind         从题库移除题目
 *   POST /api/repo/import         Excel 批量导入题目（multipart/form-data）
 *   GET  /api/repo/export         导出题库题目为 Excel（文件下载）
 */

import request from './request';
import axios from 'axios';

/** 分页查询题库列表 */
export async function listRepo(params) {
  return request.get('/repo/list', { params });
}

/** 创建题库 */
export async function createRepo(data) {
  return request.post('/repo/create', data);
}

/** 更新题库 */
export async function updateRepo(data) {
  return request.post('/repo/update', data);
}

/** 删除题库 */
export async function deleteRepo(data) {
  return request.post('/repo/delete', data);
}

/** 批量添加题目到题库 */
export async function batchAddTemplate(data) {
  return request.post('/repo/batchCreate', data);
}

/** 从题库移除题目 */
export async function unbindTemplate(data) {
  return request.post('/repo/unbind', data);
}

/**
 * Excel 批量导入题目到题库
 * 使用原生 axios（不走 request 拦截器），以 multipart/form-data 方式上传文件
 * @param {Object} params - { file: File, repoId: string }
 */
export async function importTemplate({ file, repoId }) {
  const formData = new FormData();
  formData.append('file', file);
  if (repoId) formData.append('repoId', repoId);
  // 使用原生 axios 实例发送 multipart 请求（需要携带 cookie 维持登录态）
  return axios.post('/api/repo/import', formData, {
    withCredentials: true,
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

/**
 * 导出题库题目为 Excel 文件（触发浏览器下载）
 * @param {Object} params - { repoId: string }
 */
export async function exportTemplate({ repoId }) {
  const params = new URLSearchParams();
  if (repoId) params.append('id', repoId);
  // 通过创建隐藏的 a 标签触发浏览器下载
  const a = document.createElement('a');
  a.href = `/api/repo/export?${params.toString()}`;
  a.download = `questions_${Date.now()}.xlsx`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
}
