/**
 * EnglishSentenceLearnPage.jsx - 学员端英语「句式通关」
 *
 * 结构（与单词通关页同构，复用其圆环版式）:
 *   hub   ：左侧单元导航 + 「UnitX 句式通关」标题 + 句式进度条 + 复习入口
 *           + 课本跟读入口条 + 三列圆环（句子学习 / 句子练习 / 句子运用）
 *   exercise：交由 EnglishSentenceExercise 承担（听音组句 / 连词成句 / 词汇填空 /
 *           看中文选英文 / 看英文选中文 / 听英文选中文 / 句子默写）
 *
 * 四段链路（需求文档 R2.4）: 课本跟读 → 句子学习 → 句子练习 → 句子运用
 *   （句子运用列含「情景对话」与「句子默写」两个题型）
 *   课本跟读沿用独立页面 /student/english/read-aloud（跟读录音+打分，不在此页内联）
 *
 * 圆环口径（与后端 EnglishExamAngle / EnglishLearningState 对齐）:
 *   句子学习 = 听音组句 / 连词成句 / 词汇填空 三个角度覆盖的句子数
 *   句子练习 = 看中文选英文 / 看英文选中文 / 听英文选中文 / 句子默写
 *   情景对话 = 整组提交只留正确率，按 useRate 折算句子数
 *   三列统一用「覆盖句数 / 本单元句式总数」，本单元可选句子少于 4 条时
 *   四选一题型（三种选择题）置灰，因为凑不齐 1 正 3 干扰项
 *
 * URL: /student/english/sentence?unit=xxx
 * 被谁引用: App.jsx 路由表；入口来自英语学习中心「句式通关」
 *
 * 数据流:
 *   GET  /api/english/student/units       → 单元列表（左侧导航）
 *   GET  /api/english/student/sentences   → 单元句子（含熟练度 / 三态 / 已用角度）
 *   GET  /api/english/student/unit-pass   → 单元句式达标率
 */

import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Spin, message } from 'antd';
import useStudentStore from '../../stores/useStudentStore';
import { getEnglishUnits, getEnglishSentences, getEnglishGrammars, getEnglishUnitPass } from '../../api/englishStudent';
import StageRing from '../../components/student/StageRing';
import EnglishUnitAside from './EnglishUnitAside';
import EnglishSentenceExercise from './EnglishSentenceExercise';
import './EnglishCenterPage.css';
import './EnglishSentenceLearnPage.css';

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

/**
 * 三列分组（列首一块彩色标题矩形，列间不再用箭头串联）：
 *   句子学习（海蓝）/ 句子练习（珊瑚橙）/ 句子运用（通关绿）
 * 每列内题型来自后端 EnglishExamAngle 的句式角度池；
 * mode 对应 EnglishSentenceExercise 的 initialMode，scenario 走独立页面。
 *
 * 列内圆环统一尺寸（不放大首环），三列等宽，靠标题矩形的颜色区分阶段。
 * 「句子默写」归入句子运用列，答题上报 use 阶段（与页面分组一致）。
 */
const STAGE_COLUMNS = [
  {
    key: 'learn',
    label: '句子学习',
    tone: 'memory',
    items: [
      { angle: 'sentence-listen-build', label: '听音组句', mode: 'listen-build' },
      { angle: 'sentence-build', label: '连词成句', mode: 'build' },
      { angle: 'sentence-fill', label: '词汇填空', mode: 'fill' },
    ],
  },
  {
    key: 'practice',
    label: '句子练习',
    tone: 'practice',
    items: [
      { angle: 'sentence-zh-en', label: '看中文选英文', mode: 'zh-en' },
      { angle: 'sentence-en-zh', label: '看英文选中文', mode: 'en-zh' },
      { angle: 'sentence-listen-meaning', label: '听英文选中文', mode: 'listen-meaning' },
    ],
  },
  {
    key: 'use',
    label: '句子运用',
    tone: 'use',
    items: [
      { angle: 'sentence-scenario', label: '情景对话', mode: 'scenario' },
      { angle: 'sentence-dictation', label: '句子默写', mode: 'dictation' },
    ],
  },
];

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

  /** 命中指定角度的句子数（后端回填 angleUsed） */
  const angleCount = (angle) => sentences.filter((s) => (s.angleUsed || []).includes(angle)).length;

  /** 情景对话整组提交只留正确率，按 useRate 折算句子数 */
  const scenarioDone = Math.round(((unitPass?.sentencePassRate || 0) / 100) * sentences.length);

  /** 四选一题型至少需要 1 个正确项 + 3 个干扰项，本单元去重后不足 4 句时置灰 */
  const distinctEn = new Set(sentences.map((s) => String(s.en || '').trim()).filter(Boolean));
  const distinctZh = new Set(sentences.map((s) => String(s.zh || '').trim()).filter(Boolean));
  const canChoose = distinctEn.size >= 4 && distinctZh.size >= 4;

  // 左侧单元导航：任一单元按钮都回到该单元的英语学习中心主页（单元概览）
  const goUnit = (next) => navigate(`/student/english?unit=${encodeURIComponent(next)}`);
  const backToCenter = () => navigate('/student/english');

  /** 进入某题型练习：scenario 走独立页面，其余内联渲染 */
  const enterItem = (item) => {
    if (item.mode === 'scenario') {
      navigate(`/student/english/pattern-scenario?unit=${encodeURIComponent(unit)}`);
      return;
    }
    setExerciseMode(item.mode);
    setMode('exercise');
  };

  const ringDone = (item) => {
    if (item.angle === 'sentence-scenario') return scenarioDone;
    return angleCount(item.angle);
  };

  /** 需要 4 个互不相同选项的角度 */
  const CHOICE_ANGLES = new Set(['sentence-zh-en', 'sentence-en-zh', 'sentence-listen-meaning']);

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
              <div className="eng-detail-head eng-word-head">
                <div className="eng-detail-title">{unit}<span className="eng-detail-title-sub">句式通关</span></div>
                <div className="eng-word-progress">
                  <span className="eng-word-progress-label">句式</span>
                  <span className="eng-word-progress-track">
                    <span className="eng-word-progress-fill" style={{ width: `${percent(finishedCount, sentences.length)}%` }} />
                  </span>
                  <span className="eng-word-progress-count">{finishedCount}/{sentences.length}</span>
                </div>
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
                本单元共 {sentences.length} 个重点句式，按「课本跟读 → 句子学习 → 句子练习 → 句子运用」逐步推进
              </div>
              {sharedCount > 0 && (
                <div className="eng-shared-tip">
                  其中 {sharedCount} 句同时属于本单元的语法点练习，两处考法不同：这里是交际句式，语法练的是规则本身
                </div>
              )}

              <div className="eng-sentence-read-aloud">
                <button
                  type="button"
                  className="eng-sentence-ra-btn"
                  onClick={() => navigate(`/student/english/read-aloud?unit=${encodeURIComponent(unit)}`)}
                >
                  🎤 课本跟读
                  <span>先跟原音读一遍，录音自动打分</span>
                </button>
              </div>

              <div className="eng-word-flow-card">
                <div className="eng-word-flow">
                  <div className="eng-word-section-head">
                    <div className="eng-word-section-name">{unit} 重点句式</div>
                    <div className="eng-word-states">
                      <span>熟练：{sentences.filter((s) => s.masteryState === 'familiar').length}</span>
                      <span>一般：{sentences.filter((s) => s.masteryState === 'ordinary').length}</span>
                      <span>陌生：{sentences.filter((s) => s.masteryState === 'unfamiliar' || !s.masteryState).length}</span>
                    </div>
                  </div>

                  <div className="eng-word-stages eng-sentence-stages">
                    {STAGE_COLUMNS.map((column) => (
                      <div key={column.key} className={`eng-sentence-col eng-sentence-col-${column.key}`}>
                        <div className={`eng-sentence-col-head eng-sentence-col-head-${column.tone}`}>
                          {column.label}
                        </div>
                        <div className="eng-sentence-col-body">
                          {column.items.map((item) => (
                            <StageRing
                              key={item.angle}
                              tone={column.tone}
                              label={item.label}
                              done={ringDone(item)}
                              total={sentences.length}
                              onClick={() => enterItem(item)}
                              disabled={CHOICE_ANGLES.has(item.angle) && !canChoose}
                              hint={CHOICE_ANGLES.has(item.angle) && !canChoose
                                ? '本单元可用句子不足 4 条，换不出四选一'
                                : `进入「${item.label}」`}
                            />
                          ))}
                        </div>
                      </div>
                    ))}
                  </div>

                  {unitPass?.sentencePassRate != null && (
                    <div className="eng-stage-hint">
                      句子练习达标率 {unitPass.sentencePassRate}%（句子熟练度 ≥ 3 的比例），
                      四个阶段全部达标后本单元句式通关
                    </div>
                  )}

                  <div className="eng-word-legend">
                    <span className="eng-word-legend-item eng-word-legend-memory">学习</span>
                    <span className="eng-word-legend-item eng-word-legend-practice">练习</span>
                    <span className="eng-word-legend-item eng-word-legend-use">运用</span>
                    <span className="eng-word-legend-tip">点任意圆环直接进入对应题型</span>
                  </div>
                </div>
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
