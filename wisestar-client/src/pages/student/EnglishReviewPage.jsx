/**
 * EnglishReviewPage.jsx - 学员端英语分类复习（待复习清单）
 *
 * 功能:
 *   1. 只列出「待复习」内容：有作答记录、且 生疏（familiarity≤1）或 做错过（wrongCount>0）
 *      的 单词 / 重点句子 / 语法；没有做过、没有提交记录的一律不出现
 *   2. 单词：不分单元，只取「已到复习时间」的单词（按下次复习时间升序），单次最多 20 词；
 *      没有到期单词时显示 0，点击开始即可跨单元复习
 *   3. 重点句子：按单元列出待复习句子，点击进入句子练习
 *   4. 语法：按单元列出待复习语法点，点击进入语法练习（回写掌握度）
 *   5. 错题：聚合英语练习（单元练习/语法练习）做错的题目，仅回显题干、隐藏答案解析；
 *      每道错题可「消灭」，页内弹层重做，答对即从复习错题中移除（与数学错题消灭一致）
 *
 * URL: /student/english/review
 * 被谁引用: App.jsx 路由表；入口来自英语学习中心「智能复习」
 *
 * 数据流:
 *   GET /api/english/student/review/words     单词复习队列（跨单元、按熟练度/间隔排序）
 *   GET /api/english/student/review/word-summary 单词复习概览（应复习词数、熟练度分布、涉及单元）
 *   GET /api/english/student/sentences        句子列表（含熟练度）
 *   GET /api/english/student/grammars         语法列表（含掌握度）
 *   GET /api/english/student/wrong-questions  英语复习错题（来自检测记录，隐藏答案解析）
 *   GET /api/english/student/wrong-question   单道错题详情（含标准答案，重做用）
 *   POST /api/english/student/wrong/redo      错题重做（答对即消灭）
 */

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Empty, Spin, Tabs, Tag, message } from 'antd';
import useStudentStore from '../../stores/useStudentStore';
import {
  getEnglishWordReview,
  getEnglishWordReviewSummary,
  getEnglishSentences,
  getEnglishGrammars,
  getEnglishWrongQuestions,
  getEnglishWrongQuestion,
  redoEnglishWrong,
} from '../../api/englishStudent';
import QuestionPreview from '../../components/practice/QuestionPreview';
import WrongDrillModal from './WrongDrillModal';
import './EnglishCenterPage.css';

const FAMILIARITY_LABELS = ['未学习', '生疏', '熟悉', '熟练', '精通'];
const FAMILIARITY_COLORS = ['default', 'orange', 'blue', 'cyan', 'green'];

/** 英语错题消灭不增补提高题（提高题数据源为数学专用），显式返回 null */
const noopHarder = async () => null;

/** 是否有作答记录（做过 / 提交过）；未学习的一律不算 */
function hasRecord(item) {
  return (item.correctCount || 0) + (item.wrongCount || 0) > 0;
}

/** 是否待复习：做错过，或已到复习时间（按间隔复习算法，不把刚学完的单词全部列入） */
function needsReview(item) {
  if (!hasRecord(item)) return false;
  if ((item.wrongCount || 0) > 0) return true;
  return Boolean(item.nextReviewTime) && new Date(item.nextReviewTime).getTime() <= Date.now();
}

function famTag(familiarity) {
  const lv = Math.min(Math.max(familiarity || 0, 0), 4);
  return <Tag color={FAMILIARITY_COLORS[lv]}>{FAMILIARITY_LABELS[lv]}</Tag>;
}

/** 按单元分组（保持传入顺序） */
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
  const [sentences, setSentences] = useState([]);
  const [grammars, setGrammars] = useState([]);
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
      getEnglishSentences({ version, grade, term }).catch(() => ({ data: [] })),
      getEnglishGrammars({ version, grade, term }).catch(() => ({ data: [] })),
      getEnglishWrongQuestions({ limit: 100 }).catch(() => ({ data: [] })),
    ])
      .then(([wRes, sumRes, sRes, gRes, wqRes]) => {
        setWordPlan(wRes?.data || []);
        setWordSummary(sumRes?.data || null);
        setSentences(sRes?.data || []);
        setGrammars(gRes?.data || []);
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

  // 待复习清单（做过 + 生疏/做错过）
  const reviewSentences = useMemo(() => sentences.filter(needsReview), [sentences]);
  const reviewGrammars = useMemo(() => grammars.filter(needsReview), [grammars]);

  const sentenceGroups = useMemo(() => groupByUnit(reviewSentences), [reviewSentences]);
  const grammarGroups = useMemo(() => groupByUnit(reviewGrammars), [reviewGrammars]);

  const goWordReview = () => {
    if (!wordPlan.length) return;
    const ids = wordPlan.map((w) => w.id).join(',');
    navigate(`/student/english/practice?mode=review&ids=${encodeURIComponent(ids)}`);
  };
  const goSentence = (unit) => navigate(`/student/english/sentence?unit=${encodeURIComponent(unit)}`);
  const goGrammar = (unit) => {
    const sid = activeSubject || '1003';
    navigate(`/student/english/grammar-practice?unit=${encodeURIComponent(unit)}&subjectId=${encodeURIComponent(sid)}`);
  };

  if (loading) {
    return <div className="eng-learn-wrap"><Spin /></div>;
  }

  // 没有到期单词时同样出卡并显示 0，让学员看到「今天没有要复习的词」，而不是一块空白
  const wordTab = (
    <div className="eng-grid">
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
    </div>
  );

  const sentenceTab = sentenceGroups.length === 0 ? (
    <Empty description="暂无待复习的重点句子，先去学习中心学习吧" image={Empty.PRESENTED_IMAGE_SIMPLE} />
  ) : (
    <div className="eng-grammar-list">
      {sentenceGroups.map(([unit, list]) => (
        <div className="eng-grammar-card" key={unit}>
          <div className="eng-grammar-head">
            <span className="eng-grammar-index">💬</span>
            <span className="eng-grammar-title">{unit}</span>
            <span style={{ marginLeft: 'auto' }}>
              <button type="button" className="eng-btn-sentence eng-btn" onClick={() => goSentence(unit)}>
                复习该单元
              </button>
            </span>
          </div>
          <div className="eng-grammar-examples">
            <ul>
              {list.map((s) => (
                <li
                  key={s.id}
                  style={{ cursor: 'pointer' }}
                  onClick={() => goSentence(unit)}
                  title="点击进入该单元句子练习"
                >
                  <span className="eng-grammar-en">{s.en}</span>
                  {s.zh && <span className="eng-grammar-zh">{s.zh}</span>}
                  {famTag(s.familiarity)}
                </li>
              ))}
            </ul>
          </div>
        </div>
      ))}
    </div>
  );

  const grammarTab = grammarGroups.length === 0 ? (
    <Empty description="暂无待复习的语法，先去学习中心学习吧" image={Empty.PRESENTED_IMAGE_SIMPLE} />
  ) : (
    <div className="eng-grammar-list">
      {grammarGroups.map(([unit, list]) => (
        <div className="eng-grammar-card" key={unit}>
          <div className="eng-grammar-head">
            <span className="eng-grammar-index">📘</span>
            <span className="eng-grammar-title">{unit}</span>
            <span style={{ marginLeft: 'auto' }}>
              <button type="button" className="eng-btn-grammar eng-btn" onClick={() => goGrammar(unit)}>
                语法练习
              </button>
            </span>
          </div>
          <div className="eng-grammar-examples">
            <ul>
              {list.map((g) => (
                <li key={g.id}>
                  <span className="eng-grammar-en">{g.title}</span>
                  {famTag(g.familiarity)}
                </li>
              ))}
            </ul>
          </div>
        </div>
      ))}
    </div>
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
            {version} · {grade} · 只复习做过且生疏/做错的内容（陌生词汇 · 重点句子 · 语法 · 错题）
          </div>
        </div>
      </div>

      <Tabs
        items={[
          { key: 'word', label: `待复习单词（${wordPlan.length}）`, children: wordTab },
          { key: 'sentence', label: `待复习句子（${reviewSentences.length}）`, children: sentenceTab },
          { key: 'grammar', label: `待复习语法（${reviewGrammars.length}）`, children: grammarTab },
          { key: 'wrong', label: `错题（${wrongQuestions.length}）`, children: wrongTab },
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
