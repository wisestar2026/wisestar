/**
 * EnglishPatternScenarioPage.jsx - 学员端英语「重点句式情景练习」
 *
 * 功能:
 *   1. 进入某单元的重点句式情景：读取管理端已固化的情景内容（按组合并展示，不触发实时 AI）
 *   2. 情景中给出部分英文台词，若干轮次仅给中文情景提示，学员从 4 个候选项中选出正确句式
 *   3. 交卷后逐轮判分（比对所选下标），标注对错并给出解析与统计
 *   4. 结束后记录会话（type=pattern），可「重做本组」
 *
 * URL: /student/english/pattern-scenario?unit=xxx
 * 被谁引用: App.jsx 路由表；入口来自英语学习中心单元卡片「句式情景」
 *
 * 数据流:
 *   GET  /api/english/student/pattern-scenarios → 读取该单元固化句式情景（含答案下标，用于前端即时判分）
 *   POST /api/english/student/session           → 记录会话
 */

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Spin, message } from 'antd';
import useStudentStore from '../../stores/useStudentStore';
import { getEnglishPatternScenarios, recordEnglishSession } from '../../api/englishStudent';
import './EnglishCenterPage.css';
import './EnglishScenarioPracticePage.css';
import './EnglishPatternScenarioPage.css';

const OPTION_LABELS = ['A', 'B', 'C', 'D', 'E', 'F'];

/** 一轮是否为作答轮（answerIndex >= 0） */
function isRespondTurn(turn) {
  return typeof turn?.answerIndex === 'number' && turn.answerIndex >= 0;
}

export default function EnglishPatternScenarioPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const unit = searchParams.get('unit') || '';
  const version = useStudentStore((s) => s.version);
  const grade = useStudentStore((s) => s.grade);
  const term = useStudentStore((s) => s.term);

  const [step, setStep] = useState('loading'); // loading | ready | empty
  const [scenarios, setScenarios] = useState([]);
  const [page, setPage] = useState(0); // 当前情景页（每项情景一页）
  const [picked, setPicked] = useState({}); // key `si-ti` -> 所选下标
  const [submitted, setSubmitted] = useState(false);
  const startAt = useRef(Date.now());

  const load = useCallback(() => {
    if (!unit) {
      setStep('empty');
      return;
    }
    setStep('loading');
    setSubmitted(false);
    setPage(0);
    setPicked({});
    getEnglishPatternScenarios({ version, grade, term, unit })
      .then((res) => {
        const list = res?.data || [];
        setScenarios(list);
        setStep(list.length ? 'ready' : 'empty');
        startAt.current = Date.now();
      })
      .catch((e) => {
        message.error(e?.message || '句式情景加载失败，请稍后重试');
        setStep('empty');
      });
  }, [version, grade, term, unit]);

  useEffect(() => {
    load();
  }, [load]);

  // 全部作答轮（用于统计总数与判分）
  const respondKeys = useMemo(() => {
    const keys = [];
    scenarios.forEach((s, si) => s.turns.forEach((turn, ti) => {
      if (isRespondTurn(turn)) keys.push({ si, ti, answerIndex: turn.answerIndex });
    }));
    return keys;
  }, [scenarios]);

  const totalCount = respondKeys.length;

  const countCorrect = useCallback((source) => (
    respondKeys.reduce(
      (n, k) => n + (source[`${k.si}-${k.ti}`] === k.answerIndex ? 1 : 0),
      0,
    )
  ), [respondKeys]);

  const setPick = (key, idx) => {
    if (submitted) return;
    setPicked((prev) => ({ ...prev, [key]: idx }));
  };

  const handleSubmit = () => {
    if (submitted) return;
    setSubmitted(true);
    const durationSeconds = Math.round((Date.now() - startAt.current) / 1000);
    const correct = countCorrect(picked);
    recordEnglishSession({ type: 'pattern', durationSeconds, correctCount: correct }).catch(() => {});
  };

  const reset = () => {
    setSubmitted(false);
    setPage(0);
    setPicked({});
  };

  if (step === 'loading') {
    return (
      <div className="eng-learn-wrap sc-wrap eng-stage">
        <div className="sc-loading">
          <Spin size="large" />
          <div className="sc-loading-text">正在加载「{unit}」的重点句式情景…</div>
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

  const accuracy = totalCount ? Math.round((countCorrect(picked) / totalCount) * 100) : 0;

  return (
    <div className="eng-learn-wrap sc-wrap eng-stage">
      <div className="sc-hero">
        <div className="sc-hero-title">重点句式情景 · {unit}</div>
        <div className="sc-hero-sub">读一读情景，从每题的句子里选出最符合这一情景的重点句式</div>
      </div>

      {submitted && (
        <div className="sc-summary">
          <span className="sc-summary-score">{countCorrect(picked)}<small> / {totalCount} 题正确</small></span>
          <span className="sc-summary-rate">正确率 {accuracy}%</span>
          <span className="sc-summary-hint">{accuracy >= 80 ? '很棒！' : accuracy >= 60 ? '不错，再巩固一下' : '把答错的句式读一读、记一记吧'}</span>
        </div>
      )}

      {scenarios.map((scene, si) => (si !== page ? null : (
        <div className="sc-scene" key={`${scene.title || 'scene'}-${si}`}>
          <div className="sc-scene-head">
            <span className="sc-scene-index">{si + 1}</span>
            <span className="sc-scene-title">{scene.title || `情景 ${si + 1}`}</span>
          </div>
          {scene.context && <div className="ps-context">{scene.context}</div>}
          {scene.patterns?.length > 0 && (
            <div className="sc-words">
              {scene.patterns.map((p) => <span className="sc-word" key={p}>{p}</span>)}
            </div>
          )}
          <div className="sc-lines">
            {scene.turns.map((turn, ti) => {
              const respond = isRespondTurn(turn);
              const key = `${si}-${ti}`;
              const isB = turn.speaker === 'B';
              if (!respond) {
                return (
                  <div className={`sc-line${isB ? ' is-b' : ''}`} key={key}>
                    <span className={`sc-speaker ${isB ? 'b' : ''}`}>{turn.speaker || 'A'}</span>
                    <div className="sc-body">
                      <div className="sc-en">{turn.en}</div>
                      {turn.zh && <div className="sc-zh">{turn.zh}</div>}
                    </div>
                  </div>
                );
              }
              const chosen = picked[key];
              return (
                <div className={`sc-line ps-ask${isB ? ' is-b' : ''}`} key={key}>
                  <span className={`sc-speaker ${isB ? 'b' : ''}`}>{turn.speaker || 'A'}</span>
                  <div className="sc-body">
                    <div className="ps-cue">
                      <span className="ps-cue-tag">说</span>
                      {turn.cue || turn.zh || '请选择符合情景的句子'}
                    </div>
                    <div className="ps-options">
                      {(turn.options || []).map((opt, oi) => {
                        const isAnswer = oi === turn.answerIndex;
                        const isPicked = chosen === oi;
                        let cls = 'ps-option';
                        if (submitted) {
                          if (isAnswer) cls += ' is-correct';
                          else if (isPicked) cls += ' is-wrong';
                        } else if (isPicked) {
                          cls += ' is-picked';
                        }
                        return (
                          <button
                            type="button"
                            key={opt}
                            className={cls}
                            disabled={submitted}
                            onClick={() => setPick(key, oi)}
                          >
                            <span className="ps-option-label">{OPTION_LABELS[oi] || oi + 1}</span>
                            <span className="ps-option-text">{opt}</span>
                          </button>
                        );
                      })}
                    </div>
                    {submitted && turn.explanation && (
                      <div className="ps-explain">{turn.explanation}</div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )))}

      {scenarios.length > 1 && (
        <div className="sc-pager">
          <button
            type="button"
            className="sc-pager-btn"
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
          >
            上一页
          </button>
          <span className="sc-pager-info">
            <b>{page + 1}</b> / {scenarios.length}
          </span>
          <button
            type="button"
            className="sc-pager-btn"
            disabled={page === scenarios.length - 1}
            onClick={() => setPage((p) => Math.min(scenarios.length - 1, p + 1))}
          >
            下一页
          </button>
        </div>
      )}

      <div className="eng-actions">
        <button type="button" className="eng-btn eng-btn-ghost" onClick={reset} disabled={!submitted}>
          重做本组
        </button>
        {!submitted ? (
          <button type="button" className="eng-btn eng-btn-primary" onClick={handleSubmit}>
            交卷判分
          </button>
        ) : (
          <button type="button" className="eng-btn eng-btn-primary" onClick={() => navigate('/student/english')}>
            返回学习中心
          </button>
        )}
      </div>
    </div>
  );
}
