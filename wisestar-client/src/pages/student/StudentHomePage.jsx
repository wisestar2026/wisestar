/**
 * StudentHomePage.jsx - 学生端首页（学海智习系统 V2.0 · 年轻化三卡片布局）
 *
 * 布局（海底童趣视觉版）:
 *   +--------------------------------------------------------------+
 *   | 🐬 小海星，今天也要潜入知识的海洋哦                            |
 *   | [我的档案卡] [学海研习卡] [荣誉商城卡]                         |
 *   | +----------------------------------------------------------+ |
 *   | | 今日学习数据总览: 时长 / 知识点 / 积分 / 学习币  四模块     | |
 *   | | 今日待办任务快捷跳转列表                                     | |
 *   +--------------------------------------------------------------+
 *
 * 页面跳转:
 *   - 我的档案卡 → /student/profile（我的档案荣誉墙）
 *   - 学海研习卡 → /student/study（学海研习主页面·三栏）
 *   - 荣誉商城卡 → /student/mall（荣誉商城）
 *   - 今日待办   → /student/study
 *
 * 纯净学习模式: 仅保留研习卡 + 今日时长/知识点，激励模块（积分/币/商城）DOM 移除
 *
 * 被谁引用: App.jsx 路由表（/student 子路由 index）
 * 依赖: react-router-dom(useNavigate)、useStudentStore、./student.css
 */

import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import useStudentStore, { SUBJECTS, TITLES, PROFILE, TODAY, DAILY_TASKS } from '../../stores/useStudentStore';
import { getMyStudentInfo } from '../../api/student';
import './StudentHomePage.css';

export default function StudentHomePage() {
  const navigate = useNavigate();
  const { pureMode } = useStudentStore();
  const activeSubject = useStudentStore((s) => s.activeSubject);
  const subject = SUBJECTS.find((s) => s.key === activeSubject) || SUBJECTS[1];

  // 当前学员真实档案（学号/姓名/学校等，来自 GET /api/student/me；加载失败回退 mock）
  const [myInfo, setMyInfo] = useState(null);
  useEffect(() => {
    getMyStudentInfo().then((res) => setMyInfo(res?.data || null)).catch(() => setMyInfo(null));
  }, []);

  // 当前头衔（按学海积分自动晋升）
  const currentTitle = [...TITLES].reverse().find((t) => PROFILE.points >= t.need) || TITLES[0];
  // 本学期可兑换总学习币（多科合并）
  const totalCoins = SUBJECTS.reduce((sum, s) => sum + s.coins, 0);
  // 展示用学员姓名（真实档案优先，缺失回退 mock）
  const displayName = myInfo?.name || PROFILE.name;

  return (
    <div className="sll-page-enter">
      {/* 吉祥物 + 欢迎语 */}
      <div className="sll-mascot-row">
        <div className="sll-mascot"><span>🐬</span></div>
        <div className="sll-bubble">{displayName}，今天也要潜入知识的海洋哦</div>
      </div>

      {/* ---- 上半部: 三大悬浮功能卡片 ---- */}
      <div className="sh-home-cards">
        {/* 1) 我的档案卡片 */}
        {!pureMode && (
          <div
            className="sll-card sll-card-hover sh-home-card sh-home-card-archive"
            onClick={() => navigate('/student/profile')}
          >
            <div className="sh-home-card-head">
              <span className="sh-home-card-icon">📋</span>
              <span className="sh-home-card-tag">我的档案</span>
            </div>
            <div className="sh-home-archive">
              <div className="sh-home-avatar">{PROFILE.emoji}</div>
              <div className="sh-home-archive-info">
                <div className="sh-home-archive-name">{displayName}</div>
                <div className="sh-home-title">{currentTitle.emoji} {currentTitle.name}</div>
                <div className="sh-home-archive-meta">
                  {myInfo?.studentNo && <span>🎓 学号 <b>{myInfo.studentNo}</b></span>}
                  <span>⭐ 学海积分 <b>{PROFILE.points}</b></span>
                  <span>🏅 证书 <b>{PROFILE.certCount}/{PROFILE.certTotal}</b></span>
                </div>
                {myInfo?.school && <div className="sh-home-archive-school">🏫 {myInfo.school}</div>}
              </div>
            </div>
            <div className="sh-home-card-foot">查看荣誉档案 ›</div>
          </div>
        )}

        {/* 2) 学海研习卡片（核心学习入口） */}
        <div
          className={`sll-card sll-card-hover sh-home-card sh-home-card-study sh-home-card-${subject.theme}`}
          onClick={() => navigate('/student/study')}
        >
          <div className="sh-home-card-head">
            <span className="sh-home-card-icon">{subject.icon}</span>
            <span className="sh-home-card-tag">学海研习</span>
          </div>
          <div className="sh-home-study">
            <div className="sh-home-study-book">📖</div>
            <div className="sh-home-study-text">
              <div className="sh-home-study-title">开启{subject.name}研习</div>
              <div className="sh-home-study-desc">潜入「{subject.chapters[0].name}」的知识海洋</div>
            </div>
          </div>
          <div className="sh-home-card-foot">进入研习主页面 ›</div>
        </div>

        {/* 3) 荣誉商城卡片 */}
        {!pureMode && (
          <div
            className="sll-card sll-card-hover sh-home-card sh-home-card-mall"
            onClick={() => navigate('/student/mall')}
          >
            <div className="sh-home-card-head">
              <span className="sh-home-card-icon">🎁</span>
              <span className="sh-home-card-tag">荣誉商城</span>
            </div>
            <div className="sh-home-mall">
              <div className="sh-home-mall-coin">🐚</div>
              <div>
                <div className="sh-home-mall-num">{totalCoins}</div>
                <div className="sh-home-mall-label">本学期可兑换总学习币</div>
              </div>
            </div>
            {/* 各科学习币明细（hover 展示） */}
            <div className="sh-home-mall-detail">
              {SUBJECTS.map((s) => (
                <span key={s.key} className={`sh-home-mall-sub sh-home-mall-sub-${s.theme}`}>
                  {s.icon} {s.name} {s.coins}
                </span>
              ))}
            </div>
            <div className="sh-home-card-foot">去逛逛商城 ›</div>
          </div>
        )}
      </div>

      {/* ---- 下半部: 今日学习数据总览 ---- */}
      <div className="sh-home-bottom">
        <div className="sll-card sh-home-data">
          <div className="sh-home-section-title">🌊 今日学习数据总览</div>
          <div className="sh-home-data-grid">
            <div className="sh-home-data-item">
              <div className="sh-home-data-icon sh-home-data-time">⏱️</div>
              <div className="sh-home-data-num">{TODAY.minutes}<small>分钟</small></div>
              <div className="sh-home-data-label">今日学习时长</div>
            </div>
            <div className="sh-home-data-item">
              <div className="sh-home-data-icon sh-home-data-kp">🧩</div>
              <div className="sh-home-data-num">{TODAY.kps}<small>个</small></div>
              <div className="sh-home-data-label">完成知识点</div>
            </div>
            {!pureMode && (
              <>
                <div className="sh-home-data-item">
                  <div className="sh-home-data-icon sh-home-data-points">⭐</div>
                  <div className="sh-home-data-num">+{TODAY.points}<small>积分</small></div>
                  <div className="sh-home-data-label">今日获得积分</div>
                </div>
                <div className="sh-home-data-item">
                  <div className="sh-home-data-icon sh-home-data-coins">🐚</div>
                  <div className="sh-home-data-num">+{TODAY.coins}<small>币</small></div>
                  <div className="sh-home-data-label">今日获得学习币</div>
                </div>
              </>
            )}
          </div>
        </div>

        {/* 今日待办任务快捷跳转 */}
        <div className="sll-card sh-home-todo">
          <div className="sh-home-section-title">🗓️ 今日待办任务</div>
          {DAILY_TASKS.map((t) => (
            <div
              key={t.key}
              className="sh-home-todo-item"
              onClick={() => navigate('/student/study')}
            >
              <span className={`sh-home-todo-dot ${t.done ? 'done' : ''}`}>
                {t.done ? '✓' : ''}
              </span>
              <span className="sh-home-todo-label">{t.label}</span>
              <span className="sh-home-todo-reward">{t.reward}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
