/**
 * OnlineChestFloat.jsx - 学员端首页「在线时长宝箱」步骤条
 *
 * 展示形态（参照步骤条样式）:
 *   - 一条横向轨道连接三只菱形宝箱节点（30分钟 / 1小时 / 2小时），节点下方带文字
 *   - 轨道按在线分钟填充；节点内嵌宝箱，达成点亮、点一下领取学习币（每档每日一次、幂等）
 *   - 整条悬浮可拖动（位置记忆到 localStorage），拖动中不会误触领取
 *
 * 数据: 复用学习心跳（当日在线分钟 + 三档状态），挂载取一次、之后每 60 秒刷新，
 *       页面重新可见时补刷。
 *
 * 被谁引用: StudentHomePage（右侧悬浮列；纯净学习模式不挂载）
 * 依赖: ../../api/studentStudy、useStudentStore
 */

import { useCallback, useEffect, useRef, useState } from 'react';
import { sendStudyHeartbeat, claimOnlineChest } from '../../api/studentStudy';
import useStudentStore from '../../stores/useStudentStore';
import './OnlineChestFloat.css';

/** 拖动位置本地记忆键 */
const POS_KEY = 'wisestar.onlineChest.pos';

// 三个档位节点：x 为在容器内的水平位置（均分），tier 为分钟数
const NODES = [
  { tier: 30, x: 1 / 6, label: '30分钟' },
  { tier: 60, x: 1 / 2, label: '1小时' },
  { tier: 120, x: 5 / 6, label: '2小时' },
];
const X_START = NODES[0].x;
const X_END = NODES[NODES.length - 1].x;
// 分钟 → 节点位置的分段线性刻度：(0, start)、(30, start)、(60, mid)、(120, end)
const SCALE = [[0, X_START], [30, X_START], [60, NODES[1].x], [120, X_END]];

/** 在线分钟在轨道上的填充比例（0..1）。 */
function progressFrac(minutes) {
  if (minutes <= 0) return 0;
  if (minutes >= 120) return 1;
  for (let i = 1; i < SCALE.length; i += 1) {
    const [t0, x0] = SCALE[i - 1];
    const [t1, x1] = SCALE[i];
    if (minutes <= t1) {
      const x = x0 + ((x1 - x0) * (minutes - t0)) / (t1 - t0);
      return (x - X_START) / (X_END - X_START);
    }
  }
  return 1;
}

/** 卡通宝箱图标：locked 灰、claimable/claimed 亮金。 */
function ChestIcon({ state = 'locked', size = 30 }) {
  const lit = state !== 'locked';
  const wood1 = lit ? '#f0b24e' : '#c6d0da';
  const wood2 = lit ? '#c47c26' : '#9aa8b6';
  const gold = lit ? '#ffd23f' : '#d6dee6';
  const lock = lit ? '#ffe07a' : '#c2ccd6';
  return (
    <svg width={size} height={size} viewBox="0 0 48 48" aria-hidden="true">
      <defs>
        <linearGradient id={`ocf-wood-${state}`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor={wood1} />
          <stop offset="1" stopColor={wood2} />
        </linearGradient>
      </defs>
      <rect x="5" y="23" width="38" height="19" rx="5" fill={`url(#ocf-wood-${state})`} />
      <path d="M5 23 a19 11 0 0 1 38 0 z" fill={`url(#ocf-wood-${state})`} />
      <rect x="5" y="21" width="38" height="3.4" rx="1.7" fill={gold} />
      <rect x="21" y="20" width="6" height="22" rx="1.5" fill={gold} />
      <rect x="18" y="27" width="12" height="10.5" rx="2.8" fill={lock} />
      <rect x="22.6" y="30.6" width="2.8" height="4.2" rx="1.2" fill="#8a6a1a" />
      {lit && <path d="M9 24 a15 8 0 0 1 22 -6" stroke="rgba(255,255,255,0.7)" strokeWidth="2" fill="none" strokeLinecap="round" />}
    </svg>
  );
}

export default function OnlineChestFloat() {
  const activeSubject = useStudentStore((s) => s.activeSubject);
  const realSubjects = useStudentStore((s) => s.studyContent?.subjects);
  const [chest, setChest] = useState(null);
  const [claiming, setClaiming] = useState(0);
  const [message, setMessage] = useState('');
  const messageTimer = useRef(null);

  // ---------- 拖动（整条可拖，位置本地记忆） ----------
  const wrapRef = useRef(null);
  const dragRef = useRef(null);
  const movedRef = useRef(false);
  const posRef = useRef(null);
  const [pos, setPos] = useState(null);

  const clampPos = useCallback((x, y) => {
    const el = wrapRef.current;
    const root = el?.parentElement;
    const w = el?.offsetWidth || 296;
    const h = el?.offsetHeight || 112;
    const bw = root?.clientWidth || window.innerWidth;
    const bh = root?.clientHeight || window.innerHeight;
    const maxX = Math.max(8, bw - w - 8);
    const maxY = Math.max(8, bh - h - 8);
    return { x: Math.min(Math.max(8, x), maxX), y: Math.min(Math.max(8, y), maxY) };
  }, []);

  useEffect(() => {
    try {
      const raw = localStorage.getItem(POS_KEY);
      if (!raw) return;
      const p = JSON.parse(raw);
      if (p && Number.isFinite(p.x) && Number.isFinite(p.y)) {
        const safe = clampPos(p.x, p.y);
        posRef.current = safe;
        setPos(safe);
      }
    } catch { /* 忽略非法记忆值 */ }
  }, [clampPos]);

  useEffect(() => {
    const onResize = () => {
      if (!posRef.current) return;
      const next = clampPos(posRef.current.x, posRef.current.y);
      posRef.current = next;
      setPos(next);
    };
    window.addEventListener('resize', onResize);
    return () => window.removeEventListener('resize', onResize);
  }, [clampPos]);

  const onPointerDown = (e) => {
    if (e.button != null && e.button !== 0) return;
    const el = wrapRef.current;
    if (!el) return;
    const rect = el.getBoundingClientRect();
    dragRef.current = { dx: e.clientX - rect.left, dy: e.clientY - rect.top, sx: e.clientX, sy: e.clientY };
    movedRef.current = false;
    try { el.setPointerCapture(e.pointerId); } catch { /* 部分环境不支持捕获 */ }
  };

  const onPointerMove = (e) => {
    const d = dragRef.current;
    if (!d) return;
    if (!movedRef.current && Math.hypot(e.clientX - d.sx, e.clientY - d.sy) < 4) return;
    movedRef.current = true;
    // 位置相对首页主容器（.slh-root）计算，需扣除容器视口左/上偏移
    const root = wrapRef.current?.parentElement;
    const rootRect = root ? root.getBoundingClientRect() : { left: 0, top: 0 };
    const next = clampPos(e.clientX - d.dx - rootRect.left, e.clientY - d.dy - rootRect.top);
    posRef.current = next;
    setPos(next);
  };

  const onPointerUp = () => {
    if (!dragRef.current) return;
    dragRef.current = null;
    if (movedRef.current && posRef.current) {
      try { localStorage.setItem(POS_KEY, JSON.stringify(posRef.current)); } catch { /* 忽略写入失败 */ }
    }
  };

  const onPointerCancel = () => { dragRef.current = null; };

  // 拖动过则拦截本次点击，避免松开时误触宝箱领取
  const onClickCapture = (e) => {
    if (movedRef.current) {
      e.stopPropagation();
      e.preventDefault();
    }
  };

  const floatStyle = pos ? { left: pos.x, top: pos.y } : { right: 24, bottom: 12 };

  // 仅当当前学科是真实学科ID时才上报/领取，避免把 mock key 当作学科写入账本
  const realSubjectId = Array.isArray(realSubjects) && realSubjects.some((s) => s.id === activeSubject)
    ? activeSubject
    : undefined;

  const refresh = useCallback(() => {
    sendStudyHeartbeat(realSubjectId ? { subjectId: realSubjectId } : {})
      .then((res) => setChest(res?.data?.onlineChest || null))
      .catch(() => {});
  }, [realSubjectId]);

  useEffect(() => {
    refresh();
    const timer = setInterval(refresh, 60 * 1000);
    const onVisible = () => { if (!document.hidden) refresh(); };
    document.addEventListener('visibilitychange', onVisible);
    return () => {
      clearInterval(timer);
      document.removeEventListener('visibilitychange', onVisible);
    };
  }, [refresh]);

  useEffect(() => () => {
    if (messageTimer.current) clearTimeout(messageTimer.current);
  }, []);

  const handleClaim = (tier) => {
    setClaiming(tier);
    claimOnlineChest(realSubjectId ? { tier, subjectId: realSubjectId } : { tier })
      .then((res) => {
        const data = res?.data;
        if (data?.onlineChest) setChest(data.onlineChest);
        if (data?.message) {
          setMessage(data.message);
          if (messageTimer.current) clearTimeout(messageTimer.current);
          messageTimer.current = setTimeout(() => setMessage(''), 3000);
        }
      })
      .catch(() => {})
      .finally(() => setClaiming(0));
  };

  const minutes = chest?.onlineMinutes ?? 0;
  const chests = chest?.chests || [];
  const fillPct = Math.max(0, Math.min(100, progressFrac(minutes) * 100));
  const hasClaimable = chests.some((c) => c.state === 'claimable');

  return (
    <div
      className="ocf-float"
      ref={wrapRef}
      style={floatStyle}
      onPointerDown={onPointerDown}
      onPointerMove={onPointerMove}
      onPointerUp={onPointerUp}
      onPointerCancel={onPointerCancel}
      onClickCapture={onClickCapture}
      title="按住拖动可移动位置"
    >
      <div className="ocf-steps">
        {/* 连接轨道 */}
        <div className="ocf-rail">
          <div className={`ocf-fill${hasClaimable ? ' ocf-fill-hot' : ''}`} style={{ width: `${fillPct}%` }} />
        </div>

        {/* 三只菱形宝箱节点 */}
        {NODES.map((n) => {
          const c = chests.find((x) => x.tierMinutes === n.tier) || { tierMinutes: n.tier, state: 'locked', coins: 0 };
          const remain = Math.max(0, n.tier - minutes);
          return (
            <button
              key={n.tier}
              type="button"
              className={`ocf-node ocf-${c.state}`}
              style={{ left: `${n.x * 100}%` }}
              disabled={c.state !== 'claimable' || claiming === n.tier}
              onClick={() => handleClaim(n.tier)}
              title={
                c.state === 'claimable'
                  ? `${n.label} 已达成，点一下领取学习币 +${c.coins}`
                  : (c.state === 'locked' ? `${n.label}：还差 ${remain} 分钟` : `${n.label} 今日已领取`)
              }
            >
              <span className="ocf-diamond">
                <span className="ocf-diamond-inner">
                  <ChestIcon state={c.state} size={30} />
                </span>
              </span>
              {c.state === 'claimable' && <span className="ocf-badge ocf-badge-gain">+{c.coins}</span>}
              {c.state === 'claimed' && <span className="ocf-badge ocf-badge-check">✓</span>}
              <span className="ocf-label">{n.label}</span>
            </button>
          );
        })}

        {message && <div className="ocf-toast">{message}</div>}
      </div>
    </div>
  );
}
