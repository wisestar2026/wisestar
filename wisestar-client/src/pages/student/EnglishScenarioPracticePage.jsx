/**
 * EnglishScenarioPracticePage.jsx - 学员端英语「情景对话练习」
 *
 * 功能:
 *   1. 进入某单元的情景对话：读取管理端已固化的情景内容（按组合并展示，不触发实时 AI）
 *   2. 对话两名角色交替，目标单词在英文台词中挖空，学员逐空填写
 *   3. 交卷后逐空判分（复用 blankMatches 归一化比较），标注对错并给出正确答案与统计
 *   4. 结束后记录会话（type=scenario），可「重做本组」
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
import { getEnglishScenarios, recordEnglishSession } from '../../api/englishStudent';
import { blankMatches } from '../../utils/practiceHelpers';
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
  const [values, setValues] = useState({}); // key `si-li-bi` -> 输入值
  const [submitted, setSubmitted] = useState(false);
  const startAt = useRef(Date.now());

  const load = useCallback(() => {
    if (!unit) {
      setStep('empty');
      return;
    }
    setStep('loading');
    setSubmitted(false);
    setValues({});
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

  // 首次进页及 单元/版本/年级/册别 变化时生成（不落库，按需重新请求 AI）
  useEffect(() => {
    load();
  }, [load]);

  const blankTotal = useMemo(
    () => scenarios.reduce(
      (sum, s) => sum + s.lines.reduce((n, line) => n + splitLine(line.en).blankCount, 0),
      0,
    ),
    [scenarios],
  );

  const setValue = (key, v) => setValues((prev) => ({ ...prev, [key]: v }));

  const submittedBlanks = (() => {
    if (!submitted) return 0;
    let correct = 0;
    scenarios.forEach((s, si) => s.lines.forEach((line, li) => {
      const { blankCount } = splitLine(line.en);
      for (let bi = 0; bi < blankCount; bi++) {
        const expected = line.answers?.[bi] ?? '';
        if (blankMatches(expected, values[`${si}-${li}-${bi}`] || '')) correct += 1;
      }
    }));
    return correct;
  })();

  const handleSubmit = () => {
    if (submitted) return;
    setSubmitted(true);
    const durationSeconds = Math.round((Date.now() - startAt.current) / 1000);
    // 交卷后统计正确空位（本地即时判分）
    let correct = 0;
    scenarios.forEach((s, si) => s.lines.forEach((line, li) => {
      const { blankCount } = splitLine(line.en);
      for (let bi = 0; bi < blankCount; bi++) {
        const expected = line.answers?.[bi] ?? '';
        if (blankMatches(expected, values[`${si}-${li}-${bi}`] || '')) correct += 1;
      }
    }));
    recordEnglishSession({ type: 'scenario', durationSeconds, correctCount: correct }).catch(() => {});
  };

  const reset = () => {
    setSubmitted(false);
    setValues({});
  };

  if (step === 'loading') {
    return (
      <div className="eng-learn-wrap sc-wrap">
        <div className="sc-loading">
          <Spin size="large" />
          <div className="sc-loading-text">正在加载「{unit}」的情景对话…</div>
        </div>
      </div>
    );
  }

  if (step === 'empty') {
    return (
      <div className="eng-learn-wrap sc-wrap">
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

  const accuracy = blankTotal ? Math.round((submittedBlanks / blankTotal) * 100) : 0;

  return (
    <div className="eng-learn-wrap sc-wrap">
      <div className="sc-hero">
        <div className="sc-hero-title">情景对话 · {unit}</div>
        <div className="sc-hero-sub">把本单元单词填进对话里，看看你能不能完成整段交流</div>
      </div>

      {submitted && (
        <div className="sc-summary">
          <span className="sc-summary-score">{submittedBlanks}<small> / {blankTotal} 空正确</small></span>
          <span className="sc-summary-rate">正确率 {accuracy}%</span>
          <span className="sc-summary-hint">{accuracy >= 80 ? '很棒！' : accuracy >= 60 ? '不错，再巩固一下' : '把答错的单词再记一记吧'}</span>
        </div>
      )}

      {scenarios.map((scene, si) => (
        <div className="sc-scene" key={`${scene.title || 'scene'}-${si}`}>
          <div className="sc-scene-head">
            <span className="sc-scene-index">{si + 1}</span>
            <span className="sc-scene-title">{scene.title || `情景对话 ${si + 1}`}</span>
          </div>
          {scene.words?.length > 0 && (
            <div className="sc-words">
              {scene.words.map((w) => <span className="sc-word" key={w}>{w}</span>)}
            </div>
          )}
          <div className="sc-lines">
            {scene.lines.map((line, li) => {
              const { parts, blankCount } = splitLine(line.en);
              return (
                <div className={`sc-line ${line.speaker === 'B' ? 'is-b' : ''}`} key={`${si}-${li}`}>
                  <span className={`sc-speaker ${line.speaker === 'B' ? 'b' : ''}`}>{line.speaker || 'A'}</span>
                  <div className="sc-body">
                    <div className="sc-en">
                      {parts.map((part, bi) => {
                        const key = `${si}-${li}-${bi}`;
                        const expected = line.answers?.[bi] ?? '';
                        const val = values[key] || '';
                        const hit = submitted && blankMatches(expected, val);
                        return (
                          <Fragment key={bi}>
                            <span>{part}</span>
                            {bi < blankCount && (
                              <input
                                className={`sc-blank ${submitted ? (hit ? 'is-correct' : 'is-wrong') : ''}`}
                                value={val}
                                disabled={submitted}
                                autoComplete="off"
                                spellCheck={false}
                                placeholder="填单词"
                                onChange={(e) => setValue(key, e.target.value)}
                              />
                            )}
                          </Fragment>
                        );
                      })}
                    </div>
                    {line.zh && <div className="sc-zh">{line.zh}</div>}
                    {submitted && (
                      <div className="sc-answers">
                        {Array.from({ length: blankCount }).map((_, bi) => {
                          const expected = line.answers?.[bi] ?? '';
                          return (
                            <span className="sc-answer-chip" key={bi}>
                              空{bi + 1}：{expected || '（无）'}
                            </span>
                          );
                        })}
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      ))}

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
