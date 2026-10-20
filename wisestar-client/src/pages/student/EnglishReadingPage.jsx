/**
 * EnglishReadingPage.jsx - 学员端英语「阅读理解」
 *
 * 功能:
 *   1. 按当前版本/年级/册别/单元加载阅读理解（后端首次生成并缓存）
 *   2. 蓝色背景短文卡片，支持展开中文参考译文
 *   3. 5 道选择题逐题作答，提交后即时判分并展示解析
 *
 * URL: /student/english/reading?unit=xxx
 * 被谁引用: App.jsx 路由表；入口来自英语学习中心模块卡
 */

import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Spin, message } from 'antd';
import useStudentStore from '../../stores/useStudentStore';
import { getEnglishReading } from '../../api/englishStudent';
import './EnglishReadingPage.css';

function percent(correct, total) {
  return total ? Math.round((correct / total) * 100) : 0;
}

// 句末标点必须位于引号之外才断句，避免把一段直接引语拆成两个残句
function splitSentences(input) {
  const value = String(input || '').replace(/\s+/g, ' ').trim();
  const result = [];
  if (!value) return result;
  const isEnd = /[.!?]/;
  const isStarter = /[A-Z0-9"“]/;
  let start = 0;
  let inQuote = false;
  let pendingEnd = false;
  for (let i = 0; i < value.length; i += 1) {
    const ch = value[i];
    if (ch === '“') {
      inQuote = true;
      continue;
    }
    if (ch === '”') inQuote = false;
    else if (ch === '"') inQuote = !inQuote;
    if (!inQuote && pendingEnd) {
      let next = i + 1;
      while (next < value.length && /\s/.test(value[next])) next += 1;
      if (next >= value.length || isStarter.test(value[next])) {
        result.push(value.slice(start, i + 1).trim());
        start = next;
        i = next - 1;
      }
      pendingEnd = false;
      continue;
    }
    if (inQuote) {
      if (isEnd.test(ch)) pendingEnd = true;
      continue;
    }
    if (isEnd.test(ch)) {
      let end = i;
      while (end + 1 < value.length && /[.!?”")\]]/.test(value[end + 1])) end += 1;
      let next = end + 1;
      while (next < value.length && /\s/.test(value[next])) next += 1;
      if (next >= value.length || isStarter.test(value[next])) {
        result.push(value.slice(start, end + 1).trim());
        start = next;
        i = end;
      } else {
        i = end;
      }
    }
  }
  const tail = value.slice(start).trim();
  if (tail) result.push(tail);
  return result;
}

// 语义分段信号：转折、时间推进、场景切换优先成为新段开头
const PARAGRAPH_STRONG_CUES = [
  /^(But|However|Yet|Still|Instead|So|Therefore|As a result)\b/i,
  /^(Then|Next|After that|Afterwards|Later|Finally|At last|In the end|From then on|From now on)\b/i,
  /^(One (day|morning|afternoon|evening|night)|The next (day|morning|afternoon|evening)|That (day|morning|afternoon|evening))\b/i,
  /^(Today|Now|Yesterday|Tomorrow|Last (week|weekend|month|year|summer|winter|spring|autumn))\b/i,
  /^(Suddenly|Luckily|Unfortunately|To (his|her|their) surprise)\b/i,
  /^(At|In|On) (the |his |her |their )?(school|classroom|home|park|zoo|shop|library|kitchen|garden|farm|hospital|street|playground|morning|afternoon|evening)\b/i,
];
const PARAGRAPH_SOFT_CUES = [/^(After|Before|When|As soon as|While)\b/i];
const PARAGRAPH_STOP_WORDS = new Set(
  'the a an and or but so to of in on at for with from by as is are was were be been being it its he she they them their his her our we you i my me us this that these those there here not no do does did can could will would should may might must have has had'
    .split(' '),
);

function contentWords(sentence) {
  const words = String(sentence || '').toLowerCase().match(/[a-z']+/g) || [];
  return new Set(words.filter((word) => word.length > 2 && !PARAGRAPH_STOP_WORDS.has(word)));
}

function boundaryScore(sentences, index) {
  const current = sentences[index];
  let score = 0;
  if (PARAGRAPH_STRONG_CUES.some((cue) => cue.test(current))) score += 3;
  if (PARAGRAPH_SOFT_CUES.some((cue) => cue.test(current))) score += 1.2;
  const previousWords = contentWords(sentences[index - 1]);
  const currentWords = contentWords(current);
  let shared = 0;
  previousWords.forEach((word) => {
    if (currentWords.has(word)) shared += 1;
  });
  const total = previousWords.size + currentWords.size - shared;
  const overlap = total ? shared / total : 0;
  if (overlap < 0.05) score += 0.8;
  else if (overlap > 0.2) score -= 0.4;
  if (/^(He|She|It|They|I|We|You|His|Her|Their|My|Our|This|That|These|Those)\b/i.test(current)) {
    score -= 0.8;
  }
  if (/^["“]/.test(current)) score += 0.4;
  return score;
}

// 以语义边界得分分段，每段 2~4 句，避免按固定句数机械切分
function groupSentences(sentences) {
  const total = sentences.length;
  if (total <= 4) return [sentences.join(' ')];
  const scores = new Array(total + 1).fill(-Infinity);
  const previous = new Array(total + 1).fill(0);
  scores[0] = 0;
  for (let start = 0; start < total; start += 1) {
    if (!Number.isFinite(scores[start])) continue;
    for (let length = 2; length <= 4; length += 1) {
      const end = start + length;
      if (end > total || total - end === 1) continue;
      let score = scores[start] - Math.abs(length - 3) * 0.35;
      if (end < total) score += boundaryScore(sentences, end);
      if (score > scores[end]) {
        scores[end] = score;
        previous[end] = start;
      }
    }
  }
  if (!Number.isFinite(scores[total])) return [sentences.join(' ')];
  const ranges = [];
  let cursor = total;
  while (cursor > 0) {
    ranges.push([previous[cursor], cursor]);
    cursor = previous[cursor];
  }
  return ranges.reverse().map(([start, end]) => sentences.slice(start, end).join(' '));
}

function buildParagraphs(passage) {
  const raw = String(passage || '').replace(/\r\n?/g, '\n').trim();
  if (!raw) return [];
  const blocks = raw
    .split(/\n\s*\n/)
    .map((block) => block.replace(/\s+/g, ' ').trim())
    .filter(Boolean);
  if (blocks.length > 1) return blocks;
  // 原文自带换行时保留作者段落；单行文本再按语义自动分段
  const lines = raw.split(/\n+/).map((line) => line.trim()).filter(Boolean);
  if (lines.length > 1) return lines;
  return groupSentences(splitSentences(lines[0]));
}

export default function EnglishReadingPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const unit = searchParams.get('unit') || '';
  const version = useStudentStore((s) => s.version);
  const grade = useStudentStore((s) => s.grade);
  const term = useStudentStore((s) => s.term);

  const [readings, setReadings] = useState([]);
  const [activeIndex, setActiveIndex] = useState(0);
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState('');
  const [answers, setAnswers] = useState({});
  const [submitted, setSubmitted] = useState(false);
  const [showZh, setShowZh] = useState(false);
  const wrapRef = useRef(null);

  // 用实际可用高度约束整页：顶部通栏和底部导航留白之外全部给两栏容器，
  // 这样页面本身不滚动，短文和题目各自在容器内滚动。
  const measureHeight = useCallback(() => {
    const el = wrapRef.current;
    const parent = el?.parentElement;
    if (!el || !parent) return;
    const top = el.getBoundingClientRect().top + window.scrollY;
    const bottomPadding = parseFloat(getComputedStyle(parent).paddingBottom) || 0;
    const available = Math.floor(window.innerHeight - top - bottomPadding);
    if (available > 0) el.style.height = `${available}px`;
  }, []);

  useLayoutEffect(() => {
    measureHeight();
  });

  useEffect(() => {
    window.addEventListener('resize', measureHeight);
    let observer;
    const header = wrapRef.current?.closest('.sll-page')?.querySelector('.sll-header');
    if (header && typeof ResizeObserver !== 'undefined') {
      observer = new ResizeObserver(measureHeight);
      observer.observe(header);
    }
    return () => {
      window.removeEventListener('resize', measureHeight);
      observer?.disconnect();
    };
  }, [measureHeight]);

  const load = useCallback(() => {
    if (!unit) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setFailed('');
    setAnswers({});
    setSubmitted(false);
    setShowZh(false);
    getEnglishReading({ version, grade, term, unit })
      .then((res) => {
        const list = Array.isArray(res?.data) ? res.data : (res?.data ? [res.data] : []);
        setReadings(list);
        setActiveIndex(0);
      })
      .catch((e) => setFailed(e?.message || '阅读内容加载失败'))
      .finally(() => setLoading(false));
  }, [version, grade, term, unit]);

  useEffect(() => {
    load();
  }, [load]);

  const reading = readings[activeIndex] || null;
  const questions = reading?.questions || [];

  const switchArticle = (index) => {
    setActiveIndex(index);
    setAnswers({});
    setSubmitted(false);
    setShowZh(false);
  };
  const correctCount = questions.reduce(
    (sum, q, index) => sum + (answers[index] === q.answerIndex ? 1 : 0), 0,
  );

  const submit = () => {
    if (Object.keys(answers).length < questions.length) {
      message.warning('请完成全部题目后再提交');
      return;
    }
    setSubmitted(true);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  if (loading) {
    return (
      <div className="eng-read-wrap" ref={wrapRef}>
        <div className="eng-read-loading">
          <Spin size="large" />
          <div className="eng-read-loading-text">正在准备本单元阅读短文…</div>
        </div>
      </div>
    );
  }

  if (!unit || failed || !reading) {
    return (
      <div className="eng-read-wrap" ref={wrapRef}>
        <div className="eng-read-empty">
          <div className="eng-read-empty-emoji">📖</div>
          <div className="eng-read-empty-title">{!unit ? '未选择单元' : failed || '暂无阅读内容'}</div>
          {failed && <button type="button" className="eng-read-btn" onClick={load}>重新加载</button>}
          <button type="button" className="eng-read-btn ghost" onClick={() => navigate('/student/english')}>
            返回英语学习中心
          </button>
        </div>
      </div>
    );
  }

  const paragraphs = buildParagraphs(reading.passage);

  return (
    <div className="eng-read-wrap" ref={wrapRef}>
      <div className="eng-read-head">
        <button type="button" className="eng-read-back" onClick={() => navigate('/student/english')}>← 返回学习中心</button>
        <div>
          <div className="eng-read-unit">{unit} · 阅读理解</div>
          <div className="eng-read-title">{reading.title || 'Read and answer'}</div>
        </div>
        {submitted && (
          <div className="eng-read-score">
            {correctCount}<small>/{questions.length} 题</small>
            <span>{percent(correctCount, questions.length)} 分</span>
          </div>
        )}
      </div>

      {readings.length > 1 && (
        <div className="eng-read-article-tabs">
          {readings.map((item, index) => (
            <button
              type="button"
              key={item.title || index}
              className={`eng-read-article-tab ${index === activeIndex ? 'active' : ''}`}
              onClick={() => switchArticle(index)}
            >
              {index + 1}. {item.title || `第 ${index + 1} 篇`}
            </button>
          ))}
        </div>
      )}

      <div className="eng-read-main">
        <article className="eng-read-passage">
          <div className="eng-read-passage-top">
            <span className="eng-read-passage-tag">Read</span>
            {submitted && reading.passageZh && (
              <button type="button" className="eng-read-zh-toggle" onClick={() => setShowZh((v) => !v)}>
                {showZh ? '隐藏译文' : '查看译文'}
              </button>
            )}
          </div>
          {paragraphs.map((p, index) => (
            <p className="eng-read-paragraph" key={index}>{p}</p>
          ))}
          {submitted && showZh && reading.passageZh && (
            <div className="eng-read-translation">{reading.passageZh}</div>
          )}
        </article>

        <div className="eng-read-questions">
          {questions.map((q, qi) => (
            <div className="eng-read-question" key={`${q.stem}-${qi}`}>
              <div className="eng-read-question-head">
                <span className="eng-read-question-no">{qi + 1}</span>
                <span className="eng-read-question-stem">{q.stem}</span>
              </div>
              <div className="eng-read-options">
                {(q.options || []).map((option, oi) => {
                  const picked = answers[qi] === oi;
                  let cls = 'eng-read-option';
                  if (submitted && oi === q.answerIndex) cls += ' correct';
                  else if (submitted && picked) cls += ' wrong';
                  else if (picked) cls += ' picked';
                  return (
                    <button
                      key={`${option}-${oi}`}
                      type="button"
                      className={cls}
                      disabled={submitted}
                      onClick={() => setAnswers((prev) => ({ ...prev, [qi]: oi }))}
                    >
                      <span className="eng-read-option-label">{String.fromCharCode(65 + oi)}</span>
                      <span>{option}</span>
                    </button>
                  );
                })}
              </div>
              {submitted && (
                <div className={`eng-read-explain ${answers[qi] === q.answerIndex ? 'ok' : 'bad'}`}>
                  {answers[qi] === q.answerIndex ? '回答正确' : `正确答案：${String.fromCharCode(65 + q.answerIndex)}`}
                  {q.explanation ? ` · ${q.explanation}` : ''}
                </div>
              )}
            </div>
          ))}
        </div>
      </div>

      <div className="eng-read-actions">
        {!submitted ? (
          <button type="button" className="eng-read-btn" onClick={submit}>提交答案</button>
        ) : (
          <button type="button" className="eng-read-btn" onClick={() => { setAnswers({}); setSubmitted(false); setShowZh(false); window.scrollTo({ top: 0, behavior: 'smooth' }); }}>
            再做一次
          </button>
        )}
        <button type="button" className="eng-read-btn ghost" onClick={() => navigate('/student/english')}>返回学习中心</button>
      </div>
    </div>
  );
}
