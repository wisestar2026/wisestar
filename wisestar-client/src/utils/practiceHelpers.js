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
 *     填空类（FillBlank/MultipleBlank）: 标准答案以 | 分隔多空时逐空判分（每空独立
 *       归一化比较，答对 n 空累加 n 空得分，部分给分），单空按整体文本比较
 *     其他题型（单选/判断/文本）: 学生答案与任一标准答案归一化后文本等值
 *   - 比较归一化: trim、全角空格/字母/数字/符号（含 ＜＞）转半角、连续空白折叠
 *   - 返回值: 1 正确 / 0 错误 / null 无标准答案（不计分）；多空题附 blankHits/earnedScore
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
 * 答案归一化（填空/文本等值比较前对两边统一处理，避免全角/空格类格式差异误判）:
 *   1. 全角空格/不间断空格/零宽字符等不可见空白 → 普通空格或移除；
 *   2. 全角 ASCII（字母/数字/符号，含 ＜＞＝ 等）→ 半角；
 *   3. 连续空白折叠为单个空格并去首尾。
 *
 * @param {*} s 待归一化文本
 * @returns {string} 归一化后的比较串
 */
export function normalizeBlankText(s) {
  return String(s == null ? '' : s)
    .replace(/[\u00A0\u1680\u2000-\u200A\u202F\u205F\u3000\uFEFF]/g, ' ')
    .replace(/[\u200B\u200C\u200D]/g, '')
    .replace(/[\uFF01-\uFF5E]/g, (ch) => String.fromCharCode(ch.charCodeAt(0) - 0xFEE0))
    .replace(/\s+/g, ' ')
    .trim();
}

/**
 * 填空空位等值比较（两边先归一化；纯 ASCII 字母串如选项字母 A/B/C 忽略大小写）。
 */
export function blankEquals(a, b) {
  const na = normalizeBlankText(a);
  const nb = normalizeBlankText(b);
  if (na === nb) return true;
  if (/^[A-Za-z]+$/.test(na) && /^[A-Za-z]+$/.test(nb)) {
    return na.toLowerCase() === nb.toLowerCase();
  }
  return false;
}

/**
 * 填空类题目（单项填空/多项填空）逐空判分。
 *
 * <p>仅当题型为 FillBlank/MultipleBlank 且标准答案含 {@code |} 多空分隔时生效；
 * 单空/文本题返回 null，走整体等值比较。</p>
 *
 * @param {Object} question 题目对象（questionType + template.attribute/children）
 * @param {Object} userAnswer 学生答案 { type: 'text', text: '空1|空2|…' }
 * @returns {null|{blankTotal:number, blankHits:number[], allCorrect:boolean,
 *                 earnedScore:number, maxScore:number}}
 *   - blankHits[i] = 1 表示第 i+1 空命中（含未作答的全 0）
 *   - earnedScore: 按 attribute.examBlankScores（缺省整题分均摊）累加正确空位
 */
export function judgeBlankAnswers(question, userAnswer) {
  const qtype = question?.questionType;
  if (qtype !== 'MultipleBlank' && qtype !== 'FillBlank') return null;
  const correctAnswers = extractCorrectAnswers(question);
  if (!correctAnswers) return null;
  const joined = correctAnswers.find((ca) => String(ca || '').includes('|'));
  if (!joined) return null;

  const stdBlanks = String(joined).split('|');
  const total = stdBlanks.length;
  const studentText = userAnswer?.type === 'text'
    ? String(userAnswer.text == null ? '' : userAnswer.text) : '';
  const stuBlanks = studentText.trim() ? String(studentText).split('|') : [];

  const hits = Array(total).fill(0);
  if (stuBlanks.length === total) {
    // 空位数一致: 逐空等值比较（每空独立 trim/归一化，规避整串比较因单空尾随空格全判错）
    for (let i = 0; i < total; i++) {
      if (blankEquals(stdBlanks[i], stuBlanks[i])) hits[i] = 1;
    }
  } else if (studentText.trim()) {
    // 空位数不一致（如把整串答案粘进单框/漏填空位）：整串等值兜底视为全对
    if (correctAnswers.some((ca) => blankEquals(ca, studentText))) hits.fill(1);
  }

  const attr = question?.template?.attribute || {};
  const point = Number(attr.examScore) || 1;
  const blankScores = attr.examBlankScores;
  const perBlank = Array.isArray(blankScores) && blankScores.length === total
    ? blankScores.map((v) => Number(v) || 0)
    : Array(total).fill(Math.round((point / total) * 100) / 100);
  let earned = 0;
  hits.forEach((h, i) => {
    if (h === 1) earned += perBlank[i];
  });
  return {
    blankTotal: total,
    blankHits: hits,
    allCorrect: hits.every((h) => h === 1),
    earnedScore: Math.round(Math.min(earned, point) * 100) / 100,
    maxScore: Math.round(point * 100) / 100,
  };
}

/**
 * 判定题目对错（前端即时判分，语义与后端 AnswerJudgeUtil 一致）。
 *
 * @param {Object} question 题目对象
 * @param {Object} userAnswer 学生答案:
 *   - 单选/判断: { type: 'option', optionId: 'opt_xxx' }
 *   - 多选:      { type: 'options', optionIds: ['opt_1','opt_2'] }
 *   - 填空/文本: { type: 'text', text: '用户输入' }
 * @returns {{correct: (1|0|null), correctAnswers: string[], userAnswerText: string,
 *            blankTotal?: number, blankHits?: number[], earnedScore?: number}}
 *   - correct: 1 全对 / 0 非全对 / null 无标准答案（多空题答对部分空时 correct=0，
 *     但 earnedScore 反映部分得分、blankHits 反映逐空命中）
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

  if (!studentValue) {
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

  if (question?.questionType === 'MultipleBlank' || question?.questionType === 'FillBlank') {
    // 填空: 标准答案含多空分隔（|）时逐空判分（部分给分 + 每空命中），单空整体比较
    const blankResult = judgeBlankAnswers(question, userAnswer);
    if (blankResult) {
      return {
        correct: blankResult.allCorrect ? 1 : 0,
        correctAnswers,
        userAnswerText,
        blankTotal: blankResult.blankTotal,
        blankHits: blankResult.blankHits,
        earnedScore: blankResult.earnedScore,
        maxScore: blankResult.maxScore,
      };
    }
  }

  // 单选/判断/单空填空/文本: 归一化后文本等值（与任一标准答案相等即正确）
  const correct = correctAnswers.some(
    (ca) => blankEquals(ca, studentValue),
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
 * 格式化标准答案用于展示。
 * 多项填空（MultipleBlank）标准答案以 | 分隔多空（如 "m|a"），拆成 "空1: m；空2: a"；
 * 其余题型直接将答案数组 join 为顿号分隔文本。
 *
 * @param {string} qtype 题型（Radio/Checkbox/MultipleBlank 等）
 * @param {string[]} answers 标准答案数组
 * @returns {string} 展示文本
 */
export function formatCorrectAnswers(qtype, answers) {
  if (!answers || answers.length === 0) return '';
  if (qtype === 'MultipleBlank') {
    return answers
      .flatMap((a) => String(a || '').split('|'))
      .map((p, i) => `空${i + 1}: ${p}`)
      .join('；');
  }
  return answers.join('、');
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
      // 多空填空答对部分空: earnedScore 为部分得分（无则 0），仍计入答错题数
      score += Number(result.earnedScore) || 0;
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
