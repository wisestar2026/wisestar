/**
 * 练习相关 API
 *
 * 功能:
 *   submitPractice     - 交卷落库（练习会话 + 逐题明细/错题标记）
 *   listWrongQuestions - 错题库管理（题目 × 学员聚合分页查询）
 */

import request from './request';

/**
 * 提交一次练习（交卷落库 + 错题标记）
 * 后端接口: POST /api/practice/submit
 * @param {Object} data - {
 *   mode: 'special' | 'exam' | 'random',   // 练习模式
 *   repoId: string,                        // 来源题库 ID（可空）
 *   durationMs: number,                    // 练习用时（毫秒）
 *   items: [{ questionId, answer }],       // 逐题作答（answer 为前端答案结构）
 * }
 * 调用方: PracticeSessionPage（交卷后自动提交）
 */
export async function submitPractice(data) {
  return request.post('/practice/submit', data);
}

/**
 * 分页查询错题库（题目 × 学员聚合）
 * 后端接口: GET /api/practice/wrong-list
 * @param {Object} params - {
 *   current, pageSize,
 *   repoId?: string,        // 题库筛选
 *   questionType?: string,  // 题型筛选（Radio/Checkbox/Judge/FillBlank/Textarea）
 *   keyword?: string,       // 题目标题 / 学员姓名模糊
 *   startTime?: string, endTime?: string,  // 做错时间范围（ISO 字符串）
 * }
 * @returns {Object} data: { list: [...错题聚合], total }
 *   错题项: { questionId, questionType, questionTitle, repoId, repoName,
 *             userId, userName, wrongCount, lastWrongTime, lastAnswer, lastScore }
 * 调用方: WrongQuestionPage（错题库管理）
 */
export async function listWrongQuestions(params) {
  return request.get('/practice/wrong-list', { params });
}
