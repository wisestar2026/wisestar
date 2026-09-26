/**
 * cycleDecimal.js - 循环小数格式转化与数学等价判定工具
 *
 * 对应需求《循环小数格式转化需求说明》§2/§3，统一四种形态:
 *   STD     存储/标准: 循环节半角括号纯文本，如 `0.(6)`、`3.4(897)`、`0.61(8)`
 *   DISPLAY 显示:      教材循环点记法，如 `0.6̇`、`3.48̇9̇7̇`（数字后叠加组合点 U+0307）
 *   INPUT   输入:      学员可提交的多种写法（循环点 / 括号 / 省略号 / 有限小数）
 *   OBJ     判题对象:  结构化 `{ kind, int, nonrep, rep }` 或 `{ kind:'finite', int, frac }`
 *
 * 设计原则:
 *   - 判题只做数学等价判定，绝不依赖字符串相等（§3.3）；
 *   - 有限小数与循环小数类型不同即判错（`0.666` ≠ `0.(6)`）；
 *   - 同一数值有多个等价循环节写法时，一律归一为最短循环节。
 *
 * 被谁引用: practiceHelpers（填空判分）、RichContent（题干渲染）、CycleDecimalText / CycleDecimalInput 组件
 */

// 组合上点（循环点）与间隔上点，两种输入都兼容
const COMBINING_DOT = '\u0307';
const SPACING_DOT = '\u02D9';

// STD：整数部分.非循环部分(循环节)，循环节半角括号紧跟且至少 1 位
const STD_CYCLE_RE = /^(\d+)?\.(\d*)\((\d+)\)$/;
// 全局扫描（用于在长文本中定位 STD 循环小数）
export const STD_CYCLE_GLOBAL_RE = /\d+\.\d*\(\d+\)/g;

/** 是否为 STD 循环小数纯文本（如 `0.(6)`、`3.4(897)`）。 */
export function isStdCycle(text) {
  return STD_CYCLE_RE.test(String(text == null ? '' : text).trim());
}

/**
 * 解析 STD 文本为 OBJ。
 * 无括号但为合法十进制时返回有限小数对象；无法解析返回 null。
 */
export function parseStd(text) {
  const s = String(text == null ? '' : text).trim();
  const m = STD_CYCLE_RE.exec(s);
  if (m) {
    return canonicalizeCycle({ int: m[1] || '0', nonrep: m[2] || '', rep: m[3] });
  }
  const finite = parseFiniteText(s);
  return finite;
}

/** 解析纯十进制（含整数）文本为有限小数对象；失败返回 null。 */
export function parseFiniteText(text) {
  const s = String(text == null ? '' : text).trim();
  const f = /^(\d+)?\.(\d+)$/.exec(s);
  if (f) return { kind: 'finite', int: f[1] || '0', frac: f[2] };
  if (/^\d+$/.test(s)) return { kind: 'finite', int: s, frac: '' };
  return null;
}

/** 最小循环节：`66`→`6`、`1212`→`12`、`897897`→`897`。 */
export function shortestPeriod(rep) {
  const s = String(rep || '');
  if (!s) return s;
  for (let len = 1; len < s.length; len++) {
    if (s.length % len !== 0) continue;
    const unit = s.slice(0, len);
    if (unit.repeat(s.length / len) === s) return unit;
  }
  return s;
}

/**
 * 归一化循环小数：压缩循环节为最短，并把非循环部分中属于循环的前缀左移。
 * 例：`6(6)` → `(6)`；`4(897)` 保持；`1233(3)` → `123(3)`。
 */
export function canonicalizeCycle({ int, nonrep, rep }) {
  const r = shortestPeriod(rep);
  const p = r.length;
  const baseInt = int || '0';
  if (!p) return { kind: 'finite', int: baseInt, frac: String(nonrep || '') };
  const d = String(nonrep || '');
  // 展开足够长度后寻找最小的“纯循环起点” q
  const seq = (d + r + r + r).split('');
  for (let q = 0; q <= d.length; q++) {
    let periodic = true;
    for (let i = q; i + p < seq.length; i++) {
      if (seq[i] !== seq[i + p]) { periodic = false; break; }
    }
    if (periodic) {
      return {
        kind: 'cycle',
        int: baseInt,
        nonrep: seq.slice(0, q).join(''),
        rep: shortestPeriod(seq.slice(q, q + p).join('')),
      };
    }
  }
  return { kind: 'cycle', int: baseInt, nonrep: d, rep: r };
}

/**
 * 从“省略号写法”的小数位推导循环节（§3.2）。
 * 从小数末尾往前取最短重复段，需出现 ≥2 次；无法判定返回 null。
 */
export function deriveCycleFromFraction(int, fracDigits) {
  const frac = String(fracDigits || '');
  for (let len = 1; len <= Math.floor(frac.length / 2); len++) {
    const cand = frac.slice(frac.length - len);
    if (frac.slice(frac.length - 2 * len) === cand + cand) {
      return canonicalizeCycle({
        int: int || '0',
        nonrep: frac.slice(0, frac.length - 2 * len),
        rep: cand,
      });
    }
  }
  return null;
}

/**
 * 解析循环点记法（如 `0.6̇`、`3.48̇9̇7̇`）:
 * 点只能落在小数位，且必须构成连续尾段（非循环 + 循环两段）。
 * 合法返回 cycle 对象；否则返回 null。
 */
export function parseDotNotation(text) {
  const s = String(text == null ? '' : text);
  if (!s.includes(COMBINING_DOT) && !s.includes(SPACING_DOT)) return null;
  const dotAt = (ch) => ch === COMBINING_DOT || ch === SPACING_DOT;
  // 拆整数/小数
  const dotIdx = s.indexOf('.');
  if (dotIdx < 0) return null;
  const intRaw = s.slice(0, dotIdx);
  const fracRaw = s.slice(dotIdx + 1);
  // 整数位不允许加点
  if ([...intRaw].some(dotAt)) return null;
  // 逐字符扫描小数部分，构建 {digit, dotted}
  const digits = [];
  for (const ch of fracRaw) {
    if (dotAt(ch)) {
      if (digits.length === 0) return null; // 点前无数字
      digits[digits.length - 1].dotted = true;
    } else if (/\d/.test(ch)) {
      digits.push({ digit: ch, dotted: false });
    } else {
      return null; // 出现非法字符
    }
  }
  if (digits.length === 0) return null;
  const first = digits.findIndex((d) => d.dotted);
  if (first < 0) return null;
  // 必须为连续尾段
  for (let i = first; i < digits.length; i++) {
    if (!digits[i].dotted) return null;
  }
  return canonicalizeCycle({
    int: intRaw || '0',
    nonrep: digits.slice(0, first).map((d) => d.digit).join(''),
    rep: digits.slice(first).map((d) => d.digit).join(''),
  });
}

/** 解析省略号写法（`0.666…`、`3.4897897…`，兼容 `...`）；不合法返回 null。 */
export function parseEllipsis(text) {
  const s = String(text == null ? '' : text).trim();
  const m = /^(\d+)?\.(\d+)(?:…|\.{3,})$/.exec(s);
  if (!m) return null;
  return deriveCycleFromFraction(m[1], m[2]);
}

/**
 * 解析任意 INPUT 文本为 OBJ（§2.3）: 括号 → 循环点 → 省略号 → 有限小数。
 * 无法识别返回 null。
 */
export function parseInput(text) {
  const s = String(text == null ? '' : text).replace(/\s+/g, '').trim();
  if (!s) return null;
  if (STD_CYCLE_RE.test(s)) return parseStd(s);
  const dotted = parseDotNotation(s);
  if (dotted) return dotted;
  const ell = parseEllipsis(s);
  if (ell) return ell;
  return parseFiniteText(s);
}

/** 宽容解析：优先按 STD，其次按 INPUT。 */
export function parseDecimalLoose(text) {
  const s = String(text == null ? '' : text).trim();
  if (!s) return null;
  if (STD_CYCLE_RE.test(s)) return parseStd(s);
  return parseInput(s);
}

/** OBJ → STD 文本（循环节半角括号）。 */
export function toStd(obj) {
  if (!obj) return '';
  const int = obj.int || '0';
  if (obj.kind === 'cycle') {
    return `${int}.${obj.nonrep || ''}(${obj.rep})`;
  }
  return obj.frac ? `${int}.${obj.frac}` : int;
}

/**
 * OBJ → DISPLAY 零件（供 React 渲染循环点）:
 *   { int, frac: [{ digit, dot }] }
 */
export function toDisplayParts(obj) {
  if (!obj) return { int: '', frac: [] };
  const int = obj.int || '0';
  if (obj.kind === 'cycle') {
    const frac = [
      ...[...obj.nonrep].map((digit) => ({ digit, dot: false })),
      ...[...obj.rep].map((digit) => ({ digit, dot: true })),
    ];
    return { int, frac };
  }
  return { int, frac: [...(obj.frac || '')].map((digit) => ({ digit, dot: false })) };
}

/** OBJ/STD → DISPLAY 纯文本（循环点用组合点叠加，可直接用于字符串展示）。 */
export function stdToDisplayText(stdOrObj) {
  const obj = typeof stdOrObj === 'string' ? parseDecimalLoose(stdOrObj) : stdOrObj;
  if (!obj) return typeof stdOrObj === 'string' ? stdOrObj : '';
  if (obj.kind === 'finite' && !obj.frac) return String(obj.int || '0');
  const { int, frac } = toDisplayParts(obj);
  const fracText = frac.map((d) => (d.dot ? d.digit + COMBINING_DOT : d.digit)).join('');
  return `${int}.${fracText}`;
}

// 整数部分比较（忽略前导 0）
function intEquals(a, b) {
  const norm = (s) => String(s || '0').replace(/^0+(?=\d)/, '') || '0';
  return norm(a) === norm(b);
}

// 有限小数比较（去尾随 0）
function finiteEquals(a, b) {
  if (!intEquals(a.int, b.int)) return false;
  const strip = (s) => String(s || '').replace(/0+$/, '');
  return strip(a.frac) === strip(b.frac);
}

// 展开循环小数的前 L 位小数
function expandCycleFrac(obj, len) {
  const rep = obj.rep || '';
  if (!rep) return (obj.nonrep || '');
  const need = Math.max(0, len - (obj.nonrep || '').length);
  const repeatTimes = Math.ceil(need / rep.length) + 1;
  return ((obj.nonrep || '') + rep.repeat(repeatTimes)).slice(0, len);
}

/**
 * 数学等价判定（§3.3）:
 *   - 类型不同（有限 vs 循环）直接 false；
 *   - 有限小数按去尾随 0 的数值比较；
 *   - 循环小数展开前 L 位逐位比较，L = len(nonrep) + len(rep) * 2 + 4。
 */
export function cycleEquals(a, b) {
  if (!a || !b) return false;
  if (a.kind !== b.kind) return false;
  if (a.kind === 'finite') return finiteEquals(a, b);
  if (!intEquals(a.int, b.int)) return false;
  const L = Math.max(
    a.nonrep.length + a.rep.length * 2 + 4,
    b.nonrep.length + b.rep.length * 2 + 4,
  );
  return expandCycleFrac(a, L) === expandCycleFrac(b, L);
}

/**
 * 循环小数等价判定（供填空判分调用）。
 * 仅当至少一方为循环小数时返回 true/false；两边都不是循环小数时返回 null
 * （调用方回退到既有文本等值比较，避免影响普通数值/文本空）。
 */
export function cycleDecimalEquals(stdText, inputText) {
  const a = parseDecimalLoose(stdText);
  const b = parseDecimalLoose(inputText);
  const aCycle = a && a.kind === 'cycle';
  const bCycle = b && b.kind === 'cycle';
  if (!aCycle && !bCycle) return null;
  return cycleEquals(a, b);
}
