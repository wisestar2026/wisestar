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
import { Button, message } from 'antd';
import useStudentStore, { SUBJECTS, TITLES, PROFILE } from '../../stores/useStudentStore';
import { getMyStudentInfo, getStudentStats, getMyToday, getCheckin, doCheckin } from '../../api/student';
import { listMyStudentTasks, completeStudentTask } from '../../api/studentTask';
import { getMyStudySummary } from '../../api/studentStudy';
import IconTile from '../../components/common/IconTile';
import OnlineChestFloat from './OnlineChestFloat';
import './StudentHomePage.css';

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
  // 英语学科：学海研习卡片指向独立的英语学习中心（真 key 为学科 ID，如 1003；兼容 mock 'english'）
  const isEnglish = subject?.name === '英语' || subject?.key === 'english' || subject?.key === '1003';

  // 当前学员真实档案（学号/姓名/学校等，来自 GET /api/student/me；加载失败回退 mock）
  const [myInfo, setMyInfo] = useState(null);
  useEffect(() => {
    getMyStudentInfo().then((res) => setMyInfo(res?.data || null)).catch(() => setMyInfo(null));
  }, []);

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
  const [completingTaskId, setCompletingTaskId] = useState(null);

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

  // 真实学习统计：学海积分 = 累计练习得分；总学币 = 分科学币合计
  const totalPoints = stats?.totalPoints ?? 0;
  const coinsBySubject = stats?.coinsBySubject || [];
  const totalCoins = coinsBySubject.reduce((sum, c) => sum + c.coins, 0) + (stats?.manualCoins || 0);
  const coinOf = (name) => coinsBySubject.find((c) => c.subjectName === name)?.coins ?? 0;
  // 当前头衔（按真实学海积分自动晋升）
  const currentTitle = [...TITLES].reverse().find((t) => totalPoints >= t.need) || TITLES[0];
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
              <IconTile emoji="📋" tone="blue" size="sm" />
              <span className="sh-home-card-tag">我的档案</span>
            </div>
            <div className="sh-home-archive">
              <IconTile emoji={PROFILE.emoji} tone="sky" size="2xl" round />
              <div className="sh-home-archive-info">
                <div className="sh-home-archive-name">{displayName}</div>
                <div className="sh-home-title">{currentTitle.emoji} {currentTitle.name}</div>
                <div className="sh-home-archive-meta">
                  {myInfo?.studentNo && <span>🎓 学号 <b>{myInfo.studentNo}</b></span>}
                  <span>⭐ 学海积分 <b>{totalPoints}</b></span>
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
          onClick={() => navigate(isEnglish ? '/student/english' : '/student/study')}
        >
            <div className="sh-home-card-head">
              <IconTile emoji={subject.icon} tone={subject.theme} size="sm" />
              <span className="sh-home-card-tag">学海研习</span>
            </div>
          <div className="sh-home-study">
            <IconTile emoji="📖" tone="blue" size="2xl" />
            <div className="sh-home-study-text">
              <div className="sh-home-study-title">开启{subject.name}研习</div>
              <div className="sh-home-study-desc">潜入「{(subject.chapters?.[0]?.name) || '今日研习'}」的知识海洋</div>
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
              <IconTile emoji="🎁" tone="orange" size="sm" />
              <span className="sh-home-card-tag">荣誉商城</span>
            </div>
            <div className="sh-home-mall">
              <IconTile emoji="🐚" tone="gold" size="2xl" round />
              <div>
                <div className="sh-home-mall-num">{totalCoins}</div>
                <div className="sh-home-mall-label">本学期可兑换总学习币</div>
              </div>
            </div>
            {/* 各科学习币明细（hover 展示） */}
            <div className="sh-home-mall-detail">
              {SUBJECTS.map((s) => (
                <span key={s.key} className={`sh-home-mall-sub sh-home-mall-sub-${s.theme}`}>
                  {s.icon} {s.name} {coinOf(s.name)}
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
          <div className="sh-home-section-title"><IconTile emoji="🌊" tone="sky" size="xs" /> 今日学习数据总览</div>
          <div className="sh-home-data-grid">
            <div className="sh-home-data-item">
              <IconTile emoji="⏱️" tone="sky" size="lg" />
              <div className="sh-home-data-num">{(stats?.today?.minutes) ?? 0}<small>分钟</small></div>
              <div className="sh-home-data-label">今日学习时长</div>
            </div>
            <div className="sh-home-data-item">
              <IconTile emoji="🧩" tone="green" size="lg" />
              <div className="sh-home-data-num">{(stats?.today?.questionCount) ?? 0}<small>题</small></div>
              <div className="sh-home-data-label">今日答题</div>
            </div>
            {!pureMode && (
              <>
                <div className="sh-home-data-item">
                  <IconTile emoji="⭐" tone="gold" size="lg" />
                  <div className="sh-home-data-num">+{(stats?.today?.points) ?? 0}<small>积分</small></div>
                  <div className="sh-home-data-label">今日获得积分</div>
                </div>
                <div className="sh-home-data-item">
                  <IconTile emoji="🐚" tone="orange" size="lg" />
                  <div className="sh-home-data-num">+{(stats?.today?.coins) ?? 0}<small>币</small></div>
                  <div className="sh-home-data-label">今日获得学习币</div>
                </div>
              </>
            )}
          </div>
        </div>

        {/* 积分获取引导（主动预习/练习/试炼/订正错题/攻克薄弱） */}
        {!pureMode && todayView?.guides?.length > 0 && (
          <div className="sll-card sh-home-todo">
            <div className="sh-home-section-title"><IconTile emoji="⭐" tone="gold" size="xs" /> 今日积分获取引导</div>
            {todayView.guides.map((g) => (
              <div
                key={g.actionType}
                className="sh-home-todo-item"
                style={{ cursor: 'pointer' }}
                onClick={() => navigate(guideRoute(g.target))}
              >
                <IconTile emoji={g.done ? '✅' : '➕'} tone={g.done ? 'green' : 'slate'} size="xs" round />
                <span className="sh-home-todo-label">
                  {g.label}
                  <span className="sh-home-todo-desc"> · 积分+{g.points} 币+{g.coins}</span>
                </span>
              </div>
            ))}
          </div>
        )}

        {/* 每日签到（固定学习币，每自然日一次） */}
        {!pureMode && (
          <div className="sll-card sh-home-todo">
            <div className="sh-home-section-title"><IconTile emoji="📅" tone="gold" size="xs" /> 每日签到</div>
            <div className="sh-home-todo-item" style={{ cursor: 'default' }}>
              <IconTile emoji={checkin?.checkedToday ? '✅' : '➕'} tone={checkin?.checkedToday ? 'green' : 'slate'} size="xs" round />
              <span className="sh-home-todo-label">
                {checkin?.checkedToday
                  ? '今日已签到，明天再来'
                  : `签到即可领取学习币 +${checkin?.coins ?? 10}`}
              </span>
              {!checkin?.checkedToday && (
                <Button type="primary" size="small" loading={checkinLoading} onClick={handleCheckin}>签到</Button>
              )}
            </div>
          </div>
        )}

        {/* 今日任务（学管师/老师当日发布） */}
        <div className="sll-card sh-home-todo">
          <div className="sh-home-section-title"><IconTile emoji="🗓️" tone="teal" size="xs" /> 今日任务</div>
          {tasks.length === 0 && (
            <div className="sh-home-todo-item"><span className="sh-home-todo-label" style={{ color: '#90a4ae' }}>今日暂无任务，自由研习吧</span></div>
          )}
          {tasks.map((t) => (
            <div key={t.id} className="sh-home-todo-item" style={{ cursor: 'default' }}>
              <IconTile emoji={t.status === 'completed' ? '✅' : '📋'} tone={t.status === 'completed' ? 'green' : 'blue'} size="xs" round />
              <span
                className="sh-home-todo-label"
                style={t.status === 'completed' ? { color: '#90a4ae', textDecoration: 'line-through' } : undefined}
              >
                {t.taskContent || '今日任务'}
              </span>
              {t.status !== 'completed' && (
                <Button size="small" loading={completingTaskId === t.id} onClick={() => handleCompleteTask(t)}>完成</Button>
              )}
            </div>
          ))}
        </div>

        {/* 今日学习总结（学习时长累计满 1 小时后自动生成） */}
        {summary?.content && (
          <div className="sll-card sh-home-todo">
            <div className="sh-home-section-title"><IconTile emoji="📝" tone="blue" size="xs" /> 今日学习总结</div>
            <div className="sh-home-todo-item">
              <IconTile emoji="✨" tone="gold" size="xs" round />
              <span className="sh-home-todo-label" style={{ whiteSpace: 'pre-wrap' }}>{summary.content}</span>
            </div>
          </div>
        )}
      </div>

      {/* 在线时长宝箱悬浮窗（纯净学习模式隐藏） */}
      {!pureMode && <OnlineChestFloat />}
    </div>
  );
}
