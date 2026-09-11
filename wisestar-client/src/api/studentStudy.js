/**
 * studentStudy.js - 学员学习会话与总结 API
 *
 * 接口（后端 StudentStudyApi，前缀 /api）:
 *   POST /student/study/heartbeat          学习心跳（累积会话时长，满 60 分钟生成总结）
 *   GET  /student/study/summary            学员查看本人当日学习总结
 *   GET  /student/study/summary/student    教师查看指定学员总结（?studentId=&date=）
 *
 * 调用方:
 *   - StudentLayout       : 学员端统一心跳上报
 *   - StudentHomePage     : 学员端首页今日学习总结卡片
 *   - StudentSupervisionPage : 学员督学页教师查看学习总结
 */

import request from './request';

/**
 * 学习心跳（每 5 分钟及页面可见性变化时上报）
 * 后端接口: POST /api/student/study/heartbeat
 * @param {Object} data - { subjectId?: string }
 * @returns {Object} data: { durationMs, generated }
 */
export async function sendStudyHeartbeat(data) {
  return request.post('/student/study/heartbeat', data || {});
}

/**
 * 学员查看本人当日学习总结
 * 后端接口: GET /api/student/study/summary
 * @returns {Object} data: StudySummaryView | null
 */
export async function getMyStudySummary() {
  return request.get('/student/study/summary');
}

/**
 * 教师查看指定学员某日学习总结
 * 后端接口: GET /api/student/study/summary/student
 * @param {Object} params - { studentId, date? }
 * @returns {Object} data: StudySummaryView | null
 */
export async function getStudentStudySummary(params) {
  return request.get('/student/study/summary/student', { params });
}
