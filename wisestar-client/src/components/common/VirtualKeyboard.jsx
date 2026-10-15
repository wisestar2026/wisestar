/**
 * VirtualKeyboard.jsx - 独立、可收起的虚拟键盘（数学符号 / 英文字母 / 中文标点）
 *
 * 用途: 学员作答填空类题目时，普通键盘难以输入 ×、÷、√、² 等数学符号与
 *       英文标点，提供一个悬浮在右下角、可随时收起/展开的虚拟键盘。
 *       字符的插入/删除由父组件按当前聚焦的输入框处理（onPick / onDelete）。
 *
 * 设计（对齐参考图「填空题键盘」）:
 *   - 右下角悬浮开关按钮（「⌨ 虚拟键盘」），点击展开/收起，不答题时不渲染
 *   - 展开后为底部贴边面板，含 符号 / 字母 / 中文 三个页签 + 「删除」键
 *   - 面板可拖动：按住顶部标题栏（页签/按钮以外区域）或左侧拖拽把手，可把键盘
 *     移到屏幕任意位置，不再固定贴在底部；松手后停留在拖动位置（视口内自动收敛）
 *   - 字母页支持 Shift 大小写切换与空格
 *
 * Props:
 *   onPick: (key: string) => void   点击某键的回调（插入字符）
 *   onDelete: () => void            点击「删除」的回调（退格）
 *   disabled: boolean               锁定态（判题后不可再插入）
 *   defaultTab: 'symbol'|'letter'|'zh'  默认页签（数学默认符号，英语默认字母）
 *   visible: boolean                是否渲染（默认 true；无文本输入时传 false）
 *
 * 被谁引用: QuestionCard（单项填空 / 多项填空 / 多行文本）
 */

import { useCallback, useEffect, useRef, useState } from 'react';
import { CloseOutlined, DeleteOutlined } from '@ant-design/icons';
import './VirtualKeyboard.css';

// 数字与小数点
const NUMBER_KEYS = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.'];

// 数学符号分组（覆盖题库中常见符号）
const SYMBOL_GROUPS = [
  { name: '常用', keys: ['+', '−', '×', '÷', '=', '±', '≈', '≠', '<', '>', '≤', '≥', '%', '‰', '·', '…'] },
  { name: '几何', keys: ['√', '∛', '²', '³', '^', '°', '∠', '⊥', '∥', '△', '□', '○', 'π', '∞'] },
  { name: '括号', keys: ['(', ')', '[', ']', '{', '}', '|', '/', '\\', '_'] },
  { name: '关系', keys: ['∈', '∉', '⊂', '⊆', '∪', '∩', '∅', '∴', '∵', '∝'] },
  { name: '逻辑', keys: ['∑', '∏', '∫', '≌', '∽', '¬', '∧', '∨', '≡'] },
];

// 英文字母（小写，Shift 切换大写）
const LETTER_ROWS = [
  ['q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p'],
  ['a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l'],
  ['z', 'x', 'c', 'v', 'b', 'n', 'm'],
];
const LETTER_PUNCT = ["'", ',', '.', '?', '!', '-', ':', ';', '"'];

// 中文常用标点
const ZH_KEYS = ['。', '，', '、', '；', '：', '？', '！', '“', '”', '‘', '’', '《', '》', '（', '）', '【', '】', '—', '…', '·'];

const TABS = [
  { key: 'symbol', label: '符号' },
  { key: 'letter', label: '字母' },
  { key: 'zh', label: '中文' },
];

export default function VirtualKeyboard({
  onPick, onDelete, disabled, defaultTab = 'symbol', visible = true,
}) {
  const [open, setOpen] = useState(false);
  const [tab, setTab] = useState(defaultTab);
  const [shift, setShift] = useState(false);
  // 拖动后的面板左上角坐标（视口坐标系）；null 表示默认底部居中
  const [pos, setPos] = useState(null);
  const [dragging, setDragging] = useState(false);
  const panelRef = useRef(null);
  const dragRef = useRef({ dx: 0, dy: 0, active: false });

  // 把坐标收敛到视口内（留 8px 边距），避免拖出屏幕后无法拖回
  const clampPos = useCallback((x, y) => {
    const el = panelRef.current;
    const w = el?.offsetWidth || 0;
    const h = el?.offsetHeight || 0;
    const maxX = Math.max(8, window.innerWidth - w - 8);
    const maxY = Math.max(8, window.innerHeight - h - 8);
    return { x: Math.min(Math.max(8, x), maxX), y: Math.min(Math.max(8, y), maxY) };
  }, []);

  const onDragStart = (e) => {
    if (e.button != null && e.button !== 0) return;
    // 页签、删除、关闭等按钮保持正常点击，只有拖拽标题栏空白处才移动面板
    if (e.target?.closest?.('button')) return;
    const el = panelRef.current;
    if (!el) return;
    const rect = el.getBoundingClientRect();
    dragRef.current = { dx: e.clientX - rect.left, dy: e.clientY - rect.top, active: true };
    setPos({ x: rect.left, y: rect.top });
    setDragging(true);
    e.currentTarget.setPointerCapture?.(e.pointerId);
  };

  const onDragMove = (e) => {
    if (!dragRef.current.active) return;
    setPos(clampPos(e.clientX - dragRef.current.dx, e.clientY - dragRef.current.dy));
  };

  const onDragEnd = (e) => {
    if (!dragRef.current.active) return;
    dragRef.current.active = false;
    setDragging(false);
    e.currentTarget.releasePointerCapture?.(e.pointerId);
  };

  // 窗口尺寸变化时重新收敛位置，避免面板被挤出视口
  const moved = pos != null;
  useEffect(() => {
    if (!moved) return undefined;
    const onResize = () => setPos((p) => (p ? clampPos(p.x, p.y) : p));
    window.addEventListener('resize', onResize);
    return () => window.removeEventListener('resize', onResize);
  }, [moved, clampPos]);

  if (!visible) return null;

  const pick = (key) => {
    if (disabled) return;
    onPick?.(key);
  };
  const del = () => {
    if (disabled) return;
    onDelete?.();
  };

  return (
    <>
      {/* 右下角悬浮开关（位于「问小鲸」AI 按钮上方，避免重叠） */}
      <button
        type="button"
        className={`vk-toggle ${open ? 'is-open' : ''}`}
        onClick={() => setOpen((o) => !o)}
        title="虚拟键盘"
      >
        <span className="vk-toggle-icon">⌨</span>
        <span className="vk-toggle-text">{open ? '收起键盘' : '虚拟键盘'}</span>
      </button>

      {open && (
        <div
          ref={panelRef}
          className={`vk-panel ${moved ? 'is-moved' : ''} ${dragging ? 'is-dragging' : ''}`}
          role="dialog"
          aria-label="虚拟键盘"
          style={moved ? { left: pos.x, top: pos.y, bottom: 'auto', transform: 'none', animation: 'none' } : undefined}
        >
          <div
            className="vk-head"
            onPointerDown={onDragStart}
            onPointerMove={onDragMove}
            onPointerUp={onDragEnd}
            onPointerCancel={onDragEnd}
          >
            <div className="vk-head-left">
              <span className="vk-grip" aria-hidden="true" title="按住拖动移动键盘">⠿</span>
              <div className="vk-tabs">
                {TABS.map((t) => (
                  <button
                    key={t.key}
                    type="button"
                    className={`vk-tab ${tab === t.key ? 'is-active' : ''}`}
                    onClick={() => setTab(t.key)}
                  >
                    {t.label}
                  </button>
                ))}
              </div>
            </div>
            <div className="vk-head-right">
              <button type="button" className="vk-del" onClick={del} disabled={disabled}>
                <DeleteOutlined /> 删除
              </button>
              <button type="button" className="vk-close" onClick={() => setOpen(false)} title="收起">
                <CloseOutlined />
              </button>
            </div>
          </div>

          <div className="vk-body">
            {tab === 'symbol' && (
              <>
                <div className="vk-row">
                  {NUMBER_KEYS.map((k) => (
                    <button key={k} type="button" className="vk-key" onClick={() => pick(k)} disabled={disabled}>{k}</button>
                  ))}
                </div>
                {SYMBOL_GROUPS.map((group) => (
                  <div className="vk-group" key={group.name}>
                    <span className="vk-group-name">{group.name}</span>
                    <div className="vk-row vk-row-wrap">
                      {group.keys.map((k) => (
                        <button key={k} type="button" className="vk-key" onClick={() => pick(k)} disabled={disabled}>{k}</button>
                      ))}
                    </div>
                  </div>
                ))}
              </>
            )}

            {tab === 'letter' && (
              <>
                {LETTER_ROWS.map((row, ri) => (
                  <div className="vk-row" key={ri}>
                    {ri === 2 && (
                      <button
                        type="button"
                        className={`vk-key vk-key-fn ${shift ? 'is-active' : ''}`}
                        onClick={() => setShift((s) => !s)}
                      >
                        ⇧
                      </button>
                    )}
                    {row.map((ch) => {
                      const label = shift ? ch.toUpperCase() : ch;
                      return (
                        <button key={ch} type="button" className="vk-key" onClick={() => pick(label)} disabled={disabled}>{label}</button>
                      );
                    })}
                    {ri === 2 && (
                      <button type="button" className="vk-key vk-key-wide" onClick={() => pick(' ')} disabled={disabled}>空格</button>
                    )}
                  </div>
                ))}
                <div className="vk-row vk-row-wrap">
                  {LETTER_PUNCT.map((k) => (
                    <button key={k} type="button" className="vk-key" onClick={() => pick(k)} disabled={disabled}>{k}</button>
                  ))}
                </div>
              </>
            )}

            {tab === 'zh' && (
              <div className="vk-row vk-row-wrap">
                {ZH_KEYS.map((k) => (
                  <button key={k} type="button" className="vk-key" onClick={() => pick(k)} disabled={disabled}>{k}</button>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </>
  );
}
