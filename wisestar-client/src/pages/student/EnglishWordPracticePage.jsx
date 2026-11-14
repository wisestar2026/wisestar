/**
 * EnglishWordPracticePage.jsx - 学员端英语单词选择题练习
 *
 * 功能:
 *   1. 三种题型（对齐需求文档 1.3.2）：
 *        meaning 看中文选英文 / word 看英文选中文 / listen 听英文选单词
 *      由 `?type=` 指定，一进页面只出这一种题型，且该小节每个单词各出 1 题。
 *      不带 type（学习中心「单词练习」大卡、单元复习）时三种题型轮换。
 *      「看中文选英文」有配图时把图片一并作为提示，不再单独出「看图选单词」。
 *      读音只在学员点「听发音」或点单词本身时播放；仅「听英文选单词」在题目出现时自动播放一次
 *   2. 每题 4 个选项（1 正确 + 3 同单元干扰项），作答即判分并展示正确答案
 *   3. 答错单词在本轮末尾用同一题型重出一次，保证每个单词每个题型都被覆盖
 *   4. 作答回写单词熟练度与复习队列，结束记录会话（type=word-quiz）
 *
 * URL: /student/english/practice?unit=xxx&section=xxx&type=meaning
 *      /student/english/practice?mode=review&ids=1,2,3（智能复习·单词复习，跨单元）
 * 被谁引用: App.jsx 路由表；入口来自单词通关页三个练习圆环与英语学习中心「单词练习」大卡
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
import {
  getEnglishWordBook, getEnglishWordReview, getEnglishReviewCandidates, recordEnglishWord, recordEnglishSession,
} from '../../api/englishStudent';
import { prefetchEnglishAudio, speakEnglish, stopEnglishAudio, WORD_TTS_SPEED } from '../../utils/english';
import {
  buildQuiz, buildQuestionForWord, QUIZ_TYPE, QUIZ_TYPE_ANGLE, QUIZ_TYPE_LABEL, WORD_PLACEHOLDER,
} from '../../utils/englishQuiz';
import HandRaiseButton from '../../components/student/HandRaiseButton';
import MascotBubble from '../../components/student/MascotBubble';
import './EnglishCenterPage.css';

/** 同一单词本轮答错后重出的次数上限（答错后按同题型补 1 次，避免无限重做） */
const MAX_RETRY = 2;

/** 支持的题型；`?type=` 不在这三种内时退回题型轮换 */
const SUPPORTED_TYPES = [QUIZ_TYPE.MEANING, QUIZ_TYPE.WORD, QUIZ_TYPE.LISTEN];
const TYPE_ALIAS = { image: QUIZ_TYPE.MEANING };

export default function EnglishWordPracticePage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const unit = searchParams.get('unit') || '';
  // 复习页可只传待复习单词 id（逗号分隔），避免把整单元单词重做一遍
  const idsParam = searchParams.get('ids') || '';
  // 小节入口：只练该小节的单词，干扰项仍取整单元词库（不足 4 词时四选一无法出题）
  const sectionParam = searchParams.get('section') || '';
  // mode=review：智能复习的「单词复习」，跨单元取词（不分unit）
  const reviewMode = searchParams.get('mode') === 'review';
  // 圆环直达入口：只出这一种题型（每个单词各 1 题）；空值表示三题型轮换
  const typeParam = searchParams.get('type') || '';
  const onlyType = SUPPORTED_TYPES.includes(TYPE_ALIAS[typeParam] || typeParam)
    ? (TYPE_ALIAS[typeParam] || typeParam)
    : null;
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
        // 复习时按知识点推荐下一个未考角度，避免同一考法连续重刷
        getEnglishReviewCandidates({ version, grade, term, module: 'word' }).catch(() => ({ data: [] })),
      ]).then(([reviewRes, bookRes, candidateRes]) => {
        const angleByWord = {};
        (candidateRes?.data || []).forEach((c) => {
          if (c.angles?.length) angleByWord[c.knowledgeId] = c.angles[0];
        });
        return {
          targetList: reviewRes?.data || [],
          poolList: bookRes?.data?.list || [],
          angleByWord,
        };
      })
      : getEnglishWordBook({ version, grade, term, unit, pageSize: -1, speed: WORD_TTS_SPEED })
        .then((res) => {
          const list = res?.data?.list || [];
          const scoped = sectionParam
            ? list.filter((w) => String(w.section || '').trim() === sectionParam.trim())
            : list;
          const targets = scoped.length ? scoped : list;
          // 单元词汇不足 4 个时四选一凑不出干扰项（如六年级下册 Unit 4 仅 3 词），
          // 干扰项退回整册词库，避免整页提示「至少需要 4 个单词」。
          if (list.length >= 4) {
            return { targetList: targets, poolList: list, angleByWord: null };
          }
          return getEnglishWordBook({ version, grade, term, pageSize: -1, speed: WORD_TTS_SPEED })
            .then((bookRes) => {
              const bookList = bookRes?.data?.list || [];
              return {
                targetList: targets,
                poolList: bookList.length > list.length ? bookList : list,
                angleByWord: null,
              };
            })
            .catch(() => ({ targetList: targets, poolList: list, angleByWord: null }));
        });
    source
      .then(({ targetList, poolList, angleByWord }) => {
        const pool = poolList.length ? poolList : targetList;
        const ids = idsParam.split(',').map((id) => id.trim()).filter(Boolean);
        const filtered = ids.length ? targetList.filter((w) => ids.includes(w.id)) : targetList;
        // 单元复习候选中可能包含「薄弱但未到期」的词，不在当日复习队列里；
        // 这时从整册词库按候选 id 取词，保证复习页点进来的知识点都能练到。
        const fromPool = ids.length && !filtered.length
          ? pool.filter((w) => ids.includes(w.id))
          : [];
        // 复习队列按复习时间实时变化，ids 也可能已被新一批替换；仍为空时退回本批，避免空页
        const target = filtered.length ? filtered : (fromPool.length ? fromPool : targetList);
        setWords(pool);
        setQuestions(buildQuiz(target, pool, angleByWord, onlyType));
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
  }, [version, grade, term, unit, idsParam, reviewMode, sectionParam, onlyType]);

  useEffect(() => {
    load();
  }, [load]);

  const current = questions[index];
  const answered = selectedKey != null;
  const finished = !loading && questions.length > 0 && index >= questions.length;

  // 默认不自动朗读：题目出现、作答揭晓、切题都保持安静，只有学员点
  // 「听发音」或点单词本身才出声（自动播音会打断思考，也容易干扰同教室的其他同学）。
  // 例外是「听英文选单词」：题干本身就是声音，出现时自动播一次。
  useEffect(() => {
    const spell = current?.promptType === 'word'
      ? (current?.prompt || current?.word?.spell)
      : (current?.word?.spell || current?.prompt);
    if (!spell) return undefined;
    prefetchEnglishAudio([spell], WORD_TTS_SPEED);
    if (current?.promptType === 'listen' && !answered) {
      speakEnglish(spell, current?.audioUrl, WORD_TTS_SPEED);
    }
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
      angle: QUIZ_TYPE_ANGLE[current.type],
    }).catch(() => {});
    // 答错：本轮末尾重出，直到答对（每词最多重出 MAX_RETRY 次，防卡死）
    if (!option.correct) {
      setWrongIds((prev) => (prev.includes(current.wordId) ? prev : [...prev, current.wordId]));
      const attempts = (wrongAttempts.current.get(current.wordId) || 0) + 1;
      wrongAttempts.current.set(current.wordId, attempts);
      if (attempts < MAX_RETRY) {
        // 用同一题型重出：本轮承诺「每个单词每个题型都覆盖」，
        // 换题型会让这一种的题量悄悄变少、另一种多出来。
        const retry = buildQuestionForWord(
          current.word, words, current.type, `retry-${current.wordId}-${attempts}`,
        );
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
                  : `/student/english/practice?unit=${encodeURIComponent(unit)}&ids=${encodeURIComponent(wrongIds.join(','))}${sectionParam ? `&section=${encodeURIComponent(sectionParam)}` : ''}${onlyType ? `&type=${onlyType}` : ''}`)}
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
    ? `${current.typeLabel}：${current.promptType === 'listen'
      ? (current.word?.spell || QUIZ_TYPE_LABEL[QUIZ_TYPE.LISTEN])
      : current.prompt}`
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

        {current.promptType === 'meaning' && (
          <>
            <div className="eng-quiz-hint">看中文，选英文</div>
            <div className="eng-quiz-prompt-meaning">{current.prompt}</div>
            {/* 配图只是「看中文选英文」的辅助提示，不单独构成题型 */}
            {current.imageUrl && (
              <img
                className="eng-quiz-image"
                src={current.imageUrl}
                alt={current.prompt || '单词配图'}
                onError={(e) => { e.currentTarget.src = WORD_PLACEHOLDER; }}
              />
            )}
          </>
        )}

        {current.promptType === 'listen' && (
          <>
            <div className="eng-quiz-hint">听英文，选单词</div>
            <button
              type="button"
              className="eng-quiz-listen"
              onClick={() => speakEnglish(current.word?.spell, current.audioUrl, WORD_TTS_SPEED)}
            >
              🔊 再听一遍
            </button>
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
