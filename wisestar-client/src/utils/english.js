/**
 * english.js - 英语学习公共工具（语音与分词）
 *
 * 说明：
 *   - speakEnglish：优先播放音频地址，其次服务端语音包合成音频（/english/student/tts），
 *     失败即返回 false。英语语音统一走服务端语音包，不使用浏览器自带的语音合成。
 *   - getEnglishAudioUrl：取（并缓存）某段英文的服务端音频地址，供播放与跟读打分复用。
 *   - stopEnglishAudio：停止当前朗读；speakEnglish 每次调用都会先停上一段并作废旧请求。
 *   - tokenizeSentence：英文句子按空白切分为词块，标点随词块保留作为排序提示。
 *   - normalizeAnswer：作答标准化（去首尾空白、忽略大小写、合并连续空格）。
 *   - shuffle：词块乱序（用于连词成句），保证与原顺序不同。
 */

import { getEnglishTts } from '../api/englishStudent';

// 服务端合成音频地址缓存：同一句话只请求一次，超出上限后释放旧 Blob URL
// 缓存键含语速：单词卡片（偏快）与课文朗读（偏慢）互不覆盖
const ttsUrlCache = new Map();
const TTS_CACHE_LIMIT = 160;

/** 单词卡片朗读语速（espeak 词/分为口径，越大越快；单词语速偏快更接近课堂带读） */
export const WORD_TTS_SPEED = 125;

/** 预取并发上限：后端每句要起一个 Piper 进程，双核机器上不能一次放太多 */
const PREFETCH_CONCURRENCY = 2;
let prefetchRunning = 0;
const prefetchQueue = [];
const prefetchPending = new Set();

function cacheKey(text, speed) {
  return `${speed || ''}|${text}`;
}

function cachePut(key, promise) {
  ttsUrlCache.set(key, promise);
  if (ttsUrlCache.size > TTS_CACHE_LIMIT) {
    const [oldKey] = ttsUrlCache.keys();
    const oldValue = ttsUrlCache.get(oldKey);
    ttsUrlCache.delete(oldKey);
    oldValue.then((url) => { if (url) URL.revokeObjectURL(url); }).catch(() => {});
  }
}

/** 真正发起一次合成请求，返回 Blob URL（失败 null） */
function fetchAudioUrl(text, speed) {
  return getEnglishTts(text, speed)
    .then((blob) => (blob && blob.size > 0 ? URL.createObjectURL(blob) : null))
    .catch(() => null);
}

function drainPrefetchQueue() {
  while (prefetchRunning < PREFETCH_CONCURRENCY && prefetchQueue.length) {
    const job = prefetchQueue.shift();
    prefetchRunning += 1;
    job().catch(() => null).then(() => {
      prefetchRunning -= 1;
      drainPrefetchQueue();
    });
  }
}

/**
 * 取某段英文的服务端合成音频地址（Blob URL），失败返回 null。
 *
 * @param {string} text 英文文本
 * @param {number} [speed] 语速（省略用后端默认）
 * @returns {Promise<string|null>}
 */
export function getEnglishAudioUrl(text, speed) {
  if (!text) return Promise.resolve(null);
  const key = cacheKey(text, speed);
  if (ttsUrlCache.has(key)) return ttsUrlCache.get(key);
  const promise = fetchAudioUrl(text, speed);
  cachePut(key, promise);
  return promise;
}

/**
 * 预取一批英文的合成音频，让「单词出现即有声音」。
 * 已缓存/已排队的文本不会重复请求；并发受 PREFETCH_CONCURRENCY 限制。
 *
 * @param {string[]} texts 英文文本数组
 * @param {number} [speed] 语速
 */
export function prefetchEnglishAudio(texts, speed) {
  (texts || []).forEach((text) => {
    if (!text) return;
    const key = cacheKey(text, speed);
    if (ttsUrlCache.has(key) || prefetchPending.has(key)) return;
    prefetchPending.add(key);
    prefetchQueue.push(() => {
      const promise = fetchAudioUrl(text, speed);
      cachePut(key, promise);
      return promise.then((url) => {
        prefetchPending.delete(key);
        return url;
      });
    });
    drainPrefetchQueue();
  });
}

// 当前正在播放的音频与播放令牌：
// 每次朗读自增令牌，异步取音频（合成/预取）期间若用户已切到别的单词，
// 旧请求回来时令牌已过期，直接丢弃，避免「听到上一个单词的读音」。
let activeAudio = null;
let playToken = 0;

/** 停止当前朗读 */
export function stopEnglishAudio() {
  playToken += 1;
  if (activeAudio) {
    try {
      activeAudio.pause();
      activeAudio.currentTime = 0;
    } catch { /* ignore */ }
    activeAudio = null;
  }
}

/** 播放一个音频地址，返回是否成功开始播放；token 过期则放弃播放 */
function playAudioUrl(url, token) {
  return new Promise((resolve) => {
    if (!url || token !== playToken) {
      resolve(false);
      return;
    }
    const audio = new Audio(url);
    activeAudio = audio;
    const release = () => { if (activeAudio === audio) activeAudio = null; };
    audio.onended = release;
    audio.onerror = () => { release(); resolve(false); };
    audio.play().then(() => {
      if (token !== playToken) {
        try { audio.pause(); } catch { /* ignore */ }
        release();
        resolve(false);
        return;
      }
      resolve(true);
    }).catch(() => { release(); resolve(false); });
  });
}

/**
 * 朗读英文：音频地址 → 服务端语音包合成，两级回退；全部失败返回 false。
 *
 * <p>项目口径是英语语音一律由服务端语音包（Piper/espeak）合成，不使用浏览器自带语音合成，
 * 因此这里不再有 `speechSynthesis` 回退分支，避免不同机器音色、口音不一致。</p>
 *
 * @param {string} text 英文文本
 * @param {string} [audioUrl] 后端已有的音频地址（如单词音频字段）
 * @param {number} [speed] 合成语速（单词卡片传 WORD_TTS_SPEED）
 * @returns {Promise<boolean>} 是否播放成功（调用方可据此提示用户）
 */
export async function speakEnglish(text, audioUrl, speed) {
  if (!text) {
    return false;
  }
  // 新的一次朗读接管播放：先停掉上一段，并让旧的异步请求全部失效
  stopEnglishAudio();
  const token = playToken;
  if (await playAudioUrl(audioUrl, token)) {
    return true;
  }
  if (token !== playToken) {
    return false;
  }
  const audioUrlFromServer = await getEnglishAudioUrl(text, speed);
  if (token !== playToken) {
    return false;
  }
  return playAudioUrl(audioUrlFromServer, token);
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
