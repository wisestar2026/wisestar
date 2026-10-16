/**
 * english.js - 英语学习公共工具（语音与分词）
 *
 * 说明：
 *   - speakEnglish：优先播放后端音频；缺失时回退浏览器语音合成（TTS）。
 *   - tokenizeSentence：英文句子按空白切分为词块，标点随词块保留作为排序提示。
 *   - normalizeAnswer：作答标准化（去首尾空白、忽略大小写、合并连续空格）。
 *   - shuffle：词块乱序（用于连词成句），保证与原顺序不同。
 */

// 英文语音缓存：浏览器语音列表是异步加载的（Chrome 首次 getVoices() 可能为空），
// 缓存后避免每次朗读都重新遍历，并在 voiceschanged 后刷新
let englishVoice = null;
let voicesBound = false;
let warmedUp = false;

/**
 * 从浏览器语音列表中挑选最合适的英文音色。
 * 优先「本地合成」音色（localService=true），避免选中需要联网下载的音色导致首播卡顿。
 */
function pickEnglishVoice() {
  const voices = window.speechSynthesis.getVoices() || [];
  const en = voices.filter((v) => /^en([-_]|$)/i.test(v.lang));
  if (!en.length) return null;
  const local = en.filter((v) => v.localService);
  const pool = local.length ? local : en;
  return (
    pool.find((v) => /^en[-_]US/i.test(v.lang))
    || pool.find((v) => /^en[-_]GB/i.test(v.lang))
    || pool[0]
    || null
  );
}

/** 监听语音列表异步加载完成，刷新英文音色缓存 */
function ensureVoiceListener() {
  if (voicesBound) return;
  voicesBound = true;
  const synth = window.speechSynthesis;
  if (synth && typeof synth.addEventListener === 'function') {
    synth.addEventListener('voiceschanged', () => { englishVoice = pickEnglishVoice(); });
  }
}

/**
 * 预热语音引擎：模块加载即触发音色列表加载；首次用户交互时静音播报一次，
 * 消除首次 speak() 的引擎初始化延迟（这也是「点了半天才朗读」的主因之一）。
 */
export function warmupSpeechSynthesis() {
  if (typeof window === 'undefined') return;
  const synth = window.speechSynthesis;
  if (!synth) return;
  ensureVoiceListener();
  if (!englishVoice) englishVoice = pickEnglishVoice();
  if (warmedUp) return;
  warmedUp = true;
  try {
    const warm = new SpeechSynthesisUtterance(' ');
    warm.volume = 0;
    warm.lang = 'en-US';
    if (englishVoice) warm.voice = englishVoice;
    synth.speak(warm);
  } catch {
    // 预热失败不影响正常朗读
  }
}

// 模块加载即预热音色列表，并在首次用户交互时静音预热引擎
if (typeof window !== 'undefined' && window.speechSynthesis) {
  window.speechSynthesis.getVoices();
  ensureVoiceListener();
  if (typeof window.addEventListener === 'function') {
    window.addEventListener('pointerdown', warmupSpeechSynthesis, { once: true, passive: true });
  }
}

/** 浏览器语音合成朗读英文（无需音频文件；若设备无英文音色则静默） */
function speakByTTS(text) {
  if (typeof window === 'undefined' || !window.speechSynthesis || !text) {
    return;
  }
  const synth = window.speechSynthesis;
  ensureVoiceListener();
  if (!englishVoice) englishVoice = pickEnglishVoice();
  // Chrome 空闲一段时间后会「卡住」，resume 唤醒后再播
  if (synth.paused) synth.resume();
  // 仅在确实在播/排队时才打断，避免每次点击都重置队列带来的额外延迟
  if (synth.speaking || synth.pending) synth.cancel();
  const utterance = new SpeechSynthesisUtterance(String(text));
  utterance.lang = 'en-US';
  utterance.rate = 0.9;
  // 显式指定英文音色，避免系统默认音色（可能是中文）读不出英文
  if (englishVoice) utterance.voice = englishVoice;
  synth.speak(utterance);
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
