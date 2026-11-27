/**
 * MasteryBadge.jsx - 掌握度勋章（替代章节 / 小节 / 英语单元的五星评级）
 *
 * 七档位图（public/student-assets/mastery/，档位定义见 utils/mastery.js）：
 *   mastery-none 0%           灰   未学
 *   mastery-0    1% ~ 19%     铜   入门
 *   mastery-20   20% ~ 39%    银   及格
 *   mastery-40   40% ~ 59%    金   掌握
 *   mastery-60   60% ~ 79%    蓝   熟练
 *   mastery-80   80% ~ 99%    紫   变通
 *   mastery-100  100%         红金 优秀
 *
 * 固定方形渲染 + object-fit: contain，各档位视觉重心一致，不改变行高。
 * 位图已在构建前裁掉棋盘格背景并转 WebP，单张约 10~12KB；同一档位浏览器只下一次。
 *
 * Props:
 *   value   number  掌握度百分比 0-100
 *   size    number  渲染边长（px，调用方按 --page-u 换算）
 *   className string 追加类名
 *   label   string  aria-label / title 文案（默认「档位 · 掌握度 X%」）
 */

import { masteryTier } from '../../utils/mastery';
import './MasteryBadge.css';

export default function MasteryBadge({ value = 0, size = 24, className = '', label }) {
  const v = Math.max(0, Math.min(100, Math.round(Number(value) || 0)));
  const tier = masteryTier(v);
  const src = `${import.meta.env.BASE_URL}student-assets/mastery/${tier.file}.webp`;
  const text = label || `${tier.label} · 掌握度 ${v}%`;

  return (
    <img
      className={`mastery-badge ${className}`.trim()}
      src={src}
      width={size}
      height={size}
      alt=""
      role="img"
      aria-label={text}
      title={text}
      draggable={false}
      loading="lazy"
      decoding="async"
      style={{ width: size, height: size }}
    />
  );
}
