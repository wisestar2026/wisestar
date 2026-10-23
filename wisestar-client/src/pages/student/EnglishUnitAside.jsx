/**
 * EnglishUnitAside.jsx - 英语学习左侧单元导航（单元学习主界面 / 单词通关 / 句式通关 共用）
 *
 * 结构：竖排单元列表，当前单元高亮，有待复习任务的单元右上角显示橙色角标；
 * 每个单元展示达成进度（星级 + 百分比 + 进度条，口径同数学章节「学习完成度」）。
 * 仅负责展示与选中回调；数据与跳转由父页决定，当前各页点击单元统一跳转到该单元的
 * 英语学习中心主页（/student/english?unit=...），不在子页原地切换。
 *
 * Props:
 *   units       Array<{ unit: string, mastery?: number, reviewDue?: number }>  单元列表
 *   activeUnit  string                                       当前选中单元
 *   onSelect    (unit: string) => void                        点击单元回调
 *   title       string                                        列表标题（默认「单元」）
 *
 * 被谁引用: EnglishCenterPage / EnglishWordLearnPage / EnglishSentenceLearnPage / EnglishGrammarLearnPage
 */

import StarRating from '../../components/common/StarRating';

export default function EnglishUnitAside({ units = [], activeUnit, onSelect, title = '单元' }) {
  return (
    <aside className="eng-aside">
      <div className="eng-aside-title">{title}</div>
      <div className="eng-aside-list">
        {units.map((item) => {
          const name = item.unit;
          const pct = Math.max(0, Math.min(100, Number(item.mastery) || 0));
          return (
            <button
              key={name}
              type="button"
              className={`eng-aside-item ${name === activeUnit ? 'active' : ''}`}
              onClick={() => onSelect?.(name)}
              title={`${name} 达成进度 ${pct}%`}
            >
              <span className="eng-aside-name">{name}</span>
              <span className="eng-aside-progress">
                <StarRating value={pct} size={12} className="eng-aside-stars" />
                <span className="eng-aside-pct">达成 {pct}%</span>
              </span>
              <span className="eng-aside-track" aria-hidden="true">
                <span className="eng-aside-fill" style={{ width: `${pct}%` }} />
              </span>
              {item.reviewDue > 0 && <span className="eng-aside-badge">{item.reviewDue}</span>}
            </button>
          );
        })}
      </div>
    </aside>
  );
}
