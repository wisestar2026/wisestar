/**
 * detect.js - 学员端「知识点检测」API
 *
 * 接口（后端 DetectionApi，前缀 /api）：
 *   GET  /student/detect/units     可选单元列表（subjectId/grade/term，带可用题量）
 *   POST /student/detect/generate  自动组卷（剥离标准答案与解析）
 *   POST /student/detect/submit    交卷并生成薄弱点诊断报告
 *
 * 定位：检测为诊断性质，后端不发放学习币/积分，也不写入练习记录。
 *
 * 调用方：
 *   - KnowledgeDetectPage   学员端知识点检测页（配置 → 作答 → 诊断报告）
 */

import request from './request';

/** 可选单元列表（按学科 + 年级 + 册别过滤，带可用题量） */
export function getDetectUnits(params) {
  return request.get('/student/detect/units', { params });
}

/** 自动组卷（剥离标准答案与解析，防作弊） */
export function generateDetectPaper(data) {
  return request.post('/student/detect/generate', data);
}

/** 交卷并生成薄弱点诊断报告 */
export function submitDetectPaper(data) {
  return request.post('/student/detect/submit', data);
}

/** 检测历史（按时间倒序，含学前检测/阶段检测标识） */
export function getDetectHistory(params) {
  return request.get('/student/detect/history', { params });
}

/**
 * 按单元名取该单元题目（复用检测组卷接口，返回剥离答案的题目）。
 *
 * 用途：英语单元重点语法/句型页展示该单元绑定题目，以及英语学习中心「单元练习」入口。
 * 实现：先按 学科+年级+册别 取单元列表，匹配同名单元拿到章节 ID，再组卷。
 *
 * @param {Object} params - { subjectId, grade, term, unit, count? }
 * @returns {Promise<Array>} 题目列表（无标准答案）
 */
export async function getUnitQuestions({ subjectId, grade, term, unit, count = 20 }) {
  const unitsRes = await getDetectUnits({ subjectId, grade, term });
  const target = (unitsRes?.data || []).find((u) => u.name === unit);
  if (!target) return [];
  const genRes = await generateDetectPaper({
    subjectId,
    grade,
    term,
    chapterIds: [target.id],
    questionCount: count,
  });
  return genRes?.data || [];
}
