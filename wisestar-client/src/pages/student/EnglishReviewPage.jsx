/**
 * EnglishReviewPage.jsx - 学员端英语智能复习
 *
 * 结构:
 *   顶部两个 Tab：
 *     1. 单元复习：按知识点出「考察角度不同于原题」的派生题，来源是掌握状态一般/陌生、
 *        本单元做错、到达复习时间的知识点；不跨单元取题，也不动全局错题本。
 *     2. 我的错题本：沿用既有错题记录，重做原题、答对即消灭。
 *
 * 数据口径:
 *   - 单元复习的待复习池与角标数字取同一个接口 /review/candidates，避免数字与队列打架
 *   - 单词复习队列 /review/words 仍按间隔复习调度给出当日 20 词，卡片展示概览
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
  getEnglishWrongQuestion,
  redoEnglishWrong,
} from '../../api/englishStudent';
import QuestionPreview from '../../components/practice/QuestionPreview';
import WrongDrillModal from './WrongDrillModal';
import './EnglishCenterPage.css';

const MODULE_LABELS = { word: '单词', sentence: '句式', grammar: '语法' };
const MODULE_COLORS = { word: 'blue', sentence: 'orange', grammar: 'purple' };
const STATE_COLORS = { familiar: 'green', ordinary: 'gold', unfamiliar: 'red' };
const REASON_LABELS = { weak: '薄弱', wrong: '做错过', due: '到期' };

/** 英语错题消灭不增补提高题（提高题数据源为数学专用），显式返回 null */
const noopHarder = async () => null;

function groupByUnit(list) {
  const map = new Map();
  list.forEach((item) => {
    const key = item.unit || '未分单元';
    if (!map.has(key)) map.set(key, []);
    map.get(key).push(item);
  });
  return Array.from(map.entries());
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
  const [drillItem, setDrillItem] = useState(null);

  /** 英语错题重做的取题函数（含标准答案），注入给通用错题消灭弹层 */
  const fetchWrongQuestion = useCallback(async (item) => {
    const res = await getEnglishWrongQuestion({ questionId: item.questionId });
    return res?.data;
  }, []);

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
    setDrillItem(null);
    load();
  }, [load]);

  const unitGroups = useMemo(() => groupByUnit(candidates), [candidates]);

  const goWordReview = () => {
    if (!wordPlan.length) return;
    const ids = wordPlan.map((w) => w.id).join(',');
    navigate(`/student/english/practice?mode=review&ids=${encodeURIComponent(ids)}`);
  };

  /** 按模块把候选知识点送到对应练习页（单词带候选 id，句式/语法按单元直达） */
  const goModuleReview = (unit, module) => {
    if (module === 'word') {
      const ids = candidates
        .filter((c) => c.unit === unit && c.module === 'word')
        .map((c) => c.knowledgeId)
        .join(',');
      navigate(`/student/english/practice?mode=review&ids=${encodeURIComponent(ids)}`);
      return;
    }
    if (module === 'sentence') {
      navigate(`/student/english/sentence?unit=${encodeURIComponent(unit)}`);
      return;
    }
    const sid = activeSubject || '1003';
    navigate(`/student/english/grammar-practice?unit=${encodeURIComponent(unit)}&subjectId=${encodeURIComponent(sid)}`);
  };

  if (loading) {
    return <div className="eng-learn-wrap"><Spin /></div>;
  }

  const wordCard = (
    <div className="eng-unit-card">
      <div className="eng-unit-name">单词复习 · 本次 {wordPlan.length} 词</div>
      {wordPlan.length ? (
        <>
          <div style={{ margin: '10px 0 4px', fontSize: 14, color: '#5b7c9b' }}>
            {wordSummary
              ? `应复习单词 ${wordSummary.due} 个 · 陌生 ${wordSummary.unfamiliar} 个、一般 ${wordSummary.ordinary} 个、熟练 ${wordSummary.familiar} 个`
              : `本次待复习 ${wordPlan.length} 词`}
          </div>
          <div style={{ margin: '0 0 4px', fontSize: 14, color: '#5b7c9b' }}>
            {wordSummary
              ? `涉及单元 ${wordSummary.unitCount} 个${wordSummary.units?.length ? `（${wordSummary.units.join('、')}${wordSummary.unitCount > wordSummary.units.length ? ' 等' : ''}）` : ''}`
              : '涉及多个单元'}
          </div>
          <div style={{ fontSize: 13, color: '#8aa2b3', lineHeight: 1.7 }}>
            一次复习 {wordSummary?.batchSize || 20} 个，越久没复习的越靠前
          </div>
        </>
      ) : (
        <>
          <div style={{ margin: '10px 0 4px', fontSize: 14, color: '#5b7c9b' }}>
            应复习单词 0 个 · 陌生 0 个、一般 0 个、熟练 0 个
          </div>
          <div style={{ fontSize: 13, color: '#8aa2b3', lineHeight: 1.7 }}>
            当前没有到复习时间的单词，按记忆周期安排稍后再来
          </div>
        </>
      )}
      <div className="eng-unit-actions" style={{ gridTemplateColumns: '1fr', marginTop: 14 }}>
        <button
          type="button"
          className="eng-btn-practice"
          onClick={goWordReview}
          disabled={wordPlan.length === 0}
        >
          开始复习 · {wordPlan.length}
        </button>
      </div>
    </div>
  );

  const unitTab = (
    <>
      <div className="eng-grid">{wordCard}</div>
      {unitGroups.length === 0 ? (
        <Empty
          description="暂无待复习的知识点，先去学习中心学习吧"
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          style={{ marginTop: 24 }}
        />
      ) : (
        <div className="eng-grammar-list" style={{ marginTop: 20 }}>
          {unitGroups.map(([unit, list]) => {
            const modules = ['word', 'sentence', 'grammar'].filter((m) => list.some((c) => c.module === m));
            return (
              <div className="eng-grammar-card" key={unit}>
                <div className="eng-grammar-head">
                  <span className="eng-grammar-index">🔁</span>
                  <span className="eng-grammar-title">{unit}</span>
                  <span className="eng-review-unit-count">待复习 {list.length} 个知识点</span>
                </div>
                <div className="eng-grammar-examples">
                  <ul>
                    {list.slice(0, 12).map((c) => (
                      <li key={`${c.module}-${c.knowledgeId}`}>
                        <Tag color={MODULE_COLORS[c.module]}>{MODULE_LABELS[c.module] || c.module}</Tag>
                        <span className="eng-grammar-en">{c.name}</span>
                        <Tag color={STATE_COLORS[c.state] || 'default'}>{c.stateLabel}</Tag>
                        {c.weak && <Tag color="red">薄弱</Tag>}
                        {c.reasons?.map((r) => (
                          REASON_LABELS[r] ? (
                            <span key={r} className="eng-review-reason">{REASON_LABELS[r]}</span>
                          ) : null
                        ))}
                        {c.nextAngleLabel && (
                          <span className="eng-review-angle">建议考法：{c.nextAngleLabel}</span>
                        )}
                      </li>
                    ))}
                  </ul>
                  {list.length > 12 && (
                    <div className="eng-review-more">还有 {list.length - 12} 个知识点，按模块进入即可继续复习</div>
                  )}
                </div>
                <div className="eng-unit-actions" style={{ marginTop: 12 }}>
                  {modules.map((m) => (
                    <button
                      key={m}
                      type="button"
                      className="eng-btn eng-btn-practice"
                      onClick={() => goModuleReview(unit, m)}
                    >
                      {MODULE_LABELS[m]}复习
                    </button>
                  ))}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </>
  );

  const wrongTab = wrongQuestions.length === 0 ? (
    <Empty description="暂无错题，继续加油" image={Empty.PRESENTED_IMAGE_SIMPLE} />
  ) : (
    <div className="eng-wrong-list">
      {wrongQuestions.map((w) => (
        <div className="eng-wrong-item" key={w.questionId}>
          <div className="eng-wrong-head">
            <span className="eng-wrong-unit">{w.chapter || '未分单元'}</span>
            {w.knowledgePoint && <Tag color="blue">{w.knowledgePoint}</Tag>}
            <span className="eng-wrong-times">做错 {(w.wrongCount || 1)} 次</span>
            <span style={{ marginLeft: 'auto' }}>
              <button type="button" className="eng-btn-eliminate eng-btn" onClick={() => setDrillItem(w)}>
                消灭错题
              </button>
            </span>
          </div>
          <QuestionPreview question={w.question} />
        </div>
      ))}
    </div>
  );

  return (
    <div className="eng-page">
      <div className="eng-hero">
        <div>
          <div className="eng-hero-title">英语智能复习</div>
          <div className="eng-hero-sub">
            {version} · {grade} · 单元复习按知识点换角度出新题，错题本重做原题，两条线互不影响
          </div>
        </div>
      </div>

      <Tabs
        items={[
          { key: 'unit', label: `单元复习（${candidates.length}）`, children: unitTab },
          { key: 'wrong', label: `我的错题本（${wrongQuestions.length}）`, children: wrongTab },
        ]}
      />

      <div className="eng-actions" style={{ marginTop: 20 }}>
        <button type="button" className="eng-btn eng-btn-ghost" onClick={() => navigate('/student/english')}>
          返回学习中心
        </button>
      </div>

      <WrongDrillModal
        open={Boolean(drillItem)}
        mode="single"
        originals={drillItem ? [drillItem] : []}
        onClose={() => setDrillItem(null)}
        onEliminated={onEliminated}
        fetchOriginal={fetchWrongQuestion}
        redoQuestion={redoEnglishWrong}
        fetchHarder={noopHarder}
      />
    </div>
  );
}
