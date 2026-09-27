/**
 * englishQuiz.js - 英语单词选择题出题工具（纯函数）
 *
 * 三种题型:
 *   image   看图选单词：展示配图，从 4 个英文单词里选正确拼写
 *   meaning 看中文选单词：展示中文释义，从 4 个英文单词里选正确拼写
 *   word    看单词选中文：展示英文单词，从 4 个中文释义里选正确含义
 *
 * 约定:
 *   - 每道题恰好 1 个正确选项 + 3 个同单元干扰项，4 个选项互不相同
 *   - 缺图单词不出「看图选单词」题
 *   - 干扰项不足（同单元可用单词少于 4 个）时返回 null，由调用方跳过
 */

export const QUIZ_TYPE = {
  IMAGE: 'image',
  MEANING: 'meaning',
  WORD: 'word',
};

export const QUIZ_TYPE_LABEL = {
  image: '看图选单词',
  meaning: '看中文选单词',
  word: '看单词选中文',
};

/** 单词缺图时统一使用的占位图 */
export const WORD_PLACEHOLDER = '/word-placeholder.svg';

/** 每个单词可选的题型（有图才出看图题） */
export function availableTypes(word) {
  return word && word.imageUrl
    ? [QUIZ_TYPE.IMAGE, QUIZ_TYPE.MEANING, QUIZ_TYPE.WORD]
    : [QUIZ_TYPE.MEANING, QUIZ_TYPE.WORD];
}

function text(value) {
  return (value == null ? '' : String(value)).trim();
}

/** Fisher-Yates 洗牌（返回新数组） */
function shuffled(list) {
  const arr = [...list];
  for (let i = arr.length - 1; i > 0; i -= 1) {
    const j = Math.floor(Math.random() * (i + 1));
    [arr[i], arr[j]] = [arr[j], arr[i]];
  }
  return arr;
}

/**
 * 从候选池中取 n 个与答案不同的值（去重），不足则返回 null。
 * @param {string} answer 正确答案文本
 * @param {string[]} values 候选值（已去重）
 * @param {number} n 需要的干扰项数量
 */
function pickDistractors(answer, values, n) {
  const candidates = values.filter((v) => v && v !== answer);
  if (candidates.length < n) {
    return null;
  }
  return shuffled(candidates).slice(0, n);
}

/**
 * 组装一道题。
 * @param {object} word 目标单词
 * @param {object[]} pool 同单元单词池
 * @param {string} type 题型
 * @param {string} key 题目唯一键
 * @returns {object|null} 题目对象，无法出题时返回 null
 */
export function buildQuestion(word, pool, type, key) {
  if (!word || !availableTypes(word).includes(type)) {
    return null;
  }
  const answerText = type === QUIZ_TYPE.WORD ? text(word.meaning) : text(word.spell);
  if (!answerText) {
    return null;
  }

  // 候选值：按题型取全部拼写或全部释义并去重
  const sourceKey = (item) => (type === QUIZ_TYPE.WORD ? text(item.meaning) : text(item.spell));
  const values = [];
  const seen = new Set();
  [word, ...(pool || [])].forEach((item) => {
    const value = sourceKey(item);
    if (value && !seen.has(value)) {
      seen.add(value);
      values.push(value);
    }
  });

  const distractors = pickDistractors(answerText, values, 3);
  if (!distractors) {
    return null;
  }

  const options = shuffled([answerText, ...distractors]).map((label) => ({
    key: label,
    label,
    correct: label === answerText,
  }));

  const question = {
    key,
    wordId: word.id,
    word,
    type,
    typeLabel: QUIZ_TYPE_LABEL[type],
    imageUrl: text(word.imageUrl),
    audioUrl: word.audioUrl,
    options,
    answerText,
  };
  if (type === QUIZ_TYPE.IMAGE) {
    question.promptType = 'image';
    question.prompt = '';
  } else if (type === QUIZ_TYPE.MEANING) {
    question.promptType = 'meaning';
    question.prompt = text(word.meaning);
  } else {
    question.promptType = 'word';
    question.prompt = text(word.spell);
    question.phonetic = word.phonetic;
  }
  return question;
}

/** 为单词挑一个可出题的题型（优先给定题型，失败后按可用题型兜底） */
export function buildQuestionForWord(word, pool, preferredType, key) {
  const types = availableTypes(word);
  const ordered = preferredType && types.includes(preferredType)
    ? [preferredType, ...types.filter((t) => t !== preferredType)]
    : types;
  for (const type of ordered) {
    const question = buildQuestion(word, pool, type, key);
    if (question) {
      return question;
    }
  }
  return null;
}

/**
 * 为一组单元单词生成一轮题目（题型在可用题型间轮换）。
 * @param {object[]} words 单元单词列表
 * @returns {object[]} 题目数组
 */
export function buildQuiz(words) {
  const pool = words || [];
  const rotation = [QUIZ_TYPE.IMAGE, QUIZ_TYPE.MEANING, QUIZ_TYPE.WORD];
  const questions = [];
  pool.forEach((word, index) => {
    const types = availableTypes(word);
    let preferred = types[0];
    for (let k = 0; k < rotation.length; k += 1) {
      const candidate = rotation[(index + k) % rotation.length];
      if (types.includes(candidate)) {
        preferred = candidate;
        break;
      }
    }
    const question = buildQuestionForWord(word, pool, preferred, `${index}-${word.id}`);
    if (question) {
      questions.push(question);
    }
  });
  return questions;
}
