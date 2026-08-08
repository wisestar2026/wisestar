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
 *   POST /api/repo/import         Excel 批量导入题目（multipart/form-data，原生 axios）
 *   GET  /api/repo/export         导出题库题目为 Excel（文件下载，通过 a 标签触发）
 *
 * 调用方一览:
 *   - listRepo         : RepoListPage（题库列表）、RepoDetailPage（单个题库信息）、
 *                        QuestionListPage（拉取全量题库供筛选下拉和编辑弹窗）
 *   - createRepo       : RepoListPage（新建题库）
 *   - updateRepo       : 题库编辑（当前前端暂未接入，接口预留）
 *   - deleteRepo       : RepoListPage（删除题库）
 *   - bindTemplate     : SelectTemplateModal（题库详情页「批量选择题目」）
 *   - unbindTemplate   : RepoDetailPage（题库详情页移除题目，解绑后题目保留在题目管理）
 *   - importTemplate   : ImportModal（Excel 批量导入题目弹窗）
 *   - exportTemplate   : QuestionListPage.handleExport（导出当前筛选结果）
 *
 * 核心数据流:
 *   QuestionListPage → exportTemplate({repoId}) → GET /api/repo/export?id=xxx → 浏览器下载 xlsx
 *   ImportModal → importTemplate({file, repoId}) → POST /api/repo/import (FormData) → 后端解析 Excel 入库
 */

import request from './request';
import axios from 'axios';

/**
 * 分页查询题库列表
 * 后端接口: GET /api/repo/list
 * @param {Object} params - { current, pageSize, name?: 名称模糊搜索, id?: 精确查找单个 }
 * @returns {Object} data: { list: [...题库对象], total: 总数 }
 *   题库对象: { id, name, mode(survey|exam), total(题目数), tag[], shared, description, createAt }
 * 调用方: RepoListPage、RepoDetailPage、QuestionListPage
 */
export async function listRepo(params) {
  return request.get('/repo/list', { params });
}

/**
 * 创建题库
 * 后端接口: POST /api/repo/create
 * @param {Object} data - { name, mode(survey|exam), description?, tag?, shared? }
 * 调用方: RepoListPage.handleCreate
 */
export async function createRepo(data) {
  return request.post('/repo/create', data);
}

/**
 * 更新题库
 * 后端接口: POST /api/repo/update
 * @param {Object} data - { id, name?, description?, tag?, shared? }
 * 调用方: 题库编辑功能（当前前端暂未接入，接口预留）
 */
export async function updateRepo(data) {
  return request.post('/repo/update', data);
}

/**
 * 删除题库（级联删除其中所有题目，谨慎操作）
 * 后端接口: POST /api/repo/delete
 * @param {Object} data - { id }
 * 调用方: RepoListPage.handleDelete
 */
export async function deleteRepo(data) {
  return request.post('/repo/delete', data);
}

/**
 * 批量绑定已有题目到题库（题库详情页「批量选择题目」用）
 * 后端接口: POST /api/repo/bind
 * @param {Object} data - { repoId: 目标题库 id, ids: 题目 ID 数组 }
 * 仅更新题目归属（repoId），不修改题目内容；已在目标题库的题目自动跳过（幂等）
 * 调用方: SelectTemplateModal.handleConfirm
 */
export async function bindTemplate(data) {
  return request.post('/repo/bind', data);
}

/**
 * 从题库移除题目（解绑，题目保留在题目管理全局库中，不删除模板本身）
 * 后端接口: POST /api/repo/unbind
 * @param {Object} data - { repoId: 当前题库 id, ids: 题目 ID 数组 }
 * 调用方: RepoDetailPage.handleRemoveTemplate / handleBatchRemove
 */
export async function unbindTemplate(data) {
  return request.post('/repo/unbind', data);
}

/**
 * Excel 批量导入题目到题库
 * 后端接口: POST /api/repo/import
 * 使用原生 axios（不走 request 拦截器），以 multipart/form-data 方式上传文件
 * 为什么这么写: request 拦截器只解包 JSON 响应，而文件上传需要 FormData + 二进制响应
 * @param {Object} params - { file: File, repoId: string }
 * @returns 后端解析 Excel 后返回导入结果
 * 调用方: ImportModal.handleImport
 */
export async function importTemplate({ file, repoId }) {
  const formData = new FormData();
  // 将用户选择的文件放入 FormData 的 file 字段
  formData.append('file', file);
  // repoId 可选：不传表示导入到系统全局题库（未绑定任何题库）
  if (repoId) formData.append('repoId', repoId);
  // 使用原生 axios 实例发送 multipart 请求（需要携带 cookie 维持登录态）
  return axios.post('/api/repo/import', formData, {
    withCredentials: true,
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

/**
 * 导出题库题目为 Excel 文件（触发浏览器下载）
 * 后端接口: GET /api/repo/export?id=repoId&name=xxx&questionType=xxx&subject=xxx&chapter=xxx&knowledgePoint=xxx&difficulty=xxx
 * 为什么这么写: 不使用 axios，而是创建隐藏 <a> 标签直接访问下载 URL，
 *   这样浏览器会原生处理二进制流下载，避免经过 JS 内存转换
 * @param {Object} params - { repoId, name, questionType, subject, chapter, knowledgePoint, difficulty }
 *   除 repoId（题库）外，其余为题目维度筛选条件（与题目管理页筛选栏一致，AND 关系）
 * 调用方: QuestionListPage.handleExport（导出当前筛选条件的题库题目）
 */
export async function exportTemplate({ repoId, name, questionType, subject, chapter, knowledgePoint, difficulty }) {
  const params = new URLSearchParams();
  if (repoId) params.append('id', repoId);
  if (name) params.append('name', name);
  if (questionType) params.append('questionType', questionType);
  if (subject) params.append('subject', subject);
  if (chapter) params.append('chapter', chapter);
  if (knowledgePoint) params.append('knowledgePoint', knowledgePoint);
  if (difficulty) params.append('difficulty', difficulty);
  // 通过创建隐藏的 a 标签触发浏览器下载
  const a = document.createElement('a');
  a.href = `/api/repo/export?${params.toString()}`;
  // 给下载文件命名（时间戳保证唯一，避免浏览器缓存同名文件）
  a.download = `questions_${Date.now()}.xlsx`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
}

/**
 * 学员端「我的题库」
 * 后端接口: GET /api/repo/my
 * @returns {Object} data: 我的题库列表（手动分配 ∪ 标签自动匹配）
 *   题库对象: { id, name, mode(survey|exam), total(题目数), tag[], description }
 * 调用方: PracticeHomePage（学员端选题页，只能选择题库不能勾单题）
 */
export async function myRepos() {
  return request.get('/repo/my');
}

/**
 * 老师手动分配题库给学员
 * 后端接口: POST /api/repo/assign
 * @param {Object} data - { userId, repoIds: [] }
 * 调用方: RepoAssignPage.handleAssign
 */
export async function assignRepo(data) {
  return request.post('/repo/assign', data);
}

/**
 * 删除分配记录（批量）
 * 后端接口: POST /api/repo/assign/delete
 * @param {Object} data - { ids: [] }
 * 调用方: RepoAssignPage.handleDelete
 */
export async function deleteAssign(data) {
  return request.post('/repo/assign/delete', data);
}

/**
 * 查询学员分配记录（管理端）
 * 后端接口: GET /api/repo/assign/list?userId=xx
 * @param {String} userId - 学员用户 ID（可选，为空查全部）
 * @returns {Object} data: 分配记录列表 [{ id, userId, userName, username, repoId, repoName, assignType, createAt }]
 * 调用方: RepoAssignPage
 */
export async function listAssign(userId) {
  return request.get('/repo/assign/list', { params: { userId } });
}

/**
 * 查询学员标签
 * 后端接口: GET /api/repo/user/tags?userId=xx
 * @param {String} userId - 学员用户 ID
 * @returns {Object} data: 学员标签数组
 * 调用方: RepoAssignPage（自动分配规则设置）
 */
export async function getUserTags(userId) {
  return request.get('/repo/user/tags', { params: { userId } });
}

/**
 * 保存学员标签（覆盖式，category=user）
 * 后端接口: POST /api/repo/user/tags
 * @param {Object} data - { userId, tags: [] }
 * 调用方: RepoAssignPage（按标签自动分配题库）
 */
export async function saveUserTags(data) {
  return request.post('/repo/user/tags', data);
}
