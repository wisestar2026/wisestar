/**
 * StudentLayout.jsx - 学生端公共布局（海底AI自习室 · 学海智习系统 V2.0 顶部通栏）
 *
 * 结构（学海智习系统 V2.0 全局顶部通栏规范 + 海底童趣视觉）:
 *   +--------------------------------------------------------------+
 *   | [LOGO] 海底AI自习室     [🐚学习币] [⭐总积分] [🔔] [⚙] 头像  |
 *   | [语文][数学][英语] 教材版本 ▾        [返回管理端]             |
 *   +--------------------------------------------------------------+
 *   |                        <Outlet 页面内容区>                    |
 *   +--------------------------------------------------------------+
 *   | 学海积分全学科永久累计 | 学习币单科限产、多科通兑、学期清零     |
 *
 * 关键规则:
 *   - 学科 Tab 由后台绑定，学生端仅展示，无权增删改（前端 mock 展示）
 *   - 切换学科 → 版本下拉自动刷新（每科默认版本）
 *   - 纯净学习模式: 隐藏积分/学习币/头衔等全部激励模块（DOM 移除）
 *
 * 被谁引用: App.jsx 路由表（/student/* 子路由父级）
 * 依赖: react-router-dom(Outlet/useNavigate/useLocation)、antd(Select/Dropdown/Switch)、
 *       useStudentStore、../student/student.css
 */

import { useEffect, useRef } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { Select, Dropdown, Switch, message } from 'antd';
import {
  SettingOutlined, BellOutlined, LogoutOutlined, ArrowLeftOutlined,
} from '@ant-design/icons';
import useStudentStore, { SUBJECTS, TITLES, PROFILE } from '../../stores/useStudentStore';
import useUserStore from '../../stores/useUserStore';
import { uploadActivity } from '../../api/student';
import './student.css';

// 底部导航配置
const TABS = [
  { path: '/student', icon: '🏠', label: '首页' },
  { path: '/student/study', icon: '📖', label: '学习' },
  { path: '/student/wrong', icon: '📕', label: '错题本' },
  { path: '/student/profile', icon: '👤', label: '个人中心' },
];

export default function StudentLayout() {
  const {
    activeSubject, version, pureMode,
    setSubject, setVersion, togglePureMode, fetchPermissions, fetchStudySubjects,
    getVisibleSubjects, getVisibleVersions,
  } = useStudentStore();

  // 挂载时加载学员有效权限与真实学科（订单授予范围）
  useEffect(() => {
    fetchPermissions();
    fetchStudySubjects();
  }, [fetchPermissions, fetchStudySubjects]);

  // 实时位置上报：路由变化时上报当前页面（节流：同页 5 秒内不重复）
  const lastReport = useRef({ page: '', ts: 0 });
  useEffect(() => {
    const page = location.pathname;
    const now = Date.now();
    if (lastReport.current.page === page && now - lastReport.current.ts < 5000) {
      return;
    }
    lastReport.current = { page, ts: now };
    uploadActivity({ page }).catch(() => {});
  }, [location.pathname]);

  // 按订单权限过滤后的可见学科
  const visibleSubjects = getVisibleSubjects();
  const hasPermission = visibleSubjects.length > 0;
  const navigate = useNavigate();
  const location = useLocation();
  const { logout } = useUserStore();

  // 退出登录（学员端）：清登录态并返回学员端登录页
  const handleStudentLogout = () => {
    logout();
    navigate('/student-login');
  };


  // 当前头衔（按学海积分自动晋升，无降级）
  const currentTitle = [...TITLES].reverse().find((t) => PROFILE.points >= t.need) || TITLES[0];

  // 学习币合计（多科合并）
  const totalCoins = SUBJECTS.reduce((sum, s) => sum + s.coins, 0);

  // 设置菜单项（纯净学习模式开关 / 返回管理端）
  const settingsItems = [
    {
      key: 'pure',
      label: (
        <span className="sll-set-row">
          <span>纯净学习模式</span>
          <Switch size="small" checked={pureMode} onChange={togglePureMode} />
        </span>
      ),
    },
    { type: 'divider' },
    {
      key: 'logout',
      label: '退出登录',
      icon: <LogoutOutlined />,
      onClick: handleStudentLogout,
    },
  ];

  return (
    <div className="sll-page">
      {/* 背景装饰: 海面波浪 + 云朵 */}
      <div className="sll-wave sll-wave-back" />
      <div className="sll-wave sll-wave-front" />
      <div className="sll-cloud sll-cloud-left">☁️</div>
      <div className="sll-cloud sll-cloud-right">☁️</div>

      {/* ---- 顶部通栏（磨砂悬浮） ---- */}
      <header className="sll-header">
        {/* 左侧: LOGO + 标题 */}
        <div className="sll-left">
          <div className="sll-brand">
            <img src="/student-assets/logo-flower.webp" alt="logo" className="sll-logo" />
            <span className="sll-title">海底AI自习室</span>
          </div>
          {/* 学科 Tab 胶囊栏（后台绑定，仅展示切换） */}
          <div className="sll-tabs">
            {hasPermission ? visibleSubjects.map((s) => (
              <button
                key={s.key}
                className={`sll-tab sll-tab-${s.theme} ${activeSubject === s.key ? 'active' : ''}`}
                onClick={() => setSubject(s.key)}
              >
                <span className="sll-tab-icon">{s.icon}</span>
                <span>{s.name}</span>
              </button>
            )) : (
              <span className="sll-no-perm">暂无可访问学科，请联系管理员开通</span>
            )}
            {/* 教材版本下拉（按权限过滤；跟随当前学科，记忆上次选择） */}
            <Select
              className="sll-version"
              size="small"
              value={version}
              onChange={setVersion}
              options={getVisibleVersions(activeSubject).map((v) => ({ label: v, value: v }))}
              popupMatchSelectWidth={false}
            />
          </div>
        </div>

        {/* 右侧: 头像/头衔/积分/消息/设置（纯净模式隐藏激励模块） */}
        <div className="sll-right">
          {/* 学习币（学期消费币，多科合并） */}
          {!pureMode && (
            <button className="sll-badge" onClick={() => message.info(`本学期可兑换学习币 ${totalCoins} 枚`)}>
              <span>🐚</span>
              <b>{totalCoins}</b>
              <span className="sll-badge-label">学习币</span>
            </button>
          )}
          {/* 学海积分（终身成长值） */}
          {!pureMode && (
            <button className="sll-badge sll-badge-points" onClick={() => navigate('/student/profile')}>
              <span>⭐</span>
              <b>{PROFILE.points}</b>
              <span className="sll-badge-label">学海积分</span>
            </button>
          )}
          {/* 消息 */}
          <button className="sll-icon-btn" onClick={() => message.info('暂无新消息')}>
            <BellOutlined />
          </button>
          {/* 设置（含纯净模式开关） */}
          <Dropdown menu={{ items: settingsItems }} trigger={['click']} placement="bottomRight">
            <button className="sll-icon-btn"><SettingOutlined /></button>
          </Dropdown>
          {/* 头像 + 头衔 */}
          <button className="sll-user" onClick={() => navigate('/student/profile')}>
            <span className="sll-avatar">🐬</span>
            {!pureMode && <span className="sll-title-chip">{currentTitle.emoji} {currentTitle.name}</span>}
          </button>
          {/* 退出登录（学员账号无管理端权限） */}
          <button className="sll-back-btn" onClick={handleStudentLogout}>
            <ArrowLeftOutlined /> 退出登录
          </button>
        </div>
      </header>

      {/* ---- 页面内容区（淡入过渡） ---- */}
      <main className="sll-main">
        <Outlet />
      </main>

      {/* ---- 底部导航栏（首页/学习/错题本/个人中心） ---- */}
      <nav className="sll-tabbar">
        {TABS.map((t) => {
          const active = location.pathname === t.path
            || (t.path === '/student' && location.pathname.startsWith('/student/knowledge'));
          return (
            <button
              key={t.path}
              className={`sll-tabbar-item ${active ? 'active' : ''}`}
              onClick={() => navigate(t.path)}
            >
              <span className="sll-tabbar-icon">{t.icon}</span>
              <span className="sll-tabbar-label">{t.label}</span>
            </button>
          );
        })}
      </nav>

      {/* ---- 底部常驻提示小字 ---- */}
      <footer className="sll-footer">
        学海积分全学科永久累计 · 学习币单科限产、多科通兑、学期清零 · 头衔永不开级、证书终身保留
      </footer>

      {/* ---- 右下角悬浮 AI 按钮 ---- */}
      <button className="shp-ai-btn" onClick={() => message.info('小鲸向导即将上线')}>
        <span className="shp-ai-line1">问一问</span>
        <span className="shp-ai-line2">问小鲸</span>
      </button>
    </div>
  );
}
