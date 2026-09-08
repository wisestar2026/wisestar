/**
 * 练习相关 API
 *
 * 功能:
 *   submitPractice      - 交卷落库（练习会话 + 逐题明细/错题标记）
 *   listWrongQuestions  - 错题库管理（题目 × 学员聚合分页查询）
 *   saveWrongReason     - 保存单个错题错误归因
 *   saveWrongReasons    - 交卷后批量保存逐题错误归因
 *   getPracticeMastery  - 掌握度汇总（小节/知识点范围二选一）
 */

import request from './request';

/**
 * 提交一次练习（交卷落库 + 错题标记）
 * 后端接口: POST /api/practice/submit
 * @param {Object} data - {
 *   mode: 'special' | 'exam' | 'random',   // 练习模式
 *   repoId: string,                        // 来源题库 ID（可空）
 *   sectionId?: string,                    // 所属小节 ID（归因/掌握度统计）
 *   knowledgePointId?: string,             // 所属知识点 ID（归因/掌握度统计）
 *   durationMs: number,                    // 练习用时（毫秒）
 *   items: [{ questionId, answer }],       // 逐题作答（answer 为前端答案结构）
 * }
 * @returns {Object} data: PracticeResultView {
 *   practiceId, mode, totalCount, correctCount, score,
 *   items: [{ detailId, questionId, isCorrect, correct, correctAnswer }],  // 判分后明细
 *   wrongItems: [...]                       // 错题明细（含未作答）
 * }
 * 调用方: PracticeSessionPage（末题提交后自动调用，随后逐题归因）
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
 *   userId?: string,        // 学员筛选（学员端错题本必传当前学员）
 *   subjectId?: string,     // 研习学科筛选（与章节链路的学科口径一致）
 *   grade?: string,         // 年级筛选（按所属章节年级）
 *   chapterId?: string,     // 章节筛选
 *   sectionId?: string,     // 小节筛选
 *   knowledgePointId?: string, // 知识点筛选
 *   wrongReason?: string,   // 错因筛选
 *   keyword?: string,       // 题目标题 / 学员姓名模糊
 *   startTime?: string, endTime?: string,  // 做错时间范围（ISO 字符串）
 * }
 * @returns {Object} data: { list: [...错题聚合], total }
 *   错题项: { questionId, questionType, questionTitle, repoId, repoName,
 *             userId, userName, wrongCount, lastWrongTime, lastAnswer, lastScore,
 *             wrongReason, knowledgePointId, knowledgePointName,
 *             subjectId, subjectName, chapterId, chapterName, sectionId, sectionName, grade }
 * 调用方: WrongBookPage（学员端错题本）、WrongQuestionPage（错题库管理）
 */
export async function listWrongQuestions(params) {
  return request.get('/practice/wrong-list', { params });
}

/**
 * 保存单个错题错误归因（学员标注）
 * 后端接口: POST /api/practice/wrongReason
 * @param {Object} data - { detailId: string, reason: string }
 */
export async function saveWrongReason(data) {
  return request.post('/practice/wrongReason', data);
}

/**
 * 批量保存错题错误归因（交卷后强制逐题归因，一次性提交）
 * 后端接口: POST /api/practice/wrongReasons
 * @param {Object} data - { items: [{ detailId, reason }] }
 * 调用方: PracticeSessionPage（归因完成后进入结果页）
 */
export async function saveWrongReasons(data) {
  return request.post('/practice/wrongReasons', data);
}

/**
 * 练习掌握度汇总（小节/知识点范围二选一；全部历史统计）
 * 后端接口: GET /api/practice/mastery
 * @param {Object} params - { sectionId?: string, knowledgePointId?: string }
 * @returns {Object} data: PracticeMasteryView {
 *   scopeType: 'section'|'knowledgePoint', scopeId, scopeName,
 *   practiceCount, questionCount, rightCount, accuracy, avgScore,
 *   lastRecord: { recordId, mode, score, totalScore, rate, totalQuestions,
 *                 correctCount, durationMs, createAt },
 *   kps: [{ knowledgePointId, knowledgePointName, attempts, right, accuracy, lastPracticedAt }]
 * }
 * 调用方: StudyPage（上次掌握情况卡）、PracticeResultPage（掌握变化）
 */
export async function getPracticeMastery(params) {
  return request.get('/practice/mastery', { params });
}

/**
 * 范围内近期练习记录列表（掌握变化对比；最多 20 条倒序）
 * 后端接口: GET /api/practice/history
 * @param {Object} params - { sectionId?: string, knowledgePointId?: string }
 * @returns {Object} data: [...PracticeMasteryView.lastRecord]（第 0 条=最近一次）
 * 调用方: KnowledgePage 交卷结果页（本次 vs 上次掌握变化）
 */
export async function getPracticeHistory(params) {
  return request.get('/practice/history', { params });
}
