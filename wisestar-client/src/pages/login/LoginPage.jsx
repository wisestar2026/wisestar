/**
 * LoginPage.jsx - 登录页面
 *
 * 功能:
 *   1. 收集用户名和密码
 *   2. 调用 useUserStore.login() 执行登录（内部会 RSA 加密密码）
 *   3. 登录成功后跳转到首页 /
 *
 * 页面布局:
 *   - 全屏渐变背景（紫色渐变）
 *   - 居中白色卡片（400px 宽）
 *   - 表单包含：用户名输入框、密码输入框、登录按钮、注册链接
 *
 * 被谁引用: App.jsx（公开路由 /login，无需登录）
 *
 * 登录流程:
 *   LoginPage → useUserStore.login() → api/user.js login() → POST /api/public/login
 *           ↓ 成功（后端 Set-Cookie 写入 sk-token）
 *           navigate('/') → AuthGuard 检查通过 → MainLayout → 仪表盘
 *
 * 安全说明:
 *   - 密码在 api/user.js login() 中用 RSA 公钥加密后传输，不落明文
 *   - 登录态由后端 sk-token Cookie 维持，刷新页面后由 fetchCurrentUser 恢复
 *
 * 已知说明（非本次修改）:
 *   - 底部"立即注册"链接指向 /register，但 App.jsx 未注册该路由
 */

import { useState } from 'react';
import { Form, Input, Button, Card, Typography, message, Space } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { useNavigate, Link } from 'react-router-dom';
import useUserStore from '../../stores/useUserStore';

const { Title, Text } = Typography;

export default function LoginPage() {
  // 登录按钮加载状态（防止重复提交）
  const [loading, setLoading] = useState(false);

  // Zustand 中的 login 方法
  const { login } = useUserStore();

  // React Router 导航
  const navigate = useNavigate();

  // ============================================================
  // 表单提交处理
  // ============================================================
  // values = { username: "admin", password: "123456" }
  const onFinish = async (values) => {
    setLoading(true);
    try {
      // 调用 Zustand 的 login，内部会执行 RSA 加密流程
      await login(values.username, values.password);
      message.success('登录成功');
      // 按用户类型跳转：学员（学号登录，userType=Student）→ 学员端；系统用户 → 管理端
      const userType = useUserStore.getState().user?.userType;
      navigate(userType === 'Student' ? '/student' : '/');
    } catch (error) {
      // 错误已在 request.js 拦截器中处理，这里做兜底显示
      message.error(error?.response?.data?.message || '登录失败，请检查用户名和密码');
    } finally {
      setLoading(false);
    }
  };

  return (
    // 全屏容器：居中布局 + 渐变背景
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      }}
    >
      {/* 登录卡片 */}
      <Card style={{ width: 400, boxShadow: '0 8px 24px rgba(0,0,0,0.15)' }}>
        {/* ---- 标题区 ---- */}
        <div style={{ textAlign: 'center', marginBottom: 32 }}>
          <Title level={2} style={{ marginBottom: 8 }}>wisestar</Title>
          <Text type="secondary">Better Survey System</Text>
        </div>

        {/* ---- 登录表单 ---- */}
        <Form name="login" onFinish={onFinish} size="large">
          {/* 用户名 */}
          <Form.Item
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input prefix={<UserOutlined />} placeholder="用户名" />
          </Form.Item>

          {/* 密码 */}
          <Form.Item
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="密码" />
          </Form.Item>

          {/* 登录按钮（loading 状态防止重复点击） */}
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block>
              登录
            </Button>
          </Form.Item>

          {/* 注册链接 */}
          <div style={{ textAlign: 'center' }}>
            <Space>
              <Text type="secondary">还没有账号？</Text>
              <Link to="/register">立即注册</Link>
            </Space>
          </div>
        </Form>
      </Card>
    </div>
  );
}
