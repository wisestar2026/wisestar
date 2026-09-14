/**
 * MallPage.jsx - 荣誉商城页面（学海智习系统 V2.0 · 年轻化商品卡布局）
 *
 * 布局:
 *   +--------------------------------------------------------------+
 *   | 顶部数据区: 大字「本学期可兑换总学习币」+ 折叠各科明细 + 提示  |
 *   | 商品网格: [🦈 商品卡][🧽 商品卡][...] 可兑换亮色/不足置灰      |
 *   | 我的兑换记录: 商品 / 核销码 / 状态 / 申请时间                 |
 *   +--------------------------------------------------------------+
 *
 * 兑换数据流向（真实后端）:
 *   点击「立即兑换」→ POST /mall/order/create（后端校验学币并暂时扣除、下发随机 6 位核销码）
 *   → 弹窗展示核销码 → 刷新学币余额与兑换记录 → 待老师端核销后订单完成
 *
 * 被谁引用: App.jsx（/student/mall）、首页「荣誉商城卡」
 * 依赖: react-router-dom(useNavigate)、antd(Collapse/Modal/Tag/message)、useStudentStore、
 *       api/mall（商品/兑换订单）、api/student（学币）、./MallPage.css
 */

import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Collapse, Modal, Tag, message } from 'antd';
import useStudentStore from '../../stores/useStudentStore';
import { listGoods, createMallOrder, listMyMallOrders } from '../../api/mall';
import { getMyCoins } from '../../api/student';
import IconTile from '../../components/common/IconTile';
import './MallPage.css';

/** 单科单学期学习币上限（与后端 StudentRewardConstants.SUBJECT_COIN_LIMIT 保持一致） */
const COIN_LIMIT = 10000;

export default function MallPage() {
  const navigate = useNavigate();
  const { pureMode } = useStudentStore();

  const [goods, setGoods] = useState([]);
  const [coins, setCoins] = useState(0);
  const [coinList, setCoinList] = useState([]);
  const [orders, setOrders] = useState([]);
  const [successOrder, setSuccessOrder] = useState(null);
  const [purchasing, setPurchasing] = useState(null);

  const loadMall = () => {
    listGoods(1).then((res) => setGoods(res?.data || [])).catch(() => setGoods([]));
    getMyCoins().then((res) => {
      const d = res?.data || {};
      setCoins(d.total || 0);
      setCoinList(d.list || []);
    }).catch(() => {});
    listMyMallOrders().then((res) => setOrders(res?.data || [])).catch(() => setOrders([]));
  };

  useEffect(() => {
    loadMall();
  }, []);

  // 兑换: 后端校验余额、暂时扣除学币并返回核销码
  const handleExchange = (good) => {
    if (purchasing) return;
    if (coins < (good.points || 0)) {
      message.warning('学币不足，暂时无法兑换');
      return;
    }
    setPurchasing(good.id);
    createMallOrder({ goodsId: good.id })
      .then((res) => {
        setSuccessOrder(res?.data || null);
        loadMall();
      })
      .catch(() => {})
      .finally(() => setPurchasing(null));
  };

  // 纯净学习模式: 商城为激励模块，直接隐藏
  if (pureMode) {
    return (
      <div className="sll-page-enter mall-pure">
        <div className="sll-card mall-pure-card">
          <IconTile emoji="🔒" tone="slate" size="2xl" />
          <div className="mall-pure-title">纯净学习模式已开启</div>
          <div className="mall-pure-desc">迎检模式下荣誉商城已隐藏，可安心专注学习。</div>
          <button className="knowledge-back" onClick={() => navigate('/student/study')}>
            返回学海研习
          </button>
        </div>
      </div>
    );
  }

  // 各科明细折叠面板（真实学币）
  const collapseItems = [
    {
      key: 'detail',
      label: <span className="mall-collapse-label"><IconTile emoji="📋" tone="blue" size="xs" /> 各学科剩余学习币明细</span>,
      children: (
        <div className="mall-detail-list">
          {coinList.length === 0 && (
            <div className="mall-detail-empty">本学期还没有学习币，快去预习、练习积累吧</div>
          )}
          {coinList.map((s) => (
            <div key={s.subjectId} className="mall-detail-row">
              <span className="mall-detail-sub">{s.subjectName}</span>
              <div className="mall-detail-bar">
                <div
                  className="mall-detail-bar-inner blue"
                  style={{ width: `${Math.min(100, ((s.coins || 0) / COIN_LIMIT) * 100)}%` }}
                />
              </div>
              <span className="mall-detail-num">{s.coins}<small>/{COIN_LIMIT}</small></span>
            </div>
          ))}
        </div>
      ),
    },
  ];

  return (
    <div className="sll-page-enter mall-page">
      {/* ---- 顶部数据区 ---- */}
      <div className="sll-card mall-top">
        <IconTile emoji="🐚" tone="gold" size="2xl" style={{ '--it-size': '76px' }} />
        <div className="mall-top-info">
          <div className="mall-top-label">本学期可兑换总学习币</div>
          <div className="mall-top-num">{coins}</div>
          <div className="mall-top-tip">
            单科单学期上限 {COIN_LIMIT} · 同一学期多科学习币可合并兑换 · 学期结束自动清零
          </div>
        </div>
        <div className="mall-top-collapse">
          <Collapse ghost items={collapseItems} expandIconPosition="end" />
        </div>
      </div>

      {/* ---- 商品网格 ---- */}
      <div className="mall-section-title"><IconTile emoji="🎁" tone="pink" size="sm" /> 荣誉商品</div>
      <div className="mall-grid">
        {goods.length === 0 && (
          <div className="mall-goods-desc" style={{ textAlign: 'center', padding: 24 }}>暂无上架商品，敬请期待</div>
        )}
        {goods.map((g) => {
          const affordable = coins >= (g.points || 0);
          return (
            <div key={g.id} className="sll-card sll-card-hover mall-goods">
              <div className="mall-goods-emoji">
                {g.imageUrl ? <img src={g.imageUrl} alt={g.name} style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: 12 }} /> : '🎁'}
              </div>
              <div className="mall-goods-name">{g.name}</div>
              <div className="mall-goods-desc">{g.description}</div>
              <div className="mall-goods-price">⭐ {g.points} 学币</div>
              <button
                className={`mall-goods-btn ${affordable ? 'ok' : 'no'}`}
                disabled={!affordable || purchasing === g.id}
                onClick={() => handleExchange(g)}
              >
                {affordable ? (purchasing === g.id ? '兑换中…' : '立即兑换') : '学币不足'}
              </button>
            </div>
          );
        })}
      </div>

      {/* ---- 我的兑换记录 ---- */}
      <div className="mall-section-title"><IconTile emoji="🧾" tone="slate" size="sm" /> 我的兑换记录</div>
      <div className="mall-orders">
        {orders.length === 0 && (
          <div className="mall-goods-desc" style={{ textAlign: 'center', padding: 16 }}>还没有兑换记录</div>
        )}
        {orders.map((o) => (
          <div key={o.id} className="sll-card mall-order-row">
            <div className="mall-order-info">
              <div className="mall-order-name">{o.goodsName}</div>
              <div className="mall-order-time">{o.createAt}</div>
            </div>
            <div className="mall-order-code">
              核销码 <b>{o.verifyCode}</b>
            </div>
            <div className="mall-order-coins">-{o.coins} 学币</div>
            {o.status === 1
              ? <Tag color="green">已核销</Tag>
              : <Tag color="orange">待核销</Tag>}
          </div>
        ))}
      </div>

      {/* ---- 兑换成功弹窗（展示核销码） ---- */}
      <Modal
        open={!!successOrder}
        onCancel={() => setSuccessOrder(null)}
        footer={null}
        centered
        title="兑换成功"
      >
        {successOrder && (
          <div className="mall-success">
            <div className="mall-success-goods">{successOrder.goodsName}</div>
            <div className="mall-success-label">核销码</div>
            <div className="mall-success-code">{successOrder.verifyCode}</div>
            <div className="mall-success-tip">
              已暂时扣除 {successOrder.coins} 学币，请向老师出示核销码完成核销
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
