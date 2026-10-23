/**
 * EnglishScenarioPracticePage.jsx - 学员端英语「情景对话练习」
 *
 * 功能:
 *   1. 进入某单元的情景对话：读取管理端已固化的情景内容（按组合并展示，不触发实时 AI）
 *   2. 整段情景以故事方式完整呈现，任意时刻只有一个空处于作答状态：
 *      - 语境行整句纯文本
 *      - 未轮到的考查行显示灰字答案占位
 *      - 当前空显示唯一输入框，回车提交后静默跳到下一个空
 *   3. 每段结束展示本段小结（答对 X/Y + 错词与中文语境），全部结束再给总览
 *   4. 全部完成后记录会话（type=scenario）并纳入「单元·单词」掌握度
 *
 * URL: /student/english/scenario?unit=xxx
 * 被谁引用: App.jsx 路由表；入口来自英语学习中心单元卡片「场景对话」
 *
 * 数据流:
 *   GET  /api/english/student/scenarios → 读取该单元固化情景对话（含答案，用于前端即时判分）
 *   POST /api/english/student/session   → 记录会话
 */

import { Fragment, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Spin, message } from 'antd';
import useStudentStore from '../../stores/useStudentStore';
import { getEnglishScenarios, recordEnglishScenarioPractice, recordEnglishSession } from '../../api/englishStudent';
import { blankMatches, normalizeBlankText } from '../../utils/practiceHelpers';
import './EnglishCenterPage.css';
import './EnglishScenarioPracticePage.css';

const BLANK_SPLIT = /_{2,}/;

/** 拆分英文台词为「文本片段 + 空位」结构 */
function splitLine(en) {
  const parts = String(en || '').split(BLANK_SPLIT);
  return { parts, blankCount: Math.max(0, parts.length - 1) };
}

export default function EnglishScenarioPracticePage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const unit = searchParams.get('unit') || '';
  const version = useStudentStore((s) => s.version);
  const grade = useStudentStore((s) => s.grade);
  const term = useStudentStore((s) => s.term);

  const [step, setStep] = useState('loading'); // loading | ready | empty
  const [scenarios, setScenarios] = useState([]);
  const [page, setPage] = useState(0); // 当前情景页（每项情景一页）
  const [answers, setAnswers] = useState({}); // key `si-li-bi` -> { value, ok }
  const [activeKey, setActiveKey] = useState(null); // 当前唯一可作答的空
  const [draft, setDraft] = useState('');
  const startAt = useRef(Date.now());
  const recordedRef = useRef(false);
  const inputRef = useRef(null);
  const answersRef = useRef({});
  const summaryRef = useRef(null);

  const load = useCallback(() => {
    if (!unit) {
      setStep('empty');
      return;
    }
    setStep('loading');
    setPage(0);
    setAnswers({});
    setActiveKey(null);
    setDraft('');
    recordedRef.current = false;
    getEnglishScenarios({ version, grade, term, unit })
      .then((res) => {
        const list = res?.data || [];
        setScenarios(list);
        setStep(list.length ? 'ready' : 'empty');
        startAt.current = Date.now();
      })
      .catch((e) => {
        message.error(e?.message || '情景对话加载失败，请稍后重试');
        setStep('empty');
      });
  }, [version, grade, term, unit]);

  useEffect(() => {
    load();
  }, [load]);

  /** 全部情景的空位清单（按作答顺序） */
  const sceneBlanks = useMemo(() => scenarios.map((scene, si) => {
    const list = [];
    scene.lines.forEach((line, li) => {
      const { blankCount } = splitLine(line.en);
      for (let bi = 0; bi < blankCount; bi += 1) {
        list.push({
          key: `${si}-${li}-${bi}`,
          expected: line.answers?.[bi] ?? '',
          lineIndex: li,
          blankIndex: bi,
          zh: line.zh || '',
        });
      }
    });
    return list;
  }), [scenarios]);

  const totalBlanks = useMemo(
    () => sceneBlanks.reduce((sum, list) => sum + list.length, 0),
    [sceneBlanks],
  );

  const sceneStats = useCallback((si) => {
    const list = sceneBlanks[si] || [];
    const answered = list.filter((item) => answers[item.key]);
    const correct = answered.filter((item) => answers[item.key].ok).length;
    return {
      total: list.length,
      answered: answered.length,
      correct,
      wrongs: answered.filter((item) => !answers[item.key].ok),
      done: list.length === 0 || answered.length === list.length,
    };
  }, [sceneBlanks, answers]);

  useEffect(() => {
    answersRef.current = answers;
  }, [answers]);

  const answeredTotal = useMemo(
    () => Object.keys(answers).length,
    [answers],
  );
  const correctTotal = useMemo(
    () => Object.values(answers).filter((a) => a.ok).length,
    [answers],
  );

  const allDone = scenarios.length > 0
    && scenarios.every((_, si) => sceneStats(si).done);

  // 进入情景页后自动聚焦到第一个待作答空
  useEffect(() => {
    if (step !== 'ready' || !scenarios.length) return;
    const list = sceneBlanks[page] || [];
    const first = list.find((item) => !answersRef.current[item.key]);
    setActiveKey(first?.key || null);
    setDraft('');
  }, [step, page, scenarios.length, sceneBlanks]);

  // 切换空位后聚焦输入框
  useEffect(() => {
    if (activeKey) inputRef.current?.focus();
  }, [activeKey]);

  // 全部完成时统一记录一次会话与掌握度
  useEffect(() => {
    if (!allDone || recordedRef.current) return;
    recordedRef.current = true;
    const durationSeconds = Math.round((Date.now() - startAt.current) / 1000);
    recordEnglishSession({ type: 'scenario', durationSeconds, correctCount: correctTotal }).catch(() => {});
    if (totalBlanks > 0) {
      recordEnglishScenarioPractice({
        version, grade, term, unit, module: '单词', correct: correctTotal, total: totalBlanks,
      }).catch(() => {});
    }
  }, [allDone, correctTotal, totalBlanks, version, grade, term, unit]);

  const goPage = (next) => {
    if (next < 0 || next >= scenarios.length) return;
    setPage(next);
  };

  const submitBlank = () => {
    if (!activeKey) return;
    const value = draft.trim();
    if (!value) return;
    const list = sceneBlanks[page] || [];
    const currentIndex = list.findIndex((item) => item.key === activeKey);
    if (currentIndex < 0) return;
    const item = list[currentIndex];
    // 情景对话按短语整串判分：忽略大小写、首尾与多余空格，支持 @ 分隔的多种写法。
    const normalized = normalizeBlankText(value).toLowerCase();
    const ok = blankMatches(item.expected, value)
      || String(item.expected || '').split('@')
        .some((alt) => normalizeBlankText(alt).toLowerCase() === normalized);
    const nextAnswers = { ...answers, [item.key]: { value, ok } };
    setAnswers(nextAnswers);
    setDraft('');
    const next = list.slice(currentIndex + 1).find((b) => !nextAnswers[b.key])
      || list.find((b) => !nextAnswers[b.key]);
    setActiveKey(next?.key || null);
  };

  const resetScene = (si) => {
    const keys = new Set((sceneBlanks[si] || []).map((item) => item.key));
    const nextAnswers = { ...answers };
    keys.forEach((key) => { delete nextAnswers[key]; });
    setAnswers(nextAnswers);
    recordedRef.current = false;
    if (si === page) {
      const first = sceneBlanks[si]?.[0];
      setActiveKey(first?.key || null);
      setDraft('');
    }
  };

  const resetAll = () => {
    setAnswers({});
    setPage(0);
    setDraft('');
    recordedRef.current = false;
    const first = sceneBlanks[0]?.[0];
    setActiveKey(first?.key || null);
    startAt.current = Date.now();
  };

  if (step === 'loading') {
    return (
      <div className="eng-learn-wrap sc-wrap eng-stage">
        <div className="sc-loading">
          <Spin size="large" />
          <div className="sc-loading-text">正在加载「{unit}」的情景对话…</div>
        </div>
      </div>
    );
  }

  if (step === 'empty') {
    return (
      <div className="eng-learn-wrap sc-wrap eng-stage">
        <div className="eng-empty">
          <div className="eng-empty-emoji">🐚</div>
          <div className="eng-empty-title">{unit ? '该单元练习尚未生成' : '未选择单元'}</div>
          <div className="sc-empty-hint">请联系老师生成该单元情景练习后再来练习。</div>
          <div className="eng-actions">
            <button type="button" className="eng-btn eng-btn-primary" onClick={() => navigate('/student/english')}>
              返回学习中心
            </button>
          </div>
        </div>
      </div>
    );
  }

  const currentScene = scenarios[page];
  const currentList = sceneBlanks[page] || [];
  const currentStats = sceneStats(page);
  const sceneFinished = currentStats.done;
  const progress = totalBlanks ? Math.round((answeredTotal / totalBlanks) * 100) : 0;

  return (
    <div className="eng-learn-wrap sc-wrap eng-stage">
      <div className="sc-hero">
        <div className="sc-hero-title">情景对话 · {unit}</div>
        <div className="sc-hero-sub">顺着整段故事往下走，一次只填一个空，回车就能进入下一处</div>
      </div>

      <div className="sc-progress">
        <div className="sc-progress-track"><span style={{ width: `${progress}%` }} /></div>
        <div className="sc-progress-text">{answeredTotal} / {totalBlanks} 空</div>
      </div>

      {currentScene && (
        <div className="sc-scene" key={`${currentScene.title || 'scene'}-${page}`}>
          <div className="sc-scene-head">
            <span className="sc-scene-index">{page + 1}</span>
            <span className="sc-scene-title">{currentScene.title || `情景对话 ${page + 1}`}</span>
            <span className="sc-scene-count">{currentStats.correct} / {currentStats.total} 空正确</span>
          </div>
          {currentScene.words?.length > 0 && (
            <div className="sc-words">
              {currentScene.words.map((w) => <span className="sc-word" key={w}>{w}</span>)}
            </div>
          )}

          <div className="sc-lines">
            {currentScene.lines.map((line, li) => {
              const { parts, blankCount } = splitLine(line.en);
              const lineKeys = currentList
                .filter((item) => item.lineIndex === li)
                .map((item) => item.key);
              const lineActive = lineKeys.includes(activeKey);
              return (
                <div
                  className={`sc-line ${line.speaker === 'B' ? 'is-b' : ''} ${lineActive ? 'is-active' : ''}`}
                  key={`${page}-${li}`}
                >
                  <span className={`sc-speaker ${line.speaker === 'B' ? 'b' : ''}`}>{line.speaker || 'A'}</span>
                  <div className="sc-body">
                    <div className="sc-en">
                      {parts.map((part, bi) => {
                        const key = `${page}-${li}-${bi}`;
                        const expected = line.answers?.[bi] ?? '';
                        const judged = answers[key];
                        const isActive = key === activeKey;
                        return (
                          <Fragment key={bi}>
                            <span>{part}</span>
                            {bi < blankCount && (
                              isActive ? (
                                <input
                                  ref={inputRef}
                                  className="sc-blank"
                                  value={draft}
                                  autoComplete="off"
                                  spellCheck={false}
                                  placeholder="填单词"
                                  onChange={(e) => setDraft(e.target.value)}
                                  onKeyDown={(e) => {
                                    if (e.key === 'Enter') {
                                      e.preventDefault();
                                      submitBlank();
                                    }
                                  }}
                                />
                              ) : judged ? (
                                <span className={`sc-blank-static ${judged.ok ? 'is-correct' : 'is-wrong'}`}>
                                  {judged.value || '—'}
                                  {judged.ok ? <b className="sc-mark">✓</b> : (
                                    <>
                                      <b className="sc-mark">✗</b>
                                      <em className="sc-correct">→ {expected}</em>
                                    </>
                                  )}
                                </span>
                              ) : (
                                <span className="sc-blank-static is-pending">_____</span>
                              )
                            )}
                          </Fragment>
                        );
                      })}
                    </div>
                    {line.zh && <div className="sc-zh">{line.zh}</div>}
                  </div>
                </div>
              );
            })}
          </div>

          {!currentStats.done && currentStats.total > 0 && (
            <div className="sc-focus-hint">
              正在作答第 {currentStats.answered + 1} 个空，回车提交后自动进入下一个
            </div>
          )}

          {sceneFinished && (
            <div className="sc-scene-done">
              <div className="sc-scene-done-title">
                {currentStats.correct === currentStats.total ? '这一段全对，很稳！' : '这一段完成啦'}
              </div>
              <div className="sc-scene-done-score">
                答对 <b>{currentStats.correct}</b> / {currentStats.total} 空
              </div>
              {currentStats.wrongs.length > 0 && (
                <div className="sc-wrongs">
                  <div className="sc-wrongs-title">需要再看一眼的词</div>
                  {currentStats.wrongs.map((item) => (
                    <div className="sc-wrong-item" key={item.key}>
                      <span className="sc-wrong-word">{item.expected}</span>
                      {item.zh && <span className="sc-wrong-zh">{item.zh}</span>}
                    </div>
                  ))}
                </div>
              )}
              <div className="sc-scene-actions">
                <button type="button" className="sc-pager-btn" onClick={() => resetScene(page)}>
                  再练这一段
                </button>
                {page < scenarios.length - 1 ? (
                  <button type="button" className="sc-pager-btn is-primary" onClick={() => goPage(page + 1)}>
                    下一个情景
                  </button>
                ) : (
                  <button
                    type="button"
                    className="sc-pager-btn is-primary"
                    onClick={() => summaryRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' })}
                  >
                    完成，查看总览
                  </button>
                )}
              </div>
            </div>
          )}
        </div>
      )}

      {allDone && (
        <div className="sc-summary" ref={summaryRef}>
          <span className="sc-summary-score">
            {correctTotal}<small> / {totalBlanks} 空正确</small>
          </span>
          <span className="sc-summary-rate">
            正确率 {totalBlanks ? Math.round((correctTotal / totalBlanks) * 100) : 0}%
          </span>
          <span className="sc-summary-hint">
            {correctTotal === totalBlanks ? '整段对话都拿下了！' : '把标红的词再读一遍，整段就更顺了'}
          </span>
        </div>
      )}

      {scenarios.length > 1 && (
        <div className="sc-pager">
          <button
            type="button"
            className="sc-pager-btn"
            disabled={page === 0}
            onClick={() => goPage(page - 1)}
          >
            上一段
          </button>
          <span className="sc-pager-info">
            <b>{page + 1}</b> / {scenarios.length}
          </span>
          <button
            type="button"
            className="sc-pager-btn"
            disabled={page === scenarios.length - 1 || !sceneFinished}
            onClick={() => goPage(page + 1)}
          >
            下一段
          </button>
        </div>
      )}

      <div className="eng-actions">
        <button type="button" className="eng-btn eng-btn-ghost" onClick={resetAll} disabled={answeredTotal === 0}>
          重做全部
        </button>
        {unit && (
          <button
            type="button"
            className="eng-btn eng-btn-again"
            onClick={() => navigate(`/student/english/practice?unit=${encodeURIComponent(unit)}`)}
          >
            回单词练习巩固
          </button>
        )}
        <button type="button" className="eng-btn eng-btn-primary" onClick={() => navigate('/student/english')}>
          返回学习中心
        </button>
      </div>
    </div>
  );
}
