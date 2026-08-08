/**
 * StudentHomePage.jsx - 学生端主界面（海底AI自习室）
 *
 * 依据设计稿还原的纯前端页面（无后端依赖，数据均为本地 mock）。
 *
 * 布局结构（自上而下、自左而右）:
 *   +--------------------------------------------------------------+
 *   | Header: [logo] 海底AI自习室       贝壳币 128 ∨ | 今日海浪值 80% |
 *   +--------+-----------------------------------------------------+
 *   | 左导航  |  吉祥物区: 🐬 + 气泡「今天我们潜入3个小任务吧」        |
 *   | 首页    |  +------------------------------------------------+ |
 *   | 任务岛  |  | 任务卡片1   任务卡片2   任务卡片3                 | |
 *   | 阅读珊瑚|  | 语文朗读    数学口算    英语跟读                 | |
 *   | 数学贝壳|  | 15分钟      20题        10分钟                 | |
 *   | 英语浪花|  | 开始探索    开始探索    开始探索                | |
 *   | 成长宝藏|  +------------------------------------------------+ |
 *   |        |  专注倒计时 25分钟 · 休息提醒 浮出水面                 |
 *   +--------+-----------------------------------------------------+
 *                                 (右下角悬浮: 问一问/问小鲸)
 *
 * 视觉风格: 海底童趣 + 3D 圆润卡片 + 浅蓝渐变海洋背景 + 波浪海面
 * 学科配色: 橙色=语文 / 蓝色=数学 / 绿色=英语
 *
 * URL: /student（独立路由，不经 MainLayout，全屏展示设计稿）
 * 被谁引用: App.jsx 路由表；MainLayout 侧边栏「学生端主页」菜单进入
 */

import { useState } from 'react';
import { message } from 'antd';
import './StudentHomePage.css';

// ---- 左侧导航配置 ----
// 图标用 emoji 呈现童趣感；数学贝壳为当前选中项（设计稿橙色高亮）
const NAV_ITEMS = [
  { key: 'home',    label: '首页',     icon: '🏠' },
  { key: 'mission', label: '任务岛',   icon: '🗂️' },
  { key: 'reading', label: '阅读珊瑚', icon: '📖' },
  { key: 'math',    label: '数学贝壳', icon: '🐚' },
  { key: 'english', label: '英语浪花', icon: '🌊' },
  { key: 'growth',  label: '成长宝藏', icon: '⭐' },
];

// ---- 任务卡片配置 ----
// 每个学科卡片: 名称 / 主数值 / 进度 / 主题色 / 图标
const TASK_CARDS = [
  {
    key: 'chinese', title: '语文朗读', value: '15分钟', progress: 45,
    theme: 'orange', icon: '📚', desc: '朗读小诗',
  },
  {
    key: 'math', title: '数学口算', value: '20题', progress: 65,
    theme: 'blue', icon: '🧮', desc: '口算闯关',
  },
  {
    key: 'english', title: '英语跟读', value: '10分钟', progress: 30,
    theme: 'green', icon: '🔤', desc: '跟读单词',
  },
];

export default function StudentHomePage() {
  const [activeNav, setActiveNav] = useState('math'); // 设计稿中「数学贝壳」为选中态

  // mock 交互: 任务卡「开始探索」点击提示（纯前端演示，后续接入练习流程）
  const handleStart = (card) => {
    message.success(`开始「${card.title}」探索之旅`);
  };

  return (
    <div className="shp-page">
      {/* 背景装饰: 海面波浪 */}
      <div className="shp-wave shp-wave-back" />
      <div className="shp-wave shp-wave-front" />
      {/* 左右云朵/浪花装饰 */}
      <div className="shp-cloud shp-cloud-left">☁️</div>
      <div className="shp-cloud shp-cloud-right">☁️</div>

      {/* ---- Header ---- */}
      <header className="shp-header">
        <div className="shp-brand">
          {/* 素材图: 薄荷绿花朵图标作为 logo */}
          <img src="/student-assets/logo-flower.webp" alt="logo" className="shp-logo" />
          <span className="shp-title">海底AI自习室</span>
        </div>
        <div className="shp-header-right">
          {/* 贝壳币 */}
          <div className="shp-coins">
            <span className="shp-coin-icon">🐚</span>
            <span>贝壳币 128</span>
            <span className="shp-coin-arrow">▾</span>
          </div>
          {/* 今日海浪值 */}
          <div className="shp-energy">
            <span className="shp-star">⭐</span>
            <span>今日海浪值 80%</span>
          </div>
        </div>
      </header>

      {/* ---- 主体: 左侧导航 + 内容区 ---- */}
      <div className="shp-body">
        {/* 左侧导航（圆角漂浮面板） */}
        <nav className="shp-nav">
          {NAV_ITEMS.map((item) => (
            <div
              key={item.key}
              className={`shp-nav-item ${activeNav === item.key ? 'active' : ''}`}
              onClick={() => setActiveNav(item.key)}
            >
              <span className="shp-nav-icon">{item.icon}</span>
              <span>{item.label}</span>
            </div>
          ))}
          {/* 返回管理端入口（演示用，便于来回切换） */}
          <a href="/" className="shp-back-link">← 返回管理端</a>
        </nav>

        {/* 内容区 */}
        <main className="shp-main">
          {/* 吉祥物 + 气泡 */}
          <div className="shp-mascot-row">
            <div className="shp-mascot">
              <span className="shp-dolphin">🐬</span>
            </div>
            <div className="shp-bubble">今天我们潜入3个小任务吧</div>
          </div>

          {/* 主工作台面板 */}
          <div className="shp-panel">
            {/* 三张任务卡片 */}
            <div className="shp-cards">
              {TASK_CARDS.map((card) => (
                <div key={card.key} className={`shp-card shp-card-${card.theme}`}>
                  <div className="shp-card-top">
                    <span className="shp-card-icon">{card.icon}</span>
                    {/* 空心圆（未勾选状态） */}
                    <span className="shp-card-dot" />
                  </div>
                  <div className="shp-card-title">{card.title}</div>
                  <div className="shp-card-value">{card.value}</div>
                  {/* 进度条 */}
                  <div className="shp-card-progress">
                    <div className={`shp-card-progress-bar ${card.theme}`} style={{ width: `${card.progress}%` }} />
                  </div>
                  <div className="shp-card-desc">{card.desc}</div>
                  <button className={`shp-card-btn ${card.theme}`} onClick={() => handleStart(card)}>
                    开始探索
                  </button>
                </div>
              ))}
            </div>

            {/* 专注信息 */}
            <div className="shp-focus">
              <span className="shp-focus-item">
                专注倒计时 <b className="shp-focus-num">25分钟</b>
              </span>
              <span className="shp-focus-divider">·</span>
              <span className="shp-focus-item">休息提醒 浮出水面</span>
            </div>
          </div>
        </main>
      </div>

      {/* ---- 右下角悬浮 AI 按钮 ---- */}
      <button className="shp-ai-btn" onClick={() => message.info('小鲸助手即将上线')}>
        <span className="shp-ai-line1">问一问</span>
        <span className="shp-ai-line2">问小鲸</span>
      </button>
    </div>
  );
}
