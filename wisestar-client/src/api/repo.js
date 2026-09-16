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
 *   GET  /api/repo/import/template 下载空白导入模板（22 列标准单表，仅表头+填写说明）
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
 *   - downloadImportTemplate : ImportModal（下载空白导入模板）
 *   - exportTemplate   : QuestionListPage.handleExport（导出当前筛选结果）
 *
 * 核心数据流:
 *   QuestionListPage → exportTemplate({repoId}) → GET /api/repo/export?id=xxx → 浏览器下载 xlsx
 *   ImportModal → importTemplate({file, repoId}) → POST /api/repo/import (FormData) → 后端解析 Excel 入库
 *   ImportModal → downloadImportTemplate() → GET /api/repo/import/template → 浏览器下载空白模板
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
  // 注意: 后端业务错误（如行级校验失败）以 HTTP 200 + {code: 非200, message} 返回，
  // axios 不会 reject，必须在响应里主动检查 code，否则调用方会误判导入成功
  const res = await axios.post('/api/repo/import', formData, {
    withCredentials: true,
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  const body = res?.data;
  if (body && body.code !== undefined && body.code !== 200) {
    throw new Error(body.message || '导入失败，请检查文件格式是否正确');
  }
  return res;
}

/**
 * 下载空白 Excel 导入模板（浏览器下载）
 * 后端接口: GET /api/repo/import/template
 * 为什么这么写: 与 exportTemplate 相同，使用隐藏 <a> 标签触发浏览器原生下载，
 *   避免经过 JS 二进制处理；同源请求自动携带登录 cookie
 * 模板格式: 标准单表 29 列（学科/题型/章节/小节/知识点/题目/选项A~H/难易程度/正确答案1~12/解析/标签），
 *   第一个工作表为表头 + 示例说明，第二个工作表为填写说明（导入时仅解析第一个工作表）
 * 调用方: ImportModal.handleDownloadTemplate
 */
export async function downloadImportTemplate() {
  const a = document.createElement('a');
  a.href = '/api/repo/import/template';
  a.download = 'question_import_template.xlsx';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
}

/**
 * 导出题库题目为 Excel 文件（触发浏览器下载）
 * 后端接口: GET /api/repo/export?id=repoId&name=xxx&questionType=xxx&subject=xxx&grade=xxx&chapter=xxx&section=xxx&knowledgePoint=xxx&difficulty=xxx&tag=xxx
 * 为什么这么写: 不使用 axios，而是创建隐藏 <a> 标签直接访问下载 URL，
 *   这样浏览器会原生处理二进制流下载，避免经过 JS 内存转换
 * @param {Object} params - { repoId, name, questionType, subject, grade, chapter, section, knowledgePoint, difficulty, tag }
 *   除 repoId（题库）外，其余为题目维度筛选条件（与题目管理页筛选栏一致，AND 关系）
 * 调用方: QuestionListPage.handleExport（导出当前筛选条件的题库题目）
 */
export async function exportTemplate({ repoId, name, questionType, subject, grade, chapter, section, knowledgePoint, difficulty, tag }) {
  const params = new URLSearchParams();
  if (repoId) params.append('id', repoId);
  if (name) params.append('name', name);
  if (questionType) params.append('questionType', questionType);
  if (subject) params.append('subject', subject);
  if (grade) params.append('grade', grade);
  if (chapter) params.append('chapter', chapter);
  if (section) params.append('section', section);
  if (knowledgePoint) params.append('knowledgePoint', knowledgePoint);
  if (difficulty) params.append('difficulty', difficulty);
  if (tag) params.append('tag', tag);
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
 * 练习绑定位置反查（练习详情回显知识投放范围）
 * 后端接口: GET /api/repo/locations?repoId=
 * @param {String} repoId - 练习 ID
 * @returns {Object} data: { bindings: [{ nodeType: CHAP|SECTION, nodeId, nodeName,
 *           parentNodeId, parentNodeName, subjectId, grade, term, version }] }
 * 调用方: RepoDetailPage（练习投放位置回显）
 */
export async function listRepoLocations(repoId) {
  return request.get('/repo/locations', { params: { repoId } });
}

/**
 * 节点刷题内容预览（原习题列表页遗留接口：按知识节点取学员端同语义题目，已不再由前端调用）
 * 后端接口: GET /api/repo/node/questions?nodeType=&nodeId=&withAnswer=
 * @param {Object} params - { nodeType: chapter|section|knowledgePoint|repo, nodeId, withAnswer? }
 * @returns {Object} data: [{ id, name, questionType, tag, difficulty, schema }]
 *   schema: { title, attribute:{examCorrectAnswer,...}, children:[{id,title,...}] }
 * 调用方: 已废弃页面 ExerciseListPage（保留兼容）
 */
export async function listNodeQuestions(params) {
  return request.get('/repo/node/questions', { params });
}
