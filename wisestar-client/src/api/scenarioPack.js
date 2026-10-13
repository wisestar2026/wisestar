/**
 * scenarioPack.js - 英语单元情景练习固化 API（后台管理端）
 *
 * 后端接口（EnglishScenarioPackApi，前缀 /api，权限 english:word:ai）：
 *   POST /english/scenario-pack/generate?version=&grade=&term=&unit=  生成预览（分组调 AI，不落库）
 *   POST /english/scenario-pack/save    body {version,grade,term,unit,wordContent,patternContent,...}  覆盖保存
 *   GET  /english/scenario-pack/list?version=&grade=&term=&unit=&current=&pageSize=  分页列表（不含正文）
 *   GET  /english/scenario-pack/detail?id=  详情（含正文）
 *   POST /english/scenario-pack/delete?id=  删除
 *   GET  /english/scenario-pack/units?version=&grade=&term=  该册可用单元（批量用）
 *
 * 生成类请求单独放大超时：一次生成包含多组 AI 调用，默认 30s 不够。
 */

import request from './request';

/** 生成单元情景内容包预览（不落库） */
export function generateScenarioPack(params) {
  return request.post('/english/scenario-pack/generate', null, { params, timeout: 900000 });
}

/** 保存（覆盖）单元情景内容包 */
export function saveScenarioPack(data) {
  return request.post('/english/scenario-pack/save', data, { timeout: 60000 });
}

/** 情景内容包分页列表（不含正文） */
export function listScenarioPacks(params) {
  return request.get('/english/scenario-pack/list', { params });
}

/** 情景内容包详情（含正文） */
export function getScenarioPackDetail(id) {
  return request.get('/english/scenario-pack/detail', { params: { id } });
}

/** 删除情景内容包 */
export function deleteScenarioPack(id) {
  return request.post('/english/scenario-pack/delete', null, { params: { id } });
}

/** 该册可用单元列表（用于批量生成） */
export function listScenarioPackUnits(params) {
  return request.get('/english/scenario-pack/units', { params });
}
