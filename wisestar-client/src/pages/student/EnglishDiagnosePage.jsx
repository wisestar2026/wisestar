/**
 * EnglishDiagnosePage.jsx - 学员端「英语学习诊断」（家长向，可打印）
 *
 * 功能:
 *   1. 介绍诊断作用：D1→D3 三档各 8 题（共 24 题），跨单元抽题
 *   2. 逐题作答（套卷模式，不即时判题）
 *   3. 交卷后生成家长可读的诊断报告：综合得分、知识模块、能力分析、
 *      学习趋势、薄弱知识点与学习建议，支持打印
 *
 * URL: /student/diagnose
 * 被谁引用: App.jsx 路由表；入口来自学员首页左侧「学习诊断」按钮
 *
 * 数据流:
 *   POST /api/english/student/diagnose-paper   三档诊断卷（剥离答案）
 *   POST /api/english/student/diagnose-submit  交卷判分 + 趋势
 *   GET  /api/student/archive                  学员姓名/学期（报告抬头）
 */

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Button, Progress, Spin, message } from 'antd';
import { LeftOutlined, PrinterOutlined, ThunderboltOutlined } from '@ant-design/icons';
import useStudentStore from '../../stores/useStudentStore';
import { getMyArchive } from '../../api/archive';
import { getEnglishDiagnosePaper, submitEnglishDiagnose } from '../../api/englishStudent';
import { getDetectHistory } from '../../api/detect';
import QuestionCard from '../../components/practice/QuestionCard';
import QuestionPreview from '../../components/practice/QuestionPreview';
import RichContent from '../../components/common/RichContent';
import { formatCorrectAnswers } from '../../utils/practiceHelpers';
import './EnglishDiagnosePage.css';

const LEVEL_COLOR = { D1: '#34c759', D2: '#2f8ae0', D3: '#f5a623' };

function scoreColor(score) {
  if (score >= 85) return '#34c759';
  if (score >= 60) return '#2f8ae0';
  if (score >= 40) return '#f5a623';
  return '#ff5b5b';
}

function statusColor(mastery) {
  if (mastery === '掌握') return '#34c759';
  if (mastery === '不稳定') return '#f5a623';
  return '#ff5b5b';
}

/** 学习趋势折线图（纯 SVG，无第三方图表依赖） */
function TrendChart({ points }) {
  const list = points || [];
  if (list.length === 0) {
    return <div className="eng-diag-empty">暂无趋势数据</div>;
  }
  const width = 680;
  const height = 210;
  const left = 42;
  const right = 20;
  const top = 20;
  const bottom = 34;
  const innerW = width - left - right;
  const innerH = height - top - bottom;
  const step = list.length > 1 ? innerW / (list.length - 1) : 0;
  const x = (i) => (list.length > 1 ? left + step * i : left + innerW / 2);
  const y = (score) => top + innerH - (innerH * Math.max(0, Math.min(100, score))) / 100;
  const path = list.map((p, i) => `${i === 0 ? 'M' : 'L'}${x(i).toFixed(1)},${y(p.masteryScore).toFixed(1)}`).join(' ');
  return (
    <div className="eng-diag-trend">
      <svg viewBox={`0 0 ${width} ${height}`} role="img" aria-label="学习趋势折线图">
        {[0, 25, 50, 75, 100].map((v) => (
          <g key={v}>
            <line x1={left} x2={width - right} y1={y(v)} y2={y(v)} stroke="#e3eef7" strokeWidth="1" />
            <text x={left - 8} y={y(v) + 4} textAnchor="end" fontSize="11" fill="#8aa2b5">{v}</text>
          </g>
        ))}
        {list.length > 1 && <path d={path} fill="none" stroke="#2f8ae0" strokeWidth="3" strokeLinecap="round" />}
        {list.map((p, i) => (
          <g key={`${p.date}-${i}`}>
            <circle cx={x(i)} cy={y(p.masteryScore)} r="5" fill="#fff" stroke="#2f8ae0" strokeWidth="3" />
            <text x={x(i)} y={y(p.masteryScore) - 12} textAnchor="middle" fontSize="12" fontWeight="600" fill="#1f5c8b">
              {p.masteryScore}
            </text>
            <text x={x(i)} y={height - 12} textAnchor="middle" fontSize="11" fill="#8aa2b5">{p.date}</text>
          </g>
        ))}
      </svg>
    </div>
  );
}

/** 维度进度条组 */
function DimensionBars({ list, emptyText = '暂无数据' }) {
  if (!list?.length) return <div className="eng-diag-empty">{emptyText}</div>;
  return (
    <div className="eng-diag-bars">
      {list.map((item) => (
        <div className="eng-diag-bar-row" key={item.name}>
          <span className="eng-diag-bar-name">{item.name}</span>
          <span className="eng-diag-bar-track">
            <span
              className="eng-diag-bar-fill"
              style={{ width: `${item.masteryScore}%`, background: scoreColor(item.masteryScore) }}
            />
          </span>
          <span className="eng-diag-bar-value">{item.masteryScore}</span>
        </div>
      ))}
    </div>
  );
}

/** 历史记录时间格式化：YYYY-MM-DD HH:mm */
function formatDate(value) {
  if (!value) return '—';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return '—';
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/** 作答用时格式化 */
function formatDuration(ms) {
  if (!ms || ms < 0) return '—';
  const minutes = Math.floor(ms / 60000);
  const seconds = Math.round((ms % 60000) / 1000);
  return minutes > 0 ? `${minutes} 分 ${seconds} 秒` : `${seconds} 秒`;
}

export default function EnglishDiagnosePage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  // 首页「学习检测」选择单元后跳转，?units=Welcome,Unit 1 限定诊断范围；为空时按当前册别全部单元出题
  const scopeUnits = useMemo(() => (searchParams.get('units') || '')
    .split(',')
    .map((u) => u.trim())
    .filter(Boolean), [searchParams]);
  const grade = useStudentStore((s) => s.grade);
  const term = useStudentStore((s) => s.term);
  const version = useStudentStore((s) => s.version);
  const activeSubject = useStudentStore((s) => s.activeSubject);
  const subjectId = activeSubject || '1003';

  const [step, setStep] = useState('intro');
  const [history, setHistory] = useState([]); // intro / answer / report
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [sections, setSections] = useState([]);
  const [answers, setAnswers] = useState({});
  const [changes, setChanges] = useState({});
  const [current, setCurrent] = useState(0);
  const [report, setReport] = useState(null);
  const [archive, setArchive] = useState(null);
  const [startAt, setStartAt] = useState(0);

  useEffect(() => {
    getMyArchive().then((res) => setArchive(res?.data || null)).catch(() => setArchive(null));
  }, []);

  // 学习诊断历史：只展示 DIAG 记录，保留每次诊断的得分与时间
  const loadHistory = useCallback(() => {
    getDetectHistory({ subjectId })
      .then((res) => setHistory((res?.data || []).filter((r) => r.detectType === 'DIAG')))
      .catch(() => setHistory([]));
  }, [subjectId]);

  useEffect(() => {
    loadHistory();
  }, [loadHistory]);

  const questions = useMemo(() => sections.flatMap((section) => (
    section.questions.map((q) => ({ ...q, level: section.level, levelLabel: section.label }))
  )), [sections]);

  const answeredCount = useMemo(() => questions.filter((item) => {
    const a = answers[item.id];
    return a && ((a.type === 'option' && a.optionId)
      || (a.type === 'options' && a.optionIds?.length > 0)
      || (a.type === 'text' && String(a.text || '').trim()));
  }).length, [questions, answers]);

  // 开始诊断：组卷
  const handleStart = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getEnglishDiagnosePaper({
        version,
        grade,
        term,
        units: scopeUnits.length > 0 ? scopeUnits : undefined,
      });
      const list = (res?.data?.sections || []).filter((s) => s.questions?.length);
      if (list.length === 0) {
        message.warning('当前年级/册别暂无可用诊断题，请先完成单元学习');
        setLoading(false);
        return;
      }
      setSections(list);
      setAnswers({});
      setChanges({});
      setCurrent(0);
      setReport(null);
      setStartAt(Date.now());
      setStep('answer');
    } catch (e) {
      message.error(e?.message || '诊断卷加载失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  }, [version, grade, term, scopeUnits]);

  // 记录选项改选次数：交给后端做「掌握 / 不稳定 / 漏洞」三态判定
  const handleAnswerChange = (id, value) => {
    const before = answers[id];
    if (value?.type === 'option' && before?.type === 'option'
      && before.optionId && before.optionId !== value.optionId) {
      setChanges((prev) => ({ ...prev, [id]: (prev[id] || 0) + 1 }));
    }
    setAnswers((prev) => ({ ...prev, [id]: value }));
  };

  const handleSubmit = async () => {
    const items = questions.map((q) => ({
      contentKey: q.id,
      answer: answers[q.id] || {},
      changes: changes[q.id] || 0,
    }));
    setSubmitting(true);
    try {
      const res = await submitEnglishDiagnose({
        version,
        grade,
        term,
        items,
        durationMs: startAt ? Date.now() - startAt : undefined,
      });
      setReport(res?.data || null);
      setStep('report');
      loadHistory();
      window.scrollTo({ top: 0, behavior: 'smooth' });
      message.success('诊断完成，报告已生成');
    } catch (e) {
      message.error(e?.message || '交卷失败，请稍后重试');
    } finally {
      setSubmitting(false);
    }
  };

  // ============================ 介绍页 ============================
  if (step === 'intro') {
    return (
      <div className="eng-diag-wrap sll-page-enter">
        <div className="eng-diag-intro">
          <div className="eng-diag-intro-badge">英语学习诊断</div>
          <h1 className="eng-diag-intro-title">一次最多 24 题，看清孩子的英语掌握情况</h1>
          <p className="eng-diag-intro-desc">
            诊断卷按基础、进阶、挑战三档递进，从当前册别所选单元抽取真实题目。
            交卷后生成一份可打印的学习报告，包含综合得分、知识模块、能力分析与学习趋势，
            方便老师与家长一起看。
          </p>
          {scopeUnits.length > 0 && (
            <div className="eng-diag-intro-scope">本次范围：{scopeUnits.join(' / ')}</div>
          )}
          <div className="eng-diag-intro-grid">
            <div className="eng-diag-intro-item">
              <span className="eng-diag-intro-num">24</span>
              <span className="eng-diag-intro-label">道诊断题</span>
            </div>
            <div className="eng-diag-intro-item">
              <span className="eng-diag-intro-num">3</span>
              <span className="eng-diag-intro-label">档难度递进</span>
            </div>
            <div className="eng-diag-intro-item">
              <span className="eng-diag-intro-num">6</span>
              <span className="eng-diag-intro-label">维报告分析</span>
            </div>
          </div>
          <div className="eng-diag-intro-tips">
            <div>基础 8 题：确认已学内容是否扎实</div>
            <div>进阶 8 题：检验能否灵活运用</div>
            <div>挑战 8 题：看下一步提升空间</div>
          </div>
          <Button
            type="primary"
            size="large"
            className="eng-diag-start-btn"
            icon={<ThunderboltOutlined />}
            loading={loading}
            onClick={handleStart}
          >
            开始诊断
          </Button>
          <Button type="link" onClick={() => navigate('/student')}>返回首页</Button>
        </div>

        {history.length > 0 && (
          <div className="eng-diag-history">
            <div className="eng-diag-history-head">
              <span>历史诊断记录</span>
              <span className="eng-diag-history-count">共 {history.length} 次</span>
            </div>
            <div className="eng-diag-history-list">
              {history.map((item) => (
                <div className="eng-diag-history-item" key={item.id}>
                  <div className="eng-diag-history-score" style={{ color: scoreColor(item.accuracy || 0) }}>
                    {item.accuracy ?? 0}<small>分</small>
                  </div>
                  <div className="eng-diag-history-main">
                    <div className="eng-diag-history-date">{formatDate(item.createTime)}</div>
                    <div className="eng-diag-history-meta">
                      {item.correct ?? 0}/{item.total ?? 0} 题 · 用时 {formatDuration(item.durationMs)}
                    </div>
                  </div>
                  <div className="eng-diag-history-weak">
                    {(item.weakPoints || []).slice(0, 2).map((w) => <span key={w.name}>{w.name}</span>)}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    );
  }

  // ============================ 作答页 ============================
  if (step === 'answer') {
    const q = questions[current];
    if (!q) {
      return <div className="eng-diag-wrap"><Spin /></div>;
    }
    return (
      <div className="eng-diag-wrap sll-page-enter">
        <div className="eng-diag-answer-head">
          <Button type="text" icon={<LeftOutlined />} onClick={() => setStep('intro')}>退出诊断</Button>
          <div className="eng-diag-answer-title">英语学习诊断</div>
          <div className="eng-diag-answer-meta">已作答 {answeredCount} / {questions.length}</div>
        </div>
        <Progress
          percent={Math.round((answeredCount / questions.length) * 100)}
          showInfo={false}
          strokeColor="#2f8ae0"
          className="no-print"
        />
        <div className="eng-diag-nav">
          {questions.map((item, idx) => {
            const a = answers[item.id];
            const done = a && ((a.type === 'option' && a.optionId)
              || (a.type === 'options' && a.optionIds?.length > 0)
              || (a.type === 'text' && String(a.text || '').trim()));
            return (
              <button
                key={item.id}
                type="button"
                className={`eng-diag-nav-dot ${idx === current ? 'is-current' : ''} ${done ? 'is-done' : ''}`}
                style={{ '--dot-color': LEVEL_COLOR[item.level] || '#2f8ae0' }}
                onClick={() => setCurrent(idx)}
              >
                {idx + 1}
              </button>
            );
          })}
        </div>
        <div className="eng-diag-level-tip" style={{ color: LEVEL_COLOR[q.level] || '#2f8ae0' }}>
          第 {current + 1} 题 · {q.levelLabel || q.level}档
        </div>
        <div className="eng-diag-question quiz-stage">
          <QuestionCard
            question={{ ...q, template: q.schema }}
            index={current + 1}
            total={questions.length}
            value={answers[q.id]}
            onChange={(v) => handleAnswerChange(q.id, v)}
            judgeMode={false}
            keyboardDefaultTab="letter"
          />
        </div>
        <div className="eng-diag-actions">
          <Button size="large" disabled={current === 0} onClick={() => setCurrent((c) => c - 1)}>
            上一题
          </Button>
          {current < questions.length - 1 ? (
            <Button type="primary" size="large" onClick={() => setCurrent((c) => c + 1)}>
              下一题
            </Button>
          ) : (
            <Button type="primary" size="large" loading={submitting} onClick={handleSubmit}>
              交卷生成报告
            </Button>
          )}
        </div>
        <div className="eng-diag-submit-foot">
          <Button type="link" onClick={handleSubmit} loading={submitting}>提前交卷</Button>
        </div>
      </div>
    );
  }

  // ============================ 报告页 ============================
  const rep = report || {
    total: 0, correct: 0, accuracy: 0, masteryScore: 0,
    mastered: 0, unstable: 0, flawed: 0, details: [], weakPoints: [],
    moduleStats: [], skillStats: [], cognitiveStats: [], typeStats: [],
    difficultyStats: [], trend: [],
  };
  const total = rep.total || 0;
  const masteredPct = total ? (rep.mastered / total) * 100 : 0;
  const unstablePct = total ? (rep.unstable / total) * 100 : 0;
  const donut = `conic-gradient(#34c759 0 ${masteredPct}%, #f5a623 ${masteredPct}% ${masteredPct + unstablePct}%, #ff5b5b ${masteredPct + unstablePct}% 100%)`;
  const weakSkills = [...(rep.skillStats || [])]
    .filter((s) => s.masteryScore < 70)
    .sort((a, b) => a.masteryScore - b.masteryScore)
    .slice(0, 2);
  const weakModules = [...(rep.moduleStats || [])]
    .filter((s) => s.masteryScore < 70)
    .sort((a, b) => a.masteryScore - b.masteryScore)
    .slice(0, 2);
  const suggestions = [];
  if (rep.masteryScore >= 85) {
    suggestions.push('整体掌握扎实，可以进入下一单元或挑战更高难度，保持每周 2~3 次练习节奏。');
  } else if (rep.masteryScore >= 70) {
    suggestions.push('基础较稳，个别知识点还不稳定；把标记为「不稳定」的题目再做一遍，争取一次答对。');
  } else if (rep.masteryScore >= 55) {
    suggestions.push('基础题能作答，但遇到变化就容易卡住；建议先回到课本句子和单词，逐句读熟再练习。');
  } else {
    suggestions.push('当前需要先打基础：从单词跟读、句子模仿开始，每天 10 分钟，先保证基础题稳定答对。');
  }
  if (weakModules.length) {
    suggestions.push(`薄弱模块：${weakModules.map((m) => m.name).join('、')}。建议优先复习对应情景对话与重点句式。`);
  }
  if (weakSkills.length) {
    suggestions.push(`能力短板：${weakSkills.map((m) => m.name).join('、')}。老师可针对这两项安排专项练习。`);
  }
  if ((rep.weakPoints || []).length) {
    const names = rep.weakPoints.slice(0, 4).map((w) => `${w.chapter ? `${w.chapter}·` : ''}${w.name}`);
    suggestions.push(`本次需要重点巩固：${names.join('、')}。`);
  }

  return (
    <div className="eng-diag-wrap sll-page-enter">
      <div className="eng-diag-report-actions no-print">
        <Button type="text" icon={<LeftOutlined />} onClick={() => navigate('/student')}>返回首页</Button>
        <div className="eng-diag-report-actions-right">
          <Button icon={<ThunderboltOutlined />} onClick={() => setStep('intro')}>再测一次</Button>
          <Button type="primary" icon={<PrinterOutlined />} onClick={() => window.print()}>打印报告</Button>
        </div>
      </div>

      <article className="eng-diag-paper">
        <header className="eng-diag-paper-head">
          <div>
            <div className="eng-diag-brand">智慧星 · 英语学习诊断报告</div>
            <h2 className="eng-diag-paper-title">英语能力诊断与学习建议</h2>
          </div>
          <div className="eng-diag-paper-meta">
            <div>学员：{archive?.studentName || '同学'}</div>
            <div>年级/册别：{grade || '—'} / {term || '—'}</div>
            <div>诊断日期：{new Date().toLocaleDateString('zh-CN')}</div>
            <div>题量：{total} 题</div>
          </div>
        </header>

        <section className="eng-diag-overview">
          <div className="eng-diag-donut-wrap">
            <div className="eng-diag-donut" style={{ background: donut }}>
              <div className="eng-diag-donut-inner">
                <div className="eng-diag-donut-score">{rep.masteryScore}</div>
                <div className="eng-diag-donut-label">综合得分</div>
              </div>
            </div>
          </div>
          <div className="eng-diag-overview-main">
            <div className="eng-diag-overview-title">本次诊断结论</div>
            <div className="eng-diag-overview-line">
              共 {total} 题，一次答对 {rep.correct} 题，正确率 {rep.accuracy}%。
            </div>
            <div className="eng-diag-chips">
              <span className="eng-diag-chip" style={{ '--chip-color': '#34c759' }}>掌握 {rep.mastered}</span>
              <span className="eng-diag-chip" style={{ '--chip-color': '#f5a623' }}>不稳定 {rep.unstable}</span>
              <span className="eng-diag-chip" style={{ '--chip-color': '#ff5b5b' }}>漏洞 {rep.flawed}</span>
            </div>
            <div className="eng-diag-level-bars">
              {(rep.difficultyStats || []).filter((d) => ['D1', 'D2', 'D3'].includes(d.name)).map((d) => (
                <div className="eng-diag-level-row" key={d.name}>
                  <span className="eng-diag-level-name" style={{ color: LEVEL_COLOR[d.name] }}>{d.name}</span>
                  <span className="eng-diag-bar-track">
                    <span className="eng-diag-bar-fill" style={{ width: `${d.masteryScore}%`, background: LEVEL_COLOR[d.name] }} />
                  </span>
                  <span className="eng-diag-bar-value">{d.masteryScore}</span>
                </div>
              ))}
            </div>
          </div>
        </section>

        <section className="eng-diag-section">
          <h3 className="eng-diag-section-title">一、知识模块分析</h3>
          <div className="eng-diag-two-col">
            <div>
              <div className="eng-diag-sub-title">模块掌握度</div>
              <DimensionBars list={rep.moduleStats} />
            </div>
            <div>
              <div className="eng-diag-sub-title">本次涉及单元</div>
              <div className="eng-diag-chapter-list">
                {(rep.chapterStats || []).map((c) => (
                  <div className="eng-diag-chapter-row" key={c.name}>
                    <span>{c.name}</span>
                    <span>{c.total} 题 · {c.accuracy}%</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </section>

        <section className="eng-diag-section">
          <h3 className="eng-diag-section-title">二、能力分析</h3>
          <div className="eng-diag-two-col">
            <div>
              <div className="eng-diag-sub-title">语言技能</div>
              <DimensionBars list={rep.skillStats} />
            </div>
            <div>
              <div className="eng-diag-sub-title">认知层级</div>
              <DimensionBars list={rep.cognitiveStats} />
            </div>
          </div>
          <div className="eng-diag-two-col eng-diag-mt">
            <div>
              <div className="eng-diag-sub-title">题型表现</div>
              <DimensionBars list={rep.typeStats} />
            </div>
            <div>
              <div className="eng-diag-sub-title">难度表现</div>
              <DimensionBars list={rep.difficultyStats} />
            </div>
          </div>
        </section>

        <section className="eng-diag-section">
          <h3 className="eng-diag-section-title">三、学习趋势追踪</h3>
          <TrendChart points={rep.trend} />
          {(rep.trend || []).length < 2 && (
            <div className="eng-diag-empty">完成 2 次以上诊断后，可看到得分变化曲线。</div>
          )}
        </section>

        <section className="eng-diag-section">
          <h3 className="eng-diag-section-title">四、薄弱知识点</h3>
          {!(rep.weakPoints || []).length ? (
            <div className="eng-diag-empty">本次没有发现低于掌握线的知识点，保持得不错。</div>
          ) : (
            <table className="eng-diag-table">
              <thead>
                <tr>
                  <th>单元</th>
                  <th>知识点</th>
                  <th>题量</th>
                  <th>答对</th>
                  <th>正确率</th>
                </tr>
              </thead>
              <tbody>
                {rep.weakPoints.map((w) => (
                  <tr key={`${w.chapter}-${w.name}`}>
                    <td>{w.chapter || '—'}</td>
                    <td>{w.name}</td>
                    <td>{w.total}</td>
                    <td>{w.correct}</td>
                    <td style={{ color: scoreColor(w.accuracy), fontWeight: 600 }}>{w.accuracy}%</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>

        <section className="eng-diag-section">
          <h3 className="eng-diag-section-title">五、学习建议</h3>
          <ul className="eng-diag-advice">
            {suggestions.map((text, idx) => (
              <li key={idx}>{text}</li>
            ))}
          </ul>
        </section>

        <section className="eng-diag-section eng-diag-detail-section">
          <h3 className="eng-diag-section-title">附：逐题回顾</h3>
          <div className="eng-diag-detail-list">
            {(rep.details || []).map((d, idx) => (
              <div key={d.questionId} className="eng-diag-detail-item">
                <div className="eng-diag-detail-head">
                  <span className="eng-diag-detail-index">{idx + 1}</span>
                  <span className="eng-diag-detail-status" style={{ background: statusColor(d.mastery) }}>
                    {d.mastery || (d.correct === 1 ? '答对' : '答错')}
                  </span>
                  <span className="eng-diag-detail-kp">{d.chapter ? `${d.chapter} · ` : ''}{d.knowledgePoint}</span>
                </div>
                <div className="eng-diag-detail-body">
                  {d.question && (
                    <div className="eng-diag-detail-question">
                      <QuestionPreview question={d.question} />
                    </div>
                  )}
                  <div className="eng-diag-detail-line">
                    <span>正确答案：</span>
                    <b>{formatCorrectAnswers(d.questionType, d.correctAnswers) || '（无）'}</b>
                  </div>
                  <div className="eng-diag-detail-line">
                    <span>学生答案：</span>
                    <b style={{ color: d.correct === 1 ? '#34c759' : '#ff5b5b' }}>{d.studentAnswer || '（未作答）'}</b>
                  </div>
                  {d.analysis && (
                    <div className="eng-diag-detail-analysis">
                      <b>解析：</b><RichContent text={d.analysis} />
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        </section>

        <footer className="eng-diag-paper-foot">
          <div>教师签字：________________</div>
          <div>家长签字：________________</div>
          <div>日期：________________</div>
        </footer>
      </article>
    </div>
  );
}
