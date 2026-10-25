/**
 * pronunciationScore.js - 英语跟读打分（离线，纯前端）
 *
 * 目标是「判通顺、判有没有明显卡顿或漏读」，口音与音色差异尽量不扣分。四项：
 *   1. 流畅度 40%：只把 ≥400ms 的空档算卡顿，且停顿占比要到 32% 才开始扣分——孩子
 *      读得慢一点、词间稍作停顿都不影响，只有明显的「一句一顿」才会掉下来
 *   2. 完整度 30%：有效朗读时长与原音之比（±25% 以内算读完，容得下读快读慢；
 *      只读半句会因为时长明显偏短被压分）
 *   3. 发音接近度 20%：MFCC + DTW 与原音的频谱走势比对，零分点放宽到 8，换人/带口音
 *      读同一句只掉几个点，只在「完全不像」时才明显扣分
 *   4. 音量   10%：声压级太小扣分并提示靠近麦克风
 *
 * 说明: 这是离线练读反馈，不是音素级评测，也不做语音识别（不联网、不上传录音）。
 * 因此它无法逐词校验「读的是不是这句话」，只判断「是否完整、连贯、清楚地读完了一句差不多长的英文」。
 *
 * 依赖: Web Audio API（decodeAudioData / OfflineAudioContext）。
 */

const TARGET_RATE = 8000;
const FRAME_SIZE = 256; // 32ms
const HOP_SIZE = 128; // 16ms
const FFT_SIZE = 256;
const MEL_BANDS = 26;
const MFCC_COUNT = 12;
const MIN_VOICED_SECONDS = 0.3;

/** 频谱距离的“零分点”：距离 ≥ 该值视为完全不像（换音色/带口音通常在 3~4 之间，只掉几个点） */
const DISTANCE_ZERO = 8;
const WEIGHT_FLUENCY = 0.4;
const WEIGHT_COMPLETE = 0.3;
const WEIGHT_PRONUNCIATION = 0.2;
const WEIGHT_VOLUME = 0.1;
/** 只有超过该时长的空档才算“卡顿”，孩子词间喘口气不扣分 */
const PAUSE_SECONDS = 0.4;
const HOP_SECONDS = HOP_SIZE / TARGET_RATE;

let audioContext = null;

function getAudioContext() {
  if (!audioContext) {
    const Ctx = window.AudioContext || window.webkitAudioContext;
    audioContext = new Ctx();
  }
  return audioContext;
}

/** 把 Blob / ArrayBuffer / URL 统一读成 ArrayBuffer */
async function toArrayBuffer(source) {
  if (source instanceof ArrayBuffer) return source;
  if (source instanceof Blob) return source.arrayBuffer();
  if (typeof source === 'string') {
    const response = await fetch(source);
    if (!response.ok) throw new Error(`音频读取失败：${response.status}`);
    return response.arrayBuffer();
  }
  throw new Error('不支持的音频来源');
}

/** 解码音频并重采样到 8kHz 单声道 Float32 */
async function loadSamples(source) {
  const buffer = await toArrayBuffer(source);
  const decoded = await getAudioContext().decodeAudioData(buffer.slice(0));
  const frames = Math.max(1, Math.ceil(decoded.duration * TARGET_RATE));
  const offline = new OfflineAudioContext(1, frames, TARGET_RATE);
  const node = offline.createBufferSource();
  node.buffer = decoded;
  node.connect(offline.destination);
  node.start();
  const rendered = await offline.startRendering();
  return rendered.getChannelData(0);
}

/** 每帧能量（与 MFCC 帧对齐） */
function frameEnergy(samples) {
  const frameCount = Math.max(1, Math.floor((samples.length - FRAME_SIZE) / HOP_SIZE) + 1);
  const energy = new Float32Array(frameCount);
  for (let f = 0; f < frameCount; f += 1) {
    let sum = 0;
    const start = f * HOP_SIZE;
    const end = Math.min(start + FRAME_SIZE, samples.length);
    for (let i = start; i < end; i += 1) sum += samples[i] * samples[i];
    energy[f] = Math.sqrt(sum / FRAME_SIZE);
  }
  return energy;
}

function peakOf(values) {
  let peak = 0;
  for (let i = 0; i < values.length; i += 1) if (values[i] > peak) peak = values[i];
  return peak;
}

/** 按能量裁剪首尾静音，返回说话段的采样区间 */
function voiceBounds(energy, totalLength) {
  const threshold = Math.max(0.004, peakOf(energy) * 0.06);
  let first = -1;
  let last = -1;
  for (let i = 0; i < energy.length; i += 1) {
    if (energy[i] >= threshold) {
      if (first < 0) first = i;
      last = i;
    }
  }
  if (first < 0) return null;
  return [first * HOP_SIZE, Math.min(totalLength, (last + 1) * HOP_SIZE)];
}

/** 梅尔滤波器组（80~3800Hz，26 组三角窗） */
function buildMelFilterbank() {
  const hzToMel = (hz) => 2595 * Math.log10(1 + hz / 700);
  const melToHz = (mel) => 700 * (10 ** (mel / 2595) - 1);
  const minMel = hzToMel(80);
  const maxMel = hzToMel(3800);
  const bins = [];
  for (let i = 0; i < MEL_BANDS + 2; i += 1) {
    const mel = minMel + ((maxMel - minMel) * i) / (MEL_BANDS + 1);
    bins.push(Math.floor(((FFT_SIZE + 1) * melToHz(mel)) / TARGET_RATE));
  }
  const filters = [];
  for (let i = 0; i < MEL_BANDS; i += 1) {
    const left = bins[i];
    const center = Math.max(bins[i + 1], left + 1);
    const right = Math.max(bins[i + 2], center + 1);
    const weights = new Float32Array(FFT_SIZE / 2 + 1);
    for (let k = left; k < center && k < weights.length; k += 1) {
      weights[k] = (k - left) / (center - left);
    }
    for (let k = center; k < right && k < weights.length; k += 1) {
      weights[k] = (right - k) / (right - center);
    }
    filters.push(weights);
  }
  return filters;
}

/** 预先算好三角窗与 DCT 矩阵，避免每帧重复计算 */
const HAMMING = (() => {
  const win = new Float32Array(FRAME_SIZE);
  for (let i = 0; i < FRAME_SIZE; i += 1) {
    win[i] = 0.54 - 0.46 * Math.cos((2 * Math.PI * i) / (FRAME_SIZE - 1));
  }
  return win;
})();

const MEL_FILTERS = buildMelFilterbank();

const DCT_MATRIX = (() => {
  const matrix = [];
  for (let c = 1; c <= MFCC_COUNT; c += 1) {
    const row = new Float32Array(MEL_BANDS);
    for (let m = 0; m < MEL_BANDS; m += 1) {
      row[m] = Math.cos((Math.PI * c * (m + 0.5)) / MEL_BANDS);
    }
    matrix.push(row);
  }
  return matrix;
})();

/** 原地基 2 FFT（长度 256） */
function fft(re, im) {
  const n = re.length;
  for (let i = 1, j = 0; i < n; i += 1) {
    let bit = n >> 1;
    for (; j & bit; bit >>= 1) j ^= bit;
    j ^= bit;
    if (i < j) {
      [re[i], re[j]] = [re[j], re[i]];
      [im[i], im[j]] = [im[j], im[i]];
    }
  }
  for (let len = 2; len <= n; len <<= 1) {
    const angle = (-2 * Math.PI) / len;
    const half = len >> 1;
    for (let i = 0; i < n; i += len) {
      for (let k = 0; k < half; k += 1) {
        const cos = Math.cos(angle * k);
        const sin = Math.sin(angle * k);
        const xr = re[i + k + half] * cos - im[i + k + half] * sin;
        const xi = re[i + k + half] * sin + im[i + k + half] * cos;
        re[i + k + half] = re[i + k] - xr;
        im[i + k + half] = im[i + k] - xi;
        re[i + k] += xr;
        im[i + k] += xi;
      }
    }
  }
}

/** 计算 MFCC 序列：每帧 12 维倒谱系数 */
function mfccFrames(samples) {
  const frameCount = Math.max(1, Math.floor((samples.length - FRAME_SIZE) / HOP_SIZE) + 1);
  const frames = [];
  const re = new Float32Array(FFT_SIZE);
  const im = new Float32Array(FFT_SIZE);
  const power = new Float32Array(FFT_SIZE / 2 + 1);

  for (let f = 0; f < frameCount; f += 1) {
    const start = f * HOP_SIZE;
    re.fill(0);
    im.fill(0);
    for (let i = 0; i < FRAME_SIZE; i += 1) {
      const index = start + i;
      const value = index < samples.length ? samples[index] : 0;
      re[i] = value * HAMMING[i];
    }
    fft(re, im);
    for (let k = 0; k < power.length; k += 1) {
      power[k] = re[k] * re[k] + im[k] * im[k];
    }
    const mel = new Float32Array(MEL_BANDS);
    for (let m = 0; m < MEL_BANDS; m += 1) {
      const weights = MEL_FILTERS[m];
      let sum = 0;
      for (let k = 0; k < weights.length; k += 1) sum += weights[k] * power[k];
      mel[m] = Math.log(sum + 1e-8);
    }
    const ceps = new Float32Array(MFCC_COUNT);
    for (let c = 0; c < MFCC_COUNT; c += 1) {
      const row = DCT_MATRIX[c];
      let sum = 0;
      for (let m = 0; m < MEL_BANDS; m += 1) sum += row[m] * mel[m];
      ceps[c] = sum;
    }
    frames.push(ceps);
  }
  return frames;
}

/** 倒谱均值方差归一化 + 低阶系数降权 + 帧平滑：压掉音色/麦克风差异，只留内容轮廓 */
function normalizeFrames(frames) {
  if (!frames.length) return frames;
  const mean = new Float32Array(MFCC_COUNT);
  frames.forEach((frame) => {
    for (let c = 0; c < MFCC_COUNT; c += 1) mean[c] += frame[c] / frames.length;
  });
  const std = new Float32Array(MFCC_COUNT);
  frames.forEach((frame) => {
    for (let c = 0; c < MFCC_COUNT; c += 1) std[c] += ((frame[c] - mean[c]) ** 2) / frames.length;
  });
  for (let c = 0; c < MFCC_COUNT; c += 1) {
    std[c] = Math.max(Math.sqrt(std[c]), 1e-3);
  }
  const normalized = frames.map((frame) => {
    const next = new Float32Array(MFCC_COUNT);
    for (let c = 0; c < MFCC_COUNT; c += 1) {
      // 前两阶主要反映增益与声道，降权后换人读同一句的距离显著变小
      const weight = c < 2 ? 0.7 : 1;
      next[c] = ((frame[c] - mean[c]) / std[c]) * weight;
    }
    return next;
  });
  // 帧平滑 ±2 帧（约 ±32ms）：抑制噪声与混响造成的抖动
  const smooth = 2;
  return normalized.map((_, i) => {
    const acc = new Float32Array(MFCC_COUNT);
    let count = 0;
    for (let offset = -smooth; offset <= smooth; offset += 1) {
      const j = i + offset;
      if (j < 0 || j >= normalized.length) continue;
      for (let c = 0; c < MFCC_COUNT; c += 1) acc[c] += normalized[j][c];
      count += 1;
    }
    for (let c = 0; c < MFCC_COUNT; c += 1) acc[c] /= count;
    return acc;
  });
}

/** DTW 平均帧距离（Sakoe-Chiba 带约束），越小越像 */
function dtwDistance(a, b) {
  const n = a.length;
  const m = b.length;
  if (!n || !m) return Number.POSITIVE_INFINITY;
  const band = Math.max(Math.abs(n - m) + Math.round(0.3 * Math.max(n, m)), 8);
  const inf = Number.MAX_SAFE_INTEGER;
  let prev = new Float64Array(m + 1).fill(inf);
  prev[0] = 0;
  for (let i = 1; i <= n; i += 1) {
    const cur = new Float64Array(m + 1).fill(inf);
    const lo = Math.max(1, i - band);
    const hi = Math.min(m, i + band);
    for (let j = lo; j <= hi; j += 1) {
      let sum = 0;
      const fa = a[i - 1];
      const fb = b[j - 1];
      for (let c = 0; c < MFCC_COUNT; c += 1) {
        const diff = fa[c] - fb[c];
        sum += diff * diff;
      }
      const cost = Math.sqrt(sum);
      cur[j] = cost + Math.min(prev[j], prev[j - 1], cur[j - 1]);
    }
    prev = cur;
  }
  const total = prev[m];
  if (!Number.isFinite(total)) return Number.POSITIVE_INFINITY;
  return total / Math.max(n, m);
}

function rms(samples) {
  let sum = 0;
  for (let i = 0; i < samples.length; i += 1) sum += samples[i] * samples[i];
  return Math.sqrt(sum / Math.max(1, samples.length));
}

function clamp01(value) {
  return Math.min(1, Math.max(0, value));
}

/** 统计朗读段内的有效帧、句内长停顿帧 */
function speechStats(samples) {
  const energy = frameEnergy(samples);
  const threshold = Math.max(0.004, peakOf(energy) * 0.1);
  let voicedFrames = 0;
  let pauseFrames = 0;
  let run = 0;
  for (let i = 0; i < energy.length; i += 1) {
    if (energy[i] >= threshold) {
      if (run * HOP_SECONDS >= PAUSE_SECONDS) pauseFrames += run;
      run = 0;
      voicedFrames += 1;
    } else {
      run += 1;
    }
  }
  return { voicedSeconds: voicedFrames * HOP_SECONDS, pauseSeconds: pauseFrames * HOP_SECONDS };
}

/**
 * 用已经解码好的 8kHz 单声道样本打分（供单元自测复用，页面走 scorePronunciation）。
 *
 * @param {Float32Array} referenceSamples 原音
 * @param {Float32Array} recordedSamples 学员录音
 */
export function scoreSamples(referenceSamples, recordedSamples) {
  const fail = (reason) => ({ ok: false, score: 0, tips: [], reason });

  const recordedEnergy = frameEnergy(recordedSamples);
  const referenceEnergy = frameEnergy(referenceSamples);
  const recordedBounds = voiceBounds(recordedEnergy, recordedSamples.length);
  const referenceBounds = voiceBounds(referenceEnergy, referenceSamples.length);
  if (!recordedBounds || !referenceBounds) {
    return fail('no-speech');
  }

  const recorded = recordedSamples.subarray(recordedBounds[0], recordedBounds[1]);
  const reference = referenceSamples.subarray(referenceBounds[0], referenceBounds[1]);

  const recordedStats = speechStats(recorded);
  if (recordedStats.voicedSeconds < MIN_VOICED_SECONDS) {
    return fail('too-short');
  }
  const referenceStats = speechStats(reference);

  // 1) 流畅度：只统计 ≥400ms 的空档，占比到 30% 才开始扣分，读到一大半才扣满
  const pauseRatio = recordedStats.pauseSeconds / Math.max(0.001, recordedStats.voicedSeconds);
  const fluency = 1 - clamp01((pauseRatio - 0.32) / 0.73);

  // 2) 完整度：只比值不比起止时间，±25% 以内都算读完；明显偏短才压分
  const durationRatio = recordedStats.voicedSeconds / Math.max(0.001, referenceStats.voicedSeconds);
  let completeness;
  if (durationRatio < 0.75) {
    completeness = clamp01((durationRatio - 0.5) / 0.25);
  } else if (durationRatio > 1.5) {
    completeness = clamp01(1 - (durationRatio - 1.5) / 1);
  } else {
    completeness = 1;
  }

  // 3) 发音接近度：频谱走势相似度，零分点放宽，换人/带口音只掉几个点
  const distance = dtwDistance(
    normalizeFrames(mfccFrames(reference)),
    normalizeFrames(mfccFrames(recorded)),
  );
  const pronunciation = Number.isFinite(distance) ? clamp01(1 - distance / DISTANCE_ZERO) : 0.6;

  // 4) 音量：以录音说话段的声压级为准，太小提示靠近麦克风
  const volumeDb = 20 * Math.log10(Math.max(rms(recorded), 1e-6));
  const volume = clamp01((volumeDb + 48) / 30);

  let score = Math.round(100 * clamp01(
    WEIGHT_FLUENCY * fluency
    + WEIGHT_COMPLETE * completeness
    + WEIGHT_PRONUNCIATION * pronunciation
    + WEIGHT_VOLUME * volume
  ));
  const tips = [];
  // 只读半句、声音过小直接压分，避免「漏读也拿高分」
  if (completeness < 0.35) {
    score = Math.min(score, 45);
    tips.push('好像没读完，从头把整句读完');
  } else if (completeness < 0.7) {
    score = Math.min(score, 65);
    tips.push('比原音短了不少，注意别漏词');
  }
  if (volume < 0.25) {
    score = Math.min(score, 55);
    tips.push('声音太小，靠近麦克风大声一点');
  } else if (volume < 0.55) {
    tips.push('可以再大声一点，录音会更清楚');
  }
  if (pronunciation < 0.25) {
    score = Math.min(score, 60);
    tips.push('和原音差得比较多，再听一遍原音跟着读');
  }
  if (fluency < 0.5) tips.push('中间停顿偏多，试着连贯地读下来');
  if (score >= 85) tips.push('读得完整又连贯，保持这个节奏');

  return {
    ok: true,
    score,
    completeness: Math.round(completeness * 100),
    fluency: Math.round(fluency * 100),
    volume: Math.round(volume * 100),
    timbre: Math.round(pronunciation * 100),
    tips,
  };
}

/**
 * 给一次跟读录音打分。
 *
 * @param {Blob} recordedBlob 录音
 * @param {Blob|string|ArrayBuffer} reference 原音（Blob 或音频地址）
 * @returns {Promise<{ok:boolean, score:number, completeness:number, fluency:number, volume:number, timbre:number, tips:string[], reason?:string}>}
 */
export async function scorePronunciation(recordedBlob, reference) {
  let recorded;
  let target;
  try {
    [recorded, target] = await Promise.all([loadSamples(recordedBlob), loadSamples(reference)]);
  } catch {
    return { ok: false, score: 0, completeness: 0, fluency: 0, volume: 0, timbre: 0, tips: [], reason: 'audio-unreadable' };
  }
  return scoreSamples(target, recorded);
}

/** 分数档位（用于配色与文案） */
export function scoreLevel(score) {
  if (score >= 85) return { key: 'great', label: '很棒' };
  if (score >= 70) return { key: 'good', label: '不错' };
  if (score >= 60) return { key: 'pass', label: '及格' };
  return { key: 'retry', label: '再试一次' };
}
