/**
 * EnglishReviewPage.jsx - 学员端英语智能复习（单词 + 句子混合队列）
 *
 * 功能:
 *   1. 拉取到期复习队列（单词 + 句子，按到期时间升序）
 *   2. 单词：认识/不认识；句子：连词成句
 *   3. 作答回写熟练度与下次复习时间，结束记录会话
 *
 * URL: /student/english/review
 * 被谁引用: App.jsx 路由表；入口来自英语学习中心「智能复习」
 *
 * 数据流:
 *   GET  /api/english/student/review          → 混合复习队列
 *   POST /api/english/word/record             → 单词作答
 *   POST /api/english/student/sentence/record → 句子作答
 *   POST /api/english/student/session         → 记录会话
 */

import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Spin, message } from 'antd';
import { getEnglishReview, recordEnglishWord, recordEnglishSentence, recordEnglishSession } from '../../api/englishStudent';
import { speakEnglish, tokenizeSentence, normalizeAnswer, shuffle } from '../../utils/english';
import './EnglishCenterPage.css';

export default function EnglishReviewPage() {
  const navigate = useNavigate();

  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [index, setIndex] = useState(0);
  const [revealed, setRevealed] = useState(false);
  const [correctCount, setCorrectCount] = useState(0);
  const [pool, setPool] = useState([]);
  const [selected, setSelected] = useState([]);
  const [feedback, setFeedback] = useState(null);
  const startAt = useRef(Date.now());
  const timerRef = useRef(null);

  useEffect(() => {
    getEnglishReview({ limit: 20 })
      .then((res) => setItems(res?.data || []))
      .catch(() => message.error('复习队列加载失败'))
      .finally(() => setLoading(false));
  }, []);

  const current = items[index];
  const isSentence = current?.type === 'sentence';
  const finished = !loading && items.length > 0 && index >= items.length;

  // 切题重置
  useEffect(() => {
    if (current) {
      setRevealed(current.type === 'word' ? false : true);
      setFeedback(null);
      if (current.type === 'sentence') {
        setPool(shuffle(tokenizeSentence(current.prompt)));
        setSelected([]);
      }
    }
  }, [index, current]);

  useEffect(() => () => clearTimeout(timerRef.current), []);

  useEffect(() => {
    if (finished) {
      const durationSeconds = Math.round((Date.now() - startAt.current) / 1000);
      recordEnglishSession({ type: 'review', durationSeconds, correctCount }).catch(() => {});
    }
  }, [finished, correctCount]);

  const goNext = () => {
    clearTimeout(timerRef.current);
    setIndex((i) => i + 1);
  };

  const answerWord = (correct) => {
    if (!current) return;
    recordEnglishWord({ wordId: current.id, correct }).catch(() => {});
    if (correct) setCorrectCount((n) => n + 1);
    goNext();
  };

  const checkSentence = () => {
    if (!current) return;
    const answer = selected.map((i) => pool[i]).join(' ');
    if (normalizeAnswer(answer) === normalizeAnswer(current.prompt)) {
      setFeedback('ok');
      setCorrectCount((n) => n + 1);
      recordEnglishSentence({ sentenceId: current.id, correct: true }).catch(() => {});
      timerRef.current = setTimeout(goNext, 900);
    } else {
      setFeedback('no');
      recordEnglishSentence({ sentenceId: current.id, correct: false }).catch(() => {});
    }
  };

  const typeLabel = useMemo(() => (isSentence ? '句子' : '单词'), [isSentence]);

  if (loading) {
    return <div className="eng-learn-wrap"><Spin /></div>;
  }

  if (items.length === 0) {
    return (
      <div className="eng-learn-wrap">
        <div className="eng-empty">
          <div className="eng-empty-emoji">✅</div>
          <div className="eng-empty-title">暂无待复习内容</div>
          <div>先去学习中心学习单词和句子，到期的内容会自动出现在这里</div>
          <button
            type="button"
            className="eng-btn eng-btn-primary"
            style={{ marginTop: 16 }}
            onClick={() => navigate('/student/english')}
          >
            返回英语学习中心
          </button>
        </div>
      </div>
    );
  }

  if (finished) {
    return (
      <div className="eng-learn-wrap">
        <div className="eng-empty">
          <div className="eng-empty-emoji">🏆</div>
          <div className="eng-empty-title">本轮复习完成</div>
          <div className="eng-done-stats">
            <div className="eng-done-stat"><b>{correctCount}</b><span>答对</span></div>
            <div className="eng-done-stat"><b>{items.length}</b><span>复习总数</span></div>
          </div>
          <button type="button" className="eng-btn eng-btn-primary" onClick={() => navigate('/student/english')}>
            返回学习中心
          </button>
        </div>
      </div>
    );
  }

  const progress = Math.round((index / items.length) * 100);

  return (
    <div className="eng-learn-wrap">
      <div className="eng-progress-row">
        <div className="eng-progress-track"><span style={{ width: `${progress}%` }} /></div>
        <div className="eng-progress-text">{index + 1} / {items.length} · {typeLabel}</div>
      </div>

      <div className="eng-card">
        {!isSentence && (
          <>
            <div className="eng-word">{current?.prompt}</div>
            {current?.phonetic && <div className="eng-phonetic">/{current.phonetic}/</div>}
            {current?.imageUrl && <img className="eng-image" src={current.imageUrl} alt={current.prompt} />}
            <div className={`eng-meaning ${revealed ? '' : 'hidden'}`}>
              {revealed ? (current?.answer || '—') : '点击「显示释义」查看含义'}
            </div>
            <div className="eng-actions">
              <button type="button" className="eng-btn eng-btn-sound" onClick={() => speakEnglish(current?.prompt, current?.audioUrl)}>
                朗读
              </button>
              {!revealed && (
                <button type="button" className="eng-btn eng-btn-ghost" onClick={() => setRevealed(true)}>显示释义</button>
              )}
            </div>
            {revealed && (
              <div className="eng-actions">
                <button type="button" className="eng-btn eng-btn-ok" onClick={() => answerWord(true)}>认识</button>
                <button type="button" className="eng-btn eng-btn-again" onClick={() => answerWord(false)}>不认识</button>
              </div>
            )}
          </>
        )}

        {isSentence && (
          <>
            <div className="eng-sentence-hint">连词成句：{current?.answer || '（无中文提示）'}</div>
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
            {feedback === 'ok' && <div className="eng-feedback ok">太棒了，完全正确！</div>}
            {feedback === 'no' && (
              <div className="eng-feedback no">再想想～<div>正确答案：{current?.prompt}</div></div>
            )}
            <div className="eng-actions">
              <button type="button" className="eng-btn eng-btn-sound" onClick={() => speakEnglish(current?.prompt, current?.audioUrl)}>
                朗读
              </button>
              <button type="button" className="eng-btn eng-btn-ghost" onClick={() => setSelected([])} disabled={feedback === 'ok'}>
                清空
              </button>
              <button
                type="button"
                className="eng-btn eng-btn-primary"
                onClick={checkSentence}
                disabled={selected.length === 0 || feedback === 'ok'}
              >
                检查
              </button>
            </div>
          </>
        )}
      </div>

      <div className="eng-actions">
        <button
          type="button"
          className="eng-btn eng-btn-ghost"
          onClick={() => {
            const durationSeconds = Math.round((Date.now() - startAt.current) / 1000);
            recordEnglishSession({ type: 'review', durationSeconds, correctCount }).catch(() => {});
            navigate('/student/english');
          }}
        >
          结束复习
        </button>
      </div>
    </div>
  );
}
