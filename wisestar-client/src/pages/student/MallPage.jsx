/**
 * MallPage.jsx - 荣誉商城页面（学海智习系统 V2.0 · 年轻化商品卡布局）
 *
 * 布局:
 *   +--------------------------------------------------------------+
 *   | 顶部数据区: 大字「本学期可兑换总学习币」+ 折叠各科明细 + 提示  |
 *   | 商品网格: [🦈 商品卡][🧽 商品卡][...] 可兑换亮色/不足置灰      |
 *   +--------------------------------------------------------------+
 *
 * 兑换数据流向（前端 mock）:
 *   汇总本学期所有绑定学科币总和判断是否充足 → 充足自动多科合并扣款
 *   → 顶部轻柔提示「兑换成功」1.5s 自动消失
 *
 * 被谁引用: App.jsx（/student/mall）、首页「荣誉商城卡」
 * 依赖: react-router-dom(useNavigate)、antd(Collapse)、useStudentStore、./MallPage.css
 */

import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Collapse } from 'antd';
import useStudentStore, { SUBJECTS, GOODS } from '../../stores/useStudentStore';
import './MallPage.css';

export default function MallPage() {
  const navigate = useNavigate();
  const { pureMode } = useStudentStore();

  // 各科剩余学习币（前端 state，兑换后扣减演示）
  const [subjectCoins, setSubjectCoins] = useState(() =>
    Object.fromEntries(SUBJECTS.map((s) => [s.key, s.coins])),
  );
  const [reward, setReward] = useState(null);
  const [exchanged, setExchanged] = useState({}); // 已兑换商品记录

  const totalCoins = SUBJECTS.reduce((sum, s) => sum + subjectCoins[s.key], 0);

  // 兑换: 汇总本学期所有学科币判断充足 → 多科合并扣款
  const handleExchange = (good) => {
    if (totalCoins < good.price) return;
    let remain = good.price;
    // 从各科余额依次扣减（多科合并扣款演示）
    setSubjectCoins((prev) => {
      const next = { ...prev };
      for (const s of SUBJECTS) {
        if (remain <= 0) break;
        const take = Math.min(next[s.key], remain);
        next[s.key] -= take;
        remain -= take;
      }
      return next;
    });
    setExchanged((prev) => ({ ...prev, [good.id]: true }));
    setReward(`🎉 兑换成功：${good.name}（已扣 ${good.price} 学习币）`);
    setTimeout(() => setReward(null), 1600);
  };

  // 纯净学习模式: 商城为激励模块，直接隐藏
  if (pureMode) {
    return (
      <div className="sll-page-enter mall-pure">
        <div className="sll-card mall-pure-card">
          <div className="mall-pure-icon">🔒</div>
          <div className="mall-pure-title">纯净学习模式已开启</div>
          <div className="mall-pure-desc">迎检模式下荣誉商城已隐藏，可安心专注学习。</div>
          <button className="knowledge-back" onClick={() => navigate('/student/study')}>
            返回学海研习
          </button>
        </div>
      </div>
    );
  }

  // 各科明细折叠面板
  const collapseItems = [
    {
      key: 'detail',
      label: <span className="mall-collapse-label">📋 各学科剩余学习币明细</span>,
      children: (
        <div className="mall-detail-list">
          {SUBJECTS.map((s) => (
            <div key={s.key} className="mall-detail-row">
              <span className="mall-detail-sub">
                <span className={`mall-detail-icon mall-detail-icon-${s.theme}`}>{s.icon}</span>
                {s.name}
              </span>
              <div className="mall-detail-bar">
                <div
                  className={`mall-detail-bar-inner ${s.theme}`}
                  style={{ width: `${Math.min(100, (subjectCoins[s.key] / 3000) * 100)}%` }}
                />
              </div>
              <span className="mall-detail-num">{subjectCoins[s.key]}<small>/3000</small></span>
            </div>
          ))}
        </div>
      ),
    },
  ];

  return (
    <div className="sll-page-enter mall-page">
      {reward && <div className="sll-reward">{reward}</div>}

      {/* ---- 顶部数据区 ---- */}
      <div className="sll-card mall-top">
        <div className="mall-top-coin">🐚</div>
        <div className="mall-top-info">
          <div className="mall-top-label">本学期可兑换总学习币</div>
          <div className="mall-top-num">{totalCoins}</div>
          <div className="mall-top-tip">
            单科单学期上限 3000 · 同一学期多科学习币可合并兑换 · 学期结束自动清零
          </div>
        </div>
        <div className="mall-top-collapse">
          <Collapse ghost items={collapseItems} expandIconPosition="end" />
        </div>
      </div>

      {/* ---- 商品网格 ---- */}
      <div className="mall-section-title">🎁 荣誉商品</div>
      <div className="mall-grid">
        {GOODS.map((g) => {
          const affordable = totalCoins >= g.price;
          return (
            <div
              key={g.id}
              className={`sll-card sll-card-hover mall-goods ${exchanged[g.id] ? 'exchanged' : ''}`}
            >
              <div className="mall-goods-emoji">{g.emoji}</div>
              <div className="mall-goods-name">{g.name}</div>
              <div className="mall-goods-desc">{g.desc}</div>
              <div className="mall-goods-price">🐚 {g.price} 学习币</div>
              <button
                className={`mall-goods-btn ${affordable && !exchanged[g.id] ? 'ok' : 'no'}`}
                disabled={!affordable || exchanged[g.id]}
                onClick={() => handleExchange(g)}
              >
                {exchanged[g.id] ? '✓ 已兑换' : affordable ? '立即兑换' : '学习币不足'}
              </button>
            </div>
          );
        })}
      </div>
    </div>
  );
}
