/**
 * answer.js - 答卷管理 API
 *
 * 接口：
 *   GET  /api/answer/list?projectId=xxx   分页查询答卷列表
 *   GET  /api/answer?id=xxx               获取单条答卷详情
 *   POST /api/answer/delete               软删除答卷（移入回收站）
 *   POST /api/answer/destroy              物理删除答卷
 *   POST /api/answer/restore              从回收站恢复
 *
 * 调用方一览:
 *   - listAnswers  : ProjectAnswersPage（某问卷下的答卷列表）、AnswerListPage（全局答卷列表）
 *   - getAnswer    : ProjectAnswersPage（详情弹窗）、AnswerDetailPage（详情页）
 *   - deleteAnswer : ProjectAnswersPage、AnswerListPage（删除答卷）
 *   - destroyAnswer / restoreAnswer : 回收站管理功能，当前前端暂未接入（接口预留）
 *
 * 核心数据流:
 *   ProjectAnswersPage / AnswerListPage → listAnswers(params) → GET /api/answer/list
 *   返回 data: { list: [答卷对象], total: 总数 }
 *   答卷对象字段: { id, projectId, answer, tempSave(1=已完成/0=暂存), metaInfo, createAt }
 */

import request from './request';

/**
 * 分页查询答卷列表
 * 后端接口: GET /api/answer/list
 * @param {Object} params - 查询参数
 *   - projectId: 问卷 ID（可选，传入则只查该问卷的答卷）
 *   - current: 当前页码
 *   - pageSize: 每页条数
 *   - startTime / endTime: ISO 时间字符串（可选，按提交时间范围过滤，AnswerListPage 使用）
 * @returns {Object} data: { list: [...答卷对象], total: 总数 }
 * 调用方: ProjectAnswersPage.fetchAnswers、AnswerListPage.fetchAnswers
 */
export async function listAnswers(params) {
  return request.get('/answer/list', { params });
}

/**
 * 获取单条答卷详情
 * 后端接口: GET /api/answer?id=xxx
 * @param {string} id - 答卷 ID
 * @returns {Object} data: { id, projectId, survey(问卷JSON), answer(用户答案Map),
 *   tempSave, metaInfo: { clientInfo, answerInfo }, createAt }
 *   其中 answer 为 LinkedHashMap，key 为 questionId，value 为 { optionId: value } / { text } / { score }
 * 调用方: ProjectAnswersPage.handleViewDetail、AnswerDetailPage
 */
export async function getAnswer(id) {
  return request.get('/answer', { params: { id } });
}

/**
 * 软删除答卷（移入回收站，可恢复）
 * 后端接口: POST /api/answer/delete
 * @param {Object} data - { id: "答卷ID" }
 * 调用方: ProjectAnswersPage.handleDelete、AnswerListPage.handleDelete
 */
export async function deleteAnswer(data) {
  return request.post('/answer/delete', data);
}

/**
 * 物理删除答卷（不可恢复，回收站"彻底删除"）
 * 后端接口: POST /api/answer/destroy
 * @param {Object} data - { ids: ["id1","id2"] }
 * 调用方: 回收站管理（当前前端暂未接入，接口预留）
 */
export async function destroyAnswer(data) {
  return request.post('/answer/destroy', data);
}

/**
 * 从回收站恢复答卷
 * 后端接口: POST /api/answer/restore
 * @param {Object} data - { id: "答卷ID" }
 * 调用方: 回收站管理（当前前端暂未接入，接口预留）
 */
export async function restoreAnswer(data) {
  return request.post('/answer/restore', data);
}
