/**
 * EnglishWordPracticePage.jsx - 学员端英语单词选择题练习
 *
 * 功能:
 *   1. 以单元为单位出选择题，三种题型：看图选单词 / 看中文选单词 / 看单词选中文
 *   2. 每题 4 个选项（1 正确 + 3 同单元干扰项），作答即判分并展示正确答案
 *   3. 答错单词在本轮末尾重出，直到答对（每词最多重出 3 次，防卡死）
 *   4. 作答回写单词熟练度与复习队列，结束记录会话（type=word-quiz）
 *
 * URL: /student/english/practice?unit=xxx
 * 被谁引用: App.jsx 路由表；入口来自英语学习中心单元卡片「单词练习」
 *
 * 数据流:
 *   GET  /api/english/word/word-book → 单元单词
 *   POST /api/english/word/record    → 记录单词作答
 *   POST /api/english/student/session → 记录会话
 */

import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Spin, message } from 'antd';
import useStudentStore from '../../stores/useStudentStore';
import { getEnglishWordBook, recordEnglishWord, recordEnglishSession } from '../../api/englishStudent';
import { speakEnglish } from '../../utils/english';
import { buildQuiz, buildQuestionForWord, availableTypes, WORD_PLACEHOLDER } from '../../utils/englishQuiz';
import './EnglishCenterPage.css';

/** 同一单词本轮答错后最多重出的次数（防止一直答错无法结束） */
const MAX_RETRY = 3;

export default function EnglishWordPracticePage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const unit = searchParams.get('unit') || '';
  const version = useStudentStore((s) => s.version);
  const grade = useStudentStore((s) => s.grade);
  const term = useStudentStore((s) => s.term);

  const [words, setWords] = useState([]);
  const [questions, setQuestions] = useState([]);
  const [index, setIndex] = useState(0);
  const [selectedKey, setSelectedKey] = useState(null);
  const [correctCount, setCorrectCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);
  const startAt = useRef(Date.now());
  const sessionRecorded = useRef(false);
  // 本轮内每个单词已答错次数（答错后重出，直到答对，最多 MAX_RETRY 次）
  const wrongAttempts = useRef(new Map());

  const load = useCallback(() => {
    if (!unit) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setLoadFailed(false);
    getEnglishWordBook({ version, grade, term, unit, pageSize: -1 })
      .then((res) => {
        const list = res?.data?.list || [];
        setWords(list);
        setQuestions(buildQuiz(list));
        setIndex(0);
        setSelectedKey(null);
        setCorrectCount(0);
        wrongAttempts.current = new Map();
        sessionRecorded.current = false;
        startAt.current = Date.now();
      })
      .catch(() => {
        setLoadFailed(true);
        message.error('单词加载失败');
      })
      .finally(() => setLoading(false));
  }, [version, grade, term, unit]);

  useEffect(() => {
    load();
  }, [load]);

  const current = questions[index];
  const answered = selectedKey != null;
  const finished = !loading && questions.length > 0 && index >= questions.length;

  // 完成一轮后记录会话
  useEffect(() => {
    if (!finished || sessionRecorded.current) return;
    sessionRecorded.current = true;
    recordEnglishSession({
      type: 'word-quiz',
      durationSeconds: Math.round((Date.now() - startAt.current) / 1000),
      correctCount,
    }).catch(() => {});
  }, [finished, correctCount]);

  const handleSelect = (option) => {
    if (answered || !current) return;
    setSelectedKey(option.key);
    if (option.correct) {
      setCorrectCount((n) => n + 1);
    }
    recordEnglishWord({ wordId: current.wordId, correct: option.correct }).catch(() => {});
    // 答错：本轮末尾重出，直到答对（每词最多重出 MAX_RETRY 次，防卡死）
    if (!option.correct) {
      const attempts = (wrongAttempts.current.get(current.wordId) || 0) + 1;
      wrongAttempts.current.set(current.wordId, attempts);
      if (attempts < MAX_RETRY) {
        // 换一种题型重出，避免机械记忆同一题面
        const types = availableTypes(current.word);
        const preferred = types[(types.indexOf(current.type) + 1) % types.length];
        const retry = buildQuestionForWord(current.word, words, preferred, `retry-${current.wordId}-${attempts}`);
        if (retry) setQuestions((prev) => [...prev, retry]);
      }
    }
  };

  const handleNext = () => {
    setSelectedKey(null);
    setIndex((i) => i + 1);
  };

  if (loading) {
    return <div className="eng-learn-wrap eng-learn-wide"><Spin /></div>;
  }

  const backButton = (
    <button type="button" className="eng-btn eng-btn-primary" onClick={() => navigate('/student/english')}>
      返回英语学习中心
    </button>
  );

  if (!unit) {
    return (
      <div className="eng-learn-wrap eng-learn-wide">
        <div className="eng-empty">
          <div className="eng-empty-title">未选择单元</div>
          {backButton}
        </div>
      </div>
    );
  }

  if (loadFailed) {
    return (
      <div className="eng-learn-wrap eng-learn-wide">
        <div className="eng-empty">
          <div className="eng-empty-title">单词加载失败</div>
          <button type="button" className="eng-btn eng-btn-primary" onClick={load}>重新加载</button>
        </div>
      </div>
    );
  }

  if (words.length < 4) {
    return (
      <div className="eng-learn-wrap eng-learn-wide">
        <div className="eng-empty">
          <div className="eng-empty-title">本单元单词太少，暂时无法出题</div>
          <div>至少需要 4 个单词才能生成选择题</div>
          {backButton}
        </div>
      </div>
    );
  }

  if (questions.length === 0) {
    return (
      <div className="eng-learn-wrap eng-learn-wide">
        <div className="eng-empty">
          <div className="eng-empty-title">暂无可练习的题目</div>
          {backButton}
        </div>
      </div>
    );
  }

  if (finished) {
    return (
      <div className="eng-learn-wrap eng-learn-wide">
        <div className="eng-empty">
          <div className="eng-empty-emoji">🏆</div>
          <div className="eng-empty-title">本轮练习完成</div>
          <div className="eng-done-stats">
            <div className="eng-done-stat"><b>{correctCount}</b><span>答对</span></div>
            <div className="eng-done-stat"><b>{questions.length}</b><span>总题数</span></div>
          </div>
          {backButton}
        </div>
      </div>
    );
  }

  const progress = Math.round((index / questions.length) * 100);

  return (
    <div className="eng-learn-wrap eng-learn-wide">
      <div className="eng-progress-row">
        <div className="eng-progress-track"><span style={{ width: `${progress}%` }} /></div>
        <div className="eng-progress-text">{index + 1} / {questions.length}</div>
      </div>

      <div className="eng-card eng-card-word">
        <div className="eng-quiz-badge">{current.typeLabel}</div>

        {current.promptType === 'image' && (
          <img
            className="eng-quiz-image"
            src={current.imageUrl || WORD_PLACEHOLDER}
            alt="看图选单词"
            onError={(e) => { e.currentTarget.src = WORD_PLACEHOLDER; }}
          />
        )}

        {current.promptType === 'meaning' && (
          <>
            <div className="eng-quiz-hint">看中文，选单词</div>
            <div className="eng-quiz-prompt-meaning">{current.prompt}</div>
          </>
        )}

        {current.promptType === 'word' && (
          <>
            <div className="eng-quiz-hint">看单词，选中文</div>
            <div
              className="eng-word eng-word-clickable"
              role="button"
              tabIndex={0}
              title="点击朗读"
              onClick={() => speakEnglish(current.prompt, current.audioUrl)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault();
                  speakEnglish(current.prompt, current.audioUrl);
                }
              }}
            >
              {current.prompt}
            </div>
            {current.phonetic && <div className="eng-phonetic">/{current.phonetic}/</div>}
          </>
        )}

        <div className="eng-quiz-options">
          {current.options.map((option) => {
            let cls = 'eng-quiz-option';
            if (answered && option.correct) cls += ' correct';
            else if (answered && option.key === selectedKey) cls += ' wrong';
            return (
              <button
                key={option.key}
                type="button"
                className={cls}
                disabled={answered}
                onClick={() => handleSelect(option)}
              >
                {option.label}
              </button>
            );
          })}
        </div>

        {answered && (
          <div className={`eng-feedback ${current.options.find((o) => o.key === selectedKey)?.correct ? 'ok' : 'no'}`}>
            {current.options.find((o) => o.key === selectedKey)?.correct
              ? '答对了，真棒！'
              : `答错啦，正确答案：${current.answerText}`}
          </div>
        )}

        <div className="eng-actions">
          <button
            type="button"
            className="eng-btn eng-btn-primary"
            disabled={!answered}
            onClick={handleNext}
          >
            {index + 1 >= questions.length ? '查看结果' : '下一题'}
          </button>
        </div>
      </div>
    </div>
  );
}
