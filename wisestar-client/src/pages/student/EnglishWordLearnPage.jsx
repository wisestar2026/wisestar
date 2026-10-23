/**
 * EnglishWordLearnPage.jsx - 学员端英语「单词通关」（左侧单元导航 + 按小节 model 分组 + 卡片学习）
 *
 * 结构（对应「单词学习界面」参考图）:
 *   顶部 hub：左侧 UNIT 导航，右侧「UnitX 单词通关」标题 + 单词进度 +
 *            按小节(model)分组的卡片，每组提供「单词学习 / 单词练习 / 场景对话」入口
 *   学习态：进入某小节后逐词卡片学习（认识 / 不认识，本节内间隔重复）
 *
 * 功能:
 *   1. 展示单元单词卡片（拼写 / 音标 / 释义 / 图片 / 发音）
 *   2. 认识 / 不认识 → 回写熟练度与复习队列
 *   3. 本节内间隔重复：点「不认识」的单词会后置并轮流反复出现
 *   4. 累计「不认识」≥2 次的单词标记为「需加强」
 *
 * URL: /student/english/word?unit=xxx
 * 被谁引用: App.jsx 路由表；入口来自英语学习中心「单词通关」
 *
 * 数据流:
 *   GET  /api/english/student/units   → 单元列表（左侧导航）
 *   GET  /api/english/word/word-book  → 单元单词（含小节 section / 熟练度 familiarity）
 *   POST /api/english/word/record     → 记录作答
 *   POST /api/english/student/session → 记录会话
 */

import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Spin, message } from 'antd';
import useStudentStore from '../../stores/useStudentStore';
import { getEnglishUnits, getEnglishWordBook, recordEnglishWord, recordEnglishSession } from '../../api/englishStudent';
import { speakEnglish } from '../../utils/english';
import EnglishUnitAside from './EnglishUnitAside';
import './EnglishCenterPage.css';

/**
 * 按 section 将单词分组：有名称的小节保持后端顺序，「未分节」固定置末。
 */
function groupWordsBySection(list) {
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

/** 需加强阈值：累计「不认识」达到该次数的单词标记为需加强。 */
const WEAK_UNKNOWN_TIMES = 2;
/** 后端以 familiarity>=1 视为「已学」，完成度按此口径统计。 */
const FINISHED_FAMILIARITY = 1;

function percent(finished, total) {
  if (!total) return 0;
  return Math.min(100, Math.round((finished / total) * 100));
}

export default function EnglishWordLearnPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const unit = searchParams.get('unit') || '';
  const version = useStudentStore((s) => s.version);
  const grade = useStudentStore((s) => s.grade);

  const term = useStudentStore((s) => s.term);
  const [units, setUnits] = useState([]);
  const [mode, setMode] = useState('hub'); // hub | learn
  const [learnSection, setLearnSection] = useState(null);
  const [words, setWords] = useState([]);
  const [groups, setGroups] = useState([]);
  const [queue, setQueue] = useState([]);
  const [mastered, setMastered] = useState(0);
  const [loading, setLoading] = useState(true);
  const [revealed, setRevealed] = useState(false);
  const startAt = useRef(Date.now());
  const sessionRecorded = useRef(false);
  // 本节会话内的逐词统计：wordId -> { unknown, known }
  const statsRef = useRef(new Map());

  // 左侧单元导航
  useEffect(() => {
    let cancelled = false;
    getEnglishUnits({ version, grade, term })
      .then((res) => { if (!cancelled) setUnits(res?.data || []); })
      .catch(() => {});
    return () => { cancelled = true; };
  }, [version, grade, term]);

  // 切换单元时回到 hub
  useEffect(() => {
    setMode('hub');
    setLearnSection(null);
  }, [unit]);

  const loadWords = useCallback(() => {
    if (!unit) {
      setLoading(false);
      return;
    }
    setLoading(true);
    getEnglishWordBook({ version, grade, term, unit, pageSize: -1 })
      .then((res) => {
        const list = res?.data?.list || [];
        const gs = groupWordsBySection(list);
        setWords(list);
        setGroups(gs);
        setMode('hub');
        setLearnSection(null);
        setQueue([]);
        setMastered(0);
        setRevealed(false);
      })
      .catch(() => message.error('单词加载失败'))
      .finally(() => setLoading(false));
  }, [version, grade, term, unit]);

  useEffect(() => {
    loadWords();
  }, [loadWords]);

  const activeUnit = units.find((u) => u.unit === unit);
  const finishedCount = words.filter((w) => (w.familiarity || 0) >= FINISHED_FAMILIARITY).length;
  const unitProgress = percent(finishedCount, words.length);

  // 左侧单元导航：任一单元按钮都回到该单元的英语学习中心主页（单元概览）
  const goUnit = (next) => navigate(`/student/english?unit=${encodeURIComponent(next)}`);

  /** 进入某小节的学习卡片流；section 为空表示整单元 */
  const startLearn = (section) => {
    const group = section ? groups.find((g) => g.section === section) : null;
    const items = group ? group.items : words;
    const entries = items.map((w) => ({ word: w, section: group ? group.section : '' }));
    statsRef.current = new Map();
    sessionRecorded.current = false;
    startAt.current = Date.now();
    setLearnSection(section);
    setQueue(entries);
    setMastered(0);
    setRevealed(false);
    setMode('learn');
  };

  const backToHub = () => {
    setMode('hub');
    setLearnSection(null);
  };

  const total = queue.length + mastered;
  const current = queue[0]?.word;
  const currentUnknown = (current && statsRef.current.get(current.id)?.unknown) || 0;
  // 本节内已多次不认识，或历史累计已标记需加强
  const currentWeak = current && (currentUnknown >= WEAK_UNKNOWN_TIMES || current.weak);
  const finished = mode === 'learn' && !loading && total > 0 && queue.length === 0;

  // 本节内「不认识」≥2 次的单词（需加强）
  const weakWords = finished
    ? words.filter((w) => (statsRef.current.get(w.id)?.unknown || 0) >= WEAK_UNKNOWN_TIMES)
    : [];

  // 学完记录一次会话
  useEffect(() => {
    if (!finished || sessionRecorded.current) return;
    sessionRecorded.current = true;
    let correctCount = 0;
    statsRef.current.forEach((s) => { correctCount += s.known; });
    recordEnglishSession({
      type: 'word',
      durationSeconds: Math.round((Date.now() - startAt.current) / 1000),
      correctCount,
    }).catch(() => {});
  }, [finished]);

  /**
   * 本节内间隔重复算法：
   * - required = max(累计不认识次数, 1)：点错越多，需要补足的「认识」次数越多。
   * - 答「认识」：known+1；达到 required 即出队（掌握），否则后置轮转。
   * - 答「不认识」：unknown+1 抬高 required，并后置轮转，保证本节内轮流反复出现。
   */
  const handleAnswer = (correct) => {
    const head = queue[0];
    if (!head) return;
    const wordId = head.word.id;
    const stat = statsRef.current.get(wordId) || { unknown: 0, known: 0 };
    if (correct) stat.known += 1;
    else stat.unknown += 1;
    statsRef.current.set(wordId, stat);

    recordEnglishWord({ wordId, correct, source: 'learn' }).catch(() => {});

    const required = Math.max(stat.unknown, 1);
    const graduated = correct && stat.known >= required;
    if (graduated) setMastered((n) => n + 1);

    setRevealed(false);
    setQueue((prev) => {
      const [first, ...rest] = prev;
      return graduated ? rest : [...rest, first];
    });
  };

  // 进入新单词卡片时尝试自动朗读（部分浏览器需先有交互，失败不影响使用）
  useEffect(() => {
    if (mode !== 'learn' || finished || !current?.spell) return undefined;
    const timer = setTimeout(() => speakEnglish(current.spell, current.audioUrl), 260);
    return () => clearTimeout(timer);
  }, [mode, finished, current?.id, current?.spell, current?.audioUrl]);

  const progress = total ? Math.round((mastered / total) * 100) : 0;

  const backToCenter = () => navigate('/student/english');

  if (loading) {
    return <div className="eng-learn-wrap eng-learn-wide"><Spin /></div>;
  }

  if (!unit) {
    return (
      <div className="eng-learn-wrap eng-learn-wide">
        <div className="eng-empty">
          <div className="eng-empty-emoji">📖</div>
          <div className="eng-empty-title">未选择单元</div>
          <button type="button" className="eng-btn eng-btn-primary" onClick={backToCenter}>
            返回英语学习中心
          </button>
        </div>
      </div>
    );
  }

  if (words.length === 0) {
    return (
      <div className="eng-learn-wrap eng-learn-wide">
        <div className="eng-empty">
          <div className="eng-empty-emoji">🐚</div>
          <div className="eng-empty-title">该单元暂无单词</div>
          <button type="button" className="eng-btn eng-btn-primary" onClick={backToCenter}>
            返回英语学习中心
          </button>
        </div>
      </div>
    );
  }

  // ---------- hub：单词通关总览 ----------
  if (mode === 'hub') {
    return (
      <div className="eng-page">
        <div className="eng-master">
          <EnglishUnitAside units={units} activeUnit={unit} onSelect={goUnit} />

          <div className="eng-detail">
            <div className="eng-detail-card">
              <div className="eng-detail-head">
                <div className="eng-detail-title">{unit}<span className="eng-detail-title-sub">单词通关</span></div>
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
                本单元共 {words.length} 个单词，按小节分组学习；学完一组可以去做练习或情景对话
              </div>

              <div className="eng-progress-grid">
                <div className="eng-progress-item">
                  <div className="eng-stat-label">
                    <span>单词</span>
                    <span>{finishedCount}/{words.length}</span>
                  </div>
                  <div className="eng-stat-bar">
                    <span style={{ width: `${unitProgress}%` }} />
                  </div>
                </div>
              </div>

              {groups.map((group) => {
                const done = group.items.filter((w) => (w.familiarity || 0) >= FINISHED_FAMILIARITY).length;
                const donePct = percent(done, group.items.length);
                return (
                  <div className="eng-model-group" key={group.section}>
                    <div className="eng-model-head">
                      <span className="eng-model-name">{group.section}</span>
                      <span className="eng-model-done">完成度 {donePct}%</span>
                      <span className="eng-model-count">{done}/{group.items.length} 词</span>
                    </div>
                    <div className="eng-model-bar"><span style={{ width: `${donePct}%` }} /></div>
                    <div className="eng-model-actions">
                      <button
                        type="button"
                        className="eng-model-learn"
                        onClick={() => startLearn(group.section)}
                      >
                        单词学习
                      </button>
                      <button
                        type="button"
                        className="eng-model-practice"
                        disabled={words.length < 4}
                        onClick={() => navigate(`/student/english/practice?unit=${encodeURIComponent(unit)}`)}
                      >
                        单词练习
                      </button>
                      <button
                        type="button"
                        className="eng-model-scenario"
                        disabled={words.length < 4}
                        onClick={() => navigate(`/student/english/scenario?unit=${encodeURIComponent(unit)}`)}
                      >
                        场景对话
                      </button>
                    </div>
                  </div>
                );
              })}

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

  // ---------- learn：逐词卡片学习 ----------
  if (finished) {
    return (
      <div className="eng-learn-wrap eng-learn-wide">
        <button type="button" className="eng-learn-back" onClick={backToHub}>← 返回单词通关</button>
        <div className="eng-empty">
          <div className="eng-empty-emoji">🎉</div>
          <div className="eng-empty-title">本组单词已学完</div>
          <div className="eng-done-stats">
            <div className="eng-done-stat"><b>{mastered}</b><span>认识</span></div>
            <div className="eng-done-stat"><b>{weakWords.length}</b><span>需加强</span></div>
          </div>
          {weakWords.length > 0 && (
            <div className="eng-weak-list">
              <div className="eng-weak-list-title">多次不认识的单词，记得加强复习</div>
              {weakWords.map((w) => (
                <div className="eng-weak-item" key={w.id}>
                  <b>{w.spell}</b>
                  <span>{w.meaning || '—'}</span>
                </div>
              ))}
            </div>
          )}
          <div className="eng-tip" style={{ marginTop: 4 }}>
            先做一遍单词练习，再去场景对话：首答正确率达到 80% 才算真正掌握这批词。
          </div>
          <div className="eng-actions">
            {weakWords.length > 0 && (
              <button
                type="button"
                className="eng-btn eng-btn-again"
                onClick={() => navigate(`/student/english/practice?unit=${encodeURIComponent(unit)}&ids=${encodeURIComponent(weakWords.map((w) => w.id).join(','))}`)}
              >
                复习需加强 · {weakWords.length}
              </button>
            )}
            <button
              type="button"
              className="eng-btn eng-btn-primary"
              onClick={() => navigate(`/student/english/practice?unit=${encodeURIComponent(unit)}`)}
            >
              去单词练习（推荐下一步）
            </button>
            <button type="button" className="eng-btn eng-btn-ghost" onClick={backToHub}>返回单词通关</button>
            <button type="button" className="eng-btn eng-btn-ghost" onClick={backToCenter}>返回学习中心</button>
          </div>
        </div>
      </div>
    );
  }

  const learnTitle = learnSection ? `${unit} · ${learnSection}` : unit;

  return (
    <div className="eng-learn-wrap eng-learn-wide">
      <button type="button" className="eng-learn-back" onClick={backToHub}>← 返回单词通关</button>
      <div className="eng-section-title">{learnTitle}</div>
      <div className="eng-progress-row">
        <div className="eng-progress-track"><span style={{ width: `${progress}%` }} /></div>
        <div className="eng-progress-text">{mastered} / {total}</div>
      </div>

      <div className="eng-card eng-card-word">
        {currentWeak && <div className="eng-weak-tag">需加强</div>}
        {current?.level && (
          <div className={`eng-level-tag ${current.level === '四会' ? 'eng-level-four' : 'eng-level-three'}`}>
            {current.level}
          </div>
        )}
        <div
          className="eng-word eng-word-clickable"
          role="button"
          tabIndex={0}
          title="点击朗读"
          onClick={() => speakEnglish(current?.spell, current?.audioUrl)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') {
              e.preventDefault();
              speakEnglish(current?.spell, current?.audioUrl);
            }
          }}
        >
          {current?.spell}
        </div>
        {current?.phonetic && <div className="eng-phonetic">/{current.phonetic}/</div>}
        {revealed && current?.imageUrl && (
          <img
            className="eng-image"
            src={current.imageUrl}
            alt={current?.spell || ''}
            onError={(e) => { e.currentTarget.style.display = 'none'; }}
          />
        )}
        <div className={`eng-meaning ${revealed ? '' : 'hidden'}`}>
          {revealed ? (current?.meaning || '—') : '点击「显示释义」查看含义'}
        </div>

        <div className="eng-actions">
          <button type="button" className="eng-btn eng-btn-sound" onClick={() => speakEnglish(current?.spell, current?.audioUrl)}>
            朗读
          </button>
          {!revealed && (
            <button type="button" className="eng-btn eng-btn-ghost" onClick={() => setRevealed(true)}>显示释义</button>
          )}
        </div>

        {revealed && (
          <div className="eng-actions">
            <button type="button" className="eng-btn eng-btn-ok" onClick={() => handleAnswer(true)}>认识</button>
            <button type="button" className="eng-btn eng-btn-again" onClick={() => handleAnswer(false)}>不认识</button>
          </div>
        )}
      </div>
    </div>
  );
}
