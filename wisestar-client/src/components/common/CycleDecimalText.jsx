/**
 * CycleDecimalText.jsx - 循环小数显示组件（STD → DISPLAY）
 *
 * 对应需求《循环小数格式转化需求说明》§2.2: 把 STD 循环节括号记法渲染为教材循环点记法。
 *   `0.(6)`   → `0.6̇`
 *   `3.4(897)`→ `3.48̇9̇7̇`
 *   `0.5(10)` → `0.51̇0̇`
 *
 * 用法:
 *   <CycleNumber std="3.4(897)" />          单个循环小数
 *   <CycleDecimalText text="商用0.(6)表示" /> 文本中自动识别 STD 并渲染循环点
 *
 * 被谁引用: RichContent（题干/解析）、QuestionCard（试题回显）、知识点页
 */

import { parseDecimalLoose, toDisplayParts } from '../../utils/cycleDecimal';
import './CycleDecimalText.css';

// 在长文本中定位 STD 循环小数（非全局副本，避免 lastIndex 副作用）
const CYCLE_IN_TEXT = /\d+\.\d*\(\d+\)/;

/**
 * 单个循环小数渲染（STD 文本或 OBJ）。
 */
export function CycleNumber({ obj, className }) {
  const parsed = typeof obj === 'string' ? parseDecimalLoose(obj) : obj;
  if (!parsed) return <span className={className}>{typeof obj === 'string' ? obj : ''}</span>;
  const { int, frac } = toDisplayParts(parsed);
  return (
    <span className={`cycle-num${className ? ` ${className}` : ''}`}>
      <span className="cycle-int">{int}</span>
      <span className="cycle-dot-sep">.</span>
      {frac.map((d, i) => (
        <span key={i} className={`cycle-digit${d.dot ? ' cycle-digit--dot' : ''}`}>
          {d.digit}
        </span>
      ))}
    </span>
  );
}

/**
 * 文本渲染：自动把其中的 STD 循环小数渲染为循环点记法，其余文本原样输出。
 */
export default function CycleDecimalText({ text, std, className }) {
  if (std != null) return <CycleNumber obj={std} className={className} />;

  const s = String(text == null ? '' : text);
  if (!CYCLE_IN_TEXT.test(s)) return <span className={className}>{s}</span>;

  const nodes = [];
  let last = 0;
  let key = 0;
  for (const m of s.matchAll(/\d+\.\d*\(\d+\)/g)) {
    if (m.index > last) nodes.push(<span key={key++}>{s.slice(last, m.index)}</span>);
    nodes.push(<CycleNumber key={key++} obj={m[0]} />);
    last = m.index + m[0].length;
  }
  if (last < s.length) nodes.push(<span key={key++}>{s.slice(last)}</span>);
  return <span className={className}>{nodes}</span>;
}
