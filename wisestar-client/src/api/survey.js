/**
 * survey.js - 公开问卷 API（无需登录即可调用）
 *
 * 接口：
 *   POST /api/public/loadProject     加载问卷内容（含状态校验、密码校验）
 *   POST /api/public/saveAnswer      提交/暂存问卷答案
 *   POST /api/public/statistics      获取单选题/多选题投票统计
 */

import request from './request';

/**
 * 加载问卷
 * @param {Object} data - { id: "项目ID", password?: "密码" }
 * @returns {Object} - { data: { survey, setting, name, status, passwordRequired, ... } }
 */
export async function loadProject(data) {
  return request.post('/public/loadProject', data);
}

/**
 * 提交/保存问卷答案
 * @param {Object} data - { projectId, answer: { questionId: { optionId: value } }, tempSave: 0|1 }
 * @returns {Object} - { data: { answerId, examScore, ... } }
 */
export async function saveAnswer(data) {
  return request.post('/public/saveAnswer', data);
}

/**
 * 暂存答案
 * @param {Object} data - { projectId, tempAnswer: {...}, tempSave: 0 }
 */
export async function tempSaveAnswer(data) {
  return request.post('/public/tempSaveAnswer', data);
}

/**
 * 获取投票统计
 * @param {Object} data - { id: "项目ID" }
 * @returns {Object} - { data: { count, questionStatistics: [...] } }
 */
export async function getStatistics(data) {
  return request.post('/public/statistics', data);
}
