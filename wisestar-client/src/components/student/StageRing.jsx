/**
 * StageRing.jsx - 英语学习「阶段圆环」（单词通关 / 句式通关共用）
 *
 * 纯展示 + 点击入口：圆心显示考法名称与「已完成/总量」，环弧按比例填充。
 * 阈值判断一律留在调用方（后端接口给结果），组件只负责画环。
 *
 * Props:
 *   tone     memory（海蓝）| practice（珊瑚橙）| use（通关绿）
 *   size     md（默认）| lg（首列主环节点，直径更大）
 *   label    圆心主文案（考法名称）
 *   done     已完成数
 *   total    总量
 *   onClick  点击入口
 *   disabled 不足以出题时置灰
 *   hint     悬浮提示（默认自动拼「label done/total」）
 */

/** 圆环几何：外圈细线（整圈参考线） + 内圈粗弧（实际进度） */
const RING_OUTER_RADIUS = 50;
const RING_RADIUS = 41;
const RING_CIRCUMFERENCE = 2 * Math.PI * RING_RADIUS;

export default function StageRing({
  tone = 'memory', size = 'md', label, done, total, onClick, disabled, hint,
}) {
  const safeTotal = total || 0;
  const safeDone = Math.max(0, Math.min(done || 0, safeTotal));
  const pct = safeTotal ? safeDone / safeTotal : 0;
  return (
    <button
      type="button"
      className={`eng-ring eng-ring-${tone} eng-ring-${size}`}
      onClick={onClick}
      disabled={disabled}
      title={hint || `${label} ${safeDone}/${safeTotal}`}
    >
      <span className="eng-ring-frame">
        <svg viewBox="0 0 108 108" aria-hidden="true">
          <circle className="eng-ring-outline" cx="54" cy="54" r={RING_OUTER_RADIUS} />
          <circle className="eng-ring-track" cx="54" cy="54" r={RING_RADIUS} />
          <circle
            className="eng-ring-fill"
            cx="54"
            cy="54"
            r={RING_RADIUS}
            strokeDasharray={`${pct * RING_CIRCUMFERENCE} ${RING_CIRCUMFERENCE}`}
            transform="rotate(-90 54 54)"
          />
        </svg>
        <span className="eng-ring-text">
          <b>{label}</b>
          <em>{safeDone}/{safeTotal}</em>
        </span>
      </span>
    </button>
  );
}
