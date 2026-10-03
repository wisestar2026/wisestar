/**
 * archive.js - 学员档案 API（学习规划 / 上课记录 / 学期报告）
 *
 * 接口（后端 ArchiveApi，前缀 /api/student/archive）:
 *   GET  /detail?studentId=&semester=   档案详情（初始快照/薄弱点/上课记录/当日情况）
 *   GET  /overview?studentId=           档案概览（列表入口角标）
 *   POST /save                          保存档案（目标规划表/承诺书/报告/状态）
 *   GET  /draft?studentId=&date=        上课记录自动草稿（拉当日学习数据）
 *   POST /record/save                   保存上课记录
 *   POST /record/delete?id=             删除上课记录
 *   POST /report/generate?studentId=    生成学期报告
 *   GET  /my                            学员端我的档案
 *
 * 调用方: StudentArchivePage（管理端，student:archive[:edit]）
 *         StudentMyArchivePage（学员端只读）
 */

import request from './request';

/** 档案详情 */
export async function getArchiveDetail(studentId, semester) {
  return request.get('/student/archive/detail', { params: { studentId, semester } });
}

/** 档案概览（学员列表入口角标） */
export async function getArchiveOverview(studentId) {
  return request.get('/student/archive/overview', { params: { studentId } });
}

/** 保存档案（目标规划表/承诺书/学期报告/状态） */
export async function saveArchive(data) {
  return request.post('/student/archive/save', data);
}

/** 上课记录自动草稿（拉取当日学习数据/薄弱/强化知识点） */
export async function getArchiveDraft(studentId, date) {
  return request.get('/student/archive/draft', { params: { studentId, date } });
}

/** 保存上课记录（新增/更新） */
export async function saveArchiveRecord(data) {
  return request.post('/student/archive/record/save', data);
}

/** 删除上课记录 */
export async function deleteArchiveRecord(id) {
  return request.post('/student/archive/record/delete', null, { params: { id } });
}

/** 生成学期报告（AI 可用时润色，否则规则模板） */
export async function generateArchiveReport(studentId, semester) {
  return request.post('/student/archive/report/generate', null, { params: { studentId, semester } });
}

/** 学员端我的档案（只读） */
export async function getMyArchive() {
  return request.get('/student/archive/my');
}
