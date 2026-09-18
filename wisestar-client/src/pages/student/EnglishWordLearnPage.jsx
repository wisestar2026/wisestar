/**
 * EnglishWordLearnPage.jsx - 学员端英语单词卡片学习
 *
 * 功能:
 *   1. 展示单元单词卡片（拼写 / 音标 / 释义 / 图片 / 发音）
 *   2. 认识 / 不认识 → 回写熟练度与复习队列
 *   3. 学习结束记录会话
 *
 * URL: /student/english/word?unit=xxx
 * 被谁引用: App.jsx 路由表；入口来自英语学习中心单元卡片
 *
 * 数据流:
 *   GET  /api/english/word/word-book → 单元单词
 *   POST /api/english/word/record    → 记录作答
 *   POST /api/english/student/session → 记录会话
 */

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
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

export default function EnglishWordLearnPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const unit = searchParams.get('unit') || '';
  const version = useStudentStore((s) => s.version);
  const grade = useStudentStore((s) => s.grade);

  const [term] = useState('上册');
  const [words, setWords] = useState([]);
  const [sectionLabels, setSectionLabels] = useState([]);
  const [loading, setLoading] = useState(true);
  const [index, setIndex] = useState(0);
  const [revealed, setRevealed] = useState(false);
  const [known, setKnown] = useState(0);
  const [again, setAgain] = useState(0);
  const startAt = useRef(Date.now());

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
        const labels = [];
        groups.forEach((group) => group.items.forEach((item) => {
          flat.push(item);
          labels.push(group.section);
        }));
        setWords(flat);
        setSectionLabels(labels);
      })
      .catch(() => message.error('单词加载失败'))
      .finally(() => setLoading(false));
  }, [version, grade, term, unit]);

  useEffect(() => {
    loadWords();
  }, [loadWords]);

  const current = words[index];
  const finished = !loading && words.length > 0 && index >= words.length;
  const durationSeconds = Math.round((Date.now() - startAt.current) / 1000);

  const recordSession = useCallback(() => {
    const correct = known;
    recordEnglishSession({ type: 'word', durationSeconds, correctCount: correct }).catch(() => {});
  }, [durationSeconds, known]);

  const handleAnswer = (correct) => {
    if (!current) return;
    recordEnglishWord({ wordId: current.id, correct }).catch(() => {});
    if (correct) setKnown((n) => n + 1);
    else setAgain((n) => n + 1);
    setRevealed(false);
    setIndex((i) => i + 1);
  };

  const progress = useMemo(
    () => (words.length ? Math.round((index / words.length) * 100) : 0),
    [index, words.length],
  );

  if (loading) {
    return <div className="eng-learn-wrap"><Spin /></div>;
  }

  if (!unit) {
    return (
      <div className="eng-learn-wrap">
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

  if (words.length === 0) {
    return (
      <div className="eng-learn-wrap">
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
      <div className="eng-learn-wrap">
        <div className="eng-empty">
          <div className="eng-empty-emoji">🎉</div>
          <div className="eng-empty-title">本单元单词已学完</div>
          <div className="eng-done-stats">
            <div className="eng-done-stat"><b>{known}</b><span>认识</span></div>
            <div className="eng-done-stat"><b>{again}</b><span>需加强</span></div>
          </div>
          <button
            type="button"
            className="eng-btn eng-btn-primary"
            onClick={() => { recordSession(); navigate('/student/english'); }}
          >
            返回学习中心
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="eng-learn-wrap">
      {sectionLabels[index] && <div className="eng-section-title">{sectionLabels[index]}</div>}
      <div className="eng-progress-row">
        <div className="eng-progress-track"><span style={{ width: `${progress}%` }} /></div>
        <div className="eng-progress-text">{index + 1} / {words.length}</div>
      </div>

      <div className="eng-card">
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

      <div className="eng-actions">
        <button type="button" className="eng-btn eng-btn-ghost" onClick={() => { recordSession(); navigate('/student/english'); }}>
          结束学习
        </button>
      </div>
    </div>
  );
}
