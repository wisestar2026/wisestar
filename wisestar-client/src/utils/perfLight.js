/**
 * 低配模式探测
 *
 * 背景：学员电脑可能是十年前的笔记本（如 i7-2620M 双核 / 核显），
 * 毛玻璃与常驻动画在核显上会明显掉帧。这里按硬件能力自动切到精简效果，
 * 同时留人工开关，避免误判时用户无法自救。
 *
 * 判定顺序：
 *   1. 地址栏 `?perf=light` / `?perf=high`（一次生效，并写入 localStorage）
 *   2. localStorage `wisestar:perf`
 *   3. 硬件：逻辑核心 <= 4、或 deviceMemory <= 4、或省流量模式
 */
const STORAGE_KEY = 'wisestar:perf';

const root = document.documentElement;

function readQuery() {
  try {
    const value = new URLSearchParams(window.location.search).get('perf');
    return value === 'light' || value === 'high' ? value : null;
  } catch {
    return null;
  }
}

function readStored() {
  try {
    const value = localStorage.getItem(STORAGE_KEY);
    return value === 'light' || value === 'high' ? value : null;
  } catch {
    return null;
  }
}

function detectByHardware() {
  const cores = navigator.hardwareConcurrency || 8;
  const memory = navigator.deviceMemory || 8;
  const saveData = Boolean(navigator.connection && navigator.connection.saveData);
  return cores <= 4 || memory <= 4 || saveData ? 'light' : 'high';
}

/** 当前模式：'light'（精简效果）或 'high' */
export function resolvePerfMode() {
  const fromQuery = readQuery();
  if (fromQuery) {
    try {
      localStorage.setItem(STORAGE_KEY, fromQuery);
    } catch {
      /* 隐身模式下写不了存储，忽略 */
    }
    return fromQuery;
  }
  return readStored() || detectByHardware();
}

/** 应用模式：给 <html> 加 low-perf 类，供 low-perf.css 生效 */
export function applyPerfMode() {
  const mode = resolvePerfMode();
  root.classList.toggle('low-perf', mode === 'light');
  return mode;
}

export function isLowPerf() {
  return root.classList.contains('low-perf');
}
