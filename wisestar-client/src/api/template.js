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
 *
 * 调用方一览:
 *   - listTemplate        : QuestionListPage（题目管理列表）、RepoDetailPage（题库内题目列表）、
 *                           TemplatePickerModal（从系统题目选择弹窗）
 *   - getTemplate         : 获取单题详情（当前前端暂未直接调用，接口预留）
 *   - createTemplate      : QuestionListPage.handleSave、RepoDetailPage.handleSave（新建题目）
 *   - updateTemplate      : QuestionListPage.handleSave、RepoDetailPage.handleSave（编辑题目）
 *   - deleteTemplate      : QuestionListPage（单个/批量删除）、RepoDetailPage（删除题目）
 *   - batchCreateTemplate : 批量创建（当前前端暂未接入，接口预留）
 *
 * 题目对象（TemplateView）字段:
 *   { id, name(题名), questionType(Radio|Checkbox|Judge|...),
 *     template(问卷JSON，含 children 选项 + attribute 属性),
 *     tag[], category, repoId, repoName, subject, chapter, knowledgePoint[], difficulty, mode }
 *   attribute 扩展字段（用于考试判分）:
 *     examCorrectAnswer(正确答案，多选用 \n 分隔), examAnalysis(解析),
 *     examScore(分值), examScoreMode(计分方式), examImages(图片URL数组),
 *     subject/chapter/knowledgePoint/difficulty(知识点属性快照)
 */

import request from './request';

/**
 * 分页查询题目列表
 * 后端接口: GET /api/template/list
 * @param {Object} params - 查询参数（均可选，多个条件为 AND 关系）:
 *   - current / pageSize: 分页
 *   - name: 题目名称模糊搜索
 *   - questionType: 按题型过滤（Radio/Checkbox/Judge 等）
 *   - repoId: 按所属题库过滤
 *   - subject / chapter / difficulty / knowledgePoint: 知识点属性四维筛选（QuestionListPage 使用）
 * @returns {Object} data: { list: [...TemplateView], total: 总数 }
 * 调用方: QuestionListPage.fetchData、RepoDetailPage.fetchTemplates、TemplatePickerModal.fetchData
 */
export async function listTemplate(params) {
  return request.get('/template/list', { params });
}

/**
 * 获取单个题目
 * 后端接口: GET /api/template/get
 * @param {Object} params - { id: "题目ID" }
 * @returns {Object} data: TemplateView
 * 调用方: 单题详情功能（当前前端暂未直接调用，接口预留）
 */
export async function getTemplate(params) {
  return request.get('/template/get', { params });
}

/**
 * 创建题目
 * 后端接口: POST /api/template/create
 * @param {Object} data - TemplateRequest:
 *   { name, questionType, template(问卷JSON), tag[], category?,
 *     subject?, chapter?, knowledgePoint[]?, difficulty?, repoId?, mode? }
 *   注意: 知识点属性同时存在顶层字段（存 t_template 表）和 template.attribute 内
 *   （快照，供题目转入问卷时随卷保存）
 * 调用方: QuestionListPage.handleSave、RepoDetailPage.handleSave
 */
export async function createTemplate(data) {
  return request.post('/template/create', data);
}

/**
 * 更新题目
 * 后端接口: POST /api/template/update
 * @param {Object} data - TemplateRequest 基础上带 id
 * 调用方: QuestionListPage.handleSave、RepoDetailPage.handleSave（编辑已有题目）
 */
export async function updateTemplate(data) {
  return request.post('/template/update', data);
}

/**
 * 删除题目（支持批量，data.ids 为题目 ID 数组）
 * 后端接口: POST /api/template/delete
 * @param {Object} data - { ids: ["id1", "id2"] }（单个删除也包装成数组）
 * 调用方: QuestionListPage（handleDelete 单删 / handleBatchDelete 批量）、RepoDetailPage.handleDeleteTemplate
 */
export async function deleteTemplate(data) {
  return request.post('/template/delete', data);
}

/**
 * 批量创建题目
 * 后端接口: POST /api/template/batchCreate
 * @param {Object} data - { list: [TemplateRequest, ...] }
 * 调用方: 批量建题功能（当前前端暂未接入，接口预留）
 */
export async function batchCreateTemplate(data) {
  return request.post('/template/batchCreate', data);
}
