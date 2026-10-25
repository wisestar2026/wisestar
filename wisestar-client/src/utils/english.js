/**
 * english.js - 英语学习公共工具（语音与分词）
 *
 * 说明：
 *   - speakEnglish：优先播放音频地址，其次服务端合成音频（/english/student/tts），
 *     最后回退浏览器语音合成（TTS），返回是否播放成功。
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

// 英文语音缓存：浏览器语音列表是异步加载的（Chrome 首次 getVoices() 可能为空），
// 缓存后避免每次朗读都重新遍历，并在 voiceschanged 后刷新
let englishVoice = null;
let voicesBound = false;

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

/** 当前浏览器是否支持语音合成 */
export function isSpeechSupported() {
  return typeof window !== 'undefined' && Boolean(window.speechSynthesis);
}

/**
 * 预热语音引擎：只触发音色列表加载，不再静音试播。
 *
 * Chrome 对「空白文本 + volume=0」的试播不会回调 end，队列会一直停在 speaking 状态，
 * 后续真正点击时 cancel 后立刻 speak 会被 Chrome 丢掉，表现为「点了没声音」。
 */
export function warmupSpeechSynthesis() {
  if (typeof window === 'undefined') return;
  const synth = window.speechSynthesis;
  if (!synth) return;
  ensureVoiceListener();
  if (!englishVoice) englishVoice = pickEnglishVoice();
}

// 模块加载即预热音色列表（不发声），首次用户交互时再刷新一次音色
if (typeof window !== 'undefined' && window.speechSynthesis) {
  window.speechSynthesis.getVoices();
  ensureVoiceListener();
  if (typeof window.addEventListener === 'function') {
    window.addEventListener('pointerdown', warmupSpeechSynthesis, { once: true, passive: true });
  }
}

/**
 * 返回当前浏览器的语音能力自检信息（用于页面上的「语音自检」）。
 *
 * @returns {{supported:boolean, voiceCount:number, englishVoices:string[], chosen:string|null, speaking:boolean, paused:boolean}}
 */
export function getSpeechReport() {
  const synth = typeof window !== 'undefined' ? window.speechSynthesis : null;
  if (!synth) {
    return { supported: false, voiceCount: 0, englishVoices: [], chosen: null, speaking: false, paused: false };
  }
  const voices = synth.getVoices() || [];
  const chosen = englishVoice || pickEnglishVoice();
  const describe = (v) => `${v.name}（${v.lang}${v.localService ? '，本地' : '，联网'}）`;
  return {
    supported: true,
    voiceCount: voices.length,
    englishVoices: voices.filter((v) => /^en([-_]|$)/i.test(v.lang)).map(describe),
    chosen: chosen ? describe(chosen) : null,
    speaking: Boolean(synth.speaking),
    paused: Boolean(synth.paused),
  };
}

/**
 * 朗读一次。以「是否收到 start/end 回调」判定成功，超时视为引擎卡死。
 *
 * @param {string} text 英文文本
 * @param {SpeechSynthesisVoice|null} voice 指定音色，null 表示交给浏览器默认音色
 * @param {number} timeoutMs 看门狗超时
 * @returns {Promise<{ok:boolean, reason?:string}>}
 */
function speakOnce(text, voice, timeoutMs) {
  return new Promise((resolve) => {
    const synth = window.speechSynthesis;
    const utterance = new SpeechSynthesisUtterance(String(text));
    utterance.lang = 'en-US';
    utterance.rate = 0.9;
    utterance.volume = 1;
    if (voice) utterance.voice = voice;
    let settled = false;
    let started = false;
    const finish = (ok, reason) => {
      if (settled) return;
      settled = true;
      resolve({ ok, reason });
    };
    utterance.onstart = () => {
      started = true;
      finish(true);
    };
    utterance.onend = () => {
      started = true;
      finish(true);
    };
    utterance.onerror = (event) => {
      started = true;
      finish(false, event?.error || 'error');
    };
    try {
      synth.speak(utterance);
    } catch (error) {
      finish(false, `throw:${error?.message || error}`);
      return;
    }
    setTimeout(() => {
      if (!started) finish(false, 'timeout');
    }, timeoutMs);
  });
}

/**
 * 浏览器语音合成朗读英文，失败时逐级降级重试。
 *
 * Chrome 的坑（按踩到的顺序）：
 *   1. 同一轮事件里先 cancel() 再 speak()，新语音会被丢弃 → 两者之间隔一个事件循环；
 *   2. cancel() 之后 paused 可能仍为 true，此时 speak() 不生效 → 每次朗读前 resume()；
 *   3. 缓存音色可能已失效（语音包卸载 / 列表刷新）→ 最后一次不带音色，交给浏览器默认；
 *   4. 语音引擎卡死时不会有任何回调 → 1.5 秒看门狗，超时判定失败并进入下一级重试。
 *
 * @param {string} text 英文文本
 * @returns {Promise<boolean>} 是否成功播放
 */
async function speakByTTS(text) {
  if (typeof window === 'undefined' || !window.speechSynthesis || !text) {
    return false;
  }
  const synth = window.speechSynthesis;
  ensureVoiceListener();
  if (!englishVoice) englishVoice = pickEnglishVoice();
  const attempts = englishVoice ? [englishVoice, null] : [null];
  for (let i = 0; i < attempts.length; i += 1) {
    try { synth.cancel(); } catch { /* ignore */ }
    if (synth.paused) synth.resume();
    // 与 cancel 隔开一个事件循环，规避 Chrome 丢弃紧接着的 speak()
    // eslint-disable-next-line no-await-in-loop
    await new Promise((resolve) => { setTimeout(resolve, i === 0 ? 60 : 150); });
    if (synth.paused) synth.resume();
    // eslint-disable-next-line no-await-in-loop
    const result = await speakOnce(text, attempts[i], 1500);
    if (result.ok) return true;
    if (result.reason === 'not-allowed') {
      console.warn('[speakEnglish] 浏览器拦截了朗读（缺少用户手势或站点被静音）');
      return false;
    }
    if (result.reason === 'interrupted') {
      return false;
    }
    console.warn('[speakEnglish] 第 %d 次朗读失败：%s', i + 1, result.reason);
  }
  return false;
}

// 当前正在播放的音频与播放令牌：
// 每次朗读自增令牌，异步取音频（合成/预取）期间若用户已切到别的单词，
// 旧请求回来时令牌已过期，直接丢弃，避免「听到上一个单词的读音」。
let activeAudio = null;
let playToken = 0;

/** 停止当前朗读（音频与浏览器语音合成都停） */
export function stopEnglishAudio() {
  playToken += 1;
  if (activeAudio) {
    try {
      activeAudio.pause();
      activeAudio.currentTime = 0;
    } catch { /* ignore */ }
    activeAudio = null;
  }
  if (typeof window !== 'undefined' && window.speechSynthesis) {
    try { window.speechSynthesis.cancel(); } catch { /* ignore */ }
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
 * 朗读英文：音频地址 → 服务端合成 → 浏览器语音合成，逐级回退。
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
  if (await playAudioUrl(await getEnglishAudioUrl(text, speed), token)) {
    return true;
  }
  if (token !== playToken) {
    return false;
  }
  if (!isSpeechSupported()) {
    return false;
  }
  return speakByTTS(text);
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
