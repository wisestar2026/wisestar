/**
 * mathPaper.js - 数学组卷（后台管理端）API
 *
 * 接口（后端 MathPaperApi，前缀 /api，权限 template:list）：
 *   GET  /math/paper/meta       组卷配置元数据（年级/册别/章节/小节/知识点 + 题型/难度题量）
 *   GET  /math/paper/students   按学号/姓名搜索学员
 *   POST /math/paper/generate   生成试卷（含标准答案与解析，供打印）
 *
 * 调用方：
 *   - MathPaperManagePage  习题管理 · 数学组卷
 */

import request from './request';

/** 组卷配置元数据 */
export function getMathPaperMeta() {
  return request.get('/math/paper/meta');
}

/** 按学号/姓名搜索学员（空关键字返回前 20 名） */
export function searchMathPaperStudents(keyword) {
  return request.get('/math/paper/students', { params: { keyword } });
}

/** 生成试卷 */
export function generateMathPaper(data) {
  return request.post('/math/paper/generate', data);
}
