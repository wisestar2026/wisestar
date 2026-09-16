/**
 * StarRating.jsx - 星级评分（0-5 星，按达成率点亮）
 *
 * 颜色规则（按点亮数量区分，章节标题与小节标题共用）：
 *   1 星 → 绿色；2 星 → 蓝色；3 星 → 红色；4 星 → 金色；5 星 → 彩色
 * 未点亮的星为空心灰。
 *
 * 达成率阈值与旧版一致：≥80 五星 / ≥60 四星 / ≥40 三星 / ≥20 二星 / >0 一星。
 */

const LEVEL_COLORS = {
  1: '#22c55e', // 绿
  2: '#3b82f6', // 蓝
  3: '#ef4444', // 红
  4: '#f5b301', // 金
};

// 五星：每颗星不同颜色，形成彩色
const RAINBOW = ['#ef4444', '#f97316', '#eab308', '#22c55e', '#3b82f6'];

const EMPTY_COLOR = '#cbd5e1';

/** 达成率（0-100）→ 点亮星数（0-5） */
function rateToStarCount(rate) {
  const r = Number(rate) || 0;
  if (r >= 80) return 5;
  if (r >= 60) return 4;
  if (r >= 40) return 3;
  if (r >= 20) return 2;
  return r > 0 ? 1 : 0;
}

export default function StarRating({ value = 0, size = 14, className = '' }) {
  const count = rateToStarCount(value);
  return (
    <span
      className={`star-rating ${className}`.trim()}
      style={{ fontSize: size }}
      aria-label={`${count} 星`}
    >
      {[0, 1, 2, 3, 4].map((i) => {
        const lit = i < count;
        const color = !lit ? EMPTY_COLOR : count === 5 ? RAINBOW[i] : LEVEL_COLORS[count];
        return (
          <span key={i} style={{ color }}>
            {lit ? '★' : '☆'}
          </span>
        );
      })}
    </span>
  );
}
