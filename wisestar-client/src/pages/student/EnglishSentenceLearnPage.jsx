/**
 * EnglishSentenceLearnPage.jsx - 学员端英语「句式通关」（左侧单元导航 + 句子学习/句式情景）
 *
 * 结构（对应「句式学习界面」参考图）:
 *   顶部 hub：左侧 UNIT 导航，右侧「UnitX 句式通关」标题 + 句子进度 +
 *            两张入口卡（句子学习 / 句式情景）
 *   学习态：连词成句逐句练习（中文提示 + 英文朗读，点词块拼句自动判分）
 *
 * URL: /student/english/sentence?unit=xxx
 * 被谁引用: App.jsx 路由表；入口来自英语学习中心「句式通关」
 *
 * 数据流:
 *   GET  /api/english/student/units           → 单元列表（左侧导航）
 *   GET  /api/english/student/sentences       → 单元句子（含小节 section / 熟练度 familiarity）
 *   POST /api/english/student/sentence/record → 记录作答
 *   POST /api/english/student/session          → 记录会话
 */

import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Spin, message } from 'antd';
import useStudentStore from '../../stores/useStudentStore';
import { getEnglishUnits, getEnglishSentences, recordEnglishSentence, recordEnglishSession } from '../../api/englishStudent';
import { speakEnglish, tokenizeSentence, normalizeAnswer, shuffle } from '../../utils/english';
import EnglishUnitAside from './EnglishUnitAside';
import EnglishSentenceExercise from './EnglishSentenceExercise';
import './EnglishCenterPage.css';

/** 后端以 familiarity>=1 视为「已学」，完成度按此口径统计。 */
const FINISHED_FAMILIARITY = 1;

function percent(finished, total) {
  if (!total) return 0;
  return Math.min(100, Math.round((finished / total) * 100));
}

/**
 * 按 section 将句子分组：有名称的小节保持后端顺序，「未分节」固定置末。
 */
function groupSentencesBySection(list) {
  const named = new Map();
  const unsectioned = [];
  list.forEach((item) => {
    const key = (item.section || '').trim();
    if (!key) {
      unsectioned.push(item);
      return;
    }
    if (!named.has(key)) named.set(key, []);
    named.get(key).push(item);
  });
  const groups = [...named.entries()].map(([section, items]) => ({ section, items }));
  if (unsectioned.length) groups.push({ section: '未分节', items: unsectioned });
  return groups;
}

export default function EnglishSentenceLearnPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const unit = searchParams.get('unit') || '';
  const version = useStudentStore((s) => s.version);
  const grade = useStudentStore((s) => s.grade);

  const term = useStudentStore((s) => s.term);
  const [units, setUnits] = useState([]);
  const [mode, setMode] = useState('hub'); // hub | learn | exercise
  const [sentences, setSentences] = useState([]);
  const [sectionLabels, setSectionLabels] = useState([]);
  const [loading, setLoading] = useState(true);
  const [index, setIndex] = useState(0);
  const [pool, setPool] = useState([]);
  const [selected, setSelected] = useState([]);
  const [feedback, setFeedback] = useState(null); // 'ok' | 'no'
  const [wrongTries, setWrongTries] = useState(0);
  const [correctCount, setCorrectCount] = useState(0);
  const startAt = useRef(Date.now());
  const timerRef = useRef(null);

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
    getEnglishSentences({ version, grade, term, unit })
      .then((res) => {
        const groups = groupSentencesBySection(res?.data || []);
        const flat = [];
        const labels = [];
        groups.forEach((group) => group.items.forEach((item) => {
          flat.push(item);
          labels.push(group.section);
        }));
        setSentences(flat);
        setSectionLabels(labels);
        setMode('hub');
      })
      .catch(() => message.error('句子加载失败'))
      .finally(() => setLoading(false));
  }, [version, grade, term, unit]);

  useEffect(() => {
    loadSentences();
  }, [loadSentences]);

  const activeUnit = units.find((u) => u.unit === unit);
  const finishedCount = sentences.filter((s) => (s.familiarity || 0) >= FINISHED_FAMILIARITY).length;

  // 左侧单元导航：任一单元按钮都回到该单元的英语学习中心主页（单元概览）
  const goUnit = (next) => navigate(`/student/english?unit=${encodeURIComponent(next)}`);
  const backToCenter = () => navigate('/student/english');

  /** 进入连词成句学习流 */
  const startLearn = () => {
    setIndex(0);
    setCorrectCount(0);
    startAt.current = Date.now();
    setMode('learn');
  };

  const current = sentences[index];

  // 切题时重置词块池与作答状态
  useEffect(() => {
    if (current) {
      setPool(shuffle(tokenizeSentence(current.en)));
      setSelected([]);
      setFeedback(null);
      setWrongTries(0);
    }
  }, [index, current]);

  useEffect(() => () => clearTimeout(timerRef.current), []);

  const goNext = useCallback(() => {
    clearTimeout(timerRef.current);
    setIndex((i) => i + 1);
  }, []);

  const checkAnswer = () => {
    if (!current) return;
    const answer = selected.map((i) => pool[i]).join(' ');
    if (normalizeAnswer(answer) === normalizeAnswer(current.en)) {
      setFeedback('ok');
      setCorrectCount((n) => n + 1);
      recordEnglishSentence({ sentenceId: current.id, correct: true }).catch(() => {});
      timerRef.current = setTimeout(goNext, 900);
    } else {
      const tries = wrongTries + 1;
      setFeedback('no');
      setWrongTries(tries);
      recordEnglishSentence({ sentenceId: current.id, correct: false }).catch(() => {});
      if (tries >= 3) {
        timerRef.current = setTimeout(goNext, 1600);
      }
    }
  };

  const finished = mode === 'learn' && !loading && index >= sentences.length;

  useEffect(() => {
    if (finished) {
      const durationSeconds = Math.round((Date.now() - startAt.current) / 1000);
      recordEnglishSession({ type: 'sentence', durationSeconds, correctCount }).catch(() => {});
    }
  }, [finished, correctCount]);

  if (loading) {
    return <div className="eng-learn-wrap"><Spin /></div>;
  }

  if (!unit || sentences.length === 0) {
    return (
      <div className="eng-learn-wrap">
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
                本单元共 {sentences.length} 个重点句子，先连词成句，再去情景里用一用吧
              </div>

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

              <div className="eng-hub-cards">
                <button type="button" className="eng-hub-card eng-hub-learn" onClick={startLearn}>
                  <span>句子学习</span>
                  <span className="eng-mod-sub">连词成句，边拼边记</span>
                </button>
                <button
                  type="button"
                  className="eng-hub-card eng-hub-scenario"
                  onClick={() => navigate(`/student/english/pattern-scenario?unit=${encodeURIComponent(unit)}`)}
                >
                  <span>句式情景</span>
                  <span className="eng-mod-sub">在生活情景里选一选</span>
                </button>
                <button
                  type="button"
                  className="eng-hub-card eng-hub-practice"
                  onClick={() => setMode('exercise')}
                >
                  <span>句子练习</span>
                  <span className="eng-mod-sub">拼句 · 默写 · 选择</span>
                </button>
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
                onBack={() => setMode('hub')}
              />
            </div>
          </div>
        </div>
      </div>
    );
  }

  // ---------- learn：连词成句 ----------
  if (finished) {
    return (
      <div className="eng-learn-wrap">
        <button type="button" className="eng-learn-back" onClick={() => setMode('hub')}>← 返回句式通关</button>
        <div className="eng-empty">
          <div className="eng-empty-emoji">🌟</div>
          <div className="eng-empty-title">本单元句子已学完</div>
          <div className="eng-done-stats">
            <div className="eng-done-stat"><b>{correctCount}</b><span>答对</span></div>
            <div className="eng-done-stat"><b>{sentences.length}</b><span>句子总数</span></div>
          </div>
          <div className="eng-actions">
            <button type="button" className="eng-btn eng-btn-ghost" onClick={() => setMode('hub')}>返回句式通关</button>
            <button type="button" className="eng-btn eng-btn-primary" onClick={backToCenter}>返回学习中心</button>
          </div>
        </div>
      </div>
    );
  }

  const progress = Math.round((index / sentences.length) * 100);

  return (
    <div className="eng-learn-wrap">
      <button type="button" className="eng-learn-back" onClick={() => setMode('hub')}>← 返回句式通关</button>
      {sectionLabels[index] && <div className="eng-section-title">{sectionLabels[index]}</div>}
      <div className="eng-progress-row">
        <div className="eng-progress-track"><span style={{ width: `${progress}%` }} /></div>
        <div className="eng-progress-text">{index + 1} / {sentences.length}</div>
      </div>

      <div className="eng-card">
        <div className="eng-sentence-hint">连词成句：{current?.zh || '（无中文提示）'}</div>

        <div className="eng-answer-slot">
          {selected.length === 0 && <span style={{ color: '#a9c3d1' }}>点击下方词块，按顺序拼出句子</span>}
          {selected.map((poolIndex, pos) => (
            <button
              key={`${poolIndex}-${pos}`}
              type="button"
              className={`eng-token selected ${feedback === 'ok' ? 'correct' : ''} ${feedback === 'no' ? 'wrong' : ''}`}
              onClick={() => setSelected((prev) => prev.filter((_, i) => i !== pos))}
            >
              {pool[poolIndex]}
            </button>
          ))}
        </div>

        <div className="eng-token-pool">
          {pool.map((token, poolIndex) => {
            const used = selected.includes(poolIndex);
            return (
              <button
                key={`${token}-${poolIndex}`}
                type="button"
                className={`eng-token ${used ? 'used' : ''}`}
                disabled={used || feedback === 'ok'}
                onClick={() => setSelected((prev) => [...prev, poolIndex])}
              >
                {token}
              </button>
            );
          })}
        </div>

        {feedback === 'ok' && <div className="eng-feedback ok">太棒了，完全正确！</div>}
        {feedback === 'no' && (
          <div className="eng-feedback no">
            再想想～{wrongTries >= 3 && current && <div>正确答案：{current.en}</div>}
          </div>
        )}

        <div className="eng-actions">
          <button type="button" className="eng-btn eng-btn-sound" onClick={() => speakEnglish(current?.en, current?.audioUrl)}>
            朗读
          </button>
          <button type="button" className="eng-btn eng-btn-ghost" onClick={() => setSelected([])} disabled={feedback === 'ok'}>
            清空
          </button>
          <button
            type="button"
            className="eng-btn eng-btn-primary"
            onClick={checkAnswer}
            disabled={selected.length === 0 || feedback === 'ok'}
          >
            检查
          </button>
        </div>
      </div>

      <div className="eng-actions">
        <button type="button" className="eng-btn eng-btn-ghost" onClick={() => setMode('hub')}>
          结束学习
        </button>
      </div>
    </div>
  );
}
