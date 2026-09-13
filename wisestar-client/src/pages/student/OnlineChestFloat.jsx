/**
 * OnlineChestFloat.jsx - 学员端首页「在线时长宝箱」悬浮窗
 *
 * 功能:
 *   - 复用学习心跳取宝箱状态（当日在线分钟 + 30/60/120 三档）
 *   - 挂载时取一次，之后每 60 秒刷新，页面重新可见时补刷
 *   - 出现新的可领取档位时自动展开；点击「开启」领取学习币并即时刷新
 *
 * 被谁引用: StudentHomePage（纯净学习模式下不挂载）
 * 依赖: ../../api/studentStudy、useStudentStore、IconTile
 */

import { useCallback, useEffect, useRef, useState } from 'react';
import { sendStudyHeartbeat, claimOnlineChest } from '../../api/studentStudy';
import useStudentStore from '../../stores/useStudentStore';
import IconTile from '../../components/common/IconTile';
import './OnlineChestFloat.css';

const TIER_EMOJI = { 30: '🥉', 60: '🥈', 120: '🏆' };

export default function OnlineChestFloat() {
  const activeSubject = useStudentStore((s) => s.activeSubject);
  const realSubjects = useStudentStore((s) => s.studyContent?.subjects);
  const [open, setOpen] = useState(false);
  const [chest, setChest] = useState(null);
  const [claiming, setClaiming] = useState(0);
  const [message, setMessage] = useState('');
  const seenClaimable = useRef(new Set());
  const messageTimer = useRef(null);

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

  // 出现新的可领取档位时自动展开一次
  useEffect(() => {
    if (!chest) return;
    const fresh = (chest.chests || []).filter(
      (c) => c.state === 'claimable' && !seenClaimable.current.has(c.tierMinutes),
    );
    if (fresh.length > 0) {
      fresh.forEach((c) => seenClaimable.current.add(c.tierMinutes));
      setOpen(true);
    }
  }, [chest]);

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
  const hasClaimable = chests.some((c) => c.state === 'claimable');

  return (
    <div className={`ocf-root${open ? ' ocf-open' : ''}`}>
      {open && (
        <div className="ocf-panel">
          <div className="ocf-head">
            <IconTile emoji="💎" tone="gold" size="sm" round />
            <span className="ocf-title">在线时长宝箱</span>
            <span className="ocf-online">今日在线 {minutes} 分钟</span>
            <button type="button" className="ocf-close" onClick={() => setOpen(false)}>×</button>
          </div>
          {message && <div className="ocf-msg">{message}</div>}
          <div className="ocf-list">
            {chests.map((c) => {
              const remain = Math.max(0, c.tierMinutes - minutes);
              const locked = c.state === 'locked';
              return (
                <div key={c.tierMinutes} className={`ocf-item ocf-${c.state}`}>
                  <span className={`ocf-tier${locked ? ' ocf-tier-locked' : ''}`}>
                    {TIER_EMOJI[c.tierMinutes] || '🎁'}
                  </span>
                  <div className="ocf-info">
                    <div className="ocf-tier-name">在线 {c.tierMinutes} 分钟</div>
                    <div className="ocf-tier-desc">
                      {c.state === 'claimed' && '今日已领取'}
                      {c.state === 'claimable' && `可领取 · 学习币 +${c.coins}`}
                      {locked && `还差 ${remain} 分钟 · 学习币 +${c.coins}`}
                    </div>
                  </div>
                  {c.state === 'claimable' && (
                    <button
                      type="button"
                      className="ocf-grab"
                      disabled={claiming === c.tierMinutes}
                      onClick={() => handleClaim(c.tierMinutes)}
                    >
                      {claiming === c.tierMinutes ? '开启中' : '开启'}
                    </button>
                  )}
                  {c.state === 'claimed' && <span className="ocf-done">✓</span>}
                </div>
              );
            })}
          </div>
        </div>
      )}
      <button
        type="button"
        className={`ocf-fab${hasClaimable ? ' ocf-fab-hot' : ''}`}
        onClick={() => setOpen((v) => !v)}
      >
        <span className="ocf-fab-emoji">🎁</span>
        <span className="ocf-fab-min">{minutes}<small>分</small></span>
        {hasClaimable && <span className="ocf-fab-dot" />}
      </button>
    </div>
  );
}
