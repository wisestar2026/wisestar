/**
 * handRaise.js - 学员举手反馈 API
 *
 * 接口（后端 HandRaiseApi，前缀 /api/hand-raise）:
 *   POST /hand-raise/create        学员题目页举手提交（求助讲解 / 题干有误 / 答案有误）
 *   GET  /hand-raise/list          教师端举手提醒分页（?raiseType=&status=&subject=&keyword=）
 *   POST /hand-raise/handle        标记已处理 / 已忽略
 *   GET  /hand-raise/pending-count 待处理数量（侧边菜单角标）
 *
 * 调用方:
 *   - components/student/HandRaiseButton 学员题目页悬浮举手按钮
 *   - pages/student/HandRaisePage        教师端举手提醒页
 *   - components/layout/MainLayout       菜单角标轮询
 */

import request from './request';

/**
 * 学员提交举手反馈
 * 后端接口: POST /api/hand-raise/create
 * @param {Object} data - {
 *   raiseType, questionId, questionType, subject, chapter, knowledgePoint, difficulty,
 *   questionContent, optionsJson, studentAnswer, studentOptionIds, page, sectionId, remark
 * }
 */
export async function createHandRaise(data) {
  return request.post('/hand-raise/create', data);
}

/**
 * 举手反馈分页列表
 * 后端接口: GET /api/hand-raise/list
 * @param {Object} params - { current, pageSize, raiseType?, status?, subject?, keyword?, studentId? }
 */
export async function listHandRaises(params) {
  return request.get('/hand-raise/list', { params });
}

/**
 * 处理举手反馈
 * 后端接口: POST /api/hand-raise/handle
 * @param {Object} data - { id, status(2已处理/3已忽略), handleNote? }
 */
export async function handleHandRaise(data) {
  return request.post('/hand-raise/handle', data);
}

/**
 * 待处理数量
 * 后端接口: GET /api/hand-raise/pending-count
 * @returns {Object} data: { explain, error, total }
 */
export async function getHandRaisePendingCount() {
  return request.get('/hand-raise/pending-count');
}
