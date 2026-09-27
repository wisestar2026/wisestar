/**
 * EnglishCenterPage.jsx - 学员端英语学习中心（单元列表 + 进度 + 智能复习）
 *
 * 功能:
 *   1. 按 版本/年级/册别 展示单元网格与学习进度
 *   2. 每个单元提供「单词学习」「句子练习」「句子学习」入口
 *   3. 顶部一键进入智能复习（单词 + 句子混合队列）
 *   4. 进入本页时按「最近答错 + 复习到期」弹出强制巩固弹窗（一个自然天只弹一次）
 *
 * URL: /student/english
 * 被谁引用: App.jsx 路由表（/student 子路由）；入口来自首页学海研习卡片
 *
 * 数据流:
 *   GET /api/english/student/units → 单元进度
 *   GET /api/english/student/review → 复习队列（用于待复习计数）
 *   GET /api/english/word/drill → 待巩固单词（强制巩固弹窗）
 */

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { message } from 'antd';
import useStudentStore from '../../stores/useStudentStore';
import useUserStore from '../../stores/useUserStore';
import { getEnglishUnits, getEnglishReview, getEnglishDrillWords } from '../../api/englishStudent';
import WordDrillModal from './WordDrillModal';
import './EnglishCenterPage.css';

const TERMS = ['上册', '下册'];

/** 本地自然日的键（YYYY-MM-DD），用于控制强制巩固弹窗「一天只弹一次」 */
function localDateKey() {
  const d = new Date();
  const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}

function percent(finished, total) {
  if (!total) return 0;
  return Math.min(100, Math.round((finished / total) * 100));
}

export default function EnglishCenterPage() {
  const navigate = useNavigate();
  const version = useStudentStore((s) => s.version);
  const grade = useStudentStore((s) => s.grade);
  const userId = useUserStore((s) => s.user?.userId ?? s.user?.id);

  const [term, setTerm] = useState('上册');
  const [units, setUnits] = useState([]);
  const [loading, setLoading] = useState(false);
  const [reviewCount, setReviewCount] = useState(0);
  const [drillWords, setDrillWords] = useState([]);
  const [drillOpen, setDrillOpen] = useState(false);

  const loadUnits = useCallback(() => {
    setLoading(true);
    getEnglishUnits({ version, grade, term })
      .then((res) => setUnits(res?.data || []))
      .catch(() => message.error('单元加载失败'))
      .finally(() => setLoading(false));
  }, [version, grade, term]);

  useEffect(() => {
    loadUnits();
  }, [loadUnits]);

  useEffect(() => {
    getEnglishReview({ limit: 50 })
      .then((res) => setReviewCount((res?.data || []).length))
      .catch(() => setReviewCount(0));
  }, [version, grade, term]);

  // 进入本页时的强制巩固弹窗：同一天只弹一次（按学员维度记录本地日期）
  useEffect(() => {
    if (!userId) {
      return undefined;
    }
    const storageKey = `eng-word-drill-${userId}`;
    const today = localDateKey();
    try {
      if (localStorage.getItem(storageKey) === today) {
        return undefined;
      }
    } catch {
      // localStorage 不可用时按可弹出处理
    }
    let cancelled = false;
    getEnglishDrillWords({ limit: 10 })
      .then((res) => {
        if (cancelled) {
          return;
        }
        try {
          localStorage.setItem(storageKey, today);
        } catch {
          // 忽略存储失败
        }
        const list = res?.data || [];
        if (list.length) {
          setDrillWords(list);
          setDrillOpen(true);
        }
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, [userId]);

  const finishDrill = () => {
    setDrillOpen(false);
    getEnglishReview({ limit: 50 })
      .then((res) => setReviewCount((res?.data || []).length))
      .catch(() => {});
  };

  const totalDue = useMemo(
    () => units.reduce((sum, u) => sum + (u.reviewDue || 0), 0),
    [units],
  );

  const goWord = (unit) => navigate(`/student/english/word?unit=${encodeURIComponent(unit)}`);
  const goSentence = (unit) => navigate(`/student/english/sentence?unit=${encodeURIComponent(unit)}`);
  const goPractice = (item) => {
    if ((item.wordCount || 0) < 4) {
      message.warning('本单元单词太少，暂不能练习');
      return;
    }
    navigate(`/student/english/practice?unit=${encodeURIComponent(item.unit)}`);
  };

  return (
    <div className="eng-page">
      <div className="eng-hero">
        <div>
          <div className="eng-hero-title">英语学习中心</div>
          <div className="eng-hero-sub">
            {version} · {grade} · 单词 + 句子，学练一体，智能复习帮你记得更牢
          </div>
        </div>
        <div className="eng-hero-actions">
          <div className="eng-term">
            {TERMS.map((t) => (
              <button key={t} type="button" className={term === t ? 'active' : ''} onClick={() => setTerm(t)}>
                {t}
              </button>
            ))}
          </div>
          <button type="button" className="eng-review-btn" onClick={() => navigate('/student/english/review')}>
            智能复习{reviewCount > 0 ? ` · ${reviewCount}` : ''}
          </button>
        </div>
      </div>

      <div className="eng-section-title">
        <span>单元列表</span>
        {totalDue > 0 && <span style={{ fontSize: 12, color: '#ff7a59' }}>待复习 {totalDue} 项</span>}
      </div>

      {!loading && units.length === 0 && (
        <div className="eng-empty">
          <div className="eng-empty-emoji">🐚</div>
          <div className="eng-empty-title">当前学期暂无英语内容</div>
          <div>换一个版本 / 年级 / 册别，或等待老师录入单元内容</div>
        </div>
      )}

      {units.length > 0 && (
        <div className="eng-grid">
          {units.map((unit) => (
            <div className="eng-unit-card" key={unit.unit}>
              {unit.reviewDue > 0 && <span className="eng-unit-badge">复习 {unit.reviewDue}</span>}
              <div className="eng-unit-name">{unit.unit}</div>
              <div className="eng-unit-stats">
                <div className="eng-stat">
                  <div className="eng-stat-label">
                    <span>单词</span>
                    <span>{unit.wordFinished || 0}/{unit.wordCount || 0}</span>
                  </div>
                  <div className="eng-stat-bar">
                    <span style={{ width: `${percent(unit.wordFinished, unit.wordCount)}%` }} />
                  </div>
                </div>
                <div className="eng-stat">
                  <div className="eng-stat-label">
                    <span>句子</span>
                    <span>{unit.sentenceFinished || 0}/{unit.sentenceCount || 0}</span>
                  </div>
                  <div className="eng-stat-bar sentence">
                    <span style={{ width: `${percent(unit.sentenceFinished, unit.sentenceCount)}%` }} />
                  </div>
                </div>
              </div>
              <div className="eng-unit-actions">
                <button type="button" className="eng-btn-word" onClick={() => goWord(unit.unit)}>单词学习</button>
                <button type="button" className="eng-btn-practice" onClick={() => goPractice(unit)}>单词练习</button>
                <button type="button" className="eng-btn-sentence" onClick={() => goSentence(unit.unit)}>句子学习</button>
              </div>
            </div>
          ))}
        </div>
      )}

      <WordDrillModal open={drillOpen} words={drillWords} onFinish={finishDrill} />
    </div>
  );
}
