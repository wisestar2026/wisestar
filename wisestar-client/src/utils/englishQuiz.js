/**
 * englishQuiz.js - 英语单词选择题出题工具（纯函数）
 *
 * 三种题型（对齐需求文档 1.3.2）:
 *   meaning 看中文选英文：展示中文释义（有配图时一并展示作提示），
 *                        从 4 个英文拼写里选正确单词
 *   word    看英文选中文：展示英文单词，从 4 个中文释义里选正确含义
 *   listen  听英文选单词：只播英文语音不给文字题干，
 *                        从 4 个英文拼写里选正确单词
 *
 * 约定:
 *   - 每道题恰好 1 个正确选项 + 3 个同单元干扰项，4 个选项互不相同
 *   - 图片只作为「看中文选英文」的提示，不单独构成题型（旧 image 题型已并入 meaning）
 *   - 干扰项不足（同单元可用单词少于 4 个）时返回 null，由调用方跳过
 */

export const QUIZ_TYPE = {
  MEANING: 'meaning',
  WORD: 'word',
  LISTEN: 'listen',
};

export const QUIZ_TYPE_LABEL = {
  meaning: '看中文选英文',
  word: '看英文选中文',
  listen: '听英文选单词',
};

/** 题型对应的考察角度（与后端 EnglishExamAngle 编码一致） */
export const QUIZ_TYPE_ANGLE = {
  meaning: 'meaning-word',
  word: 'word-meaning',
  listen: 'word-listen',
};

/**
 * 考察角度 → 选择题题型。
 * `image-word` 是旧的「看图选单词」角度，配图已并入「看中文选英文」，
 * 这里保留映射让历史复习数据继续按 meaning 出题。
 */
export const ANGLE_TO_TYPE = {
  'image-word': QUIZ_TYPE.MEANING,
  'meaning-word': QUIZ_TYPE.MEANING,
  'word-meaning': QUIZ_TYPE.WORD,
  'word-listen': QUIZ_TYPE.LISTEN,
};

/** 轮转顺序（不指定题型时使用） */
const ROTATION = [QUIZ_TYPE.MEANING, QUIZ_TYPE.WORD, QUIZ_TYPE.LISTEN];

/** 单词缺图时统一使用的占位图 */
export const WORD_PLACEHOLDER = '/word-placeholder.svg';

/** 每个单词可选的题型（三种题型对全部单词都可用） */
export function availableTypes() {
  return [QUIZ_TYPE.MEANING, QUIZ_TYPE.WORD, QUIZ_TYPE.LISTEN];
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
 * @param {string} type 题型（meaning / word / listen）
 * @param {string} key 题目唯一键
 * @returns {object|null} 题目对象，无法出题时返回 null
 */
export function buildQuestion(word, pool, type, key) {
  if (!word || !ROTATION.includes(type)) {
    return null;
  }
  // 「看英文选中文」的答案是中文释义，另外两种题型都选英文拼写
  const answerIsMeaning = type === QUIZ_TYPE.WORD;
  const answerText = answerIsMeaning ? text(word.meaning) : text(word.spell);
  if (!answerText) {
    return null;
  }

  // 候选值：按题型取全部拼写或全部释义并去重
  const sourceKey = (item) => (answerIsMeaning ? text(item.meaning) : text(item.spell));
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
  if (type === QUIZ_TYPE.MEANING) {
    question.promptType = 'meaning';
    question.prompt = text(word.meaning);
  } else if (type === QUIZ_TYPE.LISTEN) {
    question.promptType = 'listen';
    question.prompt = '';
    question.autoPlay = true;
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
 * 为一组单元单词生成一轮题目。
 * @param {object[]} words 需要出题的单词列表
 * @param {object[]} [pool] 干扰项候选池（默认同 words），复习时传入整单元单词
 * @param {Object<string,string>} [angleByWord] 复习时每个单词推荐的考察角度，键为单词 ID
 * @param {string} [onlyType] 只出这一种题型（圆环直达入口使用）；为空时三种题型轮换
 * @returns {object[]} 题目数组
 */
export function buildQuiz(words, pool = words, angleByWord = null, onlyType = null) {
  const source = words || [];
  const candidates = pool || source;
  const questions = [];

  // 圆环直达：每个单词各出 1 道该题型，保证该题型覆盖全部单词
  if (onlyType && ROTATION.includes(onlyType)) {
    source.forEach((word, index) => {
      const question = buildQuestion(word, candidates, onlyType, `${index}-${word.id}`);
      if (question) {
        questions.push(question);
      }
    });
    return questions;
  }

  source.forEach((word, index) => {
    const types = availableTypes(word);
    // 复习优先用后端推荐的下一个未考角度，避免连续重刷同一考法
    const recommended = angleByWord ? ANGLE_TO_TYPE[angleByWord[word.id]] : null;
    let preferred = types[0];
    if (recommended && types.includes(recommended)) {
      preferred = recommended;
    } else {
      for (let k = 0; k < ROTATION.length; k += 1) {
        const candidate = ROTATION[(index + k) % ROTATION.length];
        if (types.includes(candidate)) {
          preferred = candidate;
          break;
        }
      }
    }
    const question = buildQuestionForWord(word, candidates, preferred, `${index}-${word.id}`);
    if (question) {
      questions.push(question);
    }
  });
  return questions;
}
