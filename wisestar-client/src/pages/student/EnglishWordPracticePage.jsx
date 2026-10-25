/**
 * EnglishWordPracticePage.jsx - 学员端英语单词选择题练习
 *
 * 功能:
 *   1. 以单元为单位出选择题，三种题型：看图选单词 / 看中文选单词 / 看单词选中文
 *      读音只在学员点「听发音」或点单词本身时播放，不自动朗读
 *   2. 每题 4 个选项（1 正确 + 3 同单元干扰项），作答即判分并展示正确答案
 *   3. 答错单词在本轮末尾重出，直到答对（每词最多重出 3 次，防卡死）
 *   4. 作答回写单词熟练度与复习队列，结束记录会话（type=word-quiz）
 *
 * URL: /student/english/practice?unit=xxx
 *      /student/english/practice?mode=review&ids=1,2,3（智能复习·单词复习，跨单元）
 * 被谁引用: App.jsx 路由表；入口来自英语学习中心单元卡片「单词练习」
 *
 * 数据流:
 *   GET  /api/english/word/word-book → 单元单词（单元练习）
 *   GET  /api/english/student/review/words → 跨单元复习队列（mode=review）
 *   POST /api/english/word/record    → 记录单词作答
 *   POST /api/english/student/session → 记录会话
 */

import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Spin, message } from 'antd';
import useStudentStore from '../../stores/useStudentStore';
import { getEnglishWordBook, getEnglishWordReview, recordEnglishWord, recordEnglishSession } from '../../api/englishStudent';
import { prefetchEnglishAudio, speakEnglish, stopEnglishAudio, WORD_TTS_SPEED } from '../../utils/english';
import { buildQuiz, buildQuestionForWord, availableTypes, WORD_PLACEHOLDER } from '../../utils/englishQuiz';
import HandRaiseButton from '../../components/student/HandRaiseButton';
import MascotBubble from '../../components/student/MascotBubble';
import './EnglishCenterPage.css';

/** 同一单词本轮答错后最多重出的次数（防止一直答错无法结束） */
const MAX_RETRY = 3;

export default function EnglishWordPracticePage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const unit = searchParams.get('unit') || '';
  // 复习页可只传待复习单词 id（逗号分隔），避免把整单元单词重做一遍
  const idsParam = searchParams.get('ids') || '';
  // mode=review：智能复习的「单词复习」，跨单元取词（不分unit）
  const reviewMode = searchParams.get('mode') === 'review';
  const version = useStudentStore((s) => s.version);
  const grade = useStudentStore((s) => s.grade);
  const term = useStudentStore((s) => s.term);

  const [words, setWords] = useState([]);
  const [questions, setQuestions] = useState([]);
  const [index, setIndex] = useState(0);
  const [selectedKey, setSelectedKey] = useState(null);
  const [correctCount, setCorrectCount] = useState(0);
  const [wrongIds, setWrongIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);
  const startAt = useRef(Date.now());
  const sessionRecorded = useRef(false);
  // 本轮内每个单词已答错次数（答错后重出，直到答对，最多 MAX_RETRY 次）
  const wrongAttempts = useRef(new Map());
  // 本轮首答统计（同一单词只计首次作答，重做不计入；掌握度判定以此为准）
  const firstAttempts = useRef(new Set());
  const firstTryCorrect = useRef(0);

  const load = useCallback(() => {
    if (!unit && !reviewMode) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setLoadFailed(false);
    // 复习模式的取词（只含到期单词）与干扰项词库分开取：到期单词可能少于 4 个，
    // 选项干扰项仍从该年级整册词库中挑，保证四选一能出题。
    const source = reviewMode
      ? Promise.all([
        getEnglishWordReview({ version, grade }),
        getEnglishWordBook({ version, grade, pageSize: -1 }),
      ]).then(([reviewRes, bookRes]) => ({
        targetList: reviewRes?.data || [],
        poolList: bookRes?.data?.list || [],
      }))
      : getEnglishWordBook({ version, grade, term, unit, pageSize: -1, speed: WORD_TTS_SPEED }).then((res) => ({
        targetList: res?.data?.list || [],
        poolList: res?.data?.list || [],
      }));
    source
      .then(({ targetList, poolList }) => {
        const pool = poolList.length ? poolList : targetList;
        const ids = idsParam.split(',').map((id) => id.trim()).filter(Boolean);
        const filtered = ids.length ? targetList.filter((w) => ids.includes(w.id)) : targetList;
        // 复习队列按复习时间实时变化，ids 可能已被新一批替换；过滤为空时退回本批，避免空页
        const target = filtered.length ? filtered : targetList;
        setWords(pool);
        setQuestions(buildQuiz(target, pool));
        setIndex(0);
        setSelectedKey(null);
        setCorrectCount(0);
        setWrongIds([]);
        wrongAttempts.current = new Map();
        firstAttempts.current = new Set();
        firstTryCorrect.current = 0;
        sessionRecorded.current = false;
        startAt.current = Date.now();
      })
      .catch(() => {
        setLoadFailed(true);
        message.error('单词加载失败');
      })
      .finally(() => setLoading(false));
  }, [version, grade, term, unit, idsParam, reviewMode]);

  useEffect(() => {
    load();
  }, [load]);

  const current = questions[index];
  const answered = selectedKey != null;
  const finished = !loading && questions.length > 0 && index >= questions.length;

  // 不做任何自动朗读：题目出现、作答揭晓、切题都保持安静，只有学员点
  // 「听发音」或点单词本身才出声（自动播音会打断思考，也容易干扰同教室的其他同学）
  // 这里只预取当前题的读音，让点击时立刻出声
  useEffect(() => {
    const spell = current?.promptType === 'word'
      ? (current?.prompt || current?.word?.spell)
      : (current?.word?.spell || current?.prompt);
    if (!spell) return undefined;
    prefetchEnglishAudio([spell], WORD_TTS_SPEED);
    return undefined;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [index, current?.wordId, current?.promptType]);

  // 离开页面时停掉正在播的读音
  useEffect(() => () => stopEnglishAudio(), []);

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
    const isFirstAttempt = !firstAttempts.current.has(current.wordId);
    if (isFirstAttempt) {
      firstAttempts.current.add(current.wordId);
      if (option.correct) {
        firstTryCorrect.current += 1;
      }
    }
    recordEnglishWord({
      wordId: current.wordId,
      correct: option.correct,
      source: 'practice',
      firstAttempt: isFirstAttempt,
    }).catch(() => {});
    // 答错：本轮末尾重出，直到答对（每词最多重出 MAX_RETRY 次，防卡死）
    if (!option.correct) {
      setWrongIds((prev) => (prev.includes(current.wordId) ? prev : [...prev, current.wordId]));
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

  if (!unit && !reviewMode) {
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

  if (questions.length === 0) {
    return (
      <div className="eng-learn-wrap eng-learn-wide">
        <div className="eng-empty">
          <div className="eng-empty-title">{reviewMode ? '今天没有需要复习的单词' : '暂无可练习的题目'}</div>
          {reviewMode ? (
            <div>到期单词已经复习完，按记忆周期安排，晚点再来吧</div>
          ) : (
            <div>至少需要 4 个单词才能生成选择题</div>
          )}
          {backButton}
        </div>
      </div>
    );
  }

  if (finished) {
    const firstTryTotal = firstAttempts.current.size;
    const firstTryRate = firstTryTotal ? Math.round((firstTryCorrect.current / firstTryTotal) * 100) : 0;
    const canGoScenario = !reviewMode && firstTryRate >= 80 && words.length >= 4;
    return (
      <div className="eng-learn-wrap eng-learn-wide">
        <div className="eng-empty">
          <div className="eng-empty-emoji">🏆</div>
          <div className="eng-empty-title">本轮练习完成</div>
          <div className="eng-done-stats">
            <div className="eng-done-stat"><b>{correctCount}</b><span>答对</span></div>
            <div className="eng-done-stat"><b>{questions.length}</b><span>总题数</span></div>
            <div className="eng-done-stat"><b>{firstTryRate}%</b><span>首答正确率</span></div>
          </div>
          <div className="eng-tip" style={{ marginTop: 4 }}>
            {canGoScenario
              ? '这批词已经练稳了，去场景对话把它们用起来。'
              : '首答正确率到 80% 再去做场景对话，掌握度判定才准。'}
          </div>
          <div className="eng-actions">
            {wrongIds.length > 0 && (
              <button
                type="button"
                className={canGoScenario ? 'eng-btn eng-btn-again' : 'eng-btn eng-btn-primary'}
                onClick={() => navigate(reviewMode
                  ? `/student/english/practice?mode=review&ids=${encodeURIComponent(wrongIds.join(','))}`
                  : `/student/english/practice?unit=${encodeURIComponent(unit)}&ids=${encodeURIComponent(wrongIds.join(','))}`)}
              >
                复习错词 · {wrongIds.length}
              </button>
            )}
            {canGoScenario && (
              <button
                type="button"
                className="eng-btn eng-btn-primary"
                onClick={() => navigate(`/student/english/scenario?unit=${encodeURIComponent(unit)}`)}
              >
                去做场景对话
              </button>
            )}
            {backButton}
          </div>
        </div>
      </div>
    );
  }

  const progress = Math.round((index / questions.length) * 100);
  // 判题结论改由白卡外的海豚气泡播报，卡片里不再重复显示
  const selectedOption = current?.options.find((option) => option.key === selectedKey);
  const verdict = !answered ? 'idle' : (selectedOption?.correct ? 'ok' : 'no');

  // 举手用题目视图：单词练习是选择题，按当前题拼出单选结构，便于老师看懂题意与选项
  const raiseId = current ? `word:${current.key}` : '';
  const raiseTitle = current
    ? `${current.typeLabel}：${current.promptType === 'image' ? (current.word?.meaning || '看图选单词') : current.prompt}`
    : '';
  const raiseQuestion = current ? {
    id: raiseId,
    name: raiseTitle,
    questionType: 'Radio',
    tag: ['单词', unit || '复习'],
    schema: {
      id: raiseId,
      title: raiseTitle,
      type: 'Radio',
      attribute: { examCorrectAnswer: current.answerText },
      children: current.options.map((option, oi) => ({
        id: `${raiseId}#${oi}`,
        title: option.label,
        type: 'Option',
      })),
    },
  } : null;
  const raiseOptionIndex = current
    ? current.options.findIndex((option) => option.key === selectedKey)
    : -1;
  const raiseAnswer = raiseOptionIndex >= 0
    ? { type: 'option', optionId: `${raiseId}#${raiseOptionIndex}` }
    : null;

  return (
    <div className="eng-learn-wrap eng-learn-wide">
      <div className="eng-progress-row">
        <div className="eng-progress-track"><span style={{ width: `${progress}%` }} /></div>
        <div className="eng-progress-text">{index + 1} / {questions.length}</div>
      </div>

      <MascotBubble tone={verdict}>
        {verdict === 'idle' && (reviewMode ? '这些是你该复习的词，选一个吧～' : '选一个答案，我在旁边看着你～')}
        {verdict === 'ok' && '答对了，真棒！'}
        {verdict === 'no' && `答错啦，正确答案：${current.answerText}`}
      </MascotBubble>

      <div className="eng-card eng-card-word eng-card-quiz quiz-stage">
        {/* 听发音固定在卡片左上角，不随题干高度上下浮动 */}
        <button
          type="button"
          className="eng-quiz-sound"
          onClick={() => speakEnglish(current.word?.spell, current.audioUrl, WORD_TTS_SPEED)}
          title="听单词发音"
        >
          🔊 听发音
        </button>
        <div className="eng-quiz-badge">{current.typeLabel}</div>

        {current.promptType === 'image' && (
          <>
            {/* 中文提示：图片上方给出该单词的中文释义，帮助理解词义 */}
            {current.word?.meaning && (
              <div className="eng-quiz-image-hint">提示：{current.word.meaning}</div>
            )}
            <img
              className="eng-quiz-image"
              src={current.imageUrl || WORD_PLACEHOLDER}
              alt="看图选单词"
              onError={(e) => { e.currentTarget.src = WORD_PLACEHOLDER; }}
            />
          </>
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
              onClick={() => speakEnglish(current.prompt, current.audioUrl, WORD_TTS_SPEED)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault();
                  speakEnglish(current.prompt, current.audioUrl, WORD_TTS_SPEED);
                }
              }}
            >
              {current.prompt}
            </div>
            {current.phonetic && <div className="eng-phonetic">/{current.phonetic}/</div>}
          </>
        )}

        {/* 答案回显位常驻：未作答时留一块空白，作答后就地填充，选项与判题区不会整体下移 */}
        <div className={`eng-quiz-reinforce${answered && current.word?.meaning ? '' : ' is-blank'}`}>
          {answered && current.word?.meaning && (
            <>
              <b className="eng-quiz-reinforce-word">{current.word.spell}</b>
              {current.word.phonetic && (
                <span className="eng-quiz-reinforce-phonetic">/{current.word.phonetic}/</span>
              )}
              <span className="eng-quiz-reinforce-zh">汉语：{current.word.meaning}</span>
            </>
          )}
        </div>

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

        {raiseQuestion && (
          <HandRaiseButton
            question={raiseQuestion}
            answer={raiseAnswer}
            page="英语单词练习"
            knowledgePointName="单词"
            subject="英语"
            chapter={unit}
          />
        )}
      </div>
    </div>
  );
}
