/**
 * StudentLoginPage.jsx - 学员端登录页（海洋智学）
 *
 * 功能:
 *   1. 学员账号（学号）+ 密码登录，登录成功后直接进入学员端（/student）
 *   2. 后台账号（userType 非 Student）在此登录时提示使用管理端登录入口
 *   3. 海洋主题背景（浅蓝渐变 + 波浪），登录卡片居中
 *
 * URL: /student-login（公开路由，无需登录）
 * 被谁引用: App.jsx 公开路由；管理端登录页「学员登录入口」链接；学员端退出登录跳转
 *
 * 数据流:
 *   StudentLoginPage → useUserStore.login() → api/user.js login()（RSA 加密）
 *   → POST /api/public/login → /api/currentUser → user.userType
 *   → Student → /student；SysUser → 提示并跳 /login
 */

import { useState } from 'react';
import { Form, Input, Button, Typography, message } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { useNavigate, Link } from 'react-router-dom';
import useUserStore from '../../stores/useUserStore';
import './studentLogin.css';

const { Title, Text } = Typography;

export default function StudentLoginPage() {
  const [loading, setLoading] = useState(false);
  const { login } = useUserStore();
  const navigate = useNavigate();

  const onFinish = async (values) => {
    setLoading(true);
    try {
      await login(values.username, values.password);
      const userType = useUserStore.getState().user?.userType;
      if (userType === 'Student') {
        message.success('登录成功');
        navigate('/student');
      } else {
        message.warning('该账号为后台账号，请使用管理端登录入口');
        navigate('/login');
      }
    } catch (error) {
      message.error(error?.response?.data?.message || '登录失败，请检查账号和密码');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="sl-page">
      {/* 海洋波浪装饰 */}
      <div className="sl-bubble sl-bubble-1">🐬</div>
      <div className="sl-bubble sl-bubble-2">🐠</div>
      <div className="sl-bubble sl-bubble-3">🪸</div>
      <div className="sl-wave sl-wave-back" />
      <div className="sl-wave sl-wave-front" />

      {/* 居中登录卡片 */}
      <div className="sl-card">
        <div className="sl-brand">
          <div className="sl-brand-logo">🌊</div>
          <Title level={2} className="sl-title">海洋智学</Title>
          <Text className="sl-subtitle">海底 AI 自习室 · 学员端</Text>
        </div>

        <Form name="student-login" onFinish={onFinish} size="large">
          <Form.Item name="username" rules={[{ required: true, message: '请输入学号' }]}>
            <Input prefix={<UserOutlined />} placeholder="学号（8 位数字）" />
          </Form.Item>
          <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="密码" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block className="sl-submit">
              登录
            </Button>
          </Form.Item>
        </Form>

        <div className="sl-footer">
          <span>学员账号由管理员开通</span>
          <Link to="/login">管理端登录入口 ›</Link>
        </div>
      </div>
    </div>
  );
}
