/**
 * EnglishSentenceExercise.jsx - 学员端英语「句子练习」（多题型，读取管理端已固化内容）
 *
 * 题型:
 *   build     连词成句（给中文与正确英文，点词块拼句）
 *   dictation 句子默写（给中文，输入英文）
 *   choice    句子选择（给中文题干，选正确英文句）
 *
 * 数据流:
 *   GET  /api/english/student/sentence-exercises?version=&grade=&term=&unit=
 *   POST /api/english/student/sentence/record  { sentenceId, correct }
 *   POST /api/english/student/session          { type:'sentence', durationSeconds, correctCount }
 *
 * 被谁引用: EnglishSentenceLearnPage（句式通关页面内「句子练习」模式）
 */

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Empty, Input, Spin, message } from 'antd';
import {
  getEnglishSentenceExercises,
  recordEnglishSentence,
  recordEnglishSession,
} from '../../api/englishStudent';
import { speakEnglish, tokenizeSentence, normalizeAnswer, shuffle } from '../../utils/english';

const KIND_LABELS = { build: '连词成句', dictation: '句子默写', choice: '句子选择' };

export default function EnglishSentenceExercise({ unit, version, grade, term, onBack }) {
  const [loading, setLoading] = useState(true);
  const [exercises, setExercises] = useState([]);
  const [index, setIndex] = useState(0);
  const [feedback, setFeedback] = useState(null); // 'ok' | 'no'
  const [correctCount, setCorrectCount] = useState(0);
  const startAt = useRef(Date.now());
  const finishedRef = useRef(false);

  // 连词成句：词块池与已选
  const [pool, setPool] = useState([]);
  const [selected, setSelected] = useState([]);
  // 句子默写：输入
  const [typed, setTyped] = useState('');

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    getEnglishSentenceExercises({ version, grade, term, unit })
      .then((res) => {
        if (cancelled) return;
        setExercises(res?.data || []);
        setIndex(0);
        setCorrectCount(0);
        setFeedback(null);
        startAt.current = Date.now();
        finishedRef.current = false;
      })
      .catch(() => { if (!cancelled) message.error('句子练习加载失败'); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [unit, version, grade, term]);

  const current = exercises[index];

  // 切题时重置作答状态
  useEffect(() => {
    if (!current) return;
    setFeedback(null);
    setSelected([]);
    setTyped('');
    if (current.kind === 'build') {
      setPool(shuffle(tokenizeSentence(current.en || '')));
    } else {
      setPool([]);
    }
  }, [index, current]);

  const finished = !loading && exercises.length > 0 && index >= exercises.length;

  useEffect(() => {
    if (finished && !finishedRef.current) {
      finishedRef.current = true;
      const durationSeconds = Math.round((Date.now() - startAt.current) / 1000);
      recordEnglishSession({ type: 'sentence', durationSeconds, correctCount }).catch(() => {});
    }
  }, [finished, correctCount]);

  const record = useCallback((ok) => {
    if (current?.sentenceId) {
      recordEnglishSentence({ sentenceId: current.sentenceId, correct: ok }).catch(() => {});
    }
  }, [current]);

  const goNext = useCallback(() => setIndex((i) => i + 1), []);

  const submit = useCallback((studentText) => {
    if (!current) return;
    const ok = normalizeAnswer(studentText) === normalizeAnswer(current.en);
    setFeedback(ok ? 'ok' : 'no');
    if (ok) setCorrectCount((n) => n + 1);
    record(ok);
  }, [current, record]);

  const handleBuildCheck = () => {
    const answer = selected.map((i) => pool[i]).join(' ');
    submit(answer);
  };

  const handleDictationCheck = () => submit(typed);

  const optionList = useMemo(() => {
    if (current?.kind !== 'choice') return [];
    const opts = Array.isArray(current.options) ? current.options : [];
    return opts.length ? opts : [current.en];
  }, [current]);

  if (loading) {
    return <div className="eng-learn-wrap"><Spin /></div>;
  }

  if (exercises.length === 0) {
    return (
      <div className="eng-learn-wrap">
        <button type="button" className="eng-learn-back" onClick={onBack}>← 返回句式通关</button>
        <Empty description="该单元暂无句子练习，请老师先在后台生成固化" image={Empty.PRESENTED_IMAGE_SIMPLE} />
      </div>
    );
  }

  if (finished) {
    return (
      <div className="eng-learn-wrap">
        <button type="button" className="eng-learn-back" onClick={onBack}>← 返回句式通关</button>
        <div className="eng-empty">
          <div className="eng-empty-emoji">🌟</div>
          <div className="eng-empty-title">本单元句子练习已完成</div>
          <div className="eng-done-stats">
            <div className="eng-done-stat"><b>{correctCount}</b><span>答对</span></div>
            <div className="eng-done-stat"><b>{exercises.length}</b><span>题目总数</span></div>
          </div>
          <div className="eng-actions">
            <button type="button" className="eng-btn eng-btn-ghost" onClick={onBack}>返回句式通关</button>
          </div>
        </div>
      </div>
    );
  }

  const progress = Math.round((index / exercises.length) * 100);
  const kindLabel = KIND_LABELS[current?.kind] || '句子练习';

  return (
    <div className="eng-learn-wrap">
      <button type="button" className="eng-learn-back" onClick={onBack}>← 返回句式通关</button>
      <div className="eng-progress-row">
        <div className="eng-progress-track"><span style={{ width: `${progress}%` }} /></div>
        <div className="eng-progress-text">{index + 1} / {exercises.length}</div>
      </div>

      <div className="eng-card">
        <div className="eng-sentence-hint">{kindLabel}：{current?.zh || '（无中文提示）'}</div>

        {/* 连词成句 */}
        {current?.kind === 'build' && (
          <>
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
          </>
        )}

        {/* 句子默写 */}
        {current?.kind === 'dictation' && (
          <div style={{ margin: '4px 0 8px' }}>
            <Input
              size="large"
              placeholder="请写出英文句子"
              value={typed}
              disabled={feedback === 'ok'}
              onChange={(e) => setTyped(e.target.value)}
              onPressEnter={handleDictationCheck}
            />
            {current?.hint && <div className="eng-mod-sub" style={{ marginTop: 6 }}>提示：{current.hint}</div>}
          </div>
        )}

        {/* 句子选择 */}
        {current?.kind === 'choice' && (
          <div className="eng-quiz-options" style={{ marginTop: 8 }}>
            {optionList.map((opt, oi) => {
              const isAnswer = opt === (current.answer || current.en);
              const cls = ['eng-quiz-option'];
              if (feedback && isAnswer) cls.push('correct');
              return (
                <button
                  key={oi}
                  type="button"
                  className={cls.join(' ')}
                  disabled={Boolean(feedback)}
                  onClick={() => submit(opt)}
                >
                  {String.fromCharCode(65 + oi)}. {opt}
                </button>
              );
            })}
          </div>
        )}

        {feedback === 'ok' && <div className="eng-feedback ok">太棒了，完全正确！</div>}
        {feedback === 'no' && (
          <div className="eng-feedback no">
            再想想～<div>正确答案：{current?.en}</div>
          </div>
        )}

        <div className="eng-actions">
          {current?.en && (
            <button type="button" className="eng-btn eng-btn-sound" onClick={() => speakEnglish(current.en)}>
              朗读
            </button>
          )}
          {current?.kind === 'build' && (
            <button type="button" className="eng-btn eng-btn-ghost" onClick={() => setSelected([])} disabled={feedback === 'ok'}>
              清空
            </button>
          )}
          {current?.kind === 'build' && (
            <button
              type="button"
              className="eng-btn eng-btn-primary"
              onClick={handleBuildCheck}
              disabled={selected.length === 0 || feedback === 'ok'}
            >
              检查
            </button>
          )}
          {current?.kind === 'dictation' && (
            <button
              type="button"
              className="eng-btn eng-btn-primary"
              onClick={handleDictationCheck}
              disabled={!typed.trim() || feedback === 'ok'}
            >
              检查
            </button>
          )}
          {feedback === 'ok' && (
            <button type="button" className="eng-btn eng-btn-primary" onClick={goNext}>
              下一题
            </button>
          )}
          {feedback === 'no' && (
            <button type="button" className="eng-btn eng-btn-ghost" onClick={goNext}>
              跳过
            </button>
          )}
        </div>
      </div>

      <div className="eng-actions">
        <button type="button" className="eng-btn eng-btn-ghost" onClick={onBack}>
          结束练习
        </button>
      </div>
    </div>
  );
}
