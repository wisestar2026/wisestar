/**
 * RichContent.jsx - 轻量富内容渲染组件（题干/选项/解析/知识点讲解通用）
 *
 * 一、行内标记（导入模板约定）：
 *   图片：![说明](http(s)://...)       —— 公网可访问的图片 URL
 *   行内公式：$...$                     —— LaTeX，如 $x^2$、$\frac{a}{b}$
 *   独立公式：$$...$$                   —— 独占一行展示
 *   加粗：**重点内容**
 *
 * 二、结构标记（长题干小问、讲解要点分行，见 utils/richText.js）：
 *   - 换行 \n 分段；连续空行折叠为段落间距
 *   - 行首编号：①②③ / （1） / (1) / [1] / 1. 1、 1) / 一、
 *   - 行首项目符号：- * • ·
 *   - 行首 2 个空格表示一级缩进（最多两级）；【小标题】整行加粗
 *   无换行但含连续小问标记时自动补换行
 *
 * 三、可选高亮（highlight=true，用于知识点讲解要点）：
 *   引号术语、数字、圈号步骤标记 加粗/变色
 *
 * 解析失败（公式非法/图片地址为空）时原样回退为纯文本，绝不抛错影响页面。
 *
 * 被谁引用：KnowledgePage（知识点/题目）、QuestionCard（练习答题卡）、
 *           KnowledgeDetectPage（检测报告）、EnglishGrammarLearnPage（语法只读）
 * 依赖：katex
 */

import { useMemo } from 'react';
import katex from 'katex';
import 'katex/contrib/mhchem';
import 'katex/dist/katex.min.css';
import './RichContent.css';
import { CycleNumber } from './CycleDecimalText';
import { CIRCLED, CIRCLED_MAP, normalizeStructuredText, parseRichBlocks } from '../../utils/richText';

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

// 循环小数（如 0.(6)、3.4(897)）
const CYCLE_IN_LINE = /\d+\.\d*\(\d+\)/g;

// 知识点讲解高亮：循环小数 | 引号术语/数字 | 圈号
const HL_RE = new RegExp(
  [`\\d+\\.\\d*\\(\\d+\\)`, `“[^”]{1,24}”|「[^」]{1,24}」|\\d[\\d,.·]*`, `[${CIRCLED}]`].join('|'),
  'g',
);

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

// 普通文本片段：把 STD 循环小数渲染为教材循环点记法
function pushCycle(out, text, keyRef) {
  if (!text) return;
  let last = 0;
  let m;
  CYCLE_IN_LINE.lastIndex = 0;
  while ((m = CYCLE_IN_LINE.exec(text)) !== null) {
    if (m.index > last) out.push(<span key={`t-${keyRef.k++}`}>{text.slice(last, m.index)}</span>);
    out.push(<CycleNumber key={`cy-${keyRef.k++}`} obj={m[0]} />);
    last = m.index + m[0].length;
  }
  if (last < text.length) out.push(<span key={`t-${keyRef.k++}`}>{text.slice(last)}</span>);
}

// 知识点高亮片段：循环小数 + 引号术语/数字 + 圈号
function pushHighlight(out, text, keyRef) {
  if (!text) return;
  let last = 0;
  let m;
  HL_RE.lastIndex = 0;
  while ((m = HL_RE.exec(text)) !== null) {
    if (m.index > last) out.push(<span key={`t-${keyRef.k++}`}>{text.slice(last, m.index)}</span>);
    const tok = m[0];
    if (/^\d+\.\d*\(\d+\)$/.test(tok)) {
      out.push(<CycleNumber key={`cy-${keyRef.k++}`} obj={tok} />);
    } else if (tok.length === 1 && CIRCLED_MAP[tok]) {
      out.push(<em key={`st-${keyRef.k++}`} className="rich-content-step">{tok}</em>);
    } else {
      out.push(<strong key={`hl-${keyRef.k++}`} className="rich-content-hl">{tok}</strong>);
    }
    last = m.index + tok.length;
  }
  if (last < text.length) out.push(<span key={`t-${keyRef.k++}`}>{text.slice(last)}</span>);
}

// 单行行内富文本（图片/公式/加粗/循环小数/可选高亮），不处理换行
function renderInline(text, keyRef, highlight) {
  const out = [];
  const s = String(text == null ? '' : text);
  if (!s) return out;

  let last = 0;
  let m;
  RICH_RE.lastIndex = 0;
  while ((m = RICH_RE.exec(s)) !== null) {
    if (m.index > last) {
      const seg = s.slice(last, m.index);
      (highlight ? pushHighlight : pushCycle)(out, seg, keyRef);
    }
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
  if (last < s.length) {
    const seg = s.slice(last);
    (highlight ? pushHighlight : pushCycle)(out, seg, keyRef);
  }
  return out;
}

export default function RichContent({ text, className, highlight = false }) {
  const s = useMemo(() => normalizeStructuredText(text), [text]);
  const blocks = useMemo(() => parseRichBlocks(s), [s]);

  if (blocks.length === 0) return null;

  // 单段纯文本：保持行内 <span>，兼容选项/短题干等内联场景
  if (blocks.length === 1 && blocks[0].type === 'para') {
    const inline = renderInline(blocks[0].text, { k: 0 }, highlight);
    return inline.length ? <span className={className}>{inline}</span> : null;
  }

  const keyRef = { k: 0 };
  return (
    <span className={`rich-content-block${className ? ` ${className}` : ''}`}>
      {blocks.map((b, i) => {
        const key = `blk-${i}`;
        if (b.type === 'blank') return <span className="rich-content-gap" key={key} />;
        if (b.type === 'heading') {
          return (
            <span className="rich-content-h" key={key}>
              {renderInline(b.text, keyRef, highlight)}
              {b.tail ? <span> {renderInline(b.tail, keyRef, highlight)}</span> : null}
            </span>
          );
        }
        if (b.type === 'item') {
          return (
            <span className={`rich-content-item level-${b.level}`} key={key}>
              <span className={`rich-content-marker${b.bullet ? ' is-bullet' : ''}`}>{b.marker}</span>
              <span className="rich-content-item-body">{renderInline(b.body, keyRef, highlight)}</span>
            </span>
          );
        }
        return (
          <span className="rich-content-p" key={key}>
            {renderInline(b.text, keyRef, highlight)}
          </span>
        );
      })}
    </span>
  );
}
