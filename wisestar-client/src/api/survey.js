/**
 * survey.js - 公开问卷 API（无需登录即可调用）
 *
 * 接口：
 *   POST /api/public/loadProject     加载问卷内容（含状态校验、密码校验）
 *   POST /api/public/saveAnswer      提交/暂存问卷答案
 *   POST /api/public/statistics      获取单选题/多选题投票统计
 *   POST /api/public/tempSaveAnswer  暂存答案（按项目维度保存临时草稿）
 *
 * 调用方一览:
 *   - loadProject     : SurveyViewPage（公开问卷填写页，加载问卷）
 *   - saveAnswer      : SurveyViewPage（提交答案，tempSave=1 表示已完成）
 *   - tempSaveAnswer  : 草稿暂存功能（当前前端暂未接入，接口预留）
 *   - getStatistics   : 投票统计页（当前前端暂未接入，接口预留）
 *
 * 核心数据流:
 *   SurveyViewPage → loadProject({id}) → POST /api/public/loadProject
 *   返回 data: { survey(问卷JSON), setting, name, status, passwordRequired }
 *   SurveyViewPage → saveAnswer({projectId, answer, tempSave:1}) → POST /api/public/saveAnswer
 *   返回 data: { answerId, examScore, ... }
 */

import request from './request';

/**
 * 加载问卷
 * 后端接口: POST /api/public/loadProject
 * @param {Object} data - { id: "项目ID", password?: "密码" }
 *   password 用于设置了访问密码的问卷，未设置可不传
 * @returns {Object} - { data: { survey, setting, name, status, passwordRequired, ... } }
 *   status: 问卷状态（如 'closed' 表示已关闭，SurveyViewPage 据此拦截）
 * 调用方: SurveyViewPage
 */
export async function loadProject(data) {
  return request.post('/public/loadProject', data);
}

/**
 * 提交/保存问卷答案
 * 后端接口: POST /api/public/saveAnswer
 * @param {Object} data - { projectId, answer: { questionId: { optionId: value } }, tempSave: 0|1 }
 *   answer 结构: key 为问题 ID，value 为:
 *     - 选择题: { optionId: optionId }（多选则为 { opt1: opt1, opt2: opt2 }）
 *     - 填空题/文本: { text: "用户输入" }
 *     - 评分题: { score: 4 }
 *   tempSave: 1=正式提交(已完成)，0=暂存(草稿)
 * @returns {Object} - { data: { answerId, examScore, ... } }
 * 调用方: SurveyViewPage.handleSubmit
 */
export async function saveAnswer(data) {
  return request.post('/public/saveAnswer', data);
}

/**
 * 暂存答案
 * 后端接口: POST /api/public/tempSaveAnswer
 * @param {Object} data - { projectId, tempAnswer: {...}, tempSave: 0 }
 * 调用方: 草稿暂存功能（当前前端暂未接入，接口预留）
 */
export async function tempSaveAnswer(data) {
  return request.post('/public/tempSaveAnswer', data);
}

/**
 * 获取投票统计
 * 后端接口: POST /api/public/statistics
 * @param {Object} data - { id: "项目ID" }
 * @returns {Object} - { data: { count, questionStatistics: [...] } }
 *   统计每个单选题/多选题的选项被选次数
 * 调用方: 投票统计页面（当前前端暂未接入，接口预留）
 */
export async function getStatistics(data) {
  return request.post('/public/statistics', data);
}
