/**
 * EnglishSentenceLearnPage.jsx - 学员端英语「句式通关」（左侧单元导航 + 句子学习/句式情景）
 *
 * 结构（对应「句式学习界面」参考图）:
 *   顶部 hub：左侧 UNIT 导航，右侧「UnitX 句式通关」标题 + 句子进度 +
 *            四段入口卡（课本跟读 / 句子学习 / 句子练习 / 情景对话）
 *   学习态：交由 EnglishSentenceExercise 承担（听音组句 / 连词成句 / 词汇填空 / 选择 / 默写）
 *
 * URL: /student/english/sentence?unit=xxx
 * 被谁引用: App.jsx 路由表；入口来自英语学习中心「句式通关」
 *
 * 数据流:
 *   GET  /api/english/student/units       → 单元列表（左侧导航）
 *   GET  /api/english/student/sentences   → 单元句子（含熟练度 familiarity，用于完成度）
 *   GET  /api/english/student/unit-pass   → 单元句式达标率
 */

import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Spin, message } from 'antd';
import useStudentStore from '../../stores/useStudentStore';
import { getEnglishUnits, getEnglishSentences, getEnglishGrammars, getEnglishUnitPass } from '../../api/englishStudent';
import EnglishUnitAside from './EnglishUnitAside';
import EnglishSentenceExercise from './EnglishSentenceExercise';
import './EnglishCenterPage.css';

/** 后端以 familiarity>=1 视为「已学」，完成度按此口径统计。 */
const FINISHED_FAMILIARITY = 1;

function percent(finished, total) {
  if (!total) return 0;
  return Math.min(100, Math.round((finished / total) * 100));
}

/** 解析语法例句列表（JSON 字符串），容错为空数组 */
function parseExamples(raw) {
  if (!raw) return [];
  try {
    const value = typeof raw === 'string' ? JSON.parse(raw) : raw;
    return Array.isArray(value) ? value : [];
  } catch {
    return [];
  }
}

/** 例句既可能是纯字符串，也可能是 {en} / {text} 结构 */
function exampleText(item) {
  if (item == null) return '';
  if (typeof item === 'string') return item;
  return item.en || item.text || item.example || '';
}

/** 句子归一化：去标点与多余空格并转小写，用于判定句式与语法是否共用同一句 */
function normalizeSentence(text) {
  return String(text || '').toLowerCase().replace(/[^a-z0-9' ]/g, ' ').replace(/\s+/g, ' ').trim();
}

/** 句式四段入口卡：段序 + 段名 + 说明 +（可选）达标率 */
function SentenceStageCard({ index, name, hint, rate, onClick }) {
  return (
    <button type="button" className="eng-sentence-stage" onClick={onClick}>
      <span className="eng-stage-index">{index}</span>
      <span className="eng-sentence-stage-name">{name}</span>
      <span className="eng-sentence-stage-hint">{hint}</span>
      {rate != null && <span className="eng-sentence-stage-rate">已掌握 {rate}%</span>}
    </button>
  );
}

export default function EnglishSentenceLearnPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const unit = searchParams.get('unit') || '';
  const version = useStudentStore((s) => s.version);
  const grade = useStudentStore((s) => s.grade);

  const term = useStudentStore((s) => s.term);
  const [units, setUnits] = useState([]);
  const [mode, setMode] = useState('hub'); // hub | exercise
  const [sentences, setSentences] = useState([]);
  const [unitPass, setUnitPass] = useState(null);
  // 语法例句的归一化文本集合（R8：句式与语法共用同一句时给出提示，避免学员以为重复刷题）
  const [grammarSentenceKeys, setGrammarSentenceKeys] = useState(() => new Set());
  // 句式学习 / 句子练习各自从不同题型进入，进入后仍可在步骤条切换
  const [exerciseMode, setExerciseMode] = useState('mix');
  const [loading, setLoading] = useState(true);

  // 左侧单元导航
  useEffect(() => {
    let cancelled = false;
    getEnglishUnits({ version, grade, term })
      .then((res) => { if (!cancelled) setUnits(res?.data || []); })
      .catch(() => {});
    return () => { cancelled = true; };
  }, [version, grade, term]);

  const loadSentences = useCallback(() => {
    if (!unit) {
      setLoading(false);
      return;
    }
    setLoading(true);
    getEnglishUnitPass({ version, grade, term, unit })
      .then((res) => setUnitPass(res?.data || null))
      .catch(() => setUnitPass(null));
    getEnglishSentences({ version, grade, term, unit })
      .then((res) => {
        setSentences(res?.data || []);
        setMode('hub');
      })
      .catch(() => message.error('句子加载失败'))
      .finally(() => setLoading(false));
    getEnglishGrammars({ version, grade, term, unit })
      .then((res) => {
        const keys = new Set();
        (res?.data || []).forEach((g) => parseExamples(g.examples).forEach((ex) => {
          const key = normalizeSentence(exampleText(ex));
          if (key) keys.add(key);
        }));
        setGrammarSentenceKeys(keys);
      })
      .catch(() => setGrammarSentenceKeys(new Set()));
  }, [version, grade, term, unit]);

  useEffect(() => {
    loadSentences();
  }, [loadSentences]);

  const activeUnit = units.find((u) => u.unit === unit);
  const finishedCount = sentences.filter((s) => (s.familiarity || 0) >= FINISHED_FAMILIARITY).length;
  // 与语法例句重合的句子数量：大于 0 时在句式页提示「同时属于语法点练习」
  const sharedCount = sentences.filter((s) => grammarSentenceKeys.has(normalizeSentence(s.en))).length;

  // 左侧单元导航：任一单元按钮都回到该单元的英语学习中心主页（单元概览）
  const goUnit = (next) => navigate(`/student/english?unit=${encodeURIComponent(next)}`);
  const backToCenter = () => navigate('/student/english');

  if (loading) {
    return <div className="eng-learn-wrap eng-stage"><Spin /></div>;
  }

  if (!unit || sentences.length === 0) {
    return (
      <div className="eng-learn-wrap eng-stage">
        <div className="eng-empty">
          <div className="eng-empty-emoji">🐚</div>
          <div className="eng-empty-title">{unit ? '该单元暂无句子' : '未选择单元'}</div>
          <button type="button" className="eng-btn eng-btn-primary" onClick={backToCenter}>
            返回英语学习中心
          </button>
        </div>
      </div>
    );
  }

  // ---------- hub：句式通关总览 ----------
  if (mode === 'hub') {
    return (
      <div className="eng-page">
        <div className="eng-master">
          <EnglishUnitAside units={units} activeUnit={unit} onSelect={goUnit} />

          <div className="eng-detail">
            <div className="eng-detail-card">
              <div className="eng-detail-head">
                <div className="eng-detail-title">{unit}<span className="eng-detail-title-sub">句式通关</span></div>
                {activeUnit?.reviewDue > 0 && (
                  <button
                    type="button"
                    className="eng-review-pill"
                    onClick={() => navigate('/student/english/review')}
                  >
                    复习 {activeUnit.reviewDue}
                  </button>
                )}
              </div>
              <div className="eng-detail-tip">
                本单元共 {sentences.length} 个重点句式，句子学习用 10~15 道连词成句做课本复习，句子练习再用 20~25 道分层题灵活运用
              </div>
              {sharedCount > 0 && (
                <div className="eng-shared-tip">
                  其中 {sharedCount} 句同时属于本单元的语法点练习，两处考法不同：这里是交际句式，语法练的是规则本身
                </div>
              )}

              <div className="eng-progress-grid">
                <div className="eng-progress-item">
                  <div className="eng-stat-label">
                    <span>句子</span>
                    <span>{finishedCount}/{sentences.length}</span>
                  </div>
                  <div className="eng-stat-bar sentence">
                    <span style={{ width: `${percent(finishedCount, sentences.length)}%` }} />
                  </div>
                </div>
              </div>

              <div className="eng-stage-row eng-stage-row-sentence">
                <SentenceStageCard
                  index="1"
                  name="课本跟读"
                  hint="跟着课本原音朗读，录音打分"
                  onClick={() => navigate(`/student/english/read-aloud?unit=${encodeURIComponent(unit)}`)}
                />
                <SentenceStageCard
                  index="2"
                  name="句子学习"
                  hint="听音组句 · 连词成句 · 词汇填空"
                  onClick={() => {
                    setExerciseMode('build');
                    setMode('exercise');
                  }}
                />
                <SentenceStageCard
                  index="3"
                  name="句子练习"
                  hint="三种选择 + 句子默写"
                  rate={unitPass?.sentencePassRate}
                  onClick={() => {
                    setExerciseMode('mix');
                    setMode('exercise');
                  }}
                />
                <SentenceStageCard
                  index="4"
                  name="情景对话"
                  hint="在生活情景里把句子用起来"
                  onClick={() => navigate(`/student/english/pattern-scenario?unit=${encodeURIComponent(unit)}`)}
                />
              </div>

              <div className="eng-actions" style={{ marginTop: 22 }}>
                <button type="button" className="eng-btn eng-btn-ghost" onClick={backToCenter}>
                  返回英语学习中心
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // ---------- exercise：多题型句子练习（读取管理端固化内容） ----------
  if (mode === 'exercise') {
    return (
      <div className="eng-page">
        <div className="eng-master">
          <EnglishUnitAside units={units} activeUnit={unit} onSelect={goUnit} />
          <div className="eng-detail">
            <div className="eng-detail-card">
              <EnglishSentenceExercise
                unit={unit}
                version={version}
                grade={grade}
                term={term}
                initialMode={exerciseMode}
                onBack={() => setMode('hub')}
              />
            </div>
          </div>
        </div>
      </div>
    );
  }

  return null;
}
