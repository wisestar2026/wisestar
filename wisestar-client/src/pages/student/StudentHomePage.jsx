/**
 * StudentHomePage.jsx - 学生端首页（海洋智学 · 航海驾驶舱布局）
 *
 * 布局（参考设计稿「海洋智学」主界面）:
 *   +--------------------------------------------------------------+
 *   |  商城   |          ⛵ 整张船图（仅展示，不可点击）            |   ← 同一容器
 *   |  错题本 |       「开始学习」按钮浮于船图下方，是唯一入口       |
 *   |  今日任务|                                                    |
 *   +--------------------------------------------------------------+
 *        ↑ 右侧「今日数据/积分引导/签到/总结」四张悬浮卡片：默认只露出图标贴在右缘，
 *          点击图标才展开为悬浮窗，不占布局
 *        ↑ 容器浮于「虚化背景」之上（.slh-root::before backdrop-filter），与背景不在同一平面
 *
 * 功能保持不变（仅重排布局）:
 *   - 船图仅展示；仅「开始学习」按钮进入学习（英语学科指向英语学习中心）
 *   - 荣誉商城 / 错题本 / 今日任务（任务改为右侧抽屉展示，含完成结算）
 *   - 今日学习数据总览 / 今日积分获取引导 / 每日签到 / 今日学习总结 → 右缘图标悬浮卡（默认只露图标）
 *   - 在线时长宝箱 → 可拖动悬浮条（位置本地记忆）
 *
 * 个人信息（学号/证书/头衔/积分）展示在顶部状态栏（StudentLayout），不再占用首页容器
 *
 * 纯净学习模式: 隐藏商城入口、积分/学习币、积分引导、签到等激励模块
 *
 * 被谁引用: App.jsx 路由表（/student 子路由 index）
 * 依赖: react-router-dom(useNavigate)、useStudentStore、./StudentHomePage.css
 */

import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Drawer, message } from 'antd';
import useStudentStore, { SUBJECTS } from '../../stores/useStudentStore';
import { getStudentStats, getMyToday, getCheckin, doCheckin } from '../../api/student';
import { listMyStudentTasks, completeStudentTask } from '../../api/studentTask';
import { getMyStudySummary } from '../../api/studentStudy';
import IconTile from '../../components/common/IconTile';
import OnlineChestFloat from './OnlineChestFloat';
import './StudentHomePage.css';

/**
 * 右侧可折叠悬浮卡片（标题栏点击展开/收起，默认收起）。
 */
function FloatCard({ title, icon, tone = 'sky', defaultOpen = false, headExtra, children, className = '' }) {
  const [open, setOpen] = useState(defaultOpen);
  return (
    <div className={`slh-float-card ${open ? 'is-open' : ''} ${className}`}>
      <button
        type="button"
        className="slh-float-head"
        onClick={() => setOpen((o) => !o)}
        title={title}
        aria-label={title}
        aria-expanded={open}
      >
        <IconTile emoji={icon} tone={tone} size="xs" />
        <span className="slh-float-title">{title}</span>
        {headExtra && <span className="slh-float-extra">{headExtra}</span>}
        <span className="slh-float-caret">{open ? '▾' : '▸'}</span>
      </button>
      {open && <div className="slh-float-body">{children}</div>}
    </div>
  );
}

export default function StudentHomePage() {
  const navigate = useNavigate();
  const { pureMode } = useStudentStore();
  const activeSubject = useStudentStore((s) => s.activeSubject);
  const getVisibleSubjects = useStudentStore((s) => s.getVisibleSubjects);
  // 学科按当前选择（真实学科优先，随下拉切换变化）
  const visibleSubjects = getVisibleSubjects();
  const subject = visibleSubjects.find((s) => s.key === activeSubject)
    || SUBJECTS.find((s) => s.key === activeSubject)
    || SUBJECTS[1];
  // 英语学科：开始学习指向独立的英语学习中心（真 key 为学科 ID，如 1003；兼容 mock 'english'）
  const isEnglish = subject?.name === '英语' || subject?.key === 'english' || subject?.key === '1003';
  const studyPath = isEnglish ? '/student/english' : '/student/study';

  // 真实学习统计（基于练习记录聚合；未加载时为 0）
  const [stats, setStats] = useState(null);
  useEffect(() => {
    getStudentStats().then((res) => setStats(res?.data || null)).catch(() => setStats(null));
  }, []);

  // 今日总览 + 积分获取引导（统一账本）
  const [todayView, setTodayView] = useState(null);
  useEffect(() => {
    getMyToday().then((res) => setTodayView(res?.data || null)).catch(() => setTodayView(null));
  }, []);
  // 引导跳转目标 → 学员端路由
  const guideRoute = (target) => (target === 'wrong' || target === 'weak' ? '/student/wrong' : '/student/study');

  // 今日任务（学管师/老师当天发布，按发布时间升序）
  const [tasks, setTasks] = useState([]);
  useEffect(() => {
    listMyStudentTasks().then((res) => setTasks(res?.data || [])).catch(() => setTasks([]));
  }, []);
  const [taskOpen, setTaskOpen] = useState(false);
  const [completingTaskId, setCompletingTaskId] = useState(null);
  const pendingTaskCount = tasks.filter((t) => t.status !== 'completed').length;

  // 每日签到状态（固定学习币，每自然日一次）
  const [checkin, setCheckin] = useState(null);
  const [checkinLoading, setCheckinLoading] = useState(false);
  useEffect(() => {
    getCheckin().then((res) => setCheckin(res?.data || null)).catch(() => setCheckin(null));
  }, []);

  // 领取签到奖励
  const handleCheckin = async () => {
    setCheckinLoading(true);
    try {
      const res = await doCheckin();
      const data = res?.data || null;
      setCheckin(data);
      if (data?.firstTime) message.success(data.message || `签到成功，学习币 +${data.coins}`);
      else message.info(data?.message || '今日已签到');
    } catch (e) {
      message.error(e?.message || '签到失败，请稍后重试');
    } finally {
      setCheckinLoading(false);
    }
  };

  // 完成任务并结算学习币
  const handleCompleteTask = async (task) => {
    if (!task || task.status === 'completed') return;
    setCompletingTaskId(task.id);
    try {
      const res = await completeStudentTask(task.id);
      const data = res?.data || null;
      setTasks((prev) => prev.map((t) => (t.id === task.id ? { ...t, status: 'completed' } : t)));
      if (data?.firstTime) message.success(data.message || `任务完成，学习币 +${data.coins}`);
      else message.info(data?.message || '任务已完成');
    } catch (e) {
      message.error(e?.message || '操作失败，请稍后重试');
    } finally {
      setCompletingTaskId(null);
    }
  };

  // 今日学习总结（会话累计满 60 分钟后由系统生成）
  const [summary, setSummary] = useState(null);
  useEffect(() => {
    getMyStudySummary().then((res) => setSummary(res?.data || null)).catch(() => setSummary(null));
  }, []);

  // 真实学习统计：总学币 = 分科学币合计
  const coinsBySubject = stats?.coinsBySubject || [];
  const totalCoins = coinsBySubject.reduce((sum, c) => sum + c.coins, 0) + (stats?.manualCoins || 0);

  return (
    <div className="sll-page-enter slh-root">
      {/* ---- 主容器：左栏入口 + 整张船图按钮（同一容器，无分隔） ---- */}
      <div className="slh-panel">
        <aside className="slh-side">
          {!pureMode && (
            <button type="button" className="slh-side-btn slh-side-mall" onClick={() => navigate('/student/mall')}>
              <span className="slh-side-emoji">🎁</span>
              <span className="slh-side-main">商城</span>
              <span className="slh-side-sub">学习币 {totalCoins}</span>
            </button>
          )}
          <button type="button" className="slh-side-btn slh-side-wrong" onClick={() => navigate('/student/wrong')}>
            <span className="slh-side-emoji">📕</span>
            <span className="slh-side-main">错题本</span>
            <span className="slh-side-sub">订正涨积分</span>
          </button>
          <button type="button" className="slh-side-btn slh-side-task" onClick={() => setTaskOpen(true)}>
            <span className="slh-side-emoji">🗓️</span>
            <span className="slh-side-main">今日任务</span>
            <span className="slh-side-sub">
              {pendingTaskCount > 0 ? `${pendingTaskCount} 项待完成` : '全部完成'}
            </span>
          </button>
          <button type="button" className="slh-side-btn slh-side-detect" onClick={() => navigate('/student/detect')}>
            <span className="slh-side-emoji">🎯</span>
            <span className="slh-side-main">知识点检测</span>
            <span className="slh-side-sub">测薄弱点</span>
          </button>
        </aside>

        <div className="slh-stage">
          {/* 船图仅作展示，不可点击 */}
          <div className="slh-ship-wrap" aria-hidden="true">
            <img className="slh-ship" src="/student-assets/ship-start.webp" alt="" />
          </div>
          {/* 仅「开始学习」按钮可进入学习界面 */}
          <button
            type="button"
            className="slh-start"
            onClick={() => navigate(studyPath)}
            aria-label={`开始学习 · ${subject.name}`}
          >
            <span className="slh-start-btn">
              <span className="slh-start-play" aria-hidden="true">▶</span>
              <span className="slh-start-text">开始学习</span>
            </span>
          </button>
        </div>
      </div>

      {/* ---- 右侧悬浮卡片：绝对定位，浮在容器之上，默认收起、不占布局 ---- */}
      <aside className="slh-float">
        <FloatCard title="今日学习数据总览" icon="🌊" tone="sky">
          <div className="slh-data-grid">
            <div className="slh-data-item">
              <IconTile emoji="⏱️" tone="sky" size="sm" />
              <div>
                <div className="slh-data-num">{(stats?.today?.minutes) ?? 0}<small>分钟</small></div>
                <div className="slh-data-label">今日学习时长</div>
              </div>
            </div>
            <div className="slh-data-item">
              <IconTile emoji="🧩" tone="green" size="sm" />
              <div>
                <div className="slh-data-num">{(stats?.today?.questionCount) ?? 0}<small>题</small></div>
                <div className="slh-data-label">今日答题</div>
              </div>
            </div>
            {!pureMode && (
              <>
                <div className="slh-data-item">
                  <IconTile emoji="⭐" tone="gold" size="sm" />
                  <div>
                    <div className="slh-data-num">+{(stats?.today?.points) ?? 0}<small>积分</small></div>
                    <div className="slh-data-label">今日获得积分</div>
                  </div>
                </div>
                <div className="slh-data-item">
                  <IconTile emoji="🐚" tone="orange" size="sm" />
                  <div>
                    <div className="slh-data-num">+{(stats?.today?.coins) ?? 0}<small>币</small></div>
                    <div className="slh-data-label">今日获得学习币</div>
                  </div>
                </div>
              </>
            )}
          </div>
        </FloatCard>

        {/* 积分获取引导（主动预习/练习/试炼/订正错题/攻克薄弱） */}
        {!pureMode && todayView?.guides?.length > 0 && (
          <FloatCard title="今日积分获取引导" icon="⭐" tone="gold">
            {todayView.guides.map((g) => (
              <div
                key={g.actionType}
                className="slh-guide-item"
                style={{ cursor: 'pointer' }}
                onClick={() => navigate(guideRoute(g.target))}
              >
                <IconTile emoji={g.done ? '✅' : '➕'} tone={g.done ? 'green' : 'slate'} size="xs" round />
                <span className="slh-guide-label">
                  {g.label}
                  <span className="slh-guide-desc"> · 积分+{g.points} 币+{g.coins}</span>
                </span>
              </div>
            ))}
          </FloatCard>
        )}

        {/* 每日签到（固定学习币，每自然日一次） */}
        {!pureMode && (
          <FloatCard title="每日签到" icon="📅" tone="gold">
            <div className="slh-guide-item">
              <IconTile emoji={checkin?.checkedToday ? '✅' : '➕'} tone={checkin?.checkedToday ? 'green' : 'slate'} size="xs" round />
              <span className="slh-guide-label">
                {checkin?.checkedToday
                  ? '今日已签到，明天再来'
                  : `签到即可领取学习币 +${checkin?.coins ?? 10}`}
              </span>
              {!checkin?.checkedToday && (
                <Button type="primary" size="small" loading={checkinLoading} onClick={handleCheckin}>签到</Button>
              )}
            </div>
          </FloatCard>
        )}

        {/* 今日学习总结（学习时长累计满 1 小时后自动生成） */}
        {summary?.content && (
          <FloatCard title="今日学习总结" icon="📝" tone="blue">
            <div className="slh-summary-text">{summary.content}</div>
          </FloatCard>
        )}
      </aside>

      {/* 在线时长宝箱：时长进度条 + 三只小宝箱（达成点击领取；纯净学习模式隐藏）
          置于主容器内，拖动位置以主容器为基准，随容器居中/缩窄一起移动 */}
      {!pureMode && <OnlineChestFloat />}

      {/* ---- 今日任务抽屉 ---- */}
      <Drawer
        title="今日任务"
        placement="right"
        width={380}
        open={taskOpen}
        onClose={() => setTaskOpen(false)}
      >
        {tasks.length === 0 && (
          <div className="slh-task-empty">今日暂无任务，自由研习吧</div>
        )}
        {tasks.map((t) => (
          <div key={t.id} className="slh-task-item">
            <IconTile emoji={t.status === 'completed' ? '✅' : '📋'} tone={t.status === 'completed' ? 'green' : 'blue'} size="xs" round />
            <span className={`slh-task-label ${t.status === 'completed' ? 'is-done' : ''}`}>
              {t.taskContent || '今日任务'}
            </span>
            {t.status !== 'completed' && (
              <Button size="small" loading={completingTaskId === t.id} onClick={() => handleCompleteTask(t)}>完成</Button>
            )}
          </div>
        ))}
      </Drawer>
    </div>
  );
}
