/**
 * growth.js - 成长档案 API（学习轨迹 + 成长对比 + 成长报告）
 *
 * 学员端（后端 GrowthArchiveApi，前缀 /api）：
 *   GET  /student/growth/timeline   学习轨迹时间轴
 *   GET  /student/growth/compare    成长对比（基线 vs 当前）
 *   GET  /student/growth/report     读取成长报告
 *   POST /student/growth/report/generate  生成/重新生成成长报告
 *
 * 管理端（后端 ArchiveApi）：
 *   GET  /student/archive/detect-history       检测历史
 *   GET  /student/archive/timeline             学习轨迹
 *   GET  /student/archive/compare              成长对比
 *   GET  /student/archive/growth/report        读取成长报告
 *   POST /student/archive/growth/report/generate  生成成长报告
 */

import request from './request';

/** 学员本人：学习轨迹时间轴 */
export function getGrowthTimeline(params) {
  return request.get('/student/growth/timeline', { params });
}

/** 学员本人：成长对比 */
export function getGrowthCompare(params) {
  return request.get('/student/growth/compare', { params });
}

/** 学员本人：读取成长报告 */
export function getGrowthReport(params) {
  return request.get('/student/growth/report', { params });
}

/** 学员本人：生成/重新生成成长报告 */
export function generateGrowthReport(data) {
  return request.post('/student/growth/report/generate', data);
}

/** 管理端：检测历史 */
export function getArchiveDetectHistory(params) {
  return request.get('/student/archive/detect-history', { params });
}

/** 管理端：学习轨迹 */
export function getArchiveTimeline(params) {
  return request.get('/student/archive/timeline', { params });
}

/** 管理端：成长对比 */
export function getArchiveCompare(params) {
  return request.get('/student/archive/compare', { params });
}

/** 管理端：读取成长报告 */
export function getArchiveGrowthReport(params) {
  return request.get('/student/archive/growth/report', { params });
}

/** 管理端：生成成长报告 */
export function generateArchiveGrowthReport(data) {
  return request.post('/student/archive/growth/report/generate', data);
}
