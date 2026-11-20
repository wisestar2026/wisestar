/**
 * EnglishReviewPage.jsx - 学员端英语智能复习
 *
 * 结构:
 *   顶部「复习总览」Tab 分三块，单词 / 句式 / 语法各自成区，互不混排：
 *     1. 今日单词队列：跨单元按间隔复习调度给出本次可复习的词与熟练度概览
 *     2. 分模块单元卡片：每个模块下按单元列待复习知识点，标注掌握程度与薄弱点数
 *     3. 我的错题本：沿用既有错题记录，重做原题、答对即消灭
 *
 * 数据口径:
 *   - 待复习池统一取 /review/candidates（含 module / unit / state / weak / 推荐角度），
 *     前端按模块再按单元分组，数量与练习入口共用同一份数据，避免数字与队列打架
 *   - 单词队列 /review/words 与概览 /review/word-summary 仍按间隔复习调度给出当日 20 词
 *   - 错题本来自检测记录聚合，与单元复习互不影响
 *
 * URL: /student/english/review
 * 被谁引用: App.jsx 路由表；入口来自英语学习中心「智能复习」
 *
 * 数据流:
 *   GET  /api/english/student/review/candidates   单元复习候选（全册或指定单元，含推荐角度）
 *   GET  /api/english/student/review/words        单词复习队列（跨单元、按熟练度/间隔排序）
 *   GET  /api/english/student/review/word-summary 单词复习概览
 *   GET  /api/english/student/wrong-questions     英语复习错题（隐藏答案解析）
 *   GET  /api/english/student/wrong-question      单道错题详情（含标准答案，重做用）
 *   POST /api/english/student/wrong/redo          错题重做（答对即消灭）
 */

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Empty, Spin, Tabs, Tag, message } from 'antd';
import useStudentStore from '../../stores/useStudentStore';
import {
  getEnglishWordReview,
  getEnglishWordReviewSummary,
  getEnglishReviewCandidates,
  getEnglishWrongQuestions,
} from '../../api/englishStudent';
import EnglishWrongBookPanel from '../../components/student/EnglishWrongBookPanel';
import './EnglishCenterPage.css';

const MODULE_LABELS = { word: '单词', sentence: '句式', grammar: '语法' };
const MODULE_HINTS = {
  word: '按拼写与词义换角度复习，题目只取本单元单词',
  sentence: '按中文提示、连词成句等角度复习本单元句式',
  grammar: '按结构、例句与例题角度复习本单元语法点',
};
const STATE_COLORS = { familiar: 'green', ordinary: 'gold', unfamiliar: 'red' };
const STATE_LABELS = { familiar: '熟练', ordinary: '一般', unfamiliar: '陌生' };
const STATE_ORDER = ['familiar', 'ordinary', 'unfamiliar'];
const REASON_LABELS = { weak: '薄弱', wrong: '做错过', due: '到期' };
const MODULE_ORDER = ['word', 'sentence', 'grammar'];

/** 按单元分组，保留单元出现顺序（候选列表已按薄弱优先排序） */
function groupByUnit(list) {
  const map = new Map();
  list.forEach((item) => {
    const key = item.unit || '未分单元';
    if (!map.has(key)) map.set(key, []);
    map.get(key).push(item);
  });
  return Array.from(map.entries());
}

/** 统计一组知识点的掌握分布与薄弱数量 */
function summarize(list) {
  const counts = { familiar: 0, ordinary: 0, unfamiliar: 0 };
  let weak = 0;
  list.forEach((item) => {
    if (counts[item.state] != null) counts[item.state] += 1;
    else counts.unfamiliar += 1;
    if (item.weak) weak += 1;
  });
  return { counts, weak, total: list.length };
}

function MasteryBar({ counts, total }) {
  if (!total) {
    return <div className="eng-rv-bar" aria-hidden="true" />;
  }
  return (
    <div className="eng-rv-bar" aria-hidden="true">
      {STATE_ORDER.map((state) => (
        counts[state] > 0 && (
          <span
            key={state}
            className={`seg-${state}`}
            style={{ width: `${(counts[state] / total) * 100}%` }}
          />
        )
      ))}
    </div>
  );
}

function MasteryLegend({ counts }) {
  return (
    <div className="eng-rv-legend">
      {STATE_ORDER.map((state) => (
        <span key={state} className={`eng-rv-legend-item state-${state}`}>
          <i />
          {STATE_LABELS[state]} {counts[state]}
        </span>
      ))}
    </div>
  );
}

export default function EnglishReviewPage() {
  const navigate = useNavigate();
  const version = useStudentStore((s) => s.version);
  const grade = useStudentStore((s) => s.grade);
  const term = useStudentStore((s) => s.term);
  const activeSubject = useStudentStore((s) => s.activeSubject);

  const [loading, setLoading] = useState(true);
  const [wordPlan, setWordPlan] = useState([]);
  const [wordSummary, setWordSummary] = useState(null);
  const [candidates, setCandidates] = useState([]);
  const [wrongQuestions, setWrongQuestions] = useState([]);

  const load = useCallback(() => {
    setLoading(true);
    Promise.all([
      getEnglishWordReview({ version, grade }).catch(() => ({ data: [] })),
      getEnglishWordReviewSummary({ version, grade }).catch(() => ({ data: null })),
      getEnglishReviewCandidates({ version, grade, term }).catch(() => ({ data: [] })),
      getEnglishWrongQuestions({ limit: 100 }).catch(() => ({ data: [] })),
    ])
      .then(([wRes, sumRes, cRes, wqRes]) => {
        setWordPlan(wRes?.data || []);
        setWordSummary(sumRes?.data || null);
        setCandidates(cRes?.data || []);
        setWrongQuestions(wqRes?.data || []);
      })
      .catch(() => message.error('复习数据加载失败'))
      .finally(() => setLoading(false));
  }, [version, grade, term]);

  useEffect(() => {
    load();
  }, [load]);

  const onEliminated = useCallback(() => {
    load();
  }, [load]);

  const overview = useMemo(() => {
    const stats = summarize(candidates);
    const byModule = {};
    MODULE_ORDER.forEach((module) => {
      byModule[module] = summarize(candidates.filter((c) => c.module === module));
    });
    const units = new Set(candidates.map((c) => c.unit || '未分单元'));
    return { ...stats, byModule, unitCount: units.size };
  }, [candidates]);

  const moduleGroups = useMemo(() => {
    return MODULE_ORDER
      .map((module) => ({
        module,
        list: candidates.filter((c) => c.module === module),
      }))
      .filter((group) => group.list.length > 0);
  }, [candidates]);

  const goWordReview = () => {
    if (!wordPlan.length) return;
    const ids = wordPlan.map((w) => w.id).join(',');
    navigate(`/student/english/practice?mode=review&ids=${encodeURIComponent(ids)}`);
  };

  /** 按模块把候选知识点送到对应练习页（单词带候选 id，句式/语法按单元直达） */
  const goModuleReview = (unit, module, list) => {
    if (module === 'word') {
      const ids = list.map((c) => c.knowledgeId).join(',');
      navigate(`/student/english/practice?mode=review&ids=${encodeURIComponent(ids)}`);
      return;
    }
    if (module === 'sentence') {
      navigate(`/student/english/sentence?unit=${encodeURIComponent(unit)}`);
      return;
    }
    const sid = activeSubject || '1003';
    navigate(`/student/english/grammar-practice?unit=${encodeURIComponent(unit)}&subjectId=${encodeURIComponent(sid)}&from=review`);
  };

  if (loading) {
    return <div className="eng-learn-wrap"><Spin /></div>;
  }

  const wordQueueCard = (
    <div className="eng-rv-queue">
      <div className="eng-rv-queue-main">
        <div className="eng-rv-queue-title">
          今日单词队列
          <span className="eng-rv-queue-count">{wordPlan.length} 词</span>
        </div>
        <div className="eng-rv-queue-desc">
          {wordSummary
            ? `应复习 ${wordSummary.due} 词 · 陌生 ${wordSummary.unfamiliar} · 一般 ${wordSummary.ordinary} · 熟练 ${wordSummary.familiar}`
            : `本次待复习 ${wordPlan.length} 词`}
        </div>
        <div className="eng-rv-queue-units">
          {wordSummary
            ? `涉及单元 ${wordSummary.unitCount} 个${wordSummary.units?.length ? `（${wordSummary.units.join('、')}${wordSummary.unitCount > wordSummary.units.length ? ' 等' : ''}）` : ''}`
            : '按记忆周期跨单元取词'}
          <span className="eng-rv-queue-hint">一次复习 {wordSummary?.batchSize || 20} 个，越久没复习的越靠前</span>
        </div>
      </div>
      <button
        type="button"
        className="eng-btn-practice eng-rv-queue-btn"
        onClick={goWordReview}
        disabled={wordPlan.length === 0}
      >
        {wordPlan.length ? `开始复习 · ${wordPlan.length}` : '暂无到期单词'}
      </button>
    </div>
  );

  const kpiRow = (
    <div className="eng-rv-kpis">
      <div className="eng-rv-kpi accent-all">
        <div className="eng-rv-kpi-label">全部待复习</div>
        <div className="eng-rv-kpi-value">{overview.total}</div>
        <div className="eng-rv-kpi-sub">覆盖 {overview.unitCount} 个单元</div>
      </div>
      {MODULE_ORDER.map((module) => (
        <div className={`eng-rv-kpi accent-${module}`} key={module}>
          <div className="eng-rv-kpi-label">{MODULE_LABELS[module]}</div>
          <div className="eng-rv-kpi-value">{overview.byModule[module].total}</div>
          <div className="eng-rv-kpi-sub">{overview.byModule[module].weak} 个薄弱点</div>
        </div>
      ))}
      <div className="eng-rv-kpi accent-weak">
        <div className="eng-rv-kpi-label">薄弱知识点</div>
        <div className="eng-rv-kpi-value">{overview.weak}</div>
        <div className="eng-rv-kpi-sub">优先攻克，答对且换角度可消灭</div>
      </div>
    </div>
  );

  const unitTab = candidates.length === 0 ? (
    <>
      {wordQueueCard}
      <Empty
        description="暂无待复习的知识点，先去学习中心学习吧"
        image={Empty.PRESENTED_IMAGE_SIMPLE}
        style={{ marginTop: 24 }}
      />
    </>
  ) : (
    <>
      {wordQueueCard}
      {kpiRow}
      <div className="eng-rv-overall">
        <div className="eng-rv-overall-head">
          <span className="eng-rv-overall-title">掌握程度分布</span>
          <span className="eng-rv-overall-weak">薄弱 {overview.weak} 个</span>
        </div>
        <MasteryBar counts={overview.counts} total={overview.total} />
        <MasteryLegend counts={overview.counts} />
      </div>

      {moduleGroups.map(({ module, list }) => (
        <section className="eng-rv-module" key={module}>
          <div className="eng-rv-module-head">
            <span className={`eng-rv-module-tag module-${module}`}>{MODULE_LABELS[module]}</span>
            <span className="eng-rv-module-title">{MODULE_LABELS[module]}待复习</span>
            <span className="eng-rv-module-desc">{MODULE_HINTS[module]}</span>
            <span className="eng-rv-module-count">{list.length} 个 · {summarize(list).weak} 薄弱</span>
          </div>
          <div className="eng-rv-unit-grid">
            {groupByUnit(list).map(([unit, items]) => {
              const stat = summarize(items);
              return (
                <div className="eng-rv-unit" key={unit}>
                  <div className="eng-rv-unit-head">
                    <span className="eng-rv-unit-name">{unit}</span>
                    <span className="eng-rv-unit-badge">待复习 {items.length}</span>
                  </div>
                  <MasteryBar counts={stat.counts} total={stat.total} />
                  <MasteryLegend counts={stat.counts} />
                  {stat.weak > 0 && (
                    <div className="eng-rv-weak">薄弱知识点 {stat.weak} 个，建议优先复习</div>
                  )}
                  <ul className="eng-rv-items">
                    {items.slice(0, 5).map((c) => (
                      <li className="eng-rv-item" key={`${c.module}-${c.knowledgeId}`}>
                        <span className="eng-rv-item-name" title={c.name}>{c.name}</span>
                        {items.length > 0 && (
                          <Tag color={STATE_COLORS[c.state] || 'default'}>
                            {c.stateLabel || STATE_LABELS[c.state] || c.state}
                          </Tag>
                        )}
                        {c.reasons?.map((r) => (
                          REASON_LABELS[r] ? (
                            <span key={r} className="eng-review-reason">{REASON_LABELS[r]}</span>
                          ) : null
                        ))}
                      </li>
                    ))}
                  </ul>
                  {items.length > 5 && (
                    <div className="eng-review-more">还有 {items.length - 5} 个知识点，进入练习继续</div>
                  )}
                  <div className="eng-unit-actions" style={{ gridTemplateColumns: '1fr', marginTop: 12 }}>
                    <button
                      type="button"
                      className="eng-btn eng-btn-practice"
                      onClick={() => goModuleReview(unit, module, items)}
                    >
                      复习本单元 · {items.length}
                    </button>
                  </div>
                  {items.some((c) => c.nextAngleLabel) && (
                    <div className="eng-rv-angle">
                      建议考法：{items[0].nextAngleLabel}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </section>
      ))}
    </>
  );

  const wrongTab = (
    <EnglishWrongBookPanel items={wrongQuestions} onEliminated={onEliminated} />
  );

  return (
    <div className="eng-page">
      <div className="eng-hero">
        <div>
          <div className="eng-hero-title">英语智能复习</div>
          <div className="eng-hero-sub">
            {version} · {grade} · 单词、句式、语法分开复习并精确到单元，错题本重做原题，两条线互不影响
          </div>
        </div>
      </div>

      <Tabs
        items={[
          { key: 'unit', label: `复习总览（${candidates.length}）`, children: unitTab },
          { key: 'wrong', label: `我的错题本（${wrongQuestions.length}）`, children: wrongTab },
        ]}
      />

      <div className="eng-actions" style={{ marginTop: 20 }}>
        <button type="button" className="eng-btn eng-btn-ghost" onClick={() => navigate('/student/english')}>
          返回学习中心
        </button>
      </div>
    </div>
  );
}
