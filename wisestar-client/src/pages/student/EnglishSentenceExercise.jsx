/**
 * EnglishSentenceExercise.jsx - 学员端英语「句子练习」（多题型，读取管理端已固化内容）
 *
 * 题型:
 *   build          连词成句（看中文，点词块拼出英文）
 *   listen-build   听音组句（只播英文，点词块拼句，不看文本）
 *   listen-meaning 听力理解（只播英文，从 4 个中文释义中选）
 *   dictation      句子默写（给中文，按词数逐词填空）
 *   choice         句子选择（给中文题干，选正确英文句）
 *
 * 「连词成句 / 听音组句 / 听力理解」三种为按单元句子即时生成（不依赖 AI 固化内容），
 * 「综合练习」沿用管理端固化的混合题型。
 *
 * 数据流:
 *   GET  /api/english/student/sentence-exercises?version=&grade=&term=&unit=
 *   GET  /api/english/student/sentences       单元句子（用于生成听音/组句题型）
 *   POST /api/english/student/sentence/record  { sentenceId, correct }
 *   POST /api/english/student/session          { type:'sentence', durationSeconds, correctCount }
 *
 * 被谁引用: EnglishSentenceLearnPage（句式通关页面内「句子练习」模式）
 */

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Empty, Input, Spin, message } from 'antd';
import {
  getEnglishSentenceExercises,
  getEnglishSentences,
  recordEnglishSentence,
  recordEnglishSession,
} from '../../api/englishStudent';
import { speakEnglish, tokenizeSentence, normalizeAnswer, shuffle } from '../../utils/english';
import MascotBubble from '../../components/student/MascotBubble';

const KIND_LABELS = {
  build: '连词成句',
  'listen-build': '听音组句',
  'listen-meaning': '听力理解',
  dictation: '句子默写',
  choice: '句子选择',
  fill: '词汇填空',
};

/** 句子练习模式：mix 为管理端固化的综合题，其余三种按单元句子即时生成 */
const MODES = [
  { key: 'mix', label: '综合练习' },
  { key: 'build', label: '连词成句' },
  { key: 'listen-build', label: '听音组句' },
  { key: 'listen-meaning', label: '听力理解' },
];

/** 每轮即时生成题型的题量 */
const GENERATED_MAX = 10;

/** 是否听力类题型（进入即自动播放） */
function isListenKind(kind) {
  return kind === 'listen-build' || kind === 'listen-meaning';
}
const DIFFICULTY_LABELS = { 1: '基础', 2: '提高', 3: '挑战' };

/** 默写分组：每个单词一个输入框，标点跟随前一个单词展示 */
function buildDictationGroups(sentence) {
  const tokens = String(sentence || '').match(/[A-Za-z0-9][A-Za-z0-9'’-]*|[^A-Za-z0-9\s]/g) || [];
  const groups = [];
  tokens.forEach((token) => {
    if (/^[A-Za-z0-9]/.test(token)) {
      groups.push({ word: token, prefix: '', suffix: '' });
      return;
    }
    if (!groups.length) {
      return;
    }
    groups[groups.length - 1].suffix += token;
  });
  return groups;
}

export default function EnglishSentenceExercise({ unit, version, grade, term, onBack }) {
  const [loading, setLoading] = useState(true);
  const [exercises, setExercises] = useState([]);
  const [sentences, setSentences] = useState([]);
  const [mode, setMode] = useState('mix');
  const [index, setIndex] = useState(0);
  const [feedback, setFeedback] = useState(null); // 'ok' | 'no'
  const [correctCount, setCorrectCount] = useState(0);
  const startAt = useRef(Date.now());
  const finishedRef = useRef(false);

  // 连词成句：词块池与已选
  const [pool, setPool] = useState([]);
  const [selected, setSelected] = useState([]);
  // 句子默写：逐词输入；词汇填空：单框输入
  const [typed, setTyped] = useState('');
  const [dictationWords, setDictationWords] = useState([]);
  const dictationRefs = useRef([]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    Promise.all([
      getEnglishSentenceExercises({ version, grade, term, unit }).catch(() => ({ data: [] })),
      getEnglishSentences({ version, grade, term, unit }).catch(() => ({ data: [] })),
    ])
      .then(([exRes, senRes]) => {
        if (cancelled) return;
        setExercises(exRes?.data || []);
        setSentences(senRes?.data || []);
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

  // 即时生成题型：按单元句子直接产出，答案/释义都来自句子本体，不需要 AI
  const generated = useMemo(() => {
    if (mode === 'mix') {
      return [];
    }
    const pool = sentences.filter((s) => s?.en).slice(0, GENERATED_MAX);
    if (mode === 'listen-meaning') {
      const zhPool = Array.from(new Set(sentences.map((s) => String(s?.zh || '').trim()).filter(Boolean)));
      return pool.map((s) => {
        const correct = String(s.zh || '').trim();
        const distractors = shuffle(zhPool.filter((zh) => zh !== correct)).slice(0, 3);
        return {
          kind: 'listen-meaning',
          sentenceId: s.id,
          zh: correct,
          en: s.en,
          options: shuffle([correct, ...distractors]),
        };
      }).filter((item) => item.options.length >= 2);
    }
    return pool.map((s) => ({
      kind: mode,
      sentenceId: s.id,
      zh: s.zh,
      en: s.en,
    }));
  }, [mode, sentences]);

  const taskList = mode === 'mix' ? exercises : generated;
  const current = taskList[index];

  // 切换模式重新开始一轮（题量与来源都不一样）
  const switchMode = useCallback((next) => {
    setMode(next);
    setIndex(0);
    setCorrectCount(0);
    setFeedback(null);
    startAt.current = Date.now();
    finishedRef.current = false;
  }, []);
  const dictationGroups = useMemo(
    () => (current?.kind === 'dictation' ? buildDictationGroups(current.en) : []),
    [current],
  );

  // 切题时重置作答状态
  useEffect(() => {
    if (!current) return;
    setFeedback(null);
    setSelected([]);
    setTyped('');
    setDictationWords(dictationGroups.map(() => ''));
    if (current.kind === 'build' || current.kind === 'listen-build') {
      setPool(shuffle(tokenizeSentence(current.en || '')));
    } else {
      setPool([]);
    }
    // 听力题进入即播放，不用学员再点一次
    if (isListenKind(current.kind) && current.en) {
      speakEnglish(current.en);
    }
  }, [index, current, dictationGroups]);

  const finished = !loading && taskList.length > 0 && index >= taskList.length;

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

  const submit = useCallback((studentText, expected) => {
    if (!current) return;
    const target = expected || current.en;
    const ok = normalizeAnswer(studentText) === normalizeAnswer(target);
    setFeedback(ok ? 'ok' : 'no');
    if (ok) setCorrectCount((n) => n + 1);
    record(ok);
  }, [current, record]);

  const handleBuildCheck = () => {
    const answer = selected.map((i) => pool[i]).join(' ');
    submit(answer);
  };

  const dictationComplete = dictationWords.length > 0
    && dictationWords.every((word) => String(word || '').trim());
  const handleDictationCheck = () => {
    if (!dictationComplete) return;
    const expected = dictationGroups.map((group) => group.word).filter(Boolean).join(' ');
    const answer = dictationWords
      .map((word) => String(word || '').replace(/[^A-Za-z0-9'’-]/g, ''))
      .filter(Boolean)
      .join(' ');
    submit(answer, expected);
  };
  const handleFillCheck = () => submit(typed, current?.answer);

  const optionList = useMemo(() => {
    if (current?.kind === 'listen-meaning') {
      return Array.isArray(current.options) ? current.options : [];
    }
    if (current?.kind !== 'choice') return [];
    const opts = Array.isArray(current.options) ? current.options : [];
    return opts.length ? opts : [current.en];
  }, [current]);

  if (loading) {
    return <div className="eng-learn-wrap"><Spin /></div>;
  }

  if (taskList.length === 0) {
    return (
      <div className="eng-learn-wrap">
        <button type="button" className="eng-learn-back" onClick={onBack}>← 返回句式通关</button>
        <div className="eng-mode-tabs">
          {MODES.map((item) => (
            <button
              key={item.key}
              type="button"
              className={`eng-mode-tab${mode === item.key ? ' active' : ''}`}
              onClick={() => switchMode(item.key)}
            >
              {item.label}
            </button>
          ))}
        </div>
        <Empty
          description={sentences.length === 0 ? '该单元暂无句子，请老师先配置单元句子' : '当前题型暂无题目，换个题型试试'}
          image={Empty.PRESENTED_IMAGE_SIMPLE}
        />
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
            <div className="eng-done-stat"><b>{taskList.length}</b><span>题目总数</span></div>
          </div>
          <div className="eng-actions">
            <button type="button" className="eng-btn eng-btn-ghost" onClick={onBack}>返回句式通关</button>
          </div>
        </div>
      </div>
    );
  }

  const progress = Math.round((index / taskList.length) * 100);
  const kindLabel = KIND_LABELS[current?.kind] || '句子练习';
  const difficultyLabel = DIFFICULTY_LABELS[current?.difficulty || 1] || '基础';

  return (
    <div className="eng-learn-wrap">
      <button type="button" className="eng-learn-back" onClick={onBack}>← 返回句式通关</button>
      <div className="eng-mode-tabs">
        {MODES.map((item) => (
          <button
            key={item.key}
            type="button"
            className={`eng-mode-tab${mode === item.key ? ' active' : ''}`}
            onClick={() => switchMode(item.key)}
          >
            {item.label}
          </button>
        ))}
      </div>
      <div className="eng-progress-row">
        <div className="eng-progress-track"><span style={{ width: `${progress}%` }} /></div>
        <div className="eng-progress-text">{index + 1} / {taskList.length}</div>
      </div>

      <MascotBubble tone={feedback === 'ok' ? 'ok' : feedback === 'no' ? 'no' : 'idle'}>
        {!feedback && '读一读、连一连，做完我告诉你对不对～'}
        {feedback === 'ok' && '太棒了，完全正确！'}
        {feedback === 'no' && (
          <>
            <div>再想想～</div>
            <div>正确答案：{current?.en}</div>
            {isListenKind(current?.kind) && current?.zh && <div>句意：{current.zh}</div>}
          </>
        )}
      </MascotBubble>

      <div className="eng-card quiz-stage">
        <div className="eng-sentence-hint">
          <span className={`eng-difficulty-tag level-${current?.difficulty || 1}`}>{difficultyLabel}</span>
          {kindLabel}
          {isListenKind(current?.kind) ? '：听音频作答' : `：${current?.zh || '（无中文提示）'}`}
        </div>

        {/* 听力题：大按钮重播，不带任何英文文本 */}
        {isListenKind(current?.kind) && (
          <div className="eng-listen-box">
            <button
              type="button"
              className="eng-listen-btn"
              onClick={() => speakEnglish(current.en)}
              aria-label="播放英文音频"
            >
              🔊 点击听音频
            </button>
            <div className="eng-listen-tip">
              {current?.kind === 'listen-build' ? '听完后，按顺序点下面的单词拼出这句话' : '听完后，选出这句话的中文意思'}
            </div>
          </div>
        )}

        {/* 连词成句 / 听音组句：共用词块拼句交互 */}
        {(current?.kind === 'build' || current?.kind === 'listen-build') && (
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

        {/* 句子默写：一个单词一个透明下划线输入框 */}
        {current?.kind === 'dictation' && (
          <div style={{ margin: '4px 0 8px' }}>
            <div className="eng-dictation-fields">
              {dictationGroups.map((group, gi) => (
                <span className="eng-dictation-item" key={`${group.word}-${gi}`}>
                  {group.prefix && <span className="eng-dictation-punct">{group.prefix}</span>}
                  {group.word && (
                    <Input
                      ref={(node) => { dictationRefs.current[gi] = node; }}
                      className="eng-dictation-field"
                      variant="underlined"
                      value={dictationWords[gi] || ''}
                      disabled={feedback === 'ok'}
                      autoComplete="off"
                      spellCheck={false}
                      aria-label={`第 ${gi + 1} 个单词`}
                      onChange={(e) => {
                        const value = e.target.value.replace(/[^A-Za-z0-9'’-]/g, '');
                        setDictationWords((prev) => {
                          const next = [...prev];
                          next[gi] = value;
                          return next;
                        });
                      }}
                      onKeyDown={(e) => {
                        if (e.key === ' ' && dictationRefs.current[gi + 1]) {
                          e.preventDefault();
                          dictationRefs.current[gi + 1].focus();
                        }
                      }}
                      onPressEnter={handleDictationCheck}
                    />
                  )}
                  {group.suffix && <span className="eng-dictation-punct">{group.suffix}</span>}
                </span>
              ))}
            </div>
            {current?.hint && <div className="eng-mod-sub" style={{ marginTop: 6 }}>提示：{current.hint}</div>}
          </div>
        )}

        {/* 词汇填空 */}
        {current?.kind === 'fill' && (
          <div style={{ margin: '4px 0 8px' }}>
            {current?.stem && <div className="eng-fill-stem">{current.stem}</div>}
            <Input
              size="large"
              placeholder={current?.hint ? `提示：${current.hint}...` : '请填写缺失的单词'}
              value={typed}
              disabled={feedback === 'ok'}
              onChange={(e) => setTyped(e.target.value)}
              onPressEnter={handleFillCheck}
            />
            {current?.hint && <div className="eng-mod-sub" style={{ marginTop: 6 }}>首字母提示：{current.hint}</div>}
          </div>
        )}

        {/* 听力理解：听英文，选中文 */}
        {current?.kind === 'listen-meaning' && (
          <div className="eng-quiz-options" style={{ marginTop: 8 }}>
            {optionList.map((opt, oi) => {
              const isAnswer = opt === current.zh;
              const cls = ['eng-quiz-option'];
              if (feedback && isAnswer) cls.push('correct');
              return (
                <button
                  key={`${opt}-${oi}`}
                  type="button"
                  className={cls.join(' ')}
                  disabled={Boolean(feedback)}
                  onClick={() => submit(opt, current.zh)}
                >
                  {String.fromCharCode(65 + oi)}. {opt}
                </button>
              );
            })}
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
                  onClick={() => submit(opt, current.answer || current.en)}
                >
                  {String.fromCharCode(65 + oi)}. {opt}
                </button>
              );
            })}
          </div>
        )}

        <div className="eng-actions">
          {current?.en && (
            <button type="button" className="eng-btn eng-btn-sound" onClick={() => speakEnglish(current.en)}>
              朗读
            </button>
          )}
          {(current?.kind === 'build' || current?.kind === 'listen-build') && (
            <button type="button" className="eng-btn eng-btn-ghost" onClick={() => setSelected([])} disabled={feedback === 'ok'}>
              清空
            </button>
          )}
          {(current?.kind === 'build' || current?.kind === 'listen-build') && (
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
              disabled={!dictationComplete || feedback === 'ok'}
            >
              检查
            </button>
          )}
          {current?.kind === 'fill' && (
            <button
              type="button"
              className="eng-btn eng-btn-primary"
              onClick={handleFillCheck}
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
