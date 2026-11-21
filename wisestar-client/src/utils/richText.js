/**
 * richText.js - 纯文本结构解析工具（无 React/JSX 依赖，便于单测与复用）
 *
 * 负责把题干/讲解要点的纯文本解析为结构化块：
 *   - 统一换行；无换行时按连续小问标记（（1）（2）/ ①② / 1. 2.）自动补换行
 *   - 识别行首编号、项目符号、两级缩进、【小标题】
 *
 * 供 RichContent 渲染层消费；解析失败不影响调用方（始终返回数组/字符串）。
 */

// CJK 圈号 1~20
export const CIRCLED = '①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳';
export const CIRCLED_MAP = Object.fromEntries([...CIRCLED].map((c, i) => [c, i + 1]));
const CN_NUM_MAP = { 一: 1, 二: 2, 三: 3, 四: 4, 五: 5, 六: 6, 七: 7, 八: 8, 九: 9, 十: 10 };

// 行首列表标记：[缩进][编号/符号]
const LIST_RE = new RegExp(
  [
    '^(\\s*)(',
    `（\\s*[0-9]{1,3}\\s*）|（\\s*[一二三四五六七八九十]{1,3}\\s*）`,
    `|\\(\\s*[0-9]{1,3}\\s*\\)|\\[\\s*[0-9]{1,3}\\s*\\]`,
    // 「3.4.9×…」= 第 3 小问 + 小数 4.9：先按「编号 + 小数内容」识别，避免被小数守卫挡成正文
    `|[0-9]{1,2}[.、．)](?=[0-9][0-9]*[.、．)])`,
    `|[0-9]{1,2}[.、．)](?![0-9])`,
    `|[${CIRCLED}]`,
    '|[-*•·]',
    ')\\s*(.*)$',
  ].join(''),
);

// 空位标记 __①__（单项/多项填空题干规范，见 utils/blankMarkers.js）：
// 圈号是空位编号而非小问分隔符。判断小问断行时要把 __①__ 整段排除，
// 否则「① …；② …」这类小问会因题干含空位标记而被误判、整段挤成一行。
const BLANK_RANGE_RE = /__[①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳]__/g;

// 先补换行再解析：统一 CRLF；逐行把「① …；② …」这类连续小问标记断到新行
// （即使整题原本已有换行、或题干含 __①__ 空位标记，也要处理行内小问）
export function normalizeStructuredText(raw) {
  const s = String(raw == null ? '' : raw).replace(/\r\n?/g, '\n');
  if (!s) return s;
  return s.split('\n').map(splitInlineSubQuestions).join('\n');
}

// 把单行内的连续小问标记（①② / （1）（2） …）断到新行
function splitInlineSubQuestions(line) {
  if (!line) return line;
  const markers = collectSubQuestionMarkers(line);
  if (!markers) return line;
  let out = '';
  let last = 0;
  for (const c of markers) {
    const seg = line.slice(last, c.index);
    if (seg.trim()) {
      out += seg.replace(/[ \t]+$/, '');
      if (!out.endsWith('\n')) out += '\n';
    } else {
      out += seg;
    }
    out += line.slice(c.index, c.index + c.len);
    last = c.index + c.len;
  }
  out += line.slice(last);
  return out;
}

// 把（已规范化的）文本解析为块序列
export function parseRichBlocks(s) {
  const blocks = [];
  for (const raw of String(s).split('\n')) {
    if (!raw.trim()) {
      if (blocks.length && blocks[blocks.length - 1].type !== 'blank') blocks.push({ type: 'blank' });
      continue;
    }
    const list = raw.match(LIST_RE);
    if (list) {
      blocks.push({
        type: 'item',
        level: Math.min(2, Math.floor(list[1].length / 2)),
        marker: list[2],
        body: list[3],
        bullet: /^[-*•·]$/.test(list[2]),
      });
      continue;
    }
    const head = raw.match(/^【(.+?)】\s*(.*)$/);
    if (head) {
      blocks.push({ type: 'heading', text: head[1], tail: head[2] || '' });
      continue;
    }
    blocks.push({ type: 'para', text: raw });
  }
  while (blocks.length && blocks[0].type === 'blank') blocks.shift();
  while (blocks.length && blocks[blocks.length - 1].type === 'blank') blocks.pop();
  return blocks;
}

// 找出公式区间，避免在 $...$ / $$...$$ 内部误判小问编号
function findMathRanges(s) {
  const re = /\$\$[\s\S]+?\$\$|\$[^\s$][^$\n]*?[^\s$]\$|\$[^\s$]\$/g;
  const ranges = [];
  let m;
  while ((m = re.exec(s)) !== null) ranges.push([m.index, m.index + m[0].length]);
  return ranges;
}

// 识别连续小问标记；仅当编号严格为 1,2,3… 且数量在 2~12 之间时返回，避免误伤小数/日期
function collectSubQuestionMarkers(s) {
  const cands = [];
  const strongRe = new RegExp(
    `（\\s*([0-9]{1,3})\\s*）|\\(\\s*([0-9]{1,3})\\s*\\)|（\\s*([一二三四五六七八九十]{1,3})\\s*）|([${CIRCLED}])`,
    'g',
  );
  let m;
  while ((m = strongRe.exec(s)) !== null) {
    const value = m[1]
      ? Number(m[1])
      : m[2]
        ? Number(m[2])
        : m[3]
          ? CN_NUM_MAP[m[3]]
          : CIRCLED_MAP[m[4]];
    cands.push({ index: m.index, len: m[0].length, value, strong: true });
  }
  // 弱标记（1. / 2、 / 3)）：仅当位于句首/标点/空白/右括号之后，避免误伤小数。
  // 兼容「判断：1. …（__①__）2. …（__②__）3.4.9×5.1…」：后一项编号紧跟右括号，
  // 需把 ）) 】] 也算作合法前置；且编号后紧跟小数（如「3.4.9」= 第3小问 + 4.9）时，
  // 仅靠「右括号 + 后面还有一个小数点」判定为小问，避免把「3.5米」这类小数误拆。
  const weakRe = /([0-9]{1,2})[.、．)]/g;
  while ((m = weakRe.exec(s)) !== null) {
    const prev = m.index === 0 ? '' : s[m.index - 1];
    const prevOk = m.index === 0 || /[。；;！？!?：:，,、\s）)】\]]/.test(prev);
    if (!prevOk) continue;
    const next = s[m.index + m[0].length];
    const nextIsDigit = next !== undefined && /[0-9]/.test(next);
    if (nextIsDigit) {
      const afterBracket = /[）)】\]]/.test(prev);
      const decimalContent = /^[0-9]{1,2}[.、．)][0-9][0-9]*[.、．)]/.test(s.slice(m.index));
      if (!(afterBracket && decimalContent)) continue;
    }
    cands.push({ index: m.index, len: m[0].length, value: Number(m[1]), strong: false });
  }
  const strongRanges = cands.filter((c) => c.strong).map((c) => [c.index, c.index + c.len]);
  const mathRanges = findMathRanges(s);
  const blankRanges = [];
  BLANK_RANGE_RE.lastIndex = 0;
  let bm;
  while ((bm = BLANK_RANGE_RE.exec(s)) !== null) blankRanges.push([bm.index, bm.index + bm[0].length]);
  const kept = cands
    .filter((c) => c.strong || !strongRanges.some(([a, b]) => c.index >= a && c.index < b))
    .filter((c) => !mathRanges.some(([a, b]) => c.index >= a && c.index < b))
    .filter((c) => !blankRanges.some(([a, b]) => c.index >= a && c.index < b))
    .sort((a, b) => a.index - b.index);
  if (kept.length < 2 || kept.length > 12) return null;
  for (let i = 0; i < kept.length; i += 1) {
    if (kept[i].value !== i + 1) return null;
  }
  return kept;
}
