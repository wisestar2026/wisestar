/**
 * RichContent.jsx - 轻量富内容渲染组件（题干/选项/解析通用）
 *
 * 支持在纯文本中混排以下写法（导入模板约定，AI 平台按此汇编即可）：
 *   图片：![说明](http(s)://...)       —— 公网可访问的图片 URL
 *   行内公式：$...$                     —— LaTeX，如 $x^2$、$\frac{a}{b}$
 *   独立公式：$$...$$                   —— 独占一行展示
 *   加粗：**重点内容**
 *   换行：单元格内换行（\n）自动转为 <br/>
 *
 * 解析失败（公式非法/图片地址为空）时原样回退为纯文本，绝不抛错影响页面。
 *
 * 被谁引用：KnowledgePage（知识点/题目）、QuestionCard（练习答题卡）
 * 依赖：katex
 */

import { useMemo } from 'react';
import katex from 'katex';
import 'katex/contrib/mhchem';
import 'katex/dist/katex.min.css';
import './RichContent.css';

// 组合正则（顺序敏感）：独立公式 → 图片 → 行内公式 → 加粗
// 行内公式要求首尾均非空白，避免把货币符号（如 $5 与后续 $）误判为公式
const RICH_RE = new RegExp(
  [
    '\\$\\$([\\s\\S]+?)\\$\\$',
    '!\\[([^\\]]*)\\]\\(([^)\\s]+)\\)',
    '\\$([^\\s$](?:[^$\\n]*?[^\\s$])?)\\$',
    '\\*\\*([\\s\\S]+?)\\*\\*',
  ].join('|'),
  'g',
);

const MATH_OPTS = { throwOnError: false, strict: false };

// KaTeX 渲染（失败回退纯文本）
function MathTex({ tex, display }) {
  let html = null;
  try {
    html = katex.renderToString(tex, { ...MATH_OPTS, displayMode: !!display });
  } catch {
    html = null;
  }
  if (!html) {
    return <span className="rich-content-math-fallback">{display ? `$$${tex}$$` : `$${tex}$`}</span>;
  }
  return (
    <span
      className={display ? 'rich-content-math-block' : 'rich-content-math'}
      dangerouslySetInnerHTML={{ __html: html }}
    />
  );
}

// 普通文本：按换行切分并插入 <br/>
function pushText(out, s, keyRef) {
  const lines = String(s).split('\n');
  lines.forEach((line, i) => {
    if (i > 0) out.push(<br key={`br-${keyRef.k++}`} />);
    if (line) out.push(<span key={`t-${keyRef.k++}`}>{line}</span>);
  });
}

export default function RichContent({ text, className }) {
  const nodes = useMemo(() => {
    const s = String(text == null ? '' : text);
    if (!s) return null;

    // 快速路径：无任何富内容标记时直接纯文本，减少开销
    if (!/[!$*]/.test(s)) {
      const out = [];
      pushText(out, s, { k: 0 });
      return out;
    }

    const out = [];
    const keyRef = { k: 0 };
    let last = 0;
    RICH_RE.lastIndex = 0;
    let m;
    while ((m = RICH_RE.exec(s)) !== null) {
      if (m.index > last) pushText(out, s.slice(last, m.index), keyRef);
      if (m[1] !== undefined) {
        out.push(<MathTex key={`mb-${keyRef.k++}`} tex={m[1]} display />);
      } else if (m[2] !== undefined) {
        out.push(
          <img
            key={`img-${keyRef.k++}`}
            className="rich-content-img"
            src={m[3]}
            alt={m[2] || '题目配图'}
          />,
        );
      } else if (m[4] !== undefined) {
        out.push(<MathTex key={`mi-${keyRef.k++}`} tex={m[4]} />);
      } else if (m[5] !== undefined) {
        out.push(<strong key={`b-${keyRef.k++}`}>{m[5]}</strong>);
      }
      last = m.index + m[0].length;
    }
    if (last < s.length) pushText(out, s.slice(last), keyRef);
    return out;
  }, [text]);

  return <span className={className}>{nodes}</span>;
}
