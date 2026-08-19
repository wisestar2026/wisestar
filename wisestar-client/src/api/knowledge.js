/**
 * knowledge.js - 知识管理板块 API（学科/章节/小节/知识点）
 *
 * 接口（后端 KnowledgePointApi 等 4 个 Controller，前缀 /api）:
 *   学科:   GET  /subject/list         学科列表（含章节数）
 *           POST /subject/create|update|delete
 *   章节:   GET  /chapter/list         章节列表（?subjectId=，含小节数/题库数）
 *           POST /chapter/create|update|delete
 *           GET  /chapter/repos?chapterId=    已绑定题库列表
 *           POST /chapter/repos               保存题库绑定（全量替换）
 *   小节:   GET  /section/list         小节列表（?chapterId=，含知识点数/题库数）
 *           POST /section/create|update|delete
 *           GET  /section/repos?sectionId=   已绑定题库列表
 *           POST /section/repos              保存题库绑定（全量替换）
 *   知识点: GET  /knowledge-point/list          知识点分页（?subjectId=&chapterId=&sectionId=）
 *           POST /knowledge-point/create|update|delete
 *           GET  /knowledge-point/questions?knowledgePointId=  已绑定题目列表
 *           POST /knowledge-point/questions                    保存题目绑定（全量替换）
 *
 * 数据层级: 学科 → 章节 → 小节 → 知识点
 * 题目来源: 章节/小节绑定题库（t_repo，/api/repo/list）；知识点绑定题目来自题目库（t_template，
 *           /api/template/list），不能在此新增
 *
 * 调用方:
 *   - ChapterManagePage       : listSubjects / listChapters + 章节 CRUD
 *                               + 章节题库绑定 + 章节小节查看（数据来自小节管理）
 *   - SectionManagePage       : listSubjects / listChapters / listSections + 小节 CRUD
 *                               + 小节题库绑定 + 小节知识点查看
 *   - KnowledgePointManagePage: 三级下拉 + 知识点分页/CRUD + 题目绑定
 */

import request from './request';
import axios from 'axios';

// ============================================================
// 学科
// ============================================================

/**
 * 学科列表（含各学科章节数 chapterCount）
 * 后端接口: GET /api/subject/list
 * @returns {Object} data: [SubjectView, ...]  { id, name, code, icon, themeColor, sort, chapterCount }
 * 调用方: 三个知识管理页顶部学科下拉
 */
export async function listSubjects() {
  return request.get('/subject/list');
}

/**
 * 新增学科
 * 后端接口: POST /api/subject/create
 * @param {Object} data - { name, code?, icon?, themeColor?, sort? }
 */
export async function createSubject(data) {
  return request.post('/subject/create', data);
}

/**
 * 更新学科
 * 后端接口: POST /api/subject/update
 * @param {Object} data - { id, name, code?, icon?, themeColor?, sort? }
 */
export async function updateSubject(data) {
  return request.post('/subject/update', data);
}

/**
 * 删除学科（逻辑删除）
 * 后端接口: POST /api/subject/delete
 * @param {Object} data - { id }
 */
export async function deleteSubject(data) {
  return request.post('/subject/delete', data);
}

// ============================================================
// 章节
// ============================================================

/**
 * 章节列表（按学科过滤，含小节数 sectionCount 与题库数 repoCount）
 * 后端接口: GET /api/chapter/list
 * @param {Object} params - { subjectId? }
 * @returns {Object} data: [ChapterView, ...]
 *   { id, subjectId, name, icon, sort, sectionCount, repoCount }
 * 调用方: ChapterManagePage 列表、SectionManagePage 章节下拉
 */
export async function listChapters(params) {
  return request.get('/chapter/list', { params });
}

/**
 * 新增章节
 * 后端接口: POST /api/chapter/create
 * @param {Object} data - { subjectId, name, icon?, sort? }
 */
export async function createChapter(data) {
  return request.post('/chapter/create', data);
}

/**
 * 更新章节
 * 后端接口: POST /api/chapter/update
 * @param {Object} data - { id, subjectId?, name?, icon?, sort? }
 */
export async function updateChapter(data) {
  return request.post('/chapter/update', data);
}

/**
 * 删除章节（级联逻辑删除其下小节/知识点/题目绑定/题库绑定）
 * 后端接口: POST /api/chapter/delete
 * @param {Object} data - { id }
 */
export async function deleteChapter(data) {
  return request.post('/chapter/delete', data);
}

/**
 * 保存章节-题库绑定（全量替换：传完整 repoIds，先清空旧绑定再写入）
 * 后端接口: POST /api/chapter/repos
 * @param {Object} data - { chapterId, repoIds: [题库ID] }
 * 调用方: ChapterManagePage 绑定题库弹窗保存
 */
export async function saveChapterRepos(data) {
  return request.post('/chapter/repos', data);
}

/**
 * 查询章节已绑定的题库列表（保持绑定顺序）
 * 后端接口: GET /api/chapter/repos
 * @param {String} chapterId - 章节ID
 * @returns {Object} data: [RepoView, ...]  { id, name, description, tag, subject, grade, difficulty, ... }
 * 调用方: ChapterManagePage 绑定题库弹窗回显
 */
export async function listChapterRepos(chapterId) {
  return request.get('/chapter/repos', { params: { chapterId } });
}

// ============================================================
// 小节
// ============================================================

/**
 * 小节列表（按章节过滤，含知识点数 knowledgePointCount 与题库数 repoCount）
 * 后端接口: GET /api/section/list
 * @param {Object} params - { chapterId? }
 * @returns {Object} data: [SectionView, ...]
 *   { id, chapterId, name, sort, content(JSON串), practice(JSON串),
 *     knowledgePointCount, repoCount }
 * 调用方: SectionManagePage 列表、KnowledgePointManagePage 小节下拉
 */
export async function listSections(params) {
  return request.get('/section/list', { params });
}

/**
 * 新增小节
 * 后端接口: POST /api/section/create
 * @param {Object} data - { chapterId, name, sort?, content?, practice? }
 *   content/practice 为 JSON 字符串（内容设置/练习设置，前端 JSON.stringify 后提交）
 */
export async function createSection(data) {
  return request.post('/section/create', data);
}

/**
 * 更新小节
 * 后端接口: POST /api/section/update
 * @param {Object} data - { id, chapterId?, name?, sort?, content?, practice? }
 */
export async function updateSection(data) {
  return request.post('/section/update', data);
}

/**
 * 删除小节（级联逻辑删除其下知识点/题目绑定/题库绑定）
 * 后端接口: POST /api/section/delete
 * @param {Object} data - { id }
 */
export async function deleteSection(data) {
  return request.post('/section/delete', data);
}

/**
 * 保存小节-题库绑定（全量替换：传完整 repoIds，先清空旧绑定再写入）
 * 后端接口: POST /api/section/repos
 * @param {Object} data - { sectionId, repoIds: [题库ID] }
 * 调用方: SectionManagePage 绑定题库弹窗保存
 */
export async function saveSectionRepos(data) {
  return request.post('/section/repos', data);
}

/**
 * 查询小节已绑定的题库列表（保持绑定顺序）
 * 后端接口: GET /api/section/repos
 * @param {String} sectionId - 小节ID
 * @returns {Object} data: [RepoView, ...]  { id, name, description, tag, subject, grade, difficulty, ... }
 * 调用方: SectionManagePage 绑定题库弹窗回显
 */
export async function listSectionRepos(sectionId) {
  return request.get('/section/repos', { params: { sectionId } });
}

// ============================================================
// 知识点
// ============================================================

/**
 * 知识点分页列表（三级下拉筛选，条件均可选；都不传 → 全量分页）
 * 后端接口: GET /api/knowledge-point/list
 * @param {Object} params - { current, pageSize, subjectId?, chapterId?, sectionId? }
 * @returns {Object} data: { total, list: [KnowledgePointView, ...] }
 *   { id, sectionId, name, sort, content(JSON串), imageUrl,
 *     subjectName, chapterName, sectionName, questionCount }
 * 调用方: KnowledgePointManagePage 列表
 */
export async function listKnowledgePoints(params) {
  return request.get('/knowledge-point/list', { params });
}

/**
 * 新增知识点
 * 后端接口: POST /api/knowledge-point/create
 * @param {Object} data - { sectionId, name, sort?, content?, imageUrl? }
 *   content 为 JSON 字符串（讲解要点，前端 JSON.stringify 后提交）
 */
export async function createKnowledgePoint(data) {
  return request.post('/knowledge-point/create', data);
}

/**
 * 更新知识点
 * 后端接口: POST /api/knowledge-point/update
 * @param {Object} data - { id, sectionId?, name?, sort?, content?, imageUrl? }
 */
export async function updateKnowledgePoint(data) {
  return request.post('/knowledge-point/update', data);
}

/**
 * 删除知识点（连带逻辑删除其题目绑定）
 * 后端接口: POST /api/knowledge-point/delete
 * @param {Object} data - { id }
 */
export async function deleteKnowledgePoint(data) {
  return request.post('/knowledge-point/delete', data);
}

/**
 * 保存知识点-题目绑定（全量替换：传完整 questionIds，先清空旧绑定再写入）
 * 后端接口: POST /api/knowledge-point/questions
 * @param {Object} data - { knowledgePointId, questionIds: [题目ID] }
 * 调用方: KnowledgePointManagePage 绑定题目弹窗保存
 */
export async function saveKnowledgePointQuestions(data) {
  return request.post('/knowledge-point/questions', data);
}

/**
 * 查询知识点已绑定的题目列表（保持绑定顺序）
 * 后端接口: GET /api/knowledge-point/questions
 * @param {String} knowledgePointId - 知识点ID
 * @returns {Object} data: [TemplateView, ...]  { id, name, questionType, repoName, ... }
 * 调用方: KnowledgePointManagePage 绑定题目弹窗回显
 */
export async function listKnowledgePointQuestions(knowledgePointId) {
  return request.get('/knowledge-point/questions', { params: { knowledgePointId } });
}

// ------------------------------------------------------------
// 知识批量导入（Excel，multipart）
// ------------------------------------------------------------

/** 通用 multipart 导入请求（原生 axios，不经过 JSON 拦截器） */
async function importExcel(url, formData) {
  const response = await axios.post(url, formData, {
    withCredentials: true,
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  if (response.data?.code === 200) {
    return response.data;
  }
  throw new Error(response.data?.message || '导入失败');
}

/**
 * 批量导入章节（Excel 列：学科名/章节名/图标(选填)/排序(选填)，首行表头跳过）
 * 后端接口: POST /api/chapter/import（按学科+章节名去重，归属由学科名列匹配）
 * @param {File} file - Excel 文件
 * @returns {Object} data: { imported, skipped }
 */
export async function importChapters(file) {
  const formData = new FormData();
  formData.append('file', file);
  return importExcel('/api/chapter/import', formData);
}

/**
 * 批量导入小节（Excel 列：学科名/章节名/小节名/排序(选填)，首行表头跳过）
 * 后端接口: POST /api/section/import（按学科+章节名定位归属，章节内重名跳过）
 * @param {File} file - Excel 文件
 * @returns {Object} data: { imported, skipped }
 */
export async function importSections(file) {
  const formData = new FormData();
  formData.append('file', file);
  return importExcel('/api/section/import', formData);
}

/**
 * 批量导入知识点（Excel 列：学科名/章节名/小节名/知识点名/排序(选填)，首行表头跳过）
 * 后端接口: POST /api/knowledge-point/import（按学科+章节+小节名定位归属，小节内重名跳过）
 * @param {File} file - Excel 文件
 * @returns {Object} data: { imported, skipped }
 */
export async function importKnowledgePoints(file) {
  const formData = new FormData();
  formData.append('file', file);
  return importExcel('/api/knowledge-point/import', formData);
}
