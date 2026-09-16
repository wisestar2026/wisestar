/**
 * EnglishSentenceLearnPage.jsx - 学员端英语句子学习（连词成句）
 *
 * 功能:
 *   1. 展示单元句子（中文提示 + 英文朗读）
 *   2. 连词成句：点击词块按顺序拼句，自动判分
 *   3. 作答回写句子本熟练度与复习队列，结束记录会话
 *
 * URL: /student/english/sentence?unit=xxx
 * 被谁引用: App.jsx 路由表；入口来自英语学习中心单元卡片
 *
 * 数据流:
 *   GET  /api/english/student/sentences      → 单元句子
 *   POST /api/english/student/sentence/record → 记录作答
 *   POST /api/english/student/session         → 记录会话
 */

import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Spin, message } from 'antd';
import useStudentStore from '../../stores/useStudentStore';
import { getEnglishSentences, recordEnglishSentence, recordEnglishSession } from '../../api/englishStudent';
import { speakEnglish, tokenizeSentence, normalizeAnswer, shuffle } from '../../utils/english';
import './EnglishCenterPage.css';

export default function EnglishSentenceLearnPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const unit = searchParams.get('unit') || '';
  const version = useStudentStore((s) => s.version);
  const grade = useStudentStore((s) => s.grade);

  const [term] = useState('上册');
  const [sentences, setSentences] = useState([]);
  const [loading, setLoading] = useState(true);
  const [index, setIndex] = useState(0);
  const [pool, setPool] = useState([]);
  const [selected, setSelected] = useState([]);
  const [feedback, setFeedback] = useState(null); // 'ok' | 'no'
  const [wrongTries, setWrongTries] = useState(0);
  const [correctCount, setCorrectCount] = useState(0);
  const startAt = useRef(Date.now());
  const timerRef = useRef(null);

  const loadSentences = useCallback(() => {
    if (!unit) {
      setLoading(false);
      return;
    }
    setLoading(true);
    getEnglishSentences({ version, grade, term, unit })
      .then((res) => setSentences(res?.data || []))
      .catch(() => message.error('句子加载失败'))
      .finally(() => setLoading(false));
  }, [version, grade, term, unit]);

  useEffect(() => {
    loadSentences();
  }, [loadSentences]);

  const current = sentences[index];

  // 切题时重置词块池与作答状态
  useEffect(() => {
    if (current) {
      setPool(shuffle(tokenizeSentence(current.en)));
      setSelected([]);
      setFeedback(null);
      setWrongTries(0);
    }
  }, [index, current]);

  useEffect(() => () => clearTimeout(timerRef.current), []);

  const goNext = useCallback(() => {
    clearTimeout(timerRef.current);
    setIndex((i) => i + 1);
  }, []);

  const checkAnswer = () => {
    if (!current) return;
    const answer = selected.map((i) => pool[i]).join(' ');
    if (normalizeAnswer(answer) === normalizeAnswer(current.en)) {
      setFeedback('ok');
      setCorrectCount((n) => n + 1);
      recordEnglishSentence({ sentenceId: current.id, correct: true }).catch(() => {});
      timerRef.current = setTimeout(goNext, 900);
    } else {
      const tries = wrongTries + 1;
      setFeedback('no');
      setWrongTries(tries);
      recordEnglishSentence({ sentenceId: current.id, correct: false }).catch(() => {});
      if (tries >= 3) {
        timerRef.current = setTimeout(goNext, 1600);
      }
    }
  };

  const finished = !loading && sentences.length > 0 && index >= sentences.length;

  useEffect(() => {
    if (finished) {
      const durationSeconds = Math.round((Date.now() - startAt.current) / 1000);
      recordEnglishSession({ type: 'sentence', durationSeconds, correctCount }).catch(() => {});
    }
  }, [finished, correctCount]);

  if (loading) {
    return <div className="eng-learn-wrap"><Spin /></div>;
  }

  if (!unit || sentences.length === 0) {
    return (
      <div className="eng-learn-wrap">
        <div className="eng-empty">
          <div className="eng-empty-emoji">🐚</div>
          <div className="eng-empty-title">{unit ? '该单元暂无句子' : '未选择单元'}</div>
          <button type="button" className="eng-btn eng-btn-primary" onClick={() => navigate('/student/english')}>
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
          <div className="eng-empty-emoji">🌟</div>
          <div className="eng-empty-title">本单元句子已学完</div>
          <div className="eng-done-stats">
            <div className="eng-done-stat"><b>{correctCount}</b><span>答对</span></div>
            <div className="eng-done-stat"><b>{sentences.length}</b><span>句子总数</span></div>
          </div>
          <button type="button" className="eng-btn eng-btn-primary" onClick={() => navigate('/student/english')}>
            返回学习中心
          </button>
        </div>
      </div>
    );
  }

  const progress = Math.round((index / sentences.length) * 100);

  return (
    <div className="eng-learn-wrap">
      <div className="eng-progress-row">
        <div className="eng-progress-track"><span style={{ width: `${progress}%` }} /></div>
        <div className="eng-progress-text">{index + 1} / {sentences.length}</div>
      </div>

      <div className="eng-card">
        <div className="eng-sentence-hint">连词成句：{current?.zh || '（无中文提示）'}</div>

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
          <div className="eng-feedback no">
            再想想～{wrongTries >= 3 && current && <div>正确答案：{current.en}</div>}
          </div>
        )}

        <div className="eng-actions">
          <button type="button" className="eng-btn eng-btn-sound" onClick={() => speakEnglish(current?.en, current?.audioUrl)}>
            朗读
          </button>
          <button type="button" className="eng-btn eng-btn-ghost" onClick={() => setSelected([])} disabled={feedback === 'ok'}>
            清空
          </button>
          <button
            type="button"
            className="eng-btn eng-btn-primary"
            onClick={checkAnswer}
            disabled={selected.length === 0 || feedback === 'ok'}
          >
            检查
          </button>
        </div>
      </div>

      <div className="eng-actions">
        <button type="button" className="eng-btn eng-btn-ghost" onClick={() => navigate('/student/english')}>
          结束学习
        </button>
      </div>
    </div>
  );
}
