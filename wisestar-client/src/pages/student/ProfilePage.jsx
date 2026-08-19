/**
 * ProfilePage.jsx - 我的档案荣誉页面（学海智习系统 V2.0 · 高颜值荣誉墙）
 *
 * 布局:
 *   +--------------------------------------------------------------+
 *   | 个人信息大卡: 头像 / 最高头衔 / 总学海积分 / 证书进度           |
 *   | 证书网格陈列墙: 5 级证书（解锁彩色 / 未解锁置灰，点击弹窗预览） |
 *   | 成长数据统计: 累计知识点 / 达标章节 / 薄弱板块                  |
 *   +--------------------------------------------------------------+
 *
 * 核心规则: 证书解锁永久保留、头衔永不降级（学海积分永久累计）
 *
 * 被谁引用: App.jsx（/student/profile）、首页「我的档案卡」、通栏头像/积分
 * 依赖: react-router-dom(useNavigate)、antd(Modal)、useStudentStore、./ProfilePage.css
 */

import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Modal, Form, Input, Button, message } from 'antd';
import useStudentStore, { TITLES, PROFILE, SUBJECTS } from '../../stores/useStudentStore';
import { changePassword } from '../../api/student';
import './ProfilePage.css';

// 成长数据统计（mock）
const GROWTH_STATS = [
  { label: '累计掌握知识点', value: 24, unit: '个', color: '#2196f3', pct: 48 },
  { label: '达标章节',       value: 8,  unit: '章', color: '#4caf50', pct: 53 },
  { label: '薄弱板块',       value: 3,  unit: '处', color: '#ff7043', pct: 20 },
];

export default function ProfilePage() {
  const [pwModalOpen, setPwModalOpen] = useState(false);
  const [pwSaving, setPwSaving] = useState(false);
  const [pwForm] = Form.useForm();

  // ---- 修改密码 ----
  const openPwModal = () => {
    pwForm.resetFields();
    setPwModalOpen(true);
  };
  const handlePwSave = () => {
    pwForm.validateFields().then((values) => {
      if (values.newPassword !== values.confirmPassword) {
        message.warning('两次输入的新密码不一致');
        return;
      }
      setPwSaving(true);
      changePassword({ oldPassword: values.oldPassword, newPassword: values.newPassword })
        .then(() => {
          message.success('密码修改成功');
          setPwModalOpen(false);
        })
        .finally(() => setPwSaving(false));
    });
  };
  const navigate = useNavigate();
  const { pureMode } = useStudentStore();
  const [previewCert, setPreviewCert] = useState(null); // 正在预览的证书

  // 各证书解锁状态（按当前学海积分判定）
  const certs = TITLES.map((t) => ({
    ...t,
    unlocked: PROFILE.points >= t.need,
  }));
  const unlockedCount = certs.filter((c) => c.unlocked).length;

  return (
    <div className="sll-page-enter profile-page">
      {/* ---- 顶部: 个人信息大卡 ---- */}
      <div className="sll-card profile-info">
        <div className="profile-avatar">{PROFILE.emoji}</div>
        <div className="profile-info-main">
          <div className="profile-name">{PROFILE.name}</div>
          <div className="profile-title-chip">🏆 最高头衔 · {PROFILE.title}</div>
          <div className="profile-meta">
            <span className="profile-meta-item">
              <b>⭐</b> 学海积分 <b className="profile-num">{PROFILE.points}</b>
            </span>
            <span className="profile-meta-item">
              <b>🏅</b> 已解锁证书 <b className="profile-num">{unlockedCount}/{PROFILE.certTotal}</b>
            </span>
            <span className="profile-meta-item">
              <b>📚</b> 绑定学科 <b className="profile-num">{PROFILE.subjects}</b> 门
            </span>
          </div>
        </div>
        <div className="profile-actions">
          <button className="profile-study-btn" onClick={() => navigate('/student/study')}>
            继续研习 ›
          </button>
          <Button size="small" onClick={openPwModal}>修改密码</Button>
        </div>
      </div>

      {/* ---- 修改密码弹窗 ---- */}
      <Modal title="修改登录密码" open={pwModalOpen} onOk={handlePwSave} onCancel={() => setPwModalOpen(false)}
        okText="确认修改" cancelText="取消" confirmLoading={pwSaving} destroyOnClose>
        <Form form={pwForm} layout="vertical" style={{ marginTop: 8 }}>
          <Form.Item name="oldPassword" label="原密码" rules={[{ required: true, message: '请输入原密码' }]}>
            <Input.Password placeholder="输入当前登录密码" />
          </Form.Item>
          <Form.Item name="newPassword" label="新密码" rules={[{ required: true, message: '请输入新密码' }, { min: 6, message: '新密码至少 6 位' }]}>
            <Input.Password placeholder="至少 6 位" />
          </Form.Item>
          <Form.Item name="confirmPassword" label="确认新密码" rules={[{ required: true, message: '请再次输入新密码' }]}>
            <Input.Password placeholder="再次输入新密码" />
          </Form.Item>
        </Form>
      </Modal>

      {/* ---- 中部: 证书网格陈列墙 ---- */}
      <div className="profile-section-title">🏅 证书荣誉墙（解锁永久保留）</div>
      <div className="profile-certs">
        {certs.map((c) => (
          <div
            key={c.key}
            className={`sll-card sll-card-hover profile-cert ${c.unlocked ? 'unlocked' : 'locked'}`}
            onClick={() => c.unlocked && setPreviewCert(c)}
          >
            <div className="profile-cert-emoji">{c.unlocked ? c.emoji : '🔒'}</div>
            <div className="profile-cert-name">{c.name}</div>
            <div className="profile-cert-desc">{c.cert}</div>
            <div className="profile-cert-require">
              {c.unlocked
                ? '已解锁'
                : `达到 ${c.need} 学海积分解锁`}
            </div>
            {c.unlocked && <div className="profile-cert-stamp">✓ 已颁发</div>}
          </div>
        ))}
      </div>

      {/* ---- 底部: 成长数据统计 ---- */}
      <div className="profile-section-title">📊 成长数据统计</div>
      <div className="sll-card profile-growth">
        {GROWTH_STATS.map((g) => (
          <div key={g.label} className="profile-growth-item">
            <div className="profile-growth-label">{g.label}</div>
            <div className="profile-growth-bar">
              <div
                className="profile-growth-fill"
                style={{ width: `${g.pct}%`, background: `linear-gradient(90deg, ${g.color}aa, ${g.color})` }}
              />
            </div>
            <div className="profile-growth-value" style={{ color: g.color }}>
              {g.value}<small>{g.unit}</small>
            </div>
          </div>
        ))}
      </div>

      {/* ---- 证书预览弹窗 ---- */}
      <Modal
        open={!!previewCert}
        footer={null}
        onCancel={() => setPreviewCert(null)}
        width={400}
        centered
        destroyOnClose
      >
        {previewCert && (
          <div className="profile-cert-modal">
            <div className="profile-cert-modal-card">
              <div className="profile-cert-modal-title">🎓 荣誉证书</div>
              <div className="profile-cert-modal-emoji">{previewCert.emoji}</div>
              <div className="profile-cert-modal-name">{previewCert.cert}</div>
              <div className="profile-cert-modal-to">
                兹授予 <b>{PROFILE.name}</b> 同学
              </div>
              <div className="profile-cert-modal-body">
                在学海智习系统学习中累计获得 {PROFILE.points} 学海积分，达成「{previewCert.name}」荣誉，特发此证。
              </div>
              <div className="profile-cert-modal-footer">
                学海智习系统 · 荣誉颁发委员会
              </div>
            </div>
            <div className="profile-cert-modal-actions">
              <button className="profile-cert-action" onClick={() => setPreviewCert(null)}>下载</button>
              <button className="profile-cert-action" onClick={() => setPreviewCert(null)}>保存</button>
              <button className="profile-cert-action primary" onClick={() => setPreviewCert(null)}>打印</button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
