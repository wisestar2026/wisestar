/**
 * StudyPage.jsx - 学海研习主页面（学海智习系统 V2.0 核心学习页 · 三栏悬浮卡片布局）
 *
 * 布局:
 *   +-------------------------------------------------------------------+
 *   | 左栏: 章节学海洲岛导航         | 中栏: 主内容展示区   | 右栏: 悬浮操作 |
 *   | [章节卡片▾]                  | 未选中: 学科整体进度  | [预习]       |
 *   |   ├ 知识点 92% [精通]         |  大图 + 环形统计      | [练习湾]     |
 *   |   └ 知识点 66% [熟练]        | 选中: 简介/状态/引导  | [试炼检测]   |
 *   | ...                          |                       | [错题本]     |
 *   +-------------------------------------------------------------------+
 *                                          AI 小鲸向导智能建议（轻柔文字）
 *
 * 交互: 章节卡片点击平滑展开知识点（可多开）；选中知识点 → 中栏+右栏联动
 * 跳转: 右栏四大按钮 → /student/knowledge/:kpId?tab=preview|practice|trial|wrong
 *
 * 被谁引用: App.jsx（/student/study）、首页「学海研习卡」
 * 依赖: useStudentStore、./StudyPage.css
 */

import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import useStudentStore, { SUBJECTS, masteryLevel } from '../../stores/useStudentStore';
import './StudyPage.css';

// 四大核心功能按钮配置
const ACTION_BUTTONS = [
  { key: 'preview',  label: '知识点预习',   icon: '📖', color: 'blue' },
  { key: 'practice', label: '专项练习湾',   icon: '✏️', color: 'orange' },
  { key: 'trial',    label: '知识点试炼检测', icon: '🎯', color: 'green' },
  { key: 'wrong',    label: '知识点错题本',  icon: '📕', color: 'purple' },
];

// AI 小鲸向导建议（按掌握度档位生成）
const AI_ADVICE = [
  { min: 85, text: '这个知识点你已经很熟练啦，可以去挑战试炼检测冲击高分！' },
  { min: 55, text: '掌握度稳步上升中，建议先做一次专项练习湾巩固熟练度。' },
  { min: 0,  text: '这个知识点还在攻坚期，建议先「知识点预习」打基础，再进入练习湾。' },
];

export default function StudyPage() {
  const navigate = useNavigate();
  const { activeSubject, version, pureMode } = useStudentStore();
  const subject = SUBJECTS.find((s) => s.key === activeSubject) || SUBJECTS[1];

  // 展开的章节（可多开）+ 选中的知识点
  const [openChapters, setOpenChapters] = useState([]);
  const [selectedKp, setSelectedKp] = useState(null);

  // 章节展开/收拢（平滑滑动）
  const toggleChapter = (chId) => {
    setOpenChapters((prev) => (prev.includes(chId) ? prev.filter((id) => id !== chId) : [...prev, chId]));
  };

  // 学科整体进度（章节进度均值）
  const avgProgress = Math.round(
    subject.chapters.reduce((sum, c) => sum + c.progress, 0) / subject.chapters.length,
  );

  // AI 建议
  const advice = selectedKp
    ? AI_ADVICE.find((a) => selectedKp.mastery >= a.min)
    : { text: `在「${subject.name}」的海域里，挑选一个知识点开始今天的研习吧。` };

  // 右栏按钮 → 知识点页（按知识点维度进入对应 tab）
  const goAction = (action) => {
    if (!selectedKp) return;
    navigate(`/student/knowledge/${selectedKp.id}?tab=${action.key}`);
  };

  return (
    <div className="sll-page-enter study-page">
      {/* ---- 左栏: 章节学海洲岛导航 ---- */}
      <aside className="sll-card study-left">
        <div className="study-left-title">
          <span className="study-left-icon">🗺️</span> 学海洲岛 · {subject.name}
          {!pureMode && <span className="study-left-sub">进度 {avgProgress}%</span>}
        </div>
        <div className="study-chapters">
          {subject.chapters.map((ch) => {
            const open = openChapters.includes(ch.id);
            return (
              <div key={ch.id} className={`sll-chapter study-chapter ${open ? 'open' : ''}`}>
                {/* 章节卡片头 */}
                <div className="study-chapter-head" onClick={() => toggleChapter(ch.id)}>
                  <span className="study-chapter-icon">{ch.icon}</span>
                  <div className="study-chapter-info">
                    <div className="study-chapter-name">{ch.name}</div>
                    <div className="study-chapter-progress">
                      <div className="study-chapter-progress-bar" style={{ width: `${ch.progress}%` }} />
                    </div>
                  </div>
                  <span className="study-chapter-pct">{ch.progress}%</span>
                  <span className={`study-chapter-arrow ${open ? 'open' : ''}`}>▾</span>
                </div>
                {/* 知识点条目（平滑展开） */}
                <div className={`study-kp-list ${open ? 'open' : ''}`}>
                  {ch.kps.map((kp) => {
                    const lv = masteryLevel(kp.mastery);
                    const sel = selectedKp && selectedKp.id === kp.id;
                    return (
                      <div
                        key={kp.id}
                        className={`study-kp ${sel ? 'selected' : ''}`}
                        onClick={() => setSelectedKp(kp)}
                      >
                        <span className="study-kp-name">🌊 {kp.name}</span>
                        <span className="study-kp-meta">
                          <span className="study-kp-pct">{kp.mastery}%</span>
                          <span className="sll-level" style={{ background: lv.color }}>{lv.label}</span>
                        </span>
                      </div>
                    );
                  })}
                </div>
              </div>
            );
          })}
        </div>
      </aside>

      {/* ---- 中栏: 主内容展示区 ---- */}
      <main className="sll-card study-center">
        {!selectedKp ? (
          /* 未选中: 学科整体学习进度大图 + 环形统计 */
          <div className="study-overview">
            <div className="study-overview-title">
              🌊 {subject.name} · 整体学习进度（{version}）
            </div>
            <div className="study-overview-body">
              {/* 环形统计图（SVG） */}
              <div className="study-ring">
                <svg viewBox="0 0 120 120" width="150" height="150">
                  <circle cx="60" cy="60" r="50" fill="none" stroke="#e8f2fa" strokeWidth="14" />
                  <circle
                    cx="60" cy="60" r="50" fill="none"
                    stroke={`var(--study-theme-${subject.theme})`}
                    strokeWidth="14" strokeLinecap="round"
                    strokeDasharray={`${avgProgress * 3.14} ${100 * 3.14}`}
                    transform="rotate(-90 60 60)"
                    style={{ transition: 'stroke-dasharray 0.8s ease' }}
                  />
                  <text x="60" y="57" textAnchor="middle" className="study-ring-num">{avgProgress}%</text>
                  <text x="60" y="75" textAnchor="middle" className="study-ring-label">总进度</text>
                </svg>
                <div className="study-ring-stats">
                  <div className="study-ring-stat"><span className="study-ring-dot orange" />章节 {subject.chapters.length} 个</div>
                  <div className="study-ring-stat"><span className="study-ring-dot blue" />知识点 {subject.chapters.reduce((n, c) => n + c.kps.length, 0)} 个</div>
                  <div className="study-ring-stat"><span className="study-ring-dot green" />精通 {subject.chapters.reduce((n, c) => n + c.kps.filter((k) => k.mastery >= 85).length, 0)} 个</div>
                </div>
              </div>
              {/* 章节完成度列表 */}
              <div className="study-overview-chapters">
                {subject.chapters.map((ch) => (
                  <div key={ch.id} className="study-ov-chapter">
                    <span className="study-ov-name">{ch.icon} {ch.name}</span>
                    <div className="study-ov-bar">
                      <div
                        className={`study-ov-bar-inner ${subject.theme}`}
                        style={{ width: `${ch.progress}%` }}
                      />
                    </div>
                    <span className="study-ov-pct">{ch.progress}%</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        ) : (
          /* 选中知识点: 简介 / 学习状态 / 学习引导 */
          <div className="study-kp-detail">
            <div className="study-kp-detail-title">
              🌊 {selectedKp.name}
              <span className="sll-level" style={{ background: masteryLevel(selectedKp.mastery).color }}>
                {masteryLevel(selectedKp.mastery).label}
              </span>
            </div>
            <div className="study-kp-detail-desc">{selectedKp.desc}</div>
            <div className="study-kp-detail-state">
              <div className="study-kp-state-item">
                <span className="study-kp-state-label">掌握度</span>
                <div className="study-kp-state-bar">
                  <div
                    className={`study-kp-state-fill ${subject.theme}`}
                    style={{ width: `${selectedKp.mastery}%` }}
                  />
                </div>
                <span className="study-kp-state-val">{selectedKp.mastery}%</span>
              </div>
              <div className="study-kp-state-item">
                <span className="study-kp-state-label">学习状态</span>
                <span className="study-kp-state-tag">
                  {selectedKp.mastery >= 85 ? '🟢 建议进入试炼冲高分' : selectedKp.mastery >= 55 ? '🟡 建议专项练习巩固' : '🔴 建议先预习再练习'}
                </span>
              </div>
            </div>
            <div className="study-kp-detail-guide">
              <div className="study-kp-guide-title">🧭 学习引导</div>
              <div className="study-kp-guide-step">① 点击右侧「知识点预习」了解核心内容</div>
              <div className="study-kp-guide-step">② 进入「专项练习湾」完成练习获得学习币</div>
              <div className="study-kp-guide-step">③ 掌握度达标后「试炼检测」检验成果</div>
              <div className="study-kp-guide-step">④ 订正「知识点错题本」中的错题</div>
            </div>
          </div>
        )}
      </main>

      {/* ---- 右栏: 悬浮快捷操作面板（固定跟随） ---- */}
      <aside className="sll-card study-right">
        <div className="study-right-title">🎯 快捷操作</div>
        <div className="study-actions">
          {ACTION_BUTTONS.map((a) => (
            <button
              key={a.key}
              className={`study-action-btn study-action-${a.color} ${!selectedKp ? 'disabled' : ''}`}
              onClick={goAction.bind(null, a)}
            >
              <span className="study-action-icon">{a.icon}</span>
              <span>{a.label}</span>
              <span className="study-action-arrow">›</span>
            </button>
          ))}
        </div>
        {!selectedKp && <div className="study-action-hint">👆 请先选择左侧知识点</div>}

        {/* AI 小鲸向导 */}
        <div className="study-ai">
          <div className="study-ai-title">🐬 AI 小鲸向导</div>
          <div className="study-ai-text">{advice.text}</div>
        </div>
      </aside>
    </div>
  );
}
