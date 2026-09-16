/**
 * english.js - 英语学习公共工具（语音与分词）
 *
 * 说明：
 *   - speakEnglish：优先播放后端音频；缺失时回退浏览器语音合成（TTS）。
 *   - tokenizeSentence：英文句子按空白切分为词块，标点随词块保留作为排序提示。
 *   - normalizeAnswer：作答标准化（去首尾空白、忽略大小写、合并连续空格）。
 *   - shuffle：词块乱序（用于连词成句），保证与原顺序不同。
 */

/** 浏览器语音合成朗读英文 */
function speakByTTS(text) {
  if (typeof window === 'undefined' || !window.speechSynthesis) {
    return;
  }
  const utterance = new SpeechSynthesisUtterance(text);
  utterance.lang = 'en-US';
  utterance.rate = 0.9;
  window.speechSynthesis.cancel();
  window.speechSynthesis.speak(utterance);
}

/**
 * 朗读英文：优先使用音频 URL，缺失或播放失败时回退 TTS。
 * @param {string} text 英文文本
 * @param {string} [audioUrl] 音频地址
 */
export function speakEnglish(text, audioUrl) {
  if (!text) {
    return;
  }
  if (audioUrl) {
    const audio = new Audio(audioUrl);
    audio.play().catch(() => speakByTTS(text));
    return;
  }
  speakByTTS(text);
}

/**
 * 英文句子分词：按空白切分，标点保留在词块末尾。
 * @param {string} en 英文句子
 * @returns {string[]} 词块数组
 */
export function tokenizeSentence(en) {
  if (!en) {
    return [];
  }
  return en.trim().split(/\s+/).filter(Boolean);
}

/**
 * 标准化作答文本（去首尾空白、转小写、合并连续空格）。
 * @param {string} value 文本
 * @returns {string} 标准化结果
 */
export function normalizeAnswer(value) {
  return (value || '').trim().toLowerCase().replace(/\s+/g, ' ');
}

/**
 * Fisher-Yates 乱序；若结果与原顺序相同则重新洗牌（最多尝试 5 次）。
 * @param {string[]} tokens 原始词块
 * @returns {string[]} 乱序词块
 */
export function shuffle(tokens) {
  const source = [...tokens];
  if (source.length <= 1) {
    return source;
  }
  for (let attempt = 0; attempt < 5; attempt += 1) {
    const arr = [...source];
    for (let i = arr.length - 1; i > 0; i -= 1) {
      const j = Math.floor(Math.random() * (i + 1));
      [arr[i], arr[j]] = [arr[j], arr[i]];
    }
    if (arr.join(' ') !== source.join(' ')) {
      return arr;
    }
  }
  return [...source].reverse();
}
