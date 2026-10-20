/**
 * EnglishCenterPage.jsx - 学员端英语学习中心（左侧单元导航 + 右侧单元概览 + 智能复习）
 *
 * 结构（对应「英语学习主界面」参考图）:
 *   左侧：UNIT 竖排导航（当前单元高亮、待复习角标）
 *   右侧：当前单元的 熟练度 + 单词/句子/语法进度 + 四张模块入口卡
 *        （单词通关 / 句式通关 / 语法通关 / 单元练习）
 *   顶部：一键进入智能复习；进入本页按「最近答错 + 复习到期」弹出强制巩固弹窗
 *
 * URL: /student/english
 * 被谁引用: App.jsx 路由表（/student 子路由）；入口来自首页学海研习卡片
 *
 * 数据流:
 *   GET /api/english/student/units → 单元进度
 *   GET /api/english/student/review → 复习队列（用于待复习计数）
 *   GET /api/english/word/drill/completed → 当天是否已完成巩固（服务端判定）
 *   GET /api/english/word/drill → 待巩固单词（强制巩固弹窗）
 */

import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { message } from 'antd';
import {
  BookOutlined,
  MessageOutlined,
  CompassOutlined,
  ReadOutlined,
  CustomerServiceOutlined,
  FlagOutlined,
  SyncOutlined,
} from '@ant-design/icons';
import useStudentStore from '../../stores/useStudentStore';
import useUserStore from '../../stores/useUserStore';
import StarRating from '../../components/common/StarRating';
import { getEnglishUnits, getEnglishReview, getEnglishDrillWords, getEnglishDrillCompleted } from '../../api/englishStudent';
import WordDrillModal from './WordDrillModal';
import EnglishUnitAside from './EnglishUnitAside';
import './EnglishCenterPage.css';

const TERMS = ['上册', '下册'];

function percent(finished, total) {
  if (!total) return 0;
  return Math.min(100, Math.round((finished / total) * 100));
}

export default function EnglishCenterPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const unitParam = searchParams.get('unit') || '';
  const version = useStudentStore((s) => s.version);
  const grade = useStudentStore((s) => s.grade);
  // 册别由首页统一选择，本页只读展示（不可切换）
  const term = useStudentStore((s) => s.term);
  const userId = useUserStore((s) => s.user?.userId ?? s.user?.id);

  const [units, setUnits] = useState([]);
  const [activeUnit, setActiveUnit] = useState('');
  const [loading, setLoading] = useState(false);
  const [reviewCount, setReviewCount] = useState(0);
  const [drillWords, setDrillWords] = useState([]);
  const [drillOpen, setDrillOpen] = useState(false);

  const loadUnits = useCallback(() => {
    setLoading(true);
    getEnglishUnits({ version, grade, term })
      .then((res) => {
        const list = res?.data || [];
        setUnits(list);
        setActiveUnit((prev) => (list.some((u) => u.unit === prev) ? prev : (list[0]?.unit || '')));
      })
      .catch(() => message.error('单元加载失败'))
      .finally(() => setLoading(false));
  }, [version, grade, term]);

  useEffect(() => {
    loadUnits();
  }, [loadUnits]);

  // 其它页面点击左侧单元后跳转到本页并带 ?unit=，此处按参数选中对应单元
  useEffect(() => {
    if (unitParam && units.some((u) => u.unit === unitParam)) {
      setActiveUnit(unitParam);
    }
  }, [unitParam, units]);

  useEffect(() => {
    getEnglishReview({ limit: 50 })
      .then((res) => setReviewCount((res?.data || []).length))
      .catch(() => setReviewCount(0));
  }, [version, grade, term]);

  // 进入本页时的强制巩固弹窗：以服务端记录为准（账号维度、跨设备），
  // 仅当当天尚未完成巩固时才拉取待巩固单词并弹出；当天完成后不再弹。
  useEffect(() => {
    if (!userId) {
      return undefined;
    }
    let cancelled = false;
    getEnglishDrillCompleted()
      .then((res) => {
        if (cancelled || res?.data === true) {
          return null;
        }
        return getEnglishDrillWords({ limit: 10 }).then((wordRes) => {
          if (cancelled) {
            return;
          }
          const list = wordRes?.data || [];
          if (list.length) {
            setDrillWords(list);
            setDrillOpen(true);
          }
        });
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

  const go = (path, unit) => navigate(`${path}?unit=${encodeURIComponent(unit)}`);
  // 左侧单元导航：在本页内切换单元，同时把 ?unit= 同步到地址栏
  const selectUnit = (next) => navigate(`/student/english?unit=${encodeURIComponent(next)}`, { replace: true });

  const active = units.find((u) => u.unit === activeUnit) || units[0];

  return (
    <div className="eng-page">
      <div className="eng-hero">
        <div>
          <div className="eng-hero-eyebrow">海洋智学 · 英语学习航线</div>
          <div className="eng-hero-title">英语学习中心</div>
          <div className="eng-hero-sub">
            {version} · {grade} · 单词 + 句子 + 语法，学练一体，智能复习帮你记得更牢
          </div>
        </div>
        <div className="eng-hero-actions">
          <div className="eng-term" title="册别由首页选择，此处仅展示">
            {TERMS.map((t) => (
              <button
                key={t}
                type="button"
                className={term === t ? 'active' : ''}
                disabled
              >
                {t}
              </button>
            ))}
          </div>
          <button type="button" className="eng-review-btn" onClick={() => navigate('/student/english/review')}>
            <SyncOutlined />
            智能复习{reviewCount > 0 ? ` · ${reviewCount}` : ''}
          </button>
        </div>
      </div>

      {!loading && units.length === 0 && (
        <div className="eng-empty" style={{ marginTop: 20 }}>
          <div className="eng-empty-emoji">🐚</div>
          <div className="eng-empty-title">当前学期暂无英语内容</div>
          <div>换一个版本 / 年级 / 册别，或等待老师录入单元内容</div>
        </div>
      )}

      {active && (
        <div className="eng-master" style={{ marginTop: 20 }}>
          <EnglishUnitAside units={units} activeUnit={active.unit} onSelect={selectUnit} />

          <div className="eng-detail">
            <div className="eng-detail-card">
              <div className="eng-detail-head">
                <div className="eng-detail-title">{active.unit}</div>
                {active.reviewDue > 0 && (
                  <button
                    type="button"
                    className="eng-review-pill"
                    onClick={() => navigate('/student/english/review')}
                  >
                    复习 {active.reviewDue}
                  </button>
                )}
              </div>
              <div className="eng-detail-tip">
                本单元共 {active.wordCount || 0} 个单词 · {active.sentenceCount || 0} 个句子 ·
                {active.grammarCount || 0} 个语法点，逐个点亮你的通关进度吧
              </div>

              <div className="eng-mastery-row">
                <span className="eng-mastery-label">熟练度</span>
                <StarRating value={active.mastery || 0} size={16} className="eng-unit-mastery-stars" />
                <span className="eng-mastery-val">{active.mastery || 0}%</span>
              </div>

              <div className="eng-progress-grid">
                <div className="eng-progress-item">
                  <div className="eng-stat-label">
                    <span>单词</span>
                    <span>{active.wordFinished || 0}/{active.wordCount || 0}</span>
                  </div>
                  <div className="eng-stat-bar">
                    <span style={{ width: `${percent(active.wordFinished, active.wordCount)}%` }} />
                  </div>
                </div>
                <div className="eng-progress-item">
                  <div className="eng-stat-label">
                    <span>句子</span>
                    <span>{active.sentenceFinished || 0}/{active.sentenceCount || 0}</span>
                  </div>
                  <div className="eng-stat-bar sentence">
                    <span style={{ width: `${percent(active.sentenceFinished, active.sentenceCount)}%` }} />
                  </div>
                </div>
                <div className="eng-progress-item">
                  <div className="eng-stat-label">
                    <span>语法</span>
                    <span>{active.grammarFinished || 0}/{active.grammarCount || 0}</span>
                  </div>
                  <div className="eng-stat-bar grammar">
                    <span style={{ width: `${percent(active.grammarFinished, active.grammarCount)}%` }} />
                  </div>
                </div>
              </div>

              <div className="eng-mod-grid">
                <button
                  type="button"
                  className="eng-mod-card eng-mod-word"
                  onClick={() => go('/student/english/word', active.unit)}
                >
                  <span className="eng-mod-icon"><BookOutlined /></span>
                  <span className="eng-mod-title">单词通关<em>{percent(active.wordFinished, active.wordCount)}%</em></span>
                  <span className="eng-mod-sub">拼写 · 音标 · 释义</span>
                  <span className="eng-mod-track"><i style={{ width: `${percent(active.wordFinished, active.wordCount)}%` }} /></span>
                </button>
                <button
                  type="button"
                  className="eng-mod-card eng-mod-sentence"
                  onClick={() => go('/student/english/sentence', active.unit)}
                >
                  <span className="eng-mod-icon"><MessageOutlined /></span>
                  <span className="eng-mod-title">句式通关<em>{percent(active.sentenceFinished, active.sentenceCount)}%</em></span>
                  <span className="eng-mod-sub">连词成句 · 情景</span>
                  <span className="eng-mod-track"><i style={{ width: `${percent(active.sentenceFinished, active.sentenceCount)}%` }} /></span>
                </button>
                <button
                  type="button"
                  className="eng-mod-card eng-mod-grammar"
                  disabled={!active.grammarCount}
                  onClick={() => go('/student/english/grammar', active.unit)}
                >
                  <span className="eng-mod-icon"><CompassOutlined /></span>
                  <span className="eng-mod-title">语法通关<em>{percent(active.grammarFinished, active.grammarCount)}%</em></span>
                  <span className="eng-mod-sub">规则 · 例句</span>
                  <span className="eng-mod-track"><i style={{ width: `${percent(active.grammarFinished, active.grammarCount)}%` }} /></span>
                </button>
                <button
                  type="button"
                  className="eng-mod-card eng-mod-reading"
                  onClick={() => go('/student/english/reading', active.unit)}
                >
                  <span className="eng-mod-icon"><ReadOutlined /></span>
                  <span className="eng-mod-title">阅读理解</span>
                  <span className="eng-mod-sub">短文 · 选择题</span>
                </button>
                <button
                  type="button"
                  className="eng-mod-card eng-mod-readaloud"
                  onClick={() => go('/student/english/read-aloud', active.unit)}
                >
                  <span className="eng-mod-icon"><CustomerServiceOutlined /></span>
                  <span className="eng-mod-title">课本跟读</span>
                  <span className="eng-mod-sub">听原音 · 录音跟读</span>
                </button>
                <button
                  type="button"
                  className="eng-mod-card eng-mod-unit"
                  onClick={() => go('/student/english/unit-practice', active.unit)}
                >
                  <span className="eng-mod-icon"><FlagOutlined /></span>
                  <span className="eng-mod-title">单元练习</span>
                  <span className="eng-mod-sub">本单元综合题</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      <WordDrillModal open={drillOpen} words={drillWords} onFinish={finishDrill} />
    </div>
  );
}
