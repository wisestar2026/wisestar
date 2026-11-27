/**
 * MasteryLegend.jsx - 掌握度勋章等级标识（页面顶部说明条）
 *
 * 对齐参考站「勋章等级标识」：横向列出七档勋章 + 档位名 + 掌握度区间，
 * 档位与名称来自 utils/mastery.js，和列表里的 MasteryBadge 完全同源。
 */

import MasteryBadge from './MasteryBadge';
import { MASTERY_TIERS } from '../../utils/mastery';
import { usePageUnit } from '../../utils/pageScale';
import './MasteryLegend.css';

export default function MasteryLegend({ title = '勋章等级标识', compact = false }) {
  const pageUnit = usePageUnit();
  const badgeSize = Math.round((compact ? 40 : 52) * pageUnit);
  return (
    <section
      className={`mastery-legend${compact ? ' mastery-legend--compact' : ''}`}
      aria-label="掌握度勋章等级标识"
    >
      <div className="mastery-legend-head">
        <span className="mastery-legend-title">{title}</span>
        <span className="mastery-legend-sub">掌握度越高，勋章等级越高</span>
      </div>
      <div className="mastery-legend-list">
        {MASTERY_TIERS.map((tier) => (
          <div className="mastery-legend-item" key={tier.key}>
            <MasteryBadge value={tier.min} size={badgeSize} />
            <span className="mastery-legend-pill" style={{ background: tier.color }}>{tier.label}</span>
            <span className="mastery-legend-range">{tier.range}</span>
          </div>
        ))}
      </div>
    </section>
  );
}
