/**
 * EnglishWordLearnPage.jsx - 学员端英语单词卡片学习
 *
 * 功能:
 *   1. 展示单元单词卡片（拼写 / 音标 / 释义 / 图片 / 发音）
 *   2. 认识 / 不认识 → 回写熟练度与复习队列
 *   3. 本节内间隔重复：点「不认识」的单词会后置并轮流反复出现，
 *      需要补足的「认识」次数 = 该词累计「不认识」次数（至少 1 次）
 *   4. 累计「不认识」≥2 次的单词标记为「需加强」
 *
 * URL: /student/english/word?unit=xxx
 * 被谁引用: App.jsx 路由表；入口来自英语学习中心单元卡片
 *
 * 数据流:
 *   GET  /api/english/word/word-book → 单元单词
 *   POST /api/english/word/record    → 记录作答
 *   POST /api/english/student/session → 记录会话
 */

import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Spin, message } from 'antd';
import useStudentStore from '../../stores/useStudentStore';
import { getEnglishWordBook, recordEnglishWord, recordEnglishSession } from '../../api/englishStudent';
import { speakEnglish } from '../../utils/english';
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

export default function EnglishWordLearnPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const unit = searchParams.get('unit') || '';
  const version = useStudentStore((s) => s.version);
  const grade = useStudentStore((s) => s.grade);

  const [term] = useState('上册');
  const [words, setWords] = useState([]);
  const [queue, setQueue] = useState([]);
  const [mastered, setMastered] = useState(0);
  const [loading, setLoading] = useState(true);
  const [revealed, setRevealed] = useState(false);
  const startAt = useRef(Date.now());
  const sessionRecorded = useRef(false);
  // 本节会话内的逐词统计：wordId -> { unknown, known }
  const statsRef = useRef(new Map());

  const loadWords = useCallback(() => {
    if (!unit) {
      setLoading(false);
      return;
    }
    setLoading(true);
    getEnglishWordBook({ version, grade, term, unit, pageSize: -1 })
      .then((res) => {
        const groups = groupWordsBySection(res?.data?.list || []);
        const flat = [];
        const entries = [];
        groups.forEach((group) => group.items.forEach((item) => {
          flat.push(item);
          entries.push({ word: item, section: group.section });
        }));
        statsRef.current = new Map();
        sessionRecorded.current = false;
        startAt.current = Date.now();
        setWords(flat);
        setQueue(entries);
        setMastered(0);
        setRevealed(false);
      })
      .catch(() => message.error('单词加载失败'))
      .finally(() => setLoading(false));
  }, [version, grade, term, unit]);

  useEffect(() => {
    loadWords();
  }, [loadWords]);

  const total = words.length;
  const current = queue[0]?.word;
  const currentSection = queue[0]?.section;
  const currentUnknown = (current && statsRef.current.get(current.id)?.unknown) || 0;
  // 本节内已多次不认识，或历史累计已标记需加强
  const currentWeak = current && (currentUnknown >= WEAK_UNKNOWN_TIMES || current.weak);
  const finished = !loading && total > 0 && queue.length === 0;

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

    recordEnglishWord({ wordId, correct }).catch(() => {});

    const required = Math.max(stat.unknown, 1);
    const graduated = correct && stat.known >= required;
    if (graduated) setMastered((n) => n + 1);

    setRevealed(false);
    setQueue((prev) => {
      const [first, ...rest] = prev;
      return graduated ? rest : [...rest, first];
    });
  };

  const progress = total ? Math.round((mastered / total) * 100) : 0;

  if (loading) {
    return <div className="eng-learn-wrap eng-learn-wide"><Spin /></div>;
  }

  if (!unit) {
    return (
      <div className="eng-learn-wrap eng-learn-wide">
        <div className="eng-empty">
          <div className="eng-empty-emoji">📖</div>
          <div className="eng-empty-title">未选择单元</div>
          <button type="button" className="eng-btn eng-btn-primary" onClick={() => navigate('/student/english')}>
            返回英语学习中心
          </button>
        </div>
      </div>
    );
  }

  if (total === 0) {
    return (
      <div className="eng-learn-wrap eng-learn-wide">
        <div className="eng-empty">
          <div className="eng-empty-emoji">🐚</div>
          <div className="eng-empty-title">该单元暂无单词</div>
          <button type="button" className="eng-btn eng-btn-primary" onClick={() => navigate('/student/english')}>
            返回英语学习中心
          </button>
        </div>
      </div>
    );
  }

  if (finished) {
    return (
      <div className="eng-learn-wrap eng-learn-wide">
        <div className="eng-empty">
          <div className="eng-empty-emoji">🎉</div>
          <div className="eng-empty-title">本单元单词已学完</div>
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
          <button
            type="button"
            className="eng-btn eng-btn-primary"
            onClick={() => navigate('/student/english')}
          >
            返回学习中心
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="eng-learn-wrap eng-learn-wide">
      {currentSection && <div className="eng-section-title">{currentSection}</div>}
      <div className="eng-progress-row">
        <div className="eng-progress-track"><span style={{ width: `${progress}%` }} /></div>
        <div className="eng-progress-text">{mastered} / {total}</div>
      </div>

      <div className="eng-card eng-card-word">
        {currentWeak && <div className="eng-weak-tag">需加强</div>}
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
        {current?.imageUrl && <img className="eng-image" src={current.imageUrl} alt={current.spell} />}
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
