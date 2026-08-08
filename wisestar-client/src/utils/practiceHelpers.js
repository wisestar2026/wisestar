/**
 * practiceHelpers.js - 练习判分工具函数
 *
 * 功能:
 *   1. extractCorrectAnswers(question): 提取标准答案列表
 *   2. evaluateAnswer(question, userAnswer): 判定学生答案对错（与后端
 *      AnswerServiceImpl.evaluateQuestionCorrect 语义一致，纯前端即时判分用）
 *
 * 判分规则（与后端对齐）:
 *   - 标准答案来源优先级:
 *     1. 整题级: 题目 template.attribute.examCorrectAnswer（多选多个答案以 \n 分隔，如 "A\nB"）
 *     2. 选项级: 遍历子选项，收集 attribute.examCorrectAnswer 非空的选项标题
 *   - 学生答案:
 *     单选/判断 = 选中选项的 title；多选 = 选中选项 titles 数组；
 *     填空/文本 = 用户输入文本
 *   - 判定:
 *     多选题（Checkbox）: 学生答案集合与标准答案集合相等（与顺序无关）
 *     其他题型（单选/判断/填空）: 学生答案与任一标准答案 trim 后文本等值
 *   - 返回值: 1 正确 / 0 错误 / null 无标准答案（不计分）
 */

/**
 * 提取题目标准答案列表；无标准答案时返回 null。
 *
 * @param {Object} question 题目对象（TemplateView，含 template.attribute / template.children）
 * @returns {string[]|null} 标准答案字符串数组；无答案返回 null
 */
export function extractCorrectAnswers(question) {
  const attr = question?.template?.attribute;
  // 整题级答案（多选用 \n 分隔）
  if (attr?.examCorrectAnswer && String(attr.examCorrectAnswer).trim()) {
    return String(attr.examCorrectAnswer).split('\n').map((s) => s.trim()).filter(Boolean);
  }
  // 选项级答案（收集子选项中带 examCorrectAnswer 的标题）
  const correct = [];
  const children = question?.template?.children || [];
  for (const child of children) {
    if (child?.attribute?.examCorrectAnswer && String(child.attribute.examCorrectAnswer).trim()) {
      correct.push(child.title);
    }
  }
  return correct.length > 0 ? correct : null;
}

/**
 * 将选项 id 数组映射为选项标题数组（供多选判分与学生展示用）。
 *
 * @param {Object} question 题目对象
 * @param {string[]} optionIds 选中的选项 id 数组
 * @returns {string[]} 对应选项标题数组
 */
export function optionIdsToTitles(question, optionIds = []) {
  const children = question?.template?.children || [];
  return optionIds
    .map((oid) => children.find((c) => c.id === oid)?.title)
    .filter(Boolean);
}

/**
 * 判定题目对错（前端即时判分）。
 *
 * @param {Object} question 题目对象
 * @param {Object} userAnswer 学生答案:
 *   - 单选/判断: { type: 'option', optionId: 'opt_xxx' }
 *   - 多选:      { type: 'options', optionIds: ['opt_1','opt_2'] }
 *   - 填空/文本: { type: 'text', text: '用户输入' }
 * @returns {{correct: (1|0|null), correctAnswers: string[], userAnswerText: string}}
 *   - correct: 1 正确 / 0 错误 / null 无标准答案
 *   - correctAnswers: 标准答案数组（供展示"正确答案"）
 *   - userAnswerText: 学生答案的可读文本（供展示"我的答案"）
 */
export function evaluateAnswer(question, userAnswer) {
  const correctAnswers = extractCorrectAnswers(question);
  if (!correctAnswers) {
    // 无标准答案: 不判分，展示学生答案原文
    return { correct: null, correctAnswers: [], userAnswerText: '' };
  }

  // 把学生答案转成可比较/可展示的文本
  let studentValue = ''; // 用于文本等值比较的规范串
  let userAnswerText = ''; // 用于展示的文本
  const children = question?.template?.children || [];

  if (userAnswer?.type === 'option') {
    // 单选/判断: 取选中选项标题
    const opt = children.find((c) => c.id === userAnswer.optionId);
    studentValue = opt?.title || '';
    userAnswerText = opt?.title || '未作答';
  } else if (userAnswer?.type === 'options') {
    // 多选: 取选中选项标题集合
    const titles = optionIdsToTitles(question, userAnswer.optionIds);
    studentValue = titles.join(',');
    userAnswerText = titles.join('、') || '未作答';
  } else if (userAnswer?.type === 'text') {
    // 填空/文本: 直接取输入内容
    studentValue = String(userAnswer.text || '').trim();
    userAnswerText = studentValue || '未作答';
  }

  if (!studentValue.trim()) {
    return { correct: 0, correctAnswers, userAnswerText };
  }

  // 判定（与后端规则一致）
  if (question?.questionType === 'Checkbox') {
    // 多选: 集合相等（与顺序无关）
    const studentSet = new Set(studentValue.split(',').map((s) => s.trim()).filter(Boolean));
    const correctSet = new Set();
    correctAnswers.forEach((ca) => {
      String(ca).split(/[,\n]/).map((s) => s.trim()).filter(Boolean).forEach((s) => correctSet.add(s));
    });
    const correct = setsEqual(studentSet, correctSet) ? 1 : 0;
    return { correct, correctAnswers, userAnswerText };
  }

  // 其他: trim 文本等值（与任一标准答案相等即正确）
  const correct = correctAnswers.some(
    (ca) => String(ca).trim() === studentValue.trim(),
  ) ? 1 : 0;
  return { correct, correctAnswers, userAnswerText };
}

/**
 * 集合相等比较（忽略顺序与重复）。
 */
function setsEqual(a, b) {
  if (a.size !== b.size) return false;
  for (const v of a) {
    if (!b.has(v)) return false;
  }
  return true;
}

/**
 * 计算整卷得分。
 *
 * @param {Array<{question: Object, result: {correct: (1|0|null)}}>} items 每题判分结果
 * @returns {{score: number, totalScore: number, correctCount: number, wrongCount: number,
 *           unjudgedCount: number, accuracy: number}}
 *   - score: 得分（有分值字段按分值累加，无分值按题数累加）
 *   - totalScore: 满分（同样规则）
 *   - accuracy: 正确率（已判题中正确占比，无判题返回 0）
 */
export function calculateScore(items) {
  let score = 0;
  let totalScore = 0;
  let correctCount = 0;
  let wrongCount = 0;
  let unjudgedCount = 0;

  items.forEach(({ question, result }) => {
    // 分值: 题目 attribute.examScore，无则每题 1 分
    const point = Number(question?.template?.attribute?.examScore) || 1;
    totalScore += point;
    if (result.correct === null) {
      unjudgedCount += 1;
    } else if (result.correct === 1) {
      score += point;
      correctCount += 1;
    } else {
      wrongCount += 1;
    }
  });

  const judged = correctCount + wrongCount;
  const accuracy = judged > 0 ? Math.round((correctCount / judged) * 100) : 0;

  return {
    score: Math.round(score * 100) / 100,
    totalScore: Math.round(totalScore * 100) / 100,
    correctCount,
    wrongCount,
    unjudgedCount,
    accuracy,
  };
}
