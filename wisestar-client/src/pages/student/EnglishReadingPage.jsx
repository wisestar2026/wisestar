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

import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Spin, message } from 'antd';
import useStudentStore from '../../stores/useStudentStore';
import { getEnglishReading } from '../../api/englishStudent';
import './EnglishReadingPage.css';

function percent(correct, total) {
  return total ? Math.round((correct / total) * 100) : 0;
}

// 短文按句换行返回；把连续句子合并成 2~4 个自然段，避免每句一段过于零碎
function buildParagraphs(passage) {
  const blocks = String(passage || '')
    .split(/\n{2,}/)
    .map((block) => block.trim())
    .filter(Boolean);
  if (blocks.length > 1) {
    return blocks.map((block) => block.replace(/\n+/g, ' '));
  }
  const lines = (blocks[0] || '').split(/\n+/).map((line) => line.trim()).filter(Boolean);
  if (lines.length <= 3) return lines.length ? [lines.join(' ')] : [];
  const perParagraph = Math.max(2, Math.ceil(lines.length / 4));
  const paragraphs = [];
  for (let i = 0; i < lines.length; i += perParagraph) {
    paragraphs.push(lines.slice(i, i + perParagraph).join(' '));
  }
  return paragraphs;
}

export default function EnglishReadingPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const unit = searchParams.get('unit') || '';
  const version = useStudentStore((s) => s.version);
  const grade = useStudentStore((s) => s.grade);
  const term = useStudentStore((s) => s.term);

  const [reading, setReading] = useState(null);
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState('');
  const [answers, setAnswers] = useState({});
  const [submitted, setSubmitted] = useState(false);
  const [showZh, setShowZh] = useState(false);

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
      .then((res) => setReading(res?.data || null))
      .catch((e) => setFailed(e?.message || '阅读内容加载失败'))
      .finally(() => setLoading(false));
  }, [version, grade, term, unit]);

  useEffect(() => {
    load();
  }, [load]);

  const questions = reading?.questions || [];
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
      <div className="eng-read-wrap">
        <div className="eng-read-loading">
          <Spin size="large" />
          <div className="eng-read-loading-text">正在准备本单元阅读短文…</div>
        </div>
      </div>
    );
  }

  if (!unit || failed || !reading) {
    return (
      <div className="eng-read-wrap">
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
    <div className="eng-read-wrap">
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
