/**
 * EnglishWordUsePage.jsx - 学员端英语「单词运用」（选词填空 / 词汇默写）
 *
 * 功能:
 *   1. 选词填空：按小节目标单词逐词出题，题干给中文提示与备选词库，学员手动输入
 *   2. 词汇默写：给中文释义，可播放英文音频，学员默写英文拼写
 *   3. 每道题作答后回写单词熟练度（source=word-use / word-dictation），
 *      运用阶段答错会进入模块内复习池并计入薄弱点
 *
 * URL: /student/english/word-use?unit=xxx&section=xxx&stage=use
 *      stage=use（选词填空，默认）/ stage=dictation（词汇默写）
 * 被谁引用: App.jsx 路由表；入口来自单词通关 hub 的小节「单词运用」
 *
 * 数据流:
 *   GET  /api/english/word/word-book → 小节单词（含释义与音频）
 *   POST /api/english/word/record    → 记录作答（source / angle）
 *   POST /api/english/student/session → 记录会话
 */

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Spin, message } from 'antd';
import useStudentStore from '../../stores/useStudentStore';
import { getEnglishWordBook, recordEnglishSession, recordEnglishWord } from '../../api/englishStudent';
import { prefetchEnglishAudio, speakEnglish, WORD_TTS_SPEED } from '../../utils/english';
import { shuffle } from '../../utils/english';
import { normalizeBlankText } from '../../utils/practiceHelpers';
import MascotBubble from '../../components/student/MascotBubble';
import './EnglishCenterPage.css';
import './EnglishScenarioPracticePage.css';

/** 释义里的词性前缀（n. / v. / adj. 等），出题提示时去掉 */
function stripPos(meaning) {
  return String(meaning || '').replace(/^\s*[a-zA-Z]+\.\s*/, '').trim();
}

/** 单题内部状态重置用的空值 */
const EMPTY_ANSWER = '';

export default function EnglishWordUsePage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const unit = searchParams.get('unit') || '';
  const section = searchParams.get('section') || '';
  const stage = searchParams.get('stage') === 'dictation' ? 'dictation' : 'use';
  const version = useStudentStore((s) => s.version);
  const grade = useStudentStore((s) => s.grade);
  const term = useStudentStore((s) => s.term);

  const [words, setWords] = useState([]);
  const [pool, setPool] = useState([]);
  const [index, setIndex] = useState(0);
  const [draft, setDraft] = useState(EMPTY_ANSWER);
  const [verdict, setVerdict] = useState(null); // null | 'ok' | 'no'
  const [correctCount, setCorrectCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const startAt = useRef(Date.now());
  const sessionRecorded = useRef(false);
  const inputRef = useRef(null);

  const isDictation = stage === 'dictation';
  const pageTitle = isDictation ? '词汇默写' : '选词填空';

  const load = useCallback(() => {
    if (!unit) {
      setLoading(false);
      return;
    }
    setLoading(true);
    getEnglishWordBook({ version, grade, term, unit, pageSize: -1, speed: WORD_TTS_SPEED })
      .then((res) => {
        const list = res?.data?.list || [];
        const targetSection = (section || '').trim();
        const scoped = targetSection
          ? list.filter((w) => String(w.section || '').trim() === targetSection)
          : list;
        // 词库优先级：本节单词 → 整单元单词，保证备选词足够
        const merged = scoped.length ? scoped : list;
        setWords(merged);
        setPool(list.length >= 4 ? list : merged);
        setIndex(0);
        setDraft(EMPTY_ANSWER);
        setVerdict(null);
        setCorrectCount(0);
        sessionRecorded.current = false;
        startAt.current = Date.now();
        prefetchEnglishAudio(merged.slice(0, 20).map((w) => w.spell), WORD_TTS_SPEED);
      })
      .catch(() => message.error('单词加载失败'))
      .finally(() => setLoading(false));
  }, [version, grade, term, unit, section]);

  useEffect(() => {
    load();
  }, [load]);

  const current = words[index];
  const finished = !loading && words.length > 0 && index >= words.length;

  /** 备选词库：本节单词 + 同单元干扰词，最多 8 个 */
  const wordBank = useMemo(() => {
    if (!current) return [];
    const others = pool.filter((w) => w.id !== current.id && w.spell).map((w) => w.spell);
    const distractors = shuffle(others).slice(0, 5);
    return shuffle([current.spell, ...distractors]);
  }, [current, pool]);

  useEffect(() => {
    if (finished && !sessionRecorded.current) {
      sessionRecorded.current = true;
      recordEnglishSession({
        type: isDictation ? 'word-dictation' : 'word-use',
        durationSeconds: Math.round((Date.now() - startAt.current) / 1000),
        correctCount,
      }).catch(() => {});
    }
  }, [finished, correctCount, isDictation]);

  // 切题聚焦输入框；默写题自动播放一次发音
  useEffect(() => {
    if (!current) return;
    setDraft(EMPTY_ANSWER);
    setVerdict(null);
    if (isDictation) {
      speakEnglish(current.spell, current.audioUrl, WORD_TTS_SPEED);
    }
    const timer = setTimeout(() => inputRef.current?.focus(), 120);
    return () => clearTimeout(timer);
  }, [index, current, isDictation]);

  const handleSubmit = () => {
    if (!current || verdict) return;
    const value = normalizeBlankText(draft).toLowerCase();
    if (!value) return;
    const expected = normalizeBlankText(current.spell).toLowerCase();
    // 允许带标点的短语（如 "Be careful!"）按去标点比较
    const stripPunct = (text) => text.replace(/[^a-z0-9' ]/g, '').replace(/\s+/g, ' ').trim();
    const ok = value === expected || stripPunct(value) === stripPunct(expected);
    setVerdict(ok ? 'ok' : 'no');
    if (ok) setCorrectCount((n) => n + 1);
    recordEnglishWord({
      wordId: current.id,
      correct: ok,
      source: isDictation ? 'word-dictation' : 'word-use',
      firstAttempt: true,
      angle: isDictation ? 'word-dictation' : 'word-collocation',
    }).catch(() => {});
  };

  const goNext = () => {
    setIndex((i) => i + 1);
    setDraft(EMPTY_ANSWER);
    setVerdict(null);
  };

  const backButton = (
    <button
      type="button"
      className="eng-btn eng-btn-ghost"
      onClick={() => navigate(`/student/english/word?unit=${encodeURIComponent(unit)}`)}
    >
      返回单词通关
    </button>
  );

  if (loading) {
    return <div className="eng-learn-wrap eng-learn-wide"><Spin /></div>;
  }

  if (!unit) {
    return (
      <div className="eng-learn-wrap eng-learn-wide">
        <div className="eng-empty">
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
      <div className="eng-learn-wrap eng-learn-wide">
        <div className="eng-empty">
          <div className="eng-empty-title">本节暂无可练习的单词</div>
          {backButton}
        </div>
      </div>
    );
  }

  if (finished) {
    const rate = words.length ? Math.round((correctCount / words.length) * 100) : 0;
    return (
      <div className="eng-learn-wrap eng-learn-wide">
        <div className="eng-empty">
          <div className="eng-empty-emoji">🎯</div>
          <div className="eng-empty-title">{pageTitle}完成</div>
          <div className="eng-done-stats">
            <div className="eng-done-stat"><b>{correctCount}</b><span>答对</span></div>
            <div className="eng-done-stat"><b>{words.length}</b><span>总题数</span></div>
            <div className="eng-done-stat"><b>{rate}%</b><span>正确率</span></div>
          </div>
          <div className="eng-tip" style={{ marginTop: 4 }}>
            {rate >= 80
              ? '这批词已经能主动写出来了，回单词通关看看小节是否达标。'
              : '正确率到 80% 这一阶就算过关，错词会进入复习池。'}
          </div>
          <div className="eng-actions">
            <button type="button" className="eng-btn eng-btn-primary" onClick={load}>再练一轮</button>
            {backButton}
          </div>
        </div>
      </div>
    );
  }

  const progress = Math.round((index / words.length) * 100);
  const meaningText = stripPos(current.meaning) || '—';

  return (
    <div className="eng-learn-wrap eng-learn-wide">
      <button
        type="button"
        className="eng-learn-back"
        onClick={() => navigate(`/student/english/word?unit=${encodeURIComponent(unit)}`)}
      >
        ← 返回单词通关
      </button>
      <div className="eng-section-title">
        {unit}
        {section ? ` · ${section}` : ''}
        {' · '}
        {pageTitle}
      </div>
      <div className="eng-progress-row">
        <div className="eng-progress-track"><span style={{ width: `${progress}%` }} /></div>
        <div className="eng-progress-text">{index + 1} / {words.length}</div>
      </div>

      <MascotBubble tone={verdict || 'idle'}>
        {!verdict && (isDictation ? '听一听，把英文拼出来～' : '看中文提示，在词库里挑一个词写上～')}
        {verdict === 'ok' && '写对了，真棒！'}
        {verdict === 'no' && `再记一下：${current.spell}`}
      </MascotBubble>

      <div className="eng-card eng-card-word quiz-stage">
        <div className="eng-quiz-badge">{pageTitle}</div>

        {isDictation ? (
          <>
            <div className="eng-quiz-hint">看中文，默写英文单词</div>
            <div className="eng-quiz-prompt-meaning">{meaningText}</div>
            <button
              type="button"
              className="eng-quiz-listen"
              onClick={() => speakEnglish(current.spell, current.audioUrl, WORD_TTS_SPEED)}
            >
              🔊 再听一遍
            </button>
          </>
        ) : (
          <>
            <div className="eng-quiz-hint">根据中文提示，写出对应的英文单词</div>
            <div className="eng-quiz-prompt-meaning">{meaningText}</div>
            <div className="eng-word-bank">
              {wordBank.map((spell) => (
                <span className="eng-word-bank-item" key={spell}>{spell}</span>
              ))}
            </div>
          </>
        )}

        <input
          ref={inputRef}
          className={`eng-use-input${verdict === 'ok' ? ' is-correct' : ''}${verdict === 'no' ? ' is-wrong' : ''}`}
          value={draft}
          autoComplete="off"
          spellCheck={false}
          placeholder={isDictation ? '输入英文单词' : '输入你选的单词'}
          disabled={Boolean(verdict)}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              e.preventDefault();
              if (verdict) goNext();
              else handleSubmit();
            }
          }}
        />

        {verdict === 'no' && (
          <div className="eng-use-answer">
            正确答案：<b>{current.spell}</b>
            {current.phonetic && <span className="eng-phonetic">/{current.phonetic}/</span>}
          </div>
        )}

        <div className="eng-actions">
          {!verdict ? (
            <button
              type="button"
              className="eng-btn eng-btn-primary"
              disabled={!draft.trim()}
              onClick={handleSubmit}
            >
              提交
            </button>
          ) : (
            <button type="button" className="eng-btn eng-btn-primary" onClick={goNext}>
              {index + 1 >= words.length ? '查看结果' : '下一题'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
