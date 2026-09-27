/**
 * WordDrillModal.jsx - 进入英语学习中心前的强制巩固弹窗
 *
 * 业务口径（与用户确认）:
 *   - 进入 /student/english 时自动弹出（由 EnglishCenterPage 控制，一个自然天只弹一次）
 *   - 取词 = 最近答错的单词 + 复习到期词，合计上限 10（后端 /english/word/drill）
 *   - 不必全部答对即可关闭：答对即从队列移除，答错则本轮末尾重出
 *   - 每个单词最多出现 5 次，达到上限仍未答对则放行
 *   - 熟练度按「首次出现」的对错回写，因此某词多次复习都一遍过会自然移出巩固池
 *
 * 出题复用 utils/englishQuiz（同批单词 >= 4 时出 4 选 1 选择题，否则自评）
 *
 * 被谁引用: EnglishCenterPage
 * 依赖: api/englishStudent、utils/english、utils/englishQuiz、antd
 */

import { useEffect, useRef, useState } from 'react';
import { Button, Modal, Progress } from 'antd';
import { CheckCircleFilled, CloseCircleFilled } from '@ant-design/icons';
import { recordEnglishWord, recordEnglishSession } from '../../api/englishStudent';
import { speakEnglish } from '../../utils/english';
import { buildQuestionForWord, QUIZ_TYPE, WORD_PLACEHOLDER } from '../../utils/englishQuiz';

/** 单个单词在本轮巩固中最多出现的次数 */
const MAX_APPEARANCE = 5;
/** 答题后展示反馈的时长（毫秒） */
const FEEDBACK_MS = 900;

/** 按出现次数轮换题型 */
function typeAt(word, seq) {
  const types = word && word.imageUrl
    ? [QUIZ_TYPE.IMAGE, QUIZ_TYPE.MEANING, QUIZ_TYPE.WORD]
    : [QUIZ_TYPE.MEANING, QUIZ_TYPE.WORD];
  return types[seq % types.length];
}

export default function WordDrillModal({ open, words, onFinish }) {
  const [queue, setQueue] = useState([]);
  const [selectedKey, setSelectedKey] = useState(null);
  const [verdict, setVerdict] = useState(null);
  const states = useRef(new Map());
  const timer = useRef(null);
  const startAt = useRef(0);
  const sessionRecorded = useRef(false);

  const total = words ? words.length : 0;

  // 打开时初始化队列与逐词状态
  useEffect(() => {
    if (!open) {
      return;
    }
    if (timer.current) {
      clearTimeout(timer.current);
      timer.current = null;
    }
    states.current = new Map();
    (words || []).forEach((w) => states.current.set(w.id, { appearances: 0, recorded: false, cleared: false }));
    setQueue((words || []).map((w) => ({ word: w, seq: 0 })));
    setSelectedKey(null);
    setVerdict(null);
    startAt.current = Date.now();
    sessionRecorded.current = false;
  }, [open, words]);

  useEffect(() => () => {
    if (timer.current) {
      clearTimeout(timer.current);
    }
  }, []);

  const current = queue[0];
  const clearedCount = (words || []).filter((w) => states.current.get(w.id)?.cleared).length;
  const done = open && total > 0 && queue.length === 0;

  // 同批单词不足 4 个时无法出选择题，退化为「认识 / 不认识」自评
  const question = current && total >= 4
    ? buildQuestionForWord(current.word, words, typeAt(current.word, current.seq), `${current.seq}-${current.word.id}`)
    : null;
  const useJudge = Boolean(current) && !question;

  // 全部巩固完成后记录一次学习会话（每个自然天至多一次）
  useEffect(() => {
    if (!done || sessionRecorded.current) {
      return;
    }
    sessionRecorded.current = true;
    recordEnglishSession({
      type: 'drill',
      durationSeconds: Math.max(1, Math.round((Date.now() - startAt.current) / 1000)),
      correctCount: clearedCount,
    }).catch(() => {});
  }, [done, clearedCount]);

  const answer = (correct, key) => {
    if (!current || verdict) {
      return;
    }
    const id = current.word.id;
    const st = states.current.get(id) || { appearances: 0, recorded: false, cleared: false };
    st.appearances += 1;
    if (!st.recorded) {
      st.recorded = true;
      recordEnglishWord({ wordId: id, correct }).catch(() => {});
    }
    if (correct) {
      st.cleared = true;
    }
    states.current.set(id, st);

    const rest = queue.slice(1);
    const canRetry = !correct && !st.cleared && st.appearances < MAX_APPEARANCE;
    const nextQueue = canRetry
      ? [...rest, { word: current.word, seq: st.appearances }]
      : rest;

    setSelectedKey(key ?? null);
    setVerdict(correct ? 'ok' : 'no');
    timer.current = setTimeout(() => {
      setQueue(nextQueue);
      setVerdict(null);
      setSelectedKey(null);
      timer.current = null;
    }, FEEDBACK_MS);
  };

  const renderJudge = () => (
    <div className="eng-drill-judge">
      <div
        className="eng-word eng-word-clickable"
        role="button"
        tabIndex={0}
        title="点击朗读"
        onClick={() => speakEnglish(current.word.spell, current.word.audioUrl)}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            speakEnglish(current.word.spell, current.word.audioUrl);
          }
        }}
      >
        {current.word.spell}
      </div>
      {current.word.phonetic ? <div className="eng-phonetic">/{current.word.phonetic}/</div> : null}
      {verdict ? <div className="eng-drill-meaning">{current.word.meaning}</div> : null}
      <div className="eng-drill-judge-actions">
        <button type="button" className="eng-drill-judge-btn is-no" disabled={Boolean(verdict)} onClick={() => answer(false)}>
          <CloseCircleFilled /> 不认识
        </button>
        <button type="button" className="eng-drill-judge-btn is-ok" disabled={Boolean(verdict)} onClick={() => answer(true)}>
          <CheckCircleFilled /> 认识
        </button>
      </div>
    </div>
  );

  const renderChoice = () => (
    <>
      <div className="eng-quiz-badge">{question.typeLabel}</div>
      {question.promptType === 'image' && (
        <img
          className="eng-quiz-image"
          src={question.imageUrl || WORD_PLACEHOLDER}
          alt="看图选单词"
          onError={(e) => { e.currentTarget.src = WORD_PLACEHOLDER; }}
        />
      )}
      {question.promptType === 'meaning' && (
        <>
          <div className="eng-quiz-hint">看中文，选单词</div>
          <div className="eng-quiz-prompt-meaning">{question.prompt}</div>
        </>
      )}
      {question.promptType === 'word' && (
        <>
          <div className="eng-quiz-hint">看单词，选中文</div>
          <div
            className="eng-word eng-word-clickable"
            role="button"
            tabIndex={0}
            title="点击朗读"
            onClick={() => speakEnglish(question.prompt, question.audioUrl)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                speakEnglish(question.prompt, question.audioUrl);
              }
            }}
          >
            {question.prompt}
          </div>
          {question.phonetic ? <div className="eng-phonetic">/{question.phonetic}/</div> : null}
        </>
      )}
      <div className="eng-quiz-options">
        {question.options.map((option) => {
          let cls = 'eng-quiz-option';
          if (verdict && option.correct) cls += ' correct';
          else if (verdict && option.key === selectedKey) cls += ' wrong';
          return (
            <button
              key={option.key}
              type="button"
              className={cls}
              disabled={Boolean(verdict)}
              onClick={() => {
                setSelectedKey(option.key);
                answer(option.correct, option.key);
              }}
            >
              {option.label}
            </button>
          );
        })}
      </div>
    </>
  );

  return (
    <Modal
      open={open}
      title="先巩固一下，再开始今天的学习"
      width={560}
      closable={false}
      maskClosable={false}
      keyboard={false}
      footer={done ? (
        <Button type="primary" size="large" onClick={onFinish}>开始今天的学习</Button>
      ) : (
        <Button type="primary" size="large" disabled>完成巩固后开始学习</Button>
      )}
    >
      <div className="eng-drill">
        <div className="eng-drill-head">
          <span className="eng-drill-tip">最近答错 / 复习到期的单词，先练一练再进入单元学习。</span>
          <span className="eng-drill-progress-text">已掌握 {clearedCount}/{total}</span>
        </div>
        <Progress percent={total ? Math.round((clearedCount / total) * 100) : 0} showInfo={false} strokeColor="#3aa6dd" />

        {done ? (
          <div className="eng-drill-done">
            <CheckCircleFilled /> 今天的巩固完成啦，去学习新单元吧！
          </div>
        ) : current ? (
          <>
            <div className="eng-drill-meta">
              {question ? question.typeLabel : '看单词自评'} · 第 {current.seq + 1} 遍
            </div>
            {useJudge ? renderJudge() : renderChoice()}
            {verdict ? (
              <div className={`eng-feedback ${verdict}`}>
                {verdict === 'ok' ? '答对啦！' : '再想想，这个单词稍后会再出现一次'}
              </div>
            ) : null}
          </>
        ) : null}
      </div>
    </Modal>
  );
}
