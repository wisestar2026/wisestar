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
    `|[0-9]{1,2}[.、．)](?![0-9])`,
    `|[${CIRCLED}]`,
    '|[-*•·]',
    ')\\s*(.*)$',
  ].join(''),
);

// 先补换行再解析：统一 CRLF；无换行时按连续小问标记断行
export function normalizeStructuredText(raw) {
  const s = String(raw == null ? '' : raw).replace(/\r\n?/g, '\n');
  if (!s || s.includes('\n')) return s;
  const markers = collectSubQuestionMarkers(s);
  if (!markers) return s;
  let out = '';
  let last = 0;
  for (const c of markers) {
    const seg = s.slice(last, c.index);
    if (seg.trim()) {
      out += seg.replace(/[ \t]+$/, '');
      if (!out.endsWith('\n')) out += '\n';
    } else {
      out += seg;
    }
    out += s.slice(c.index, c.index + c.len);
    last = c.index + c.len;
  }
  out += s.slice(last);
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
  // 弱标记（1. / 2、 / 3)）：仅当位于句首/标点/空白之后，避免误伤小数
  const weakRe = /([0-9]{1,2})[.、．)](?![0-9])/g;
  while ((m = weakRe.exec(s)) !== null) {
    const prev = m.index === 0 ? '' : s[m.index - 1];
    if (m.index === 0 || /[。；;！？!?：:，,、\s]/.test(prev)) {
      cands.push({ index: m.index, len: m[0].length, value: Number(m[1]), strong: false });
    }
  }
  const strongRanges = cands.filter((c) => c.strong).map((c) => [c.index, c.index + c.len]);
  const mathRanges = findMathRanges(s);
  const kept = cands
    .filter((c) => c.strong || !strongRanges.some(([a, b]) => c.index >= a && c.index < b))
    .filter((c) => !mathRanges.some(([a, b]) => c.index >= a && c.index < b))
    .sort((a, b) => a.index - b.index);
  if (kept.length < 2 || kept.length > 12) return null;
  for (let i = 0; i < kept.length; i += 1) {
    if (kept[i].value !== i + 1) return null;
  }
  return kept;
}
