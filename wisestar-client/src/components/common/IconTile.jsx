/**
 * IconTile.jsx - 3D 黏土质感图标底座（学员端统一图标语言）
 *
 * 把 emoji（或任意节点）放进一个圆角渐变方块里：顶部高光 + 底部黏土内阴影 + 外投影，
 * 形成有层次感的立体图标，替代直接裸露在文字前的 emoji。
 *
 * Props:
 *   emoji     图形字符（最常用，如 '📖'）
 *   children  自定义内容（传入时优先于 emoji，可放 antd 图标等节点）
 *   tone      blue | sky | teal | green | orange | gold | purple | pink | red | slate
 *   size      xs | sm | md | lg | xl | 2xl
 *   round     圆形（头像 / 金币）
 *   dim       降饱和（未激活态）
 *   className / style / title
 *
 * 被谁引用：StudentLayout / StudyPage / StudentHomePage / KnowledgePage 等学员端页面
 */

import './IconTile.css';

export default function IconTile({
  emoji, children, tone = 'blue', size = 'md', round = false, dim = false,
  className = '', style, title,
}) {
  const cls = [
    'icon-tile',
    `it-${tone}`,
    `it-${size}`,
    round ? 'is-round' : '',
    dim ? 'is-dim' : '',
    className,
  ].filter(Boolean).join(' ');

  return (
    <span className={cls} style={style} title={title} aria-hidden={title ? undefined : true}>
      <span className="icon-tile-face">{children ?? emoji}</span>
    </span>
  );
}
